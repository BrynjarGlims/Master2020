package Individual;

import DataFiles.Data;

import java.util.ArrayList;
import java.util.Arrays;

import DataFiles.*;


public class LabelEntry implements Comparable<LabelEntry> {

    //Lable values
    public int vehicleID;
    public int accumulatedTravelCost;
    public ArrayList<Integer> tripAssigment;

    public double vehicleTotalTravelTime;
    public double currentVehicleTime;
    public double loadInfeasibility;
    public double timeWarpInfeasibility;
    public double vehicleCost;  //this is the cost vidal uses
    public double vehicleDrivingDistance; // this is driving distance without waiting

    public int periodID;
    public int vehicleTypeID;

    public boolean inUse;



    //Data
    public Data data;
    public double[][] orderDistribution;  //period, customer




    public LabelEntry(int vehicleID, int vehicleTypeID, int periodID, Data data, double[][] orderDistribution){
        //data loading
        this.data = data;
        this.orderDistribution = orderDistribution;
        this.vehicleID = vehicleID;
        this.accumulatedTravelCost = 0;
        this.tripAssigment = new ArrayList<Integer>();
        this.vehicleTotalTravelTime = 0;
        this.vehicleDrivingDistance = 0;
        this.currentVehicleTime = 0;
        this.loadInfeasibility = 0;
        this.timeWarpInfeasibility = 0;
        this.vehicleCost = 0;
        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.inUse = false;

    }

    private LabelEntry(LabelEntry parentLabelEntry){

        // TODO: 02.03.2020 Expensive call, discuss if it can be simplified.
        this.tripAssigment = cloneTripAssignment(parentLabelEntry.tripAssigment);


        this.data = parentLabelEntry.data;
        this.orderDistribution = parentLabelEntry.orderDistribution;
        this.vehicleID = parentLabelEntry.vehicleID;
        this.accumulatedTravelCost = parentLabelEntry.accumulatedTravelCost;
        this.vehicleTotalTravelTime = parentLabelEntry.vehicleTotalTravelTime;
        this.vehicleDrivingDistance = parentLabelEntry.vehicleDrivingDistance;
        this.currentVehicleTime = parentLabelEntry.currentVehicleTime;
        this.loadInfeasibility = parentLabelEntry.loadInfeasibility;
        this.timeWarpInfeasibility = parentLabelEntry.timeWarpInfeasibility;
        this.vehicleCost = parentLabelEntry.vehicleCost;
        this.vehicleTypeID = parentLabelEntry.vehicleTypeID;
        this.periodID = parentLabelEntry.periodID;
        this.inUse = parentLabelEntry.inUse;
    }


    private ArrayList<Integer> cloneTripAssignment(ArrayList<Integer> prevTripAssignment){
        ArrayList<Integer> tripAssignment = new ArrayList<Integer>();
        for (Integer trips : prevTripAssignment){
            tripAssignment.add(trips);
        }
        return tripAssignment;
    }


    public LabelEntry copyLabelEntry(){
        return new LabelEntry(this);
    }





    public void updateLabelEntryValues(ArrayList<Integer> customers, int tripIndex){
        this.inUse = true;
        this.tripAssigment.add(tripIndex);
        this.updateTravelTime(customers);
        this.updateLoadInfeasibility(customers);
        this.updateTimeWarp(customers);
    }


