package Master2020.Testing;

import Master2020.DataFiles.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestResults {
    public static Data data;


    public static void evaluateSolution(String path, String name){
        data = DataReader.loadData();
        List<String[]> tripData = readCSVFile(path + "/" + name + "_trip.csv");
        List<String[]> ordersData = readCSVFile(path + "/" + name + "_orders.csv");
        List<String[]> vehicleData = readCSVFile(path + "/" + name + "_vehicle.csv");


        double travelCost = findObjectiveValuesOfTrip(tripData);
        double vehicleCost = findObjectiveValuesOfVehicle(vehicleData);
        double overtimeAtDepotCost = findObjectiveValuesOfOrders(ordersData);
        double objectiveValue = travelCost + vehicleCost + overtimeAtDepotCost;
        double timeWarpValue = findTimeWarpValue(tripData);
        double overloadValue = findOverLoadValue(tripData, ordersData);
        System.out.println("Objective value: " + objectiveValue);
        System.out.println("Fitness ink inf: " + (objectiveValue + timeWarpValue + overloadValue));
        System.out.println("Travel Cost: " + travelCost);
        System.out.println("Vehicle Cost: " + vehicleCost );
        System.out.println("Overtime at depot cost: " + overtimeAtDepotCost);
        System.out.println("TimeWarp cost: " + timeWarpValue);
        System.out.println("Overload cost: " + overloadValue);

    }

    private static double findObjectiveValuesOfTrip(List<String[]> tripData){
        double travelValue = 0;
        int previousCustomer;

        for(int line = 0; line < tripData.size(); line++){
            previousCustomer  = data.numberOfCustomers;
            ArrayList<Integer> customerSequence = createListFromCustomers(tripData.get(line)[9]);
            for (int c : customerSequence){
                travelValue += data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.travelCost *
                    data.distanceMatrix[previousCustomer][c];
                previousCustomer = c;
            }
            travelValue += data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.travelCost *
                                     data.distanceMatrix[previousCustomer][data.numberOfCustomers];
        }
        return travelValue;
    }

    private static double findTimeWarpValue(List<String[]> tripData){
        //if second trip, add loading time at depot
        double vehicleTotalTravelTime = 0;
        double timeWarpInfeasibility = 0;

        for (int line = 0; line < tripData.size(); line++) {  // given that vehicles get in order
            if (line != 0){
                if (tripData.get(line - 1)[3].equals(tripData.get(line)[3]) &&
                        tripData.get(line - 1)[2].equals(tripData.get(line)[2]) &&
                        Integer.parseInt(tripData.get(line - 1)[1]) + 1 == Integer.parseInt(tripData.get(line)[1]))
                {
                    vehicleTotalTravelTime += data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.loadingTimeAtDepot;
                }else{
                    vehicleTotalTravelTime = 0;
                }
            }

            ArrayList<Integer> customers = createListFromCustomers(tripData.get(line)[9]);

            //initialize
            int previousCustomer = data.numberOfCustomers;

            //three cases, from depot to cust, cust to cust, cust to depot
            for (int customerID : customers) {
                vehicleTotalTravelTime += data.distanceMatrix[previousCustomer][customerID];
                vehicleTotalTravelTime = Math.max(vehicleTotalTravelTime, data.customers[customerID].timeWindow[getDayFromString(tripData.get(line)[2])][0]);
                if (vehicleTotalTravelTime > data.customers[customerID].timeWindow[getDayFromString(tripData.get(line)[2])][1]) {
                    timeWarpInfeasibility += vehicleTotalTravelTime - data.customers[customerID].timeWindow[getDayFromString(tripData.get(line)[2])][1];
                    vehicleTotalTravelTime = data.customers[customerID].timeWindow[getDayFromString(tripData.get(line)[2])][1];
                }
                vehicleTotalTravelTime += data.customers[customerID].totalUnloadingTime;
                previousCustomer = customerID;
            }

            vehicleTotalTravelTime += data.distanceMatrix[previousCustomer][data.numberOfCustomers];
            if (vehicleTotalTravelTime > Parameters.maxJourneyDuration) {
                timeWarpInfeasibility += vehicleTotalTravelTime - Parameters.maxJourneyDuration;
                vehicleTotalTravelTime = Parameters.maxJourneyDuration;
            }
        }
        return timeWarpInfeasibility*Parameters.initialTimeWarpPenalty;
    }

    private static double findOverLoadValue(List<String[]> tripData, List<String[]> ordersData) {
        double overloadInfeasibility = 0;
        double volumeOfTrip;
        for (int line = 0; line < tripData.size(); line++) {  // given that vehicles get in order
            ArrayList<Integer> customers = createListFromCustomers(tripData.get(line)[9]);
            //initialize

            volumeOfTrip = 0;
            //three cases, from depot to cust, cust to cust, cust to depot
            for (int customerID : customers) {
                for (int line2 = 0; line2 < ordersData.size(); line2++) {  // given that vehicles get in order
                    if (ordersData.get(line2)[4].equals(tripData.get(line)[2]) &&
                            Integer.parseInt(ordersData.get(line2)[5]) == customerID &&
                            ordersData.get(line2)[7].equals(tripData.get(line)[3])){
                        volumeOfTrip += Double.parseDouble(ordersData.get(line2)[3]);
                    }
                }
            }
            if (0 < volumeOfTrip - data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.capacity){
                System.out.println("Line " + line + " incurres overload of " + Double.toString(volumeOfTrip - data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.capacity));
                System.out.println("On day: " + tripData.get(line)[2]  + " on tripnumber " + tripData.get(line)[1]  );

            }
            overloadInfeasibility += Math.max(0, volumeOfTrip - data.vehicles[Integer.parseInt(tripData.get(line)[3])].vehicleType.capacity) * Parameters.initialCapacityPenalty;
        }

        return overloadInfeasibility;
    }

    private static double findObjectiveValuesOfVehicle(List<String[]> vehicleData){
        double vehicleUseCost = 0;

        for(int line = 0; line < vehicleData.size(); line++){
            vehicleUseCost += data.vehicleTypes[Integer.parseInt(vehicleData.get(line)[3])].usageCost*Integer.parseInt(vehicleData.get(line)[10]);
        }
        return vehicleUseCost;
    }

    private static double findObjectiveValuesOfOrders(List<String[]> ordersData){
        double[] volume = new double[data.numberOfPeriods];
        double overtimeAtDepot = 0;

        for(int line = 0; line < ordersData.size(); line++){
            int day = getDayFromString(ordersData.get(line)[4]);
            volume[getDayFromString(ordersData.get(line)[4])] = Double.parseDouble(ordersData.get(line)[3]);
        }

        for (int p = 0; p < data.numberOfPeriods; p++){
            overtimeAtDepot += Math.max(0, volume[p] - Data.overtimeLimit[p])*Parameters.overtimeCost[p];
        }
        return overtimeAtDepot;
    }

    private static int getDayFromString(String str){
        switch (str){
            case "Monday":
                return 0;
            case "Tuesday":
                return 1;
            case "Wednesday":
                return 2;
            case "Thursday":
                return 3;
            case "Friday":
                return 4;
            case "Saturday":
                return 5;
        }
        return -1;
    }

    private static ArrayList<Integer> createListFromCustomers(String customers){
        ArrayList<Integer> customerList = new ArrayList<>();
        for (String str : customers.split(" ")){
            if (str.equals("[") || str.equals("]")){
                continue;
            }
            customerList.add(Integer.parseInt(str));
        }
        return customerList;
    }

    private static List<String[]> readCSVFile(String file) {
        List<String[]> content = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                content.add(line.split(","));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found exception in readCSVFile");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void main(String[] args){


        for (int i = 0; i < 1; i++) {

            Parameters.randomSeedValue = 10 + i;

            System.out.println("RANDOM SEED VALUE: " + Parameters.randomSeedValue);
            System.out.println("Results from PGA:");
            String name = "PGA_S" + Parameters.randomSeedValue + "_C5_V5_04_05_2020";
            String path = "results/results_detailed/" + name;
            evaluateSolution(path, name);
            System.out.println(" ----- ");

            /*
            Parameters.randomSeedValue = 10 + i;
            System.out.println("Results from AFM");
            name = "AFM_S" + Parameters.randomSeedValue + "_C5_V5_04_05_2020";
            path = "results/results_detailed/" + name;
            evaluateSolution(path, name);
            System.out.println(" ");
            System.out.println(" ############ ");

             */


        }
    }


}
