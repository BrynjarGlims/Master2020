package StoringResults;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.Parameters;
import Individual.Trip;

import java.util.ArrayList;

public class Converter {


    public static String periodConverter(int periodID) {
        switch (periodID) {
            case 0:
                return "Monday";
            case 1:
                return "Tuesday";
            case 2:
                return "Wednesday";
            case 3:
                return "Thursday";
            case 4:
                return "Friday";
            case 5:
                return "Saturday";
        }
        return "No day found";

    }

    public static String dividableConverter(boolean isDividable) {
        if (isDividable)
            return "dividable";
        else
            return "not dividable";
    }


    public static int getPeriod(int[] orderPeriods) {
        int counter = 0;
        for (int isPeriod : orderPeriods) {
            if (isPeriod == 1) {
                return counter;
            }
            counter++;
        }
        return -1;
    }

    public static String findCustomersFromID(ArrayList<Integer> customerIDs, Data data) {
        String customerString = "";
        for (int customerID : customerIDs) {
            customerString += data.customers[customerID].customerName + " - ";
        }
        return customerString.substring(0, customerString.length() - 3);
    }

    public static String convertTimeWindow(double startTime, double endTime) {
        if (startTime < endTime + FileParameters.indifferenceValue && startTime + FileParameters.indifferenceValue > endTime) {
            return "--------------";
        }
        return String.format("%02d:%02d", (int)((Parameters.timeShift + startTime) ), (int) ((60*(Parameters.timeShift + startTime)) % 60)) +
                 " | " + String.format("%02d:%02d", (int) (Parameters.timeShift + endTime), (int) ((60*(Parameters.timeShift + endTime)) % 60));
    }

    public static String calculateTotalOrderVolume(Customer c, Data data) {
        return "toBEImplemented";
    }


    public static String calculateTotalTripTime(Trip t, Data data) {
        ArrayList<Integer> customers = (ArrayList) t.customers;
        double vehicleTotalTravelTime = 0;
        //if second trip, add loading time at depot

        //initialize
        boolean fromDepot = true;
        int lastCustomerID = -1;

        //three cases, from depot to cust, cust to cust, cust to depot

        for ( int customerID : customers){
            if (fromDepot){
                vehicleTotalTravelTime +=
                        data.distanceMatrix[data.numberOfCustomers][customerID] + data.customers[customerID].totalUnloadingTime;
                lastCustomerID = customerID;
                fromDepot = false;
            }
            else {
                vehicleTotalTravelTime +=
                        data.distanceMatrix[lastCustomerID][customerID] + data.customers[customerID].totalUnloadingTime;
                lastCustomerID = customerID;

            }
        }
        vehicleTotalTravelTime +=
                data.distanceMatrix[lastCustomerID][data.numberOfCustomers];

        return String.format("%.1f",vehicleTotalTravelTime*60);

    }

    public static String calculateDrivingTime(Trip t, Data data) {
        ArrayList<Integer> customers = (ArrayList) t.customers;
        double vehicleDrivingDistance = 0;

        //if second trip, add loading time at depot

        //initialize
        boolean fromDepot = true;
        int lastCustomerID = -1;

        //three cases, from depot to cust, cust to cust, cust to depot

        for ( int customerID : customers){
            if (fromDepot){
                vehicleDrivingDistance = data.distanceMatrix[data.numberOfCustomers][customerID];
                lastCustomerID = customerID;
                fromDepot = false;
            }
            else {
                vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][customerID];
                lastCustomerID = customerID;

            }
        }
        vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][data.numberOfCustomers];

        return String.format("%.1f",vehicleDrivingDistance*60);

    }
}
