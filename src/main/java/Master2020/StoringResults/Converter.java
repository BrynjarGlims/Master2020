package StoringResults;

import DataFiles.*;
import Individual.Individual;
import Individual.Trip;

import java.util.ArrayList;
import java.util.List;

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

    public static String formatList(List<Integer> customerSequence){
        String returnString = "[ ";
        for (int customerID: customerSequence){
            returnString += customerID + " ";
        }
        returnString += "]";
        return returnString;
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

    public static String calculateTotalOrderVolume(Customer c) {
        double orderVolume = 0;
        for (Order order : c.orders) {
            orderVolume += order.volume;
        }
        return String.format(" %.2f", orderVolume);
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

    public static String findTimeWindowToCustomers(ArrayList<Integer> customers, Data data, int period){
        String returnString = "";
        for(int customerID : customers){
            returnString += Converter.convertTimeWindow(data.customers[customerID].timeWindow[period][0], data.customers[customerID].timeWindow[period][1]);
            if (! (customerID == customers.get(customers.size()-1)))
                returnString += " ---> ";
        }
        return returnString;
    }

    public static double calculateDistance(double xCoordinateFrom, double xCoordinateTo, double yCoordinateFrom, double yCoordinateTo) {
        return Math.sqrt(Math.pow(xCoordinateFrom - xCoordinateTo, 2)
                + Math.pow(yCoordinateFrom - yCoordinateTo, 2)) * Parameters.scalingDistanceParameter;
    }

    public static String findNumberOfTrips(Vehicle v, Individual individual){
        int numberOfTrips = 0;
        for (int p = 0; p < individual.data.numberOfPeriods; p++){
            for (Trip t : individual.tripList[p][v.vehicleType.vehicleTypeID]){
                if (t.vehicleID == v.vehicleID){
                    numberOfTrips++;
                }
            }
        }
        return Integer.toString(numberOfTrips);
    }

    public static String findNumberOfDays(Vehicle v, Individual individual){
        int numberOfDays = 0;
        for (int p = 0; p < individual.data.numberOfPeriods; p++){
            for (Trip t : individual.tripList[p][v.vehicleType.vehicleTypeID]){
                if (t.vehicleID == v.vehicleID){
                    numberOfDays++;
                    break;
                }
            }
        }
        return Integer.toString(numberOfDays);
    }

    public static String calcualteWaitingTime(Trip t, Data data){
        double totalWaitingTime = 0;
        for (int customerID : t.customers){
            return ""; // TODO: 18/03/2020 To be implementerd
        }
        return "";
    }

    public static String getTripNumber(Trip t, Individual individual ){
        int tripNumber = 0;
        for(Trip trip : individual.tripList[t.period][t.vehicleType]){
            if (trip.vehicleID == t.vehicleID){
                tripNumber++;
            }
            if (trip.equals(t)){
                break;
            }
        }
        return Integer.toString(tripNumber);
    }

    public static String getStartingTimeForTrip(Trip t, Data data){
        double returnValue =  Math.max(0, Parameters.timeShift + data.customers[t.customers.get(0)].timeWindow[t.period][0] - Converter.calculateDistance(data.customers[t.customers.get(0)].xCoordinate, data.depot.xCoordinate,
                data.customers[t.customers.get(0)].yCoordinate, data.depot.yCoordinate));
        return String.format("%02d:%02d", (int) (returnValue), (int) ((60*(returnValue)) % 60));
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
