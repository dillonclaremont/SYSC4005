package entities;

import globals.Component;
import globals.EntityState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;


public class Inspector extends Entity{
    private HashMap<Component, ArrayList<WorkBench>> componentWorkBenchHashMap;
    private HashMap<Component, Queue<Integer>> componentServiceTimes;
    private Component currentComponentUnderInspection;
    private HashMap<WorkBench, Integer> workbenchPriorities;

    public Inspector (String name) {
        super(name);
        this.componentWorkBenchHashMap = new HashMap<Component, ArrayList<WorkBench>>();
        this.componentServiceTimes = new HashMap<Component, Queue<Integer>>();
        this.workbenchPriorities = new HashMap<WorkBench, Integer>();
    }

    /**
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
     *
     * @param component
     * @param workBench
     */
    public void registerComponentForWorkbench(Component component, WorkBench workBench){
        if (this.componentWorkBenchHashMap.containsKey(component)){
            ArrayList<WorkBench> workBenches = this.componentWorkBenchHashMap.get(component);
            workBenches.add(workBench);
        } else {
            ArrayList<WorkBench> workBenches = new ArrayList<WorkBench>();
            workBenches.add(workBench);
            this.componentWorkBenchHashMap.put(component, workBenches);
        }
    }

    /**
     *
     * @param workBench
     * @param priority
     */
    public void registerWorkbenchPriority(WorkBench workBench, Integer priority){
        this.workbenchPriorities.put(workBench, priority);
    }

    /**
     *
     * @param interval
     */
    public void clockUpdate(Integer interval){
        Integer serviceTimeRemaining = super.getServiceTimeRemaining();
        EntityState currentState = this.getState();
        this.incrementStateTimer(currentState, interval);

        if (currentState == EntityState.ACTIVE && (serviceTimeRemaining <= 0)){
            if (this.putComponentOnWorkbench()){
                this.getNextComponentToInspect();
            }
        } else if (currentState == EntityState.ACTIVE && (serviceTimeRemaining > 0)){
            super.decrementServiceTimeRemaining(interval);
        } else if (currentState == EntityState.BLOCKED){
            if (this.putComponentOnWorkbench()){
                this.getNextComponentToInspect();
            }
        } else if (currentState == EntityState.DONE) {
            //TODO: Not sure if any action is required here.perhap
        } else {
            //This should only be entered in the first clock update.
            //Update the state of this Inspector
            this. getNextComponentToInspect();
        }
    }

    /**
     *
     */
    private void getNextComponentToInspect(){
        if (!this.componentServiceTimes.isEmpty()) {
            //Update the state of this Inspector
            this.setState(EntityState.ACTIVE);
            //Set the current component under inspection for this Inspector
            this.currentComponentUnderInspection = selectComponentTypeToInspect();
            //Set the service time remaining for this inspection, based on the already generated service time list (this will remove this service time from the list of service times).
            super.setServiceTimeRemaining(this.componentServiceTimes.get(this.currentComponentUnderInspection).remove());
        } else {
            this.setState(EntityState.DONE);
        }
    }

    /**
     *
     * @return
     */
    private boolean putComponentOnWorkbench(){
        //Try to put component on a workbench
        //if successful get next component service time
        //else update inspectors state to blocked
        WorkBench workbench = getNextWorkBench();
        if (workbench != null) {
            workbench.addComponent(currentComponentUnderInspection);
            return true;
        } else {
            this.setState(EntityState.BLOCKED);
            return false;
        }
    }

    /**
     *
     * @return
     */
    private Component selectComponentTypeToInspect(){
        if(this.componentWorkBenchHashMap.keySet().size() == 1){
            return (new ArrayList<Component>(componentWorkBenchHashMap.keySet())).get(0);
        } else {
            //TODO implement random selection
            return null;
        }
    }

    /**
     *      * @return
     */
    private WorkBench getNextWorkBench(){
        int minBufferSize = 999;
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
}
