package entities;

import globals.EntityState;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Entity {
    private String name;
    private EntityState state;
    private HashMap<EntityState, Integer> stateTimer;
    private Integer serviceTimeRemaining;
    private Integer servicesCompleted;

    public Entity(String name){
        this.name = name;
        this.state = EntityState.INITIALIZED;
        this.stateTimer = new HashMap<EntityState, Integer>();
        this.servicesCompleted = 0;
    }

    /**
     * Sets state.
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
     * @param value
     */
    protected void setServiceTimeRemaining(Integer value){
        this.serviceTimeRemaining = value;
    }

    /**
     * Get the service time reamining.
     * @return
     */
    protected Integer getServiceTimeRemaining(){
        return this.serviceTimeRemaining;
    }

    /**
     * Decrement service timer.
     * @param interval
     */
    protected void decrementServiceTimeRemaining(Integer interval){
        this.serviceTimeRemaining -= interval;
    }

    /**
     * Increment service completion counter.
     */
    protected void incrementServicesCompleted(){
        this.servicesCompleted ++;
    }

    /**
     * Increment the global state timer.
     * @param state
     * @param interval
     */
    protected void incrementStateTimer(EntityState state, Integer interval){
        if(this.stateTimer.containsKey(state)){
            Integer currentStateTime = this.stateTimer.get(state);
            currentStateTime += interval;
            this.stateTimer.put(state, currentStateTime);
        } else {
            this.stateTimer.put(state, 0);
        }
    }

    public abstract void clockUpdate(Integer interval);
}
