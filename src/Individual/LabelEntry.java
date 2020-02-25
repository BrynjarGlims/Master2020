package Individual;

import DataFiles.Data;

import java.util.ArrayList;
import DataFiles.*;


public class LabelEntry implements Comparable<LabelEntry> {

    //Lable values
    public int vehicleID;
    public int accumulatedTravelCost;
    public ArrayList<ArrayList<Integer>> tripAssigment;

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
        this.tripAssigment = new ArrayList<ArrayList<Integer>>();
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

    public void updateArcCost(double arcCost, ArrayList<Integer> customers){
        this.inUse = true;
        this.tripAssigment.add(customers);
        this.updateTravelTime(customers);
        this.updateLoadInfeasibility(customers);
        this.updateTimeWarp(customers);
        this.vehicleCost += arcCost;
    }


    private void updateTimeWarp(ArrayList<Integer> customers){ //todo: may be combined with the other traveling calculation

        //if second trip, add loading time at depot
        if (this.vehicleTotalTravelTime > 0){
            this.vehicleTotalTravelTime += data.vehicleTypes[vehicleTypeID].loadingTimeAtDepot;
        }

        int customerCounter = 0;
        int lastCustomerID = -1;

        //todo: make more readable

        for (int customerID : customers){
            if (customerCounter == 0){  //depot to customer
                currentVehicleTime = Math.max(currentVehicleTime + data.distanceMatrix[data.numberOfCustomers][customerID],
                data.customers[customerID].timeWindow[periodID][0]);
                if (currentVehicleTime > data.customers[customerID].timeWindow[periodID][1]){
                    timeWarpInfeasibility +=  currentVehicleTime - data.customers[customerID].timeWindow[periodID][1];
                    currentVehicleTime = data.customers[customerID].timeWindow[periodID][1];
                }
                lastCustomerID = customerID;

            }
            else if (customerCounter == customers.size()-1){ //customer to depot
                currentVehicleTime = currentVehicleTime + data.customers[customerID].totalUnloadingTime+
                        data.distanceMatrix[customerID][data.numberOfCustomers];
                if (currentVehicleTime > Parameters.maxJourneyDuration){
                    timeWarpInfeasibility += currentVehicleTime - Parameters.maxJourneyDuration;
                    currentVehicleTime = Parameters.maxJourneyDuration;
                }
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

    };


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
        int customerCounter = 0;
        int lastCustomerID = -1;

        //three cases, from depot to cust, cust to cust, cust to depot
        for ( int customerID : customers){
            if (customerCounter == 0){
                vehicleTotalTravelTime +=
                        data.distanceMatrix[data.numberOfCustomers][customerID];
                vehicleDrivingDistance = data.distanceMatrix[data.numberOfCustomers][customerID];
                lastCustomerID = customerID;
                customerCounter++;
            }
            else if (customerCounter == customers.size()-1){
                vehicleTotalTravelTime +=
                        data.distanceMatrix[customerID][data.numberOfCustomers];
                vehicleDrivingDistance += data.distanceMatrix[customerID][data.numberOfCustomers];
            }
            else {
                vehicleTotalTravelTime +=
                        data.distanceMatrix[lastCustomerID][customerID];
                vehicleDrivingDistance += data.distanceMatrix[lastCustomerID][customerID];
                lastCustomerID = customerID;
                customerCounter++;
            }
        }
    }



    public double getTravelTimeValue(){
        return vehicleTotalTravelTime;
    }

    public double getDrivingDistance(){
        return vehicleDrivingDistance;
    }


    public double getTimeWarpInfeasibility(){
        return timeWarpInfeasibility;
    }

    public double getLoadInfeasibility(){
        return loadInfeasibility;
    }

    public double getOvertimeValue() {
        return Math.max(0, vehicleTotalTravelTime - Parameters.maxJourneyDuration);
    }

    @Override
    public int compareTo(LabelEntry labelEntry) {
        if ((this.vehicleCost - labelEntry.vehicleCost) < 0 ) {
            return 1;
        } else {
            return -1;
        }

    }
}
