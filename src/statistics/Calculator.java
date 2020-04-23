package statistics;

import entities.Component;
import globals.EntityType;

import java.util.ArrayList;

public class Calculator {

    static public String evaluateLittlesLaw(Double averageNumberInSystem, ArrayList<Component> components, int numberOfinputStreams){
        StringBuilder result = new StringBuilder();
        double avgArrivalRate = 1/(getAvgInterArrivalTime(components)/3600) * numberOfinputStreams; //arrival rate is 1/avgInterArrivalTime convert to minutes, multiply by the numberOfInputStreams since this is used for the calculations involving the entire system
        double avgSystemTime = getAvgSystemTime(components)/3600; //convert to minutes
        double littlesLaw = avgArrivalRate*avgSystemTime;
        result.append(String.format("\t avgNumInSystem: %f,  avgArrivalRate: %f, avgSystemTime: %f", averageNumberInSystem, avgArrivalRate, avgSystemTime));
        result.append(String.format("\n\t Little's Law: %f=%f", averageNumberInSystem, littlesLaw));
        return result.toString();
    }

    static private double getAvgInterArrivalTime(ArrayList<Component> components){
        Double sumInterArrivalTimes = 0.0;
        for (Component component : components){
            sumInterArrivalTimes += component.getInterArrivalTime(EntityType.INSPECTOR);
        }
        return Math.round(sumInterArrivalTimes/components.size());
    }

    static private double getAvgSystemTime(ArrayList<Component> components){
        Double sumSystemTimes = 0.0;
        for (Component component : components){
            sumSystemTimes += component.getSystemTime();
        }
        return Math.round(sumSystemTimes/components.size());
    }
}
