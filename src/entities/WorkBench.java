package entities;

import globals.ComponentName;
import globals.EntityState;
import globals.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class WorkBench extends Entity {
    private Product product;                                                         //Type of product output by this WorkBench
    private int maxBufferSize;                                                       //Maximum buffer size
    private HashMap<ComponentName, ArrayList<Component>> componentBuffers;           //Mapping of buffer sizes for each component. Since this is a simulation, this only maintains the number of components that would be in a (theoretical) buffer
    private HashMap<ComponentName, ArrayList<Component>> completedComponents;
    private HashMap<ComponentName, Component> lastArrivedComponent;
    private Queue<Double> serviceTimes;                                              //A queue of service times
    private HashMap<ComponentName, ArrayList<Integer>> componentBufferSamples;


    public WorkBench(String name, Product product, int maxBufferSize){
        super(name);
        this.product = product;
        this.maxBufferSize = maxBufferSize;
        this.componentBuffers = new HashMap<ComponentName, ArrayList<Component>>();
        this.completedComponents = new HashMap<ComponentName, ArrayList<Component>>();
        this.lastArrivedComponent = new HashMap<ComponentName, Component>();
        this.componentBufferSamples = new HashMap<ComponentName, ArrayList<Integer>>();
    }

    /**
     * Registration method to initialize a Workbench. Adds a list of service times for the workbench.
     *
     * @param serviceTimes
     */
    public void setServiceTimes(ArrayList<Double> serviceTimes){
        Queue<Double> serviceTimeQueue = new LinkedList<Double>();
        for (Double serviceTime : serviceTimes){
            serviceTimeQueue.add(serviceTime);
        }
        this.serviceTimes = serviceTimeQueue;
    }

    /**
     * Registration method to configure this Workbench for a specific component type
     *
     * @param componentName
     */
    public void registerComponent(ComponentName componentName){
        this.componentBuffers.put(componentName, new ArrayList<Component>());
        this.componentBufferSamples.put(componentName, new ArrayList<Integer>());
    }

    /**
     * Returns the current size of the buffer for a specific component. The "size" of the buffer indicates the "number" of components that would be in the buffer at the time of query.
     *
     * @param componentName
     * @return
     */
    public int getBufferSize(ComponentName componentName){
        return this.componentBuffers.get(componentName).size();
    }

    /**
     * Places a component in the corresponding component buffer, only if buffer is less than maxBufferSize.
     *
     * @param componentName
     */
    public void addComponent(ComponentName componentName){
        if (this.componentBuffers.get(componentName).size() < this.maxBufferSize) {

            //Get the corresponding componentBuffer for this component
            ArrayList<Component> componentBuffer = this.componentBuffers.get(componentName);

            //Instantiate a new component
            Component component = null;
            if (!this.lastArrivedComponent.containsKey(componentName)){
                component = new Component(componentName, this.clock, this.clock);
            } else {
                Component lastComponent = this.lastArrivedComponent.get(componentName);
                component = new Component(componentName, this.clock, this.clock - lastComponent.getArrivalTime());
            }

            //Place component in componentBuffer
            componentBuffer.add(component);

            //Add component to lastArrivedComponent, this is used to measure interarrival times
            this.lastArrivedComponent.put(componentName, component);

            //Sample the component buffers
            this.sampleComponentBuffers();
        }
    }

    /**
     * Query whether room in the buffer is available for a given component.
     *
     * @param componentName
     * @return
     */
    public boolean bufferAvailable(ComponentName componentName){
        if (this.componentBuffers.containsKey(componentName) && (this.componentBuffers.get(componentName).size() < maxBufferSize)){
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method updates the clock by 'interval'. State is updated accordingly for the Workbench.
     * Starts by updating the state timer.
     * Based on the current state of the WorkBench the following behaviour is exhibited:
     *  - If the WorkBench is currently assembling a product, decrement service time counter
     *  - If the WorkBench has just finished assembling a product, removes components from buffers, increments completion counter and attempts to assemble another product
     *  - If the WorkBench is blocked (did not have enough components to begin assembly of a product on the last clock update), attempts to assemble another product
     *  - If none of the above is true, must be the first case and simply attempts to assemble a product
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
            //remove 1 component from each component buffers
            this.completeAssembledProduct();
            this.incrementServicesCompleted();
            this.attemptToAssembleProduct();
        } else if (currentState == EntityState.ACTIVE && (serviceTimeRemaining > 0)){
            this.decrementServiceTimeRemaining(interval);
        } else if (currentState == EntityState.BLOCKED){
            this.attemptToAssembleProduct();
        } else if (currentState == EntityState.DONE) {
            //TODO: Not sure if any action is required here.
        } else {
            //This should only be entered in the first clock update.
            this.attemptToAssembleProduct();
        }
        // TODO REMOVE this.sampleComponentBuffers();
    }

    /**
     * Checks the component buffers to ensure at least one of each component is available, necessary to assemble a product.
     * If the necessary components are available, sets the state of the WorkBench to ACTIVE and selects the next service time
     * from the pre generated list of service times.
     */
    private void attemptToAssembleProduct(){
        boolean componentsAvailableToAssembleProduct = true;

        //Ensure all buffers have at least one component in them
        for (ArrayList<Component> componentBuffer : this.componentBuffers.values()){
            if (componentBuffer.size() == 0){
                componentsAvailableToAssembleProduct = false;
                break;
            }
        }

        if (componentsAvailableToAssembleProduct){
            this.setState(EntityState.ACTIVE);
            this.setServiceTimeRemaining(this.serviceTimes.remove());
        } else {
            this.setState(EntityState.BLOCKED);
        }
    }

    /**
     * To simulate a completed assembled product, simply decrement the component buffers.
     *
     */
    private void completeAssembledProduct(){
        for (ComponentName componentName : this.componentBuffers.keySet()){
            ArrayList<Component> componentBuffer = this.componentBuffers.get(componentName);

            //Remove 1st component from the buffer
            Component component = componentBuffer.remove(0);

            //Retire component (this is so the component can calculate it's system time)
            component.retireComponent(this.clock);

            if(!this.completedComponents.containsKey(componentName)){
                this.completedComponents.put(componentName, new ArrayList<Component>());
            }
            ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
            completedComponents.add(component);

        }
        this.sampleComponentBuffers();
    }

    @Override
    public String produceReport(){
        double productsAssembled = this.getServicesCompleted();
        double overallTimeInHours = this.getTotalStateTime()/3600;
        StringBuilder result = new StringBuilder();
        result.append(String.format("[%s]  AssembledProducts: %.0f  Throughput(AssembledProducts/hr): %.2f", this.getName(), productsAssembled, (productsAssembled / overallTimeInHours)));
        result.append(this.calculateLittlesLaw());
        return result.toString();
    }

    private String calculateLittlesLaw(){
        StringBuilder result = new StringBuilder();
        for (ComponentName componentName : this.completedComponents.keySet()){
            Double avgArrivalRate = this.getAverageArrivalRate(componentName)/3600; //convert to minutes
            Double avgSystemTime = this.getAverageSystemTime(componentName)/3600; //convert to minutes
            Double averageNumberInSystem = this.getAverageNumberInSystem(componentName);
            result.append(String.format(" [%s] Avg # in System: %.2f,  Arrival Rate: %.2f, Avg System Time: %.2f", componentName, averageNumberInSystem, avgArrivalRate, avgSystemTime));
        }
        return result.toString();
    }

    private Double getAverageArrivalRate(ComponentName componentName){
        Double sumInterArrivalTimes = 0.0;
        ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
        for (Component component : completedComponents){
            sumInterArrivalTimes += component.getInterArrivalTime();
        }
        return sumInterArrivalTimes/completedComponents.size();
    }

    private Double getAverageSystemTime(ComponentName componentName){
        Double sumSystemTimes = 0.0;
        ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
        for (Component component : completedComponents){
            sumSystemTimes += component.getSystemTime();
        }
        return sumSystemTimes/completedComponents.size();
    }

    private Double getAverageNumberInSystem(ComponentName componentName){
        Double sumBufferSample = 0.0;
        ArrayList<Integer> componentBufferSamples = this.componentBufferSamples.get(componentName);
        for (Integer componentBufferSample : componentBufferSamples){
            sumBufferSample += componentBufferSample;
        }
        return sumBufferSample / componentBufferSamples.size();
    }

    private void sampleComponentBuffers(){
        //sample componentBuffer and add current system state
        for (ComponentName cn : this.componentBufferSamples.keySet()){
            ArrayList<Integer> componentBufferSamples = this.componentBufferSamples.get(cn);
            componentBufferSamples.add(this.componentBuffers.get(cn).size());
        }
    }
}