    private void updateTimeWarp(ArrayList<Integer> customers){ //todo: may be combined with the other traveling calculation

        //if second trip, add loading time at depot
        if (this.vehicleTotalTravelTime > 0){
            this.vehicleTotalTravelTime += data.vehicleTypes[vehicleTypeID].loadingTimeAtDepot;
        }

        boolean fromDepot = true;
        int lastCustomerID = -1;

        //todo: make more readable

        for (int customerID : customers){
            if (fromDepot){  //depot to customer
                currentVehicleTime = Math.max(currentVehicleTime + data.distanceMatrix[data.numberOfCustomers][customerID],
                data.customers[customerID].timeWindow[periodID][0]);
                if (currentVehicleTime > data.customers[customerID].timeWindow[periodID][1]){
                    timeWarpInfeasibility +=  currentVehicleTime - data.customers[customerID].timeWindow[periodID][1];
                    currentVehicleTime = data.customers[customerID].timeWindow[periodID][1];
                }
                lastCustomerID = customerID;
                fromDepot = false;
            }
            else{  //Case where one goes from customer to customer
                currentVehicleTime = Math.max(currentVehicleTime + data.customers[customerID].totalUnloadingTime +data.distanceMatrix[lastCustomerID][customerID],
                        data.customers[customerID].timeWindow[periodID][0]);
                if (currentVehicleTime > data.customers[customerID].timeWindow[periodID][1]){
                    timeWarpInfeasibility +=  currentVehicleTime - data.customers[customerID].timeWindow[periodID][1];
                    currentVehicleTime = data.customers[customerID].timeWindow[periodID][1];
                }
                lastCustomerID = customerID;
            }
        }
        currentVehicleTime = currentVehicleTime + data.customers[lastCustomerID].totalUnloadingTime+
                data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        if (currentVehicleTime > Parameters.maxJourneyDuration){
            timeWarpInfeasibility += currentVehicleTime - Parameters.maxJourneyDuration;
            currentVehicleTime = Parameters.maxJourneyDuration;
        }


    }


    private void updateLoadInfeasibility(ArrayList<Integer> customers){
        double tripLoad = 0;
        for (int customerID : customers){
            tripLoad += orderDistribution[periodID][customerID];
        }
        this.loadInfeasibility += Math.max(0, tripLoad - data.vehicleTypes[vehicleTypeID].capacity);
    }



    private void updateTravelTime(ArrayList<Integer> customers){

        //if second trip, add loading time at depot
        if (this.vehicleTotalTravelTime > 0){
            this.vehicleTotalTravelTime += data.vehicleTypes[vehicleTypeID].loadingTimeAtDepot;
        }

        //initialize
        boolean fromDepot = true;
        int lastCustomerID = -1;

        //three cases, from depot to cust, cust to cust, cust to depot

        for ( int customerID : customers){
            if (fromDepot){
                vehicleTotalTravelTime +=
                        data.distanceMatrix[data.numberOfCustomers][customerID] + data.customers[customerID].totalUnloadingTime;
                vehicleDrivingDistance = data.distanceMatrix[data.numberOfCustomers][customerID];
                lastCustomerID = customerID;
                fromDepot = false;
            }
            else {
                vehicleTotalTravelTime +=
                        data.distanceMatrix[lastCustomerID][customerID] + data.customers[customerID].totalUnloadingTime;
                vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][customerID];
                lastCustomerID = customerID;

            }
        }
        vehicleTotalTravelTime +=
                data.distanceMatrix[lastCustomerID][data.numberOfCustomers];
        vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][data.numberOfCustomers];

    }



    public double getTravelTimeValue(){
        return vehicleTotalTravelTime;
    }

    public double getDrivingDistance(){
        return vehicleDrivingDistance;
    }


    public double getTimeWarpInfeasibility(){
        return timeWarpInfeasibility ;
    }

    public double getLoadInfeasibility(){
        return loadInfeasibility ;
    }

    public double getOvertimeValue() {
        return 0; // TODO: 25/03/2020 Remove in a propper way 
        //return Math.max(0, vehicleTotalTravelTime - Parameters.maxJourneyDuration);
    }

    public String toString(){
        String string = "LabelEntry vehicle ID: " + vehicleID + " with cost " + vehicleCost + " \n ";

        /*/todo:fix

        for (ArrayList<Integer> trips :tripAssigment){
            string += "trip: ";
            for (int customer : trips){
                string += customer + " ";
            }
            string += "\n ";
        }

         */
        return string;
    }

    @Override
    public int compareTo(LabelEntry labelEntry) {
        if ((this.vehicleDrivingDistance - labelEntry.vehicleDrivingDistance) < 0 ) {
            return 1;
        } else {
            return -1;
        }

    }
}
