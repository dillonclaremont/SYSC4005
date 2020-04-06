package entities;

import globals.ComponentName;
import globals.EntityState;
import globals.EntityType;

import java.util.*;

public class Inspector extends Entity{
    private final int MAX_BUFFER_SIZE = 999;                                            //Maximum possible buffer size for a workbench, used when finding the workbench with the minimum current buffer value, this is ridiculously large compared to whats expected
    private final int SEED = 9;                                                         //Seed value for random number generator, useful for testing, by default not used.
    private HashMap<ComponentName, ArrayList<WorkBench>> componentToWorkbenchMapping;   //A Mapping of components to workbenches ex. {C1: [W1, W2, W3], C2: [W2] ... }
    private HashMap<ComponentName, Queue<Double>> componentServiceTimes;                //A mapping of service time queues to components ex. {C1: [60, 120, 240], C2: [30, 45, ... }
    private HashMap<WorkBench, Integer> workbenchPriorities;                            //A mapping of priorities to workbenches ex:. {W1: 1, W2: 2, W3: 3}
    private ComponentName currentComponentNameUnderInspection;                          //Current component under inspection
    private Random randomNumberGenerator;                                               //Random number generator


    public Inspector (String name) {
        super(name);
        this.entityType = EntityType.INSPECTOR;
        this.componentToWorkbenchMapping = new HashMap<ComponentName, ArrayList<WorkBench>>();
        this.componentServiceTimes = new HashMap<ComponentName, Queue<Double>>();
        this.workbenchPriorities = new HashMap<WorkBench, Integer>();
        this.randomNumberGenerator = new Random();
    }

    /**
     * Registration method to initialize an Inspector. Maps a list of service times for a specific component.
     *
     * @param componentName
     * @param serviceTimes
     */
    public void registerComponentServiceTimes(ComponentName componentName, ArrayList<Double> serviceTimes){
        Queue<Double> serviceTimeQueue = new LinkedList<Double>();
        for (Double serviceTime : serviceTimes){
            serviceTimeQueue.add(serviceTime);
        }
        this.componentServiceTimes.put(componentName, serviceTimeQueue);
    }

    /**
     * Registration method to map components to Workbenches.
     *
     * @param componentName
     * @param workBench
     */
    public void registerComponentForWorkbench(ComponentName componentName, WorkBench workBench){
        if (this.componentToWorkbenchMapping.containsKey(componentName)){
            ArrayList<WorkBench> workBenches = this.componentToWorkbenchMapping.get(componentName);
            workBenches.add(workBench);
        } else {
            ArrayList<WorkBench> workBenches = new ArrayList<WorkBench>();
            workBenches.add(workBench);
            this.componentToWorkbenchMapping.put(componentName, workBenches);
        }
    }

    /**
     * Registration method to map priorites to maps. Lower integer values represent higher priorities.
     *
     * @param workBench
     * @param priority
     */
    public void registerWorkbenchPriority(WorkBench workBench, Integer priority){
        this.workbenchPriorities.put(workBench, priority);
    }

    /**
     * This method updates the clock by 'interval'. State is updated accordingly for the Inspector.
     * Starts by updating the state timer.
     * Based on the current state of the Inspector the following behaviour is exhibited:
     *  - If the inspector is currently inspecting a component, decrement service time counter
     *  - If the inspector has just finished inspecting a component, attempt to put on a workbench
     *  - If the inspector is blocked (has finished inspecting a component but there was no available workbench) attempts to place on workbench again
     *  - If none of the above is true, must be the first case and simply gets the next component to inspect.
     *
     * @param interval
     */
    @Override
    public void clockUpdate(Double interval){
        Double serviceTimeRemaining = this.getServiceTimeRemaining();
        EntityState currentState = this.getState();
        this.incrementStateTimer(currentState, interval);
        this.clock += interval;

        if (currentState == EntityState.ACTIVE && (serviceTimeRemaining <= 0)){
            this.attemptToPutComponentOnWorkbench();
        } else if (currentState == EntityState.ACTIVE && (serviceTimeRemaining > 0)){
            this.decrementServiceTimeRemaining(interval);
        } else if (currentState == EntityState.BLOCKED){
            this.attemptToPutComponentOnWorkbench();
        } else if (currentState == EntityState.DONE) {
            //TODO: Not sure if any action is required here.
        } else {
            //This should only be entered in the first clock update.
            this.getNextComponentToInspect();
        }

        //Sample the component buffers
        this.sampleComponentBuffers();
    }

