package entities;

import globals.ComponentName;
import globals.EntityType;

import java.util.HashMap;

public class Component {
    private ComponentName componentName;
    private HashMap<EntityType, Double> arrivalTimes;
    private HashMap<EntityType, Double> interArrivalTimes;
    private HashMap<EntityType, Double> systemTimes;

    Component (ComponentName componentName){
        this.componentName = componentName;
        this.arrivalTimes = new HashMap<EntityType, Double>();
        this.interArrivalTimes = new HashMap<EntityType, Double>();
        this.systemTimes = new HashMap<EntityType, Double>();
    }


    public ComponentName getComponentName(){ return this.componentName; }

    public void removeComponentFromSystem(EntityType entityType, Double clock){ this.setEntitySystemTime(entityType, clock); }

    public void setArrivalTime(EntityType entityType, Double arrivalTime){ this.arrivalTimes.put(entityType, arrivalTime); }
    public Double getArrivalTime(EntityType entityType){ return this.arrivalTimes.get(entityType); }

    public void setInterArrivalTime(EntityType entityType, Double interArrivalTime){ this.interArrivalTimes.put(entityType, interArrivalTime); }
    public Double getInterArrivalTime(EntityType entityType){ return this.interArrivalTimes.get(entityType); }

    public void setEntitySystemTime(EntityType entityType, Double clock){ this.systemTimes.put(entityType, clock - this.getArrivalTime(entityType)); }
    public Double getEntitySystemTime(EntityType entityType){ return this.systemTimes.get(entityType); }

    public Double getSystemTime() {
        Double systemTime = 0.0;
        for (Double entitySystemTime : this.systemTimes.values()){
            systemTime += entitySystemTime;
        }
        return systemTime;
    }
}
