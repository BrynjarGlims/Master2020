package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.DoubleStream;

public class Label {

    //Lable values
    public double[] arcTraversalCost;
    public double loadInfeasibility;
    public Label parentNode;
    public double cost;

    //calculational value:
    public int periodID;
    public int vehicleTypeID;


    //Chromosomes
    public VehicleAssigment vehicleAssigment;
    public GiantTourSplit giantTourSplit;

    //Derived from first section of adSplit
    public ArrayList<ArrayList<Integer>> listOfTrips;
    public int tripNumber;

    //Data
    public Data data;
    public double[][] orderDistribution;  //period, customer


    //most likely not used



    //Intermediate values for calculation of cost
    public double[] vehicleTravelingTimes;
    public double travelValue;
    public double overtimeValue;
    public double loadValue;

    //create non-first labels
    public Label(Label parentNode, int vehicleID, double arcCost){

        this.arcTraversalCost = parentNode.arcTraversalCost;
        this.arcTraversalCost[vehicleID] += arcCost;
        Arrays.sort(this.arcTraversalCost);
        this.loadInfeasibility = parentNode.loadInfeasibility;
        this.orderDistribution = parentNode.orderDistribution;
        this.parentNode = parentNode;
        this.data = parentNode.data;
        this.listOfTrips = parentNode.listOfTrips;
        this.tripNumber = parentNode.tripNumber + 1;
        this.vehicleTravelingTimes = new double[Parameters.numberOfVehicles];
        this.vehicleAssigment = parentNode.vehicleAssigment;
        this.vehicleAssigment.addVehicle(vehicleID);


        this.deriveCost();
    }


    //create first label
    public Label(int numberOfVehicles, int arcCost, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution ){

        //generate first travel cost object
        this.arcTraversalCost = new double[numberOfVehicles];
        this.arcTraversalCost[0] = arcCost;

        //assign first trip
        this.vehicleAssigment = new VehicleAssigment();
        this.vehicleAssigment.chromosome.add(0);  // add vehicle id 0 to the chromosome
        this.vehicleTravelingTimes = new double[Parameters.numberOfVehicles];


        //initialization
        this.loadInfeasibility = 0;
        this.parentNode = null;
        this.data = data;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.giantTourSplit = new GiantTourSplit();

        this.travelValue = 0;
        this.overtimeValue = 0;
        this.loadValue = 0;
        this.deriveCost();
    }


    public double[] getArcTraversalCost() {
        return arcTraversalCost;
    }

    public void deriveCost() {
        calculateTravelValue();  //must be used before calculateOvertimeValue
        calculateOvertimeValue();
        calculateLoadValue();
        this.cost = travelValue + overtimeValue + loadValue;
    }

    public void calculateTravelValue(){ //implement this with overtime calculation


        int lastCustomerID = -1;
        int customerCounter = 0;
        int tripCounter = 0;

        for (ArrayList<Integer> trip : listOfTrips){
            if (tripCounter > tripNumber){
                break;
            }
            for(int customerID : trip){
                if (customerCounter == 0){

                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(tripCounter)] +=
                            data.distanceMatrix[data.numberOfCustomers][customerID];
                    lastCustomerID = customerID;
                    customerCounter++;

                }
                else if (customerCounter == trip.size()-1){
                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(tripCounter)] +=
                            data.distanceMatrix[customerID][data.numberOfCustomers];
                }
                else {
                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(tripCounter)] +=
                            data.distanceMatrix[lastCustomerID][customerID];
                    lastCustomerID = customerID;
                    customerCounter++;

                }

            }
            tripCounter++;

        }
        this.travelValue = DoubleStream.of(vehicleTravelingTimes).sum();

    }


    public void calculateOvertimeValue(){

        for (double travelTime : vehicleTravelingTimes) {
            overtimeValue += Math.max(0, travelTime - Parameters.maxJourneyDuration);
        }
        overtimeValue *= Parameters.initialOvertimePenalty;
    }

    public void  calculateLoadValue(){


        double tempQuantity = 0;
        for (ArrayList<Integer> trip : listOfTrips){
            for(int customerID : trip) {
                tempQuantity += orderDistribution[periodID][customerID];
            }
            loadValue += Math.max(0 , tempQuantity - data.vehicleTypes[vehicleTypeID].capacity);
            tempQuantity = 0;
        }
        loadValue *= Parameters.initialCapacityPenalty;
    }

    public void setVehicleAssignment(VehicleAssigment vehicleAssignment){
        this.vehicleAssigment = vehicleAssignment;
    }

    public void setGiantTourSplit(GiantTourSplit giantTourSplit){
        this.giantTourSplit = giantTourSplit;
    }




}
