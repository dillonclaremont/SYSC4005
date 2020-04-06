import entities.*;
import globals.ComponentName;
import globals.EntityState;
import globals.Lambda;
import globals.EntityType;
import globals.Product;
import statistics.Calculator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SimulationDriver {
    private static final int WORKBENCH_COMPONENT_BUFFER_SIZE = 2;
    private static final Double CLOCK_INCREMENT_SIZE = 0.01;
    private static final int NUM_SERVICE_TIMES = 300;
    private static final int NUMBER_OF_REPLICATIONS = 5;
    private static boolean PERFORM_SYSTEM_VERIFICATION = true;

    /**
     * Simulation Driver.
     *
     * @param args
     */
    public static void main(String args[]){
        HashMap<String, ArrayList<Integer>> replicationResults = new HashMap<String, ArrayList<Integer>>();
        HashMap<ComponentName, ArrayList<Component>> allCompletedComponents = new HashMap<ComponentName, ArrayList<Component>>();

        //Initialize all entities
        ArrayList<Entity> entities = init();

        //Initialize replicationResults - used to contain results for each replication
        for (Entity entity : entities) {
            ArrayList<Integer> servicesCompleted = new ArrayList<Integer>();
            replicationResults.put(entity.getName(), servicesCompleted);
        }

        //Run a replication
        int replicationsCompleted = 0;
        while (replicationsCompleted < NUMBER_OF_REPLICATIONS) {
            HashMap<ComponentName, Integer> systemComponentBufferSampleSum = new HashMap<ComponentName, Integer>();                 //Cumulative sum of component buffer sample values
            Integer clockIterations= 0;                                                                                             //Number of clock iterations
            entities = init();

            //Run simulation until all entities are in either the DONE or BLOCKED state.
            boolean replicationComplete = false;
            while (!replicationComplete) {
                ArrayList<EntityState> entityStates = new ArrayList<EntityState>();             //This is used track the states of all entities for each clock cycle

                //Iterate through each entity and trigger the entity's clock to update
                for (Entity entity : entities) {
                    //Capture current state of entity
                    EntityState entityState = entity.getState();
                    entityStates.add(entityState);

                    //Only update clock for an entity that is not in the DONE state.
                    if (entityState != EntityState.DONE) {
                        entity.clockUpdate(CLOCK_INCREMENT_SIZE);
                    }

                }

                //Stop simulation if any entity is in the DONE state
                for (EntityState entityState : entityStates) {
                    if (entityState == EntityState.DONE) {
                        replicationComplete = true;
                        break;
                    }
                }

                clockIterations ++;
            }

            //Gather results for this replication
            //Collects the servicesCompleted for each entity
            //Collects each component that has made it through the entire system (to calculate little's law for the entire system)
            for (Entity entity : entities) {
                ArrayList<Integer> servicesCompleted = replicationResults.get(entity.getName());

                //Get all components that have made it through the entire system (completed components from the workbenches)
                if (entity.getEntityType().equals(EntityType.WORKBENCH)) {
                    HashMap<ComponentName, ArrayList<Component>> completedComponents = entity.getCompletedComponents();
                    for (ComponentName componentName : completedComponents.keySet()) {
                        if (!allCompletedComponents.containsKey(componentName)){
                            allCompletedComponents.put(componentName, new ArrayList<Component>());
                        }
                        ArrayList<Component> comp = allCompletedComponents.get(componentName);
                        comp.addAll(completedComponents.get(componentName));
                    }
                }

                //Get number of services completed for each entity
                servicesCompleted.add(entity.getServicesCompleted());
            }

            replicationsCompleted ++;

            if (PERFORM_SYSTEM_VERIFICATION) {
                System.out.println("REPLICATION " + replicationsCompleted + ":");
                produceSystemReport(allCompletedComponents, entities, clockIterations);
                System.out.println("-----------------------------------------------------");
                produceEntityReport(entities);
                System.out.println("-----------------------------------------------------");
                System.out.println("-----------------------------------------------------");
            }
        }

    }

    private static void produceSystemReport(HashMap<ComponentName, ArrayList<Component>> allCompletedComponents, ArrayList<Entity> entities, Integer clockIterations){
        System.out.println("SYSTEM RESULTS");
        //Evaluate Little's law for the entire system
        for (ComponentName componentName : allCompletedComponents.keySet()){
            Double totalBufferSampleSum = 0.0;
            for (Entity entity : entities){
                if (entity.getComponentBufferSampleSum().containsKey(componentName)) {
                    totalBufferSampleSum += entity.getComponentBufferSampleSum().get(componentName);
                }
            }
            Double avgNumberInSystem = totalBufferSampleSum/clockIterations;
            System.out.println (String.format("[%s] %s",componentName, Calculator.evaluateLittlesLaw(avgNumberInSystem, allCompletedComponents.get(componentName))));
        }

    }

    private static void produceEntityReport(ArrayList<Entity> entities){
        System.out.println("INDIVIDUAL QUEUE RESULTS");
        for (Entity entity : entities){
            System.out.println(entity.produceReport());
        }
    }


    /**
     * Initialize all components.
     *
     * @return
     */
    private static ArrayList<Entity> init(){
        ArrayList<Entity> entities = new ArrayList<Entity>();

        WorkBench workbenchOne = new WorkBench("WorkBench1", Product.P1, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchOne.registerComponent(ComponentName.C1);
        //workbenchOne.setServiceTimes(NumberGenerator.readServiceTimeFile("resources/ws1.dat"));
        workbenchOne.setServiceTimes(NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.WORKSTATION1.value));

        WorkBench workbenchTwo = new WorkBench("WorkBench2", Product.P2, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchTwo.registerComponent(ComponentName.C1);
        workbenchTwo.registerComponent(ComponentName.C2);
        //workbenchTwo.setServiceTimes(NumberGenerator.readServiceTimeFile("resources/ws2.dat"));
        workbenchTwo.setServiceTimes(NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.WORKSTATION2.value));

        WorkBench workbenchThree = new WorkBench("WorkBench3", Product.P3, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchThree.registerComponent(ComponentName.C1);
        workbenchThree.registerComponent(ComponentName.C3);
        //workbenchThree.setServiceTimes(NumberGenerator.readServiceTimeFile("resources/ws3.dat"));
        workbenchThree.setServiceTimes(NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.WORKSTATION3.value));

        Inspector inspectorOne = new Inspector("Inspector1");
        inspectorOne.registerComponent(ComponentName.C1);
        inspectorOne.registerComponentForWorkbench(ComponentName.C1, workbenchOne);
        inspectorOne.registerComponentForWorkbench(ComponentName.C1, workbenchTwo);
        inspectorOne.registerComponentForWorkbench(ComponentName.C1, workbenchThree);
        inspectorOne.registerWorkbenchPriority(workbenchOne, 1);
        inspectorOne.registerWorkbenchPriority(workbenchTwo, 2);
        inspectorOne.registerWorkbenchPriority(workbenchThree, 3);
        //inspectorOne.registerComponentServiceTimes(ComponentName.C1, NumberGenerator.readServiceTimeFile("resources/servinsp1.dat"));
        inspectorOne.registerComponentServiceTimes(ComponentName.C1, NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.SERVINSP1.value));

        Inspector inspectorTwo = new Inspector("Inspector2");
        inspectorTwo.registerComponent(ComponentName.C2);
        inspectorTwo.registerComponent(ComponentName.C3);
        inspectorTwo.registerComponentForWorkbench(ComponentName.C2, workbenchTwo);
        inspectorTwo.registerComponentForWorkbench(ComponentName.C3, workbenchThree);
        inspectorTwo.registerWorkbenchPriority(workbenchTwo, 1);
        inspectorTwo.registerWorkbenchPriority(workbenchThree, 2);
        //inspectorTwo.registerComponentServiceTimes(ComponentName.C2, NumberGenerator.readServiceTimeFile("resources/servinsp22.dat"));
        //inspectorTwo.registerComponentServiceTimes(ComponentName.C3, NumberGenerator.readServiceTimeFile("resources/servinsp23.dat"));
        inspectorTwo.registerComponentServiceTimes(ComponentName.C2, NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.SERVINSP22.value));
        inspectorTwo.registerComponentServiceTimes(ComponentName.C3, NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.SERVINSP23.value));

        entities.add(inspectorOne);
        entities.add(inspectorTwo);
        entities.add(workbenchOne);
        entities.add(workbenchTwo);
        entities.add(workbenchThree);

        return entities;
    }
}
