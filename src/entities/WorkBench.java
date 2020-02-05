package entities;

import globals.Component;
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
        if (this.componentBuffers.get(component) < maxBufferSize){
            return true;
        } else {
            return false;
        }
    }
    
    public void clockUpdate(Integer interval){

    }
}
