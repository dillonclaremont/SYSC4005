package statistics;

import java.io.InputStream;
import java.util.ArrayList;

public class NumberGenerator {

    /**
     * Generates a random number from an exponential distribution
     * @return Generated random number
     */
    public static Double generateExpNumber(Double lambda){
        return Math.log(1-Math.random())/(-lambda);
    }

    /**
     * Generates an ArrayList of random numbers from an exponential distribution
     * @param numbersToGenerate Size of ArrayList to generate
     * @return ArrayList of generated random numbers
     */
    public static ArrayList<Double> generateExpNumberList(int numbersToGenerate, Double lambda){
        ArrayList<Double> generatedNumbers = new ArrayList<>();
        for (int i = 0; i < numbersToGenerate; i++){
            generatedNumbers.add(generateExpNumber(lambda)*60);
        }
        return generatedNumbers;
    }

    /**
     * Calculates the mean of a given ArrayList
     * @param items ArrayList to calculate mean of
     * @return The mean of the ArrayList
     */
    private static Double calculateAverage(ArrayList <Double> items) {
        if(!items.isEmpty()) {
            Double sum = 0.0;
            for (Double item : items) {
                sum += item;
            }
            return sum / items.size();
        }
        return null;
    }

    /**
     * Read service times from input files. Convert to seconds (integers)
     * @param filename path of file to read service times from
     * @return ArrayList of service times
     */
    public static ArrayList<Double> readServiceTimeFile (String filename){
        ArrayList<Double> serviceTimes = new ArrayList<Double>();
        try {
            ClassLoader classLoader = NumberGenerator.class.getClassLoader();
            InputStream is = classLoader.getResourceAsStream(filename);

            //Read File Content
            String[] serviceTimesStr = new String(is.readAllBytes()).split("\n");

            for (String serviceTimeStr : serviceTimesStr){
                Double serviceTimeInMinutes = new Double (serviceTimeStr);
                Double serviceTimeInSeconds = serviceTimeInMinutes * 60;
                serviceTimes.add(serviceTimeInSeconds);
            }
        } catch (Exception e){
            System.out.println(e.getStackTrace());
        }
        return serviceTimes;
    }
}