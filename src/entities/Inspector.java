package entities;

import globals.Component;
import globals.EntityState;

import java.util.*;


public class Inspector extends Entity{
    private final int MAX_BUFFER_SIZE = 999;                                            //Maximum possible buffer size for a workbench, used when finding the workbench with the minimum current buffer value, this is ridiculously large compared to whats expected
    private final int SEED = 9;                                                         //Seed value for random number generator, useful for testing, by default not used.
    private HashMap<Component, ArrayList<WorkBench>> componentToWorkbenchMapping;       //A Mapping of components to workbenches ex. {C1: [W1, W2, W3], C2: [W2] ... }
    private HashMap<Component, Queue<Integer>> componentServiceTimes;                   //A mapping of service time queues to components ex. {C1: [60, 120, 240], C2: [30, 45, ... }
    private HashMap<WorkBench, Integer> workbenchPriorities;                            //A mapping of priorities to workbenches ex:. {W1: 1, W2: 2, W3: 3}
    private Component currentComponentUnderInspection;                                  //Current component under inspection
    private Random randomNumberGenerator;                                               //Random number generator

    public Inspector (String name) {
        super(name);
        this.componentToWorkbenchMapping = new HashMap<Component, ArrayList<WorkBench>>();
        this.componentServiceTimes = new HashMap<Component, Queue<Integer>>();
        this.workbenchPriorities = new HashMap<WorkBench, Integer>();
        this.randomNumberGenerator = new Random();
    }

    /**
     * Registration method to initialize an Inspector. Maps a list of service times for a specific component.
     *
     * @param component
     * @param serviceTimes
     */
    public void registerComponentServiceTimes(Component component, ArrayList<Integer> serviceTimes){
        Queue<Integer> serviceTimeQueue = new LinkedList<Integer>();
        for (Integer serviceTime : serviceTimes){
            serviceTimeQueue.add(serviceTime);
        }
        this.componentServiceTimes.put(component, serviceTimeQueue);
    }

    /**
     * Registration method to map components to Workbenches.
     *
     * @param component
     * @param workBench
     */
    public void registerComponentForWorkbench(Component component, WorkBench workBench){
        if (this.componentToWorkbenchMapping.containsKey(component)){
            ArrayList<WorkBench> workBenches = this.componentToWorkbenchMapping.get(component);
            workBenches.add(workBench);
        } else {
            ArrayList<WorkBench> workBenches = new ArrayList<WorkBench>();
            workBenches.add(workBench);
            this.componentToWorkbenchMapping.put(component, workBenches);
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
    public void clockUpdate(Integer interval){
        Integer serviceTimeRemaining = this.getServiceTimeRemaining();
        EntityState currentState = this.getState();
        this.incrementStateTimer(currentState, interval);

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
    }

    /**
     * Determines the component type to inspect. If the Inspector can inspect multiple component types, randomly selects
     * which component to inspect.
     *
     * @return
     */
    private void getNextComponentToInspect(){
        if(this.componentToWorkbenchMapping.keySet().size() == 1){
            this.currentComponentUnderInspection = new ArrayList<Component>(this.componentToWorkbenchMapping.keySet()).get(0);
            this.setComponentServiceTime();
        } else {
            Integer randomComponentIndex = randomNumberGenerator.nextInt(this.componentToWorkbenchMapping.keySet().size());
            this.currentComponentUnderInspection = new ArrayList<Component>(this.componentToWorkbenchMapping.keySet()).get(randomComponentIndex);
            this.setComponentServiceTime();
        }
    }

    /**
     * Use the currentComponentUnderInspection value, to the state Inspector state, and get the service time.
     * If there are no service times remaining for this Inspector, then state is DONE.
     *
     */
    private void setComponentServiceTime(){
        if (!this.componentServiceTimes.get(this.currentComponentUnderInspection).isEmpty()) {
            this.setState(EntityState.ACTIVE);
            this.setServiceTimeRemaining(this.componentServiceTimes.get(this.currentComponentUnderInspection).remove());
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
            workbench.addComponent(this.currentComponentUnderInspection);
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
            if (workbench.bufferAvailable(this.currentComponentUnderInspection)) {
                Integer workbenchBufferSize = workbench.getBufferSize(this.currentComponentUnderInspection);
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

    @Override
    public String produceReport() {
        double timeInBlockedState = this.getStateTime(EntityState.BLOCKED);
        double overallTime = this.getTotalStateTime();
        String result = String.format("[%s] Idle %%: %.2f  Total Blocked Time(mins): %.2f  Total Active Time(mins): %.2f", this.getName(), (timeInBlockedState * 100/ overallTime), (timeInBlockedState/60), (overallTime/60));
        return result;
    }
}
