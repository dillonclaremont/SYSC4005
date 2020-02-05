import entities.Entity;
import entities.Inspector;
import entities.WorkBench;
import globals.Component;
import globals.EntityState;
import globals.Product;

import java.io.InputStream;
import java.util.ArrayList;

public class SimulationDriver {
    private static final int WORKBENCH_COMPONENT_BUFFER_SIZE = 2;
    private static final int CLOCK_INCREMENT_SIZE = 1;

    /**
     * Simulation Driver.
     * @param args
     */
    public static void main(String args[]){
        ArrayList<Entity> entities = init();
        boolean running = true;

        while (running) {
            for (Entity entity : entities) {
                if (entity.getState() != EntityState.DONE) {
                    entity.clockUpdate(CLOCK_INCREMENT_SIZE);
                } else {
                    running = false;
                    System.out.println("Helo");
                }
            }
        }


        System.out.println("Hello World");

    }

    /**
     * Initialize all components.
     *
     * @return
     */
    private static ArrayList<Entity> init(){
        ArrayList<Entity> entities = new ArrayList<Entity>();

        WorkBench workbenchOne = new WorkBench("W1", Product.P1, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchOne.registerComponent(Component.C1);
        workbenchOne.setServiceTimes(readServiceTimeFile("resources/ws1.dat"));

        WorkBench workbenchTwo = new WorkBench("W2", Product.P2, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchTwo.registerComponent(Component.C1);
        workbenchTwo.registerComponent(Component.C2);
        workbenchTwo.setServiceTimes(readServiceTimeFile("resources/ws2.dat"));

        WorkBench workbenchThree = new WorkBench("W3", Product.P3, WORKBENCH_COMPONENT_BUFFER_SIZE);
        workbenchThree.registerComponent(Component.C1);
        workbenchThree.registerComponent(Component.C3);
        workbenchThree.setServiceTimes(readServiceTimeFile("resources/ws3.dat"));

        Inspector inspectorOne = new Inspector("I1");
        inspectorOne.registerComponentForWorkbench(Component.C1, workbenchOne);
        inspectorOne.registerComponentForWorkbench(Component.C1, workbenchTwo);
        inspectorOne.registerComponentForWorkbench(Component.C1, workbenchThree);
        inspectorOne.registerWorkbenchPriority(workbenchOne, 1);
        inspectorOne.registerWorkbenchPriority(workbenchTwo, 2);
        inspectorOne.registerWorkbenchPriority(workbenchThree, 3);
        inspectorOne.registerComponentServiceTimes(Component.C1, readServiceTimeFile("resources/servinsp1.dat"));

        Inspector inspectorTwo = new Inspector("I2");
        inspectorTwo.registerComponentForWorkbench(Component.C2, workbenchTwo);
        inspectorTwo.registerComponentForWorkbench(Component.C3, workbenchThree);
        inspectorTwo.registerWorkbenchPriority(workbenchTwo, 1);
        inspectorTwo.registerWorkbenchPriority(workbenchThree, 2);
        inspectorTwo.registerComponentServiceTimes(Component.C2, readServiceTimeFile("resources/servinsp22.dat"));
        inspectorTwo.registerComponentServiceTimes(Component.C3, readServiceTimeFile("resources/servinsp23.dat"));

        entities.add(inspectorOne);
        /*entities.add(inspectorTwo);
        entities.add(workbenchOne);
        entities.add(workbenchTwo);
        entities.add(workbenchThree);*/

        return entities;
    }

    /**
     * Read service times from input files. Convert to seconds (integers)
     *
     * @param filename
     * @return
     */
    private static ArrayList<Integer> readServiceTimeFile (String filename){
        ArrayList<Integer> serviceTimes = new ArrayList<Integer>();
        try {
            ClassLoader classLoader = SimulationDriver.class.getClassLoader();
            InputStream is = classLoader.getResourceAsStream(filename);

            //Read File Content
            String[] serviceTimesStr = new String(is.readAllBytes()).split("\r\n");

            for (String serviceTimeStr : serviceTimesStr){
                Double serviceTimeInMinutes = new Double (serviceTimeStr);
                Integer serviceTimeInSeconds = (int) Math.round(serviceTimeInMinutes * 60);
                serviceTimes.add(serviceTimeInSeconds);
            }
        } catch (Exception e){
            System.out.println(e.getStackTrace());
        }
        return serviceTimes;
    }
}
