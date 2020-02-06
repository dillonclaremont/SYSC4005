package entities;

import globals.Component;
import globals.EntityState;
import globals.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class WorkBench extends Entity {
    private Product product;                                            //Type of product output by this WorkBench
    private int maxBufferSize;                                          //Maximum buffer size
    private HashMap<Component, Integer> componentBuffers;               //Mapping of buffer sizes for each component. Since this is a simulation, this only maintains the number of components that would be in a (theoretical) buffer
    private Queue<Integer> serviceTimes;                                //A queue of service times

    public WorkBench(String name, Product product, int maxBufferSize){
        super(name);
        this.product = product;
        this.maxBufferSize = maxBufferSize;
        this.componentBuffers = new HashMap<Component, Integer>();
    }

    /**
     * Registration method to initialize a Workbench. Adds a list of service times for the workbench.
     *
     * @param serviceTimes
     */
    public void setServiceTimes(ArrayList<Integer> serviceTimes){
        Queue<Integer> serviceTimeQueue = new LinkedList<Integer>();
        for (Integer serviceTime : serviceTimes){
            serviceTimeQueue.add(serviceTime);
        }
        this.serviceTimes = serviceTimeQueue;
    }

    /**
     * Registration method to configure this Workbench for a specific component type
     *
     * @param component
     */
    public void registerComponent(Component component){
        this.componentBuffers.put(component, 0);
    }

    /**
     * Returns the current size of the buffer for a specific component. The "size" of the buffer indicates the "number" of components that would be in the buffer at the time of query.
     *
     * @param component
     * @return
     */
    public int getBufferSize(Component component){
        return componentBuffers.get(component);
    }

    /**
     * Places a component in the corresponding component buffer, only if buffer is less than maxBufferSize.
     *
     * @param component
     */
    public void addComponent(Component component){
        if (this.componentBuffers.get(component) < this.maxBufferSize) {
            Integer componentBuffer = this.componentBuffers.get(component);
            componentBuffer++;
            this.componentBuffers.put(component, componentBuffer);
        }
    }

    /**
     * Query whether room in the buffer is available for a given component.
     *
     * @param component
     * @return
     */
    public boolean bufferAvailable(Component component){
        if (this.componentBuffers.containsKey(component) && (this.componentBuffers.get(component) < maxBufferSize)){
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
    public void clockUpdate(Integer interval){
        Integer serviceTimeRemaining = this.getServiceTimeRemaining();
        EntityState currentState = this.getState();
        this.incrementStateTimer(currentState, interval);

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
    }

    /**
     * Checks the component buffers to ensure at least one of each component is available, necessary to assemble a product.
     * If the necessary components are available, sets the state of the WorkBench to ACTIVE and selects the next service time
     * from the pre generated list of service times.
     */
    private void attemptToAssembleProduct(){
        boolean componentsAvailableToAssembleProduct = true;

        for (Integer bufferValue : this.componentBuffers.values()){
            if (bufferValue == 0){
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
        for (Component component : this.componentBuffers.keySet()){
            Integer componentBuffer = this.componentBuffers.get(component);
            componentBuffer--;
            this.componentBuffers.put(component, componentBuffer);
        }
    }

    @Override
    public String produceReport(){
        double productsAssembled = this.getServicesCompleted();
        double overallTimeInHours = this.getTotalStateTime()/3600;
        String result = String.format("[%s] Throughput (assembled products / hr): %.2f", this.getName(), (productsAssembled / overallTimeInHours));
        return result;
    }
}
