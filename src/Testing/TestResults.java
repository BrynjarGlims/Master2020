package Testing;

import DataFiles.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestResults {
    public static Data data;


    public static void evaluateSolution(String path){
        data = DataReader.loadData();
        List<String[]> tripData = readCSVFile(path + "/trip.csv");
        List<String[]> ordersData = readCSVFile(path + "/orders.csv");
        List<String[]> vehicleData = readCSVFile(path + "/vehicle.csv");


        double travelCost = findObjectiveValuesOfTrip(tripData);
        double vehicleCost = findObjectiveValuesOfVehicle(vehicleData);
        double overtimeAtDepotCost = findObjectiveValuesOfOrders(ordersData);
        double objectiveValue = travelCost + vehicleCost + overtimeAtDepotCost;
        System.out.println("Objective value: " + objectiveValue);
        System.out.println("Travel Cost: " + travelCost);
        System.out.println("Vehicle Cost: " + vehicleCost );
        System.out.println("Overtime at depot cost: " + overtimeAtDepotCost);

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
            overtimeAtDepot += Math.max(0, volume[p] - Parameters.overtimeLimit[p])*Parameters.overtimeCost[p];
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
        System.out.println("Results from GA:");
        String path = "results/results_detailed/bestGA";
        evaluateSolution(path);
        System.out.println(" ----- ");
        System.out.println("Results from MIP");
        path = "results/results_detailed/Test_MIP";
        evaluateSolution(path);
    }



}
