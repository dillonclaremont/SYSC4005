package entities;

import globals.ComponentName;
import globals.EntityState;
import globals.EntityType;
import globals.Product;

import java.util.ArrayList;

import java.util.LinkedList;
import java.util.Queue;

public class WorkBench extends Entity {
    private Product product;                                                         //Type of product output by this WorkBench
    private int maxBufferSize;                                                       //Maximum buffer size
    private Queue<Double> serviceTimes;                                              //A queue of service times


    public WorkBench(String name, Product product, int maxBufferSize){
        super(name);
        this.entityType = EntityType.WORKBENCH;
        this.product = product;
        this.maxBufferSize = maxBufferSize;
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
     * It is assumed that this method will only be called after bufferAvailable() was called immediately before (and was true)
     *
     * @param component
     */
    public void addComponent(Component component){
        ComponentName componentName = component.getComponentName();

        //Get the corresponding componentBuffer for this component
        ArrayList<Component> componentBuffer = this.componentBuffers.get(componentName);

        if (!this.lastArrivedComponent.containsKey(componentName)){
            component.setInterArrivalTime(this.entityType, this.clock);
        } else {
            Component lastComponent = this.lastArrivedComponent.get(componentName);
            component.setInterArrivalTime(this.entityType, this.clock - lastComponent.getArrivalTime(this.entityType));
        }

        component.setArrivalTime(this.entityType, this.clock);

        //Place component in componentBuffer
        componentBuffer.add(component);

        //Add component to lastArrivedComponent, this is used to measure interarrival times
        this.lastArrivedComponent.put(componentName, component);
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

        //Sample the component buffers
        this.sampleComponentBuffers();
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
            component.removeComponentFromSystem(this.entityType, this.clock);

            if(!this.completedComponents.containsKey(componentName)){
                this.completedComponents.put(componentName, new ArrayList<Component>());
            }
            ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
            completedComponents.add(component);

        }
    }

    /**
     *
     * @return
     */
    public ArrayList<ComponentName> getBlockingComponents(){
        ArrayList<ComponentName> blockingComponents = new ArrayList<ComponentName>();

        for (ComponentName componentName : this.componentBuffers.keySet()){
            if (this.componentBuffers.get(componentName).size() == 0){
                blockingComponents.add(componentName);
            }
        }
        return blockingComponents;
    }

    public double getThroughput(){
        double productsAssembled = this.getServicesCompleted();
        double overallTimeInHours = this.getTotalStateTime()/3600;
        return productsAssembled / overallTimeInHours;
    }

    @Override
    public Double getQuantityOfInterest(){
        return this.getThroughput();
    }

    @Override
    public String produceReport(){
        StringBuilder result = new StringBuilder();
        result.append(String.format("[%s]  AssembledProducts: %d  Throughput(AssembledProducts/hr): %f", this.getName(), this.getServicesCompleted(), this.getThroughput()));
        result.append(this.calculateLittlesLaw());
        return result.toString();
    }
}
