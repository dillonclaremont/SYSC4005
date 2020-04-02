package entities;

import globals.ComponentName;

public class Component {
    private ComponentName componentName;
    private Double arrivalTime;
    private Double interArrivalTime;
    private Double systemTime;

    Component (ComponentName componentName, Double arrivalTime, Double interArrivalTime){
        this.componentName = componentName;
        this.arrivalTime = arrivalTime;
        this.interArrivalTime = interArrivalTime;
    }

    public void retireComponent(Double clock){
        this.systemTime = clock - this.arrivalTime;
    }

    public ComponentName getComponentName(){
        return this.componentName;
    }

    public Double getArrivalTime(){
        return this.arrivalTime;
    }

    public Double getInterArrivalTime(){
        return this.interArrivalTime;
    }

    public Double getSystemTime(){
        return this.systemTime;
    }
}
