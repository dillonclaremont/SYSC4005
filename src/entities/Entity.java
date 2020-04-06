package entities;

import globals.ComponentName;
import globals.EntityState;
import globals.EntityType;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Entity {
    private String name;                                                                //Name of entity
    private EntityState state;                                                          //Current state, of type EntityState.
    protected EntityType entityType;
    private HashMap<EntityState, Double> stateTimer;                                    //A running counter of time spent at a given state (unit-less)
    private Double serviceTimeRemaining;                                                //A running counter to track the time remaining for the current service interval
    private Integer servicesCompleted;                                                  //A running counter to track the Number of services that have been completed
    protected HashMap<ComponentName, ArrayList<Component>> componentBuffers;            //Mapping of buffer sizes for each component. Since this is a simulation, this only maintains the number of components that would be in a (theoretical) buffer
    protected HashMap<ComponentName, Integer> componentBufferSampleSum;                 //Cumulative sum of component buffer sample values
    protected Integer bufferSampleCount;                                                //Number of buffer samples taken
    protected HashMap<ComponentName, ArrayList<Component>> completedComponents;
    protected HashMap<ComponentName, Component> lastArrivedComponent;
    protected Double clock;

    public Entity(String name){
        this.name = name;
        this.state = EntityState.INITIALIZED;
        this.stateTimer = new HashMap<EntityState, Double>();
        this.servicesCompleted = 0;
        this.clock = 0.0;
        this.componentBufferSampleSum = new HashMap<ComponentName, Integer>();
        this.completedComponents = new HashMap<ComponentName, ArrayList<Component>>();
        this.componentBuffers = new HashMap<ComponentName, ArrayList<Component>>();
        this.lastArrivedComponent = new HashMap<ComponentName, Component>();
        this.bufferSampleCount = 0;
    }

    /**
     * Return name.
     *
     * @return
     */
    public String getName(){
        return this.name;
    }

    /**
     * Sets state.
     *
     * @param state
     */
    protected void setState(EntityState state){
        this.state = state;
    }

    /**
     * Get state.
     *
     * @return
     */
    public EntityState getState(){
        return this.state;
    }

    /**
     * Set service time remaining.
     *
     * @param value
     */
    protected void setServiceTimeRemaining(Double value){
        this.serviceTimeRemaining = value;
    }

    /**
     * Get the service time reamining.
     *
     * @return
     */
    protected Double getServiceTimeRemaining(){
        return this.serviceTimeRemaining;
    }

    /**
     * Decrement service timer.
     *
     * @param interval
     */
    protected void decrementServiceTimeRemaining(Double interval){
        this.serviceTimeRemaining -= interval;
    }

    /**
     * Increment service completion counter.
     *
     */
    protected void incrementServicesCompleted(){
        this.servicesCompleted ++;
    }

    /**
     * Return the number of services completed for this entity.
     *
     * @return
     */
    public Integer getServicesCompleted(){ return this.servicesCompleted; }

    /**
     * Returns all completed components
     * @return
     */
    public HashMap<ComponentName, ArrayList<Component>>  getCompletedComponents(){ return this.completedComponents; }

    /**
     * Return entity type
     * @return
     */
    public EntityType getEntityType() { return entityType; }

    /**
     * Return current buffer sample sums
     * @return
     */
    public HashMap<ComponentName, Integer> getComponentBufferSampleSum(){ return this.componentBufferSampleSum; }

    /**
     * Returns the total time spent across all states
     *
     * @return
     */
    public Double getTotalStateTime(){
        Double totalStateTime = 0.0;
        for (Double stateTime : this.stateTimer.values()){
            totalStateTime += stateTime;
        }
        return totalStateTime;
    }

    /**
     * Returns the total time spent in a specific state
     * @param state
     * @return
     */
    public Double getStateTime(EntityState state){
        if (this.stateTimer.containsKey(state)){
            return this.stateTimer.get(state);
        }
        return 0.0;
    }

    /**
     * Increment the global state timer.
     *
     * @param state
     * @param interval
     */
    protected void incrementStateTimer(EntityState state, Double interval){
        if(this.stateTimer.containsKey(state)){
            Double currentStateTime = this.stateTimer.get(state);
            currentStateTime += interval;
            this.stateTimer.put(state, currentStateTime);
        } else {
            this.stateTimer.put(state, 0.0);
        }
    }

    /**
     * Registration method to configure this Workbench for a specific component type
     *
     * @param componentName
     */
    public void registerComponent(ComponentName componentName){
        this.componentBuffers.put(componentName, new ArrayList<Component>());
        //this.componentBufferSamples.put(componentName, new ArrayList<Integer>());
        this.componentBufferSampleSum.put(componentName, 0);
    }

    public String calculateLittlesLaw(){
        StringBuilder result = new StringBuilder();
        for (ComponentName componentName : this.completedComponents.keySet()){
            Double avgArrivalRate = 1/(this.getAvgInterArrivalTime(componentName)/3600); //arrival rate is 1/avgInterArrivalTime convert to minutes
            Double avgSystemTime = this.getAvgSystemTime(componentName)/3600; //convert to minutes
            Double averageNumberInSystem = this.getAvgNumberInSystem(componentName);
            result.append(String.format("\n\t [%s] avgNumInSystem: %.2f,  avgArrivalRate: %.2f, avgSystemTime: %.2f", componentName, averageNumberInSystem, avgArrivalRate, avgSystemTime));
            result.append(String.format("\n\t Little's Law: %.2f=%.2f", averageNumberInSystem, avgArrivalRate*avgSystemTime));
        }
        return result.toString();
    }

    private Double getAvgInterArrivalTime(ComponentName componentName){
        Double sumInterArrivalTimes = 0.0;
        ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
        for (Component component : completedComponents){
            sumInterArrivalTimes += component.getInterArrivalTime(this.entityType);
        }
        return (sumInterArrivalTimes/completedComponents.size());
    }

    private Double getAvgSystemTime(ComponentName componentName){
        Double sumSystemTimes = 0.0;
        ArrayList<Component> completedComponents = this.completedComponents.get(componentName);
        for (Component component : completedComponents){
            sumSystemTimes += component.getEntitySystemTime(this.entityType);
        }
        return sumSystemTimes/completedComponents.size();
    }

    private Double getAvgNumberInSystem(ComponentName componentName){
        return ((double)this.componentBufferSampleSum.get(componentName) / this.bufferSampleCount);
    }

    protected void sampleComponentBuffers(){
        //sample componentBuffer and add current system state
        for (ComponentName cn : this.componentBufferSampleSum.keySet()){
            Integer componentBufferSampleSum = this.componentBufferSampleSum.get(cn);
            componentBufferSampleSum += this.componentBuffers.get(cn).size();
            this.componentBufferSampleSum.put(cn, componentBufferSampleSum);
        }
        this.bufferSampleCount ++;
    }

    public abstract void clockUpdate(Double interval);
    public abstract String produceReport();
    public abstract Double getQuantityOfInterest();
}
