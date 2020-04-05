import entities.*;
import globals.ComponentName;
import globals.EntityState;
import globals.Lambda;
import globals.Product;

import java.io.InputStream;
import java.util.ArrayList;

public class SimulationDriver {
    private static final int WORKBENCH_COMPONENT_BUFFER_SIZE = 2;
    private static final Double CLOCK_INCREMENT_SIZE = 0.01;
    private static final int NUM_SERVICE_TIMES = 300;

    /**
     * Simulation Driver.
     *
     * @param args
     */
    public static void main(String args[]){
        ArrayList<Entity> entities = init();
        boolean running = true;

        //Run simulation until all entities are in either the DONE or BLOCKED state.
        while (running) {
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

            //Stop driving the simulation if all entities are in the either DONE or BLOCKED state.
            running = false;
            for (EntityState entityState : entityStates){
                if (entityState != EntityState.DONE && entityState != EntityState.BLOCKED) {
                    running = true;
                    break;
                }
            }
        }

        produceReport(entities);
    }

    private static void produceReport(ArrayList<Entity> entities){
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
        workbenchTwo.setServiceTimes(NumberGenerator.generateExpNumberList(NUM_SERVICE_TIMES, Lambda.WORKSTATION3.value));

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
