package statistics;

import entities.Component;
import globals.ComponentName;
import globals.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Calculator {

    static public String evaluateLittlesLaw(Double averageNumberInSystem, ArrayList<Component> components){
        StringBuilder result = new StringBuilder();
        Double avgArrivalRate = 1/(getAvgInterArrivalTime(components)/3600); //arrival rate is 1/avgInterArrivalTime convert to minutes
        Double avgSystemTime = getAvgSystemTime(components)/3600; //convert to minutes
        result.append(String.format("\t avgNumInSystem: %.2f,  avgArrivalRate: %.2f, avgSystemTime: %.2f", averageNumberInSystem, avgArrivalRate, avgSystemTime));
        result.append(String.format("\n\t Little's Law: %.2f=%.2f", averageNumberInSystem, avgArrivalRate*avgSystemTime));
        return result.toString();
    }

    static private Double getAvgInterArrivalTime(ArrayList<Component> components){
        Double sumInterArrivalTimes = 0.0;
        for (Component component : components){
            sumInterArrivalTimes += component.getInterArrivalTime(EntityType.INSPECTOR);
        }
        return (sumInterArrivalTimes/components.size());
    }

    static private Double getAvgSystemTime(ArrayList<Component> components){
        Double sumSystemTimes = 0.0;
        for (Component component : components){
            sumSystemTimes += component.getSystemTime();
        }
        return sumSystemTimes/components.size();
    }
}