    /**
     * Determines the component type to inspect. If the Inspector can inspect multiple component types, randomly selects
     * which component to inspect.
     *
     * @return
     */
    private void getNextComponentToInspect(){
        ComponentName componentName;

        //Determine which component will be inspected (if there are multiple components this inspector can inspect, pick one at random
        Integer componentIndex = 0;
        if(!(this.componentToWorkbenchMapping.keySet().size() == 1)){
            componentIndex = randomNumberGenerator.nextInt(this.componentToWorkbenchMapping.keySet().size());
        }
        componentName = new ArrayList<ComponentName>(this.componentToWorkbenchMapping.keySet()).get(componentIndex);

        //Create the new component, initialize inspector arrival time as current time, and interarrival time as the time since
        //the last (same type) component arrival time occurred
        Component component = new Component(componentName);
        if (!this.lastArrivedComponent.containsKey(componentName)){
            component.setInterArrivalTime(this.entityType, this.clock);
        } else {
            Component lastComponent = this.lastArrivedComponent.get(componentName);
            component.setInterArrivalTime(this.entityType, this.clock - lastComponent.getArrivalTime(this.entityType));
        }

        //Set component Inspector arrival time as the current clock time
        component.setArrivalTime(this.entityType, this.clock);

        //Ensure that component buffer is cleared before adding the current component, this is because an inspector can only inspect
        //one component at a time
        ArrayList<Component> componentBuffer = this.componentBuffers.get(componentName);
        if (componentBuffer.size() > 0) {
            componentBuffer.remove(0);
        }
        componentBuffer.add(component);

        //Update currentComponentNameUnderInspection, this is used to help maintain state
        this.currentComponentNameUnderInspection = componentName;

        //Set the new component as the lastArrivedComponent
        this.lastArrivedComponent.put(componentName, component);

        //Get the service time for this component
        this.setComponentServiceTime();
    }

    /**
     * Use the currentComponentUnderInspection value, to the state Inspector state, and get the service time.
     * If there are no service times remaining for this Inspector, then state is DONE.
     *
     */
    private void setComponentServiceTime(){
        if (!this.componentServiceTimes.get(this.currentComponentNameUnderInspection).isEmpty()) {
            this.setState(EntityState.ACTIVE);
            this.setServiceTimeRemaining(this.componentServiceTimes.get(this.currentComponentNameUnderInspection).remove());
        } else {
            this.setState(EntityState.DONE);
        }
    }

    /**
     * Determines whether any workbenches are able to take the component (assumed to have
     * been inspected by this point). If successful, updates the Inspector state accordingly.
     * If not, Inspector state is set to BLOCKED.
     *
     * @return
     */
    private void attemptToPutComponentOnWorkbench(){
        WorkBench workbench = getNextWorkBench();
        if (workbench != null) {
            Component component = this.componentBuffers.get(this.currentComponentNameUnderInspection).remove(0);
            component.removeComponentFromSystem(this.entityType, this.clock);
            workbench.addComponent(component);

            //Add this component to the Inspector's completed components collection
            ComponentName componentName = component.getComponentName();
            if(!this.completedComponents.containsKey(componentName)){
                this.completedComponents.put(componentName, new ArrayList<Component>());
            }
            ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
            completedComponents.add(component);


            this.incrementServicesCompleted();
            this.getNextComponentToInspect();
        } else {
            this.setState(EntityState.BLOCKED);
        }
    }

    /**
     * Find's appropriate workbench to place component on. Looks for workbench with least buffer size (used buffer space).
     * In the event of a tie, leverages workbenchPriorities.
     *
     * @return
     */
    private WorkBench getNextWorkBench(){
        int minBufferSize = MAX_BUFFER_SIZE;
        WorkBench candidateWorkbench = null;

        for (WorkBench workbench : this.workbenchPriorities.keySet()){
            if (workbench.bufferAvailable(this.currentComponentNameUnderInspection)) {
                Integer workbenchBufferSize = workbench.getBufferSize(this.currentComponentNameUnderInspection);
                if (workbenchBufferSize < minBufferSize) {
                    minBufferSize = workbenchBufferSize;
                    candidateWorkbench = workbench;
                } else if (workbenchBufferSize == minBufferSize) {
                    Integer workbenchPriority = this.workbenchPriorities.get(workbench);
                    Integer candidateWorkbenchPriority = this.workbenchPriorities.get(candidateWorkbench);
                    if (workbenchPriority < candidateWorkbenchPriority) {
                        candidateWorkbench = workbench;
                    }
                }
            }
        }
        return candidateWorkbench;
    }

    private double getIdleProportion(){
        double timeInBlockedState = this.getStateTime(EntityState.BLOCKED);
        double overallTime = this.getTotalStateTime();

        return timeInBlockedState * 100 / overallTime;
    }

    @Override
    public Double getQuantityOfInterest(){
        return this.getIdleProportion();
    }

    @Override
    public String produceReport() {
        StringBuilder result = new StringBuilder();
        result.append(String.format("[%s]  Idle %%: %f  TotalInspections: %d TotalBlockedTime(mins): %.2f  TotalActiveTime(mins): %.2f", this.getName(), this.getIdleProportion(), this.getServicesCompleted(), (this.getStateTime(EntityState.BLOCKED)/60), (this.getStateTime(EntityState.ACTIVE)/60)));
        result.append(this.calculateLittlesLaw());
        return result.toString();
    }
}
