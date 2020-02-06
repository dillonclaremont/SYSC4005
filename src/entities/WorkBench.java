package entities;

import globals.Component;
import globals.EntityState;
import globals.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class WorkBench extends Entity {
    private Product product;
    private int maxBufferSize;
    private HashMap<Component, Integer> componentBuffers;
    private Queue<Integer> serviceTimes;

    public WorkBench(String name, Product product, int maxBufferSize){
        super(name);
        this.product = product;
        this.maxBufferSize = maxBufferSize;
        this.componentBuffers = new HashMap<Component, Integer>();
    }

    /**
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
     *
     * @param component
     */
    public void registerComponent(Component component){
        this.componentBuffers.put(component, 0);
    }

    /**
     *
     * @param component
     * @return
     */
    public int getBufferSize(Component component){
        return componentBuffers.get(component);
    }

    /**
     *
     * @param component
     */
    public void addComponent(Component component){
        Integer componentBuffer = this.componentBuffers.get(component);
        componentBuffer ++;
        this.componentBuffers.put(component, componentBuffer);
    }

    /**
     *
     * @param component
     * @return
     */
    public boolean bufferAvailable(Component component){
        if (this.componentBuffers.containsKey(component) && (this.componentBuffers.get(component) < maxBufferSize)){
        //if (this.componentBuffers.get(component) < maxBufferSize){
            return true;
        } else {
            return false;
        }
    }
    
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
}