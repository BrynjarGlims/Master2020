package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public ArrayList<Integer> vehicleAssigment;


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


        this.periodID = parentNode.periodID;
        this.vehicleTypeID = parentNode.vehicleTypeID;
        this.arcTraversalCost = parentNode.arcTraversalCost.clone();
        this.arcTraversalCost[vehicleID] += arcCost;
        Arrays.sort(this.arcTraversalCost);
        reverseOrderarcTrversalCost();
        this.loadInfeasibility = parentNode.loadInfeasibility;
        this.orderDistribution = parentNode.orderDistribution;
        this.parentNode = parentNode;
        this.data = parentNode.data;
        this.listOfTrips = parentNode.listOfTrips;
        this.tripNumber = parentNode.tripNumber + 1;
        this.vehicleTravelingTimes = new double[Parameters.numberOfVehicles];
        this.vehicleAssigment = (ArrayList<Integer>) parentNode.vehicleAssigment.clone();
        this.vehicleAssigment.add(vehicleID);


        this.deriveCost();
    }

    private void reverseOrderarcTrversalCost(){
        double[] tempArray = new double[arcTraversalCost.length];
        for (int i = 0; i < arcTraversalCost.length; i ++){
            tempArray[i] = this.arcTraversalCost[arcTraversalCost.length-1-i];
        }
        arcTraversalCost = tempArray;
    }


    //create first label
    public Label(int numberOfVehicles, double arcCost, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution, int periodID,
                 int vehicleTypeID){

        //generate first travel cost object
        this.arcTraversalCost = new double[numberOfVehicles];
        this.arcTraversalCost[0] = arcCost;

        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.data = data;

        //assign first trip
        this.vehicleAssigment = new ArrayList<Integer>();
        this.vehicleAssigment.add(0);  // add vehicle id 0 to the chromosome
        this.vehicleTravelingTimes = new double[Parameters.numberOfVehicles];


        //initialization
        this.loadInfeasibility = 0;
        this.parentNode = null;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;


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
                    vehicleTravelingTimes[vehicleAssigment.get(tripCounter)] +=
                            data.distanceMatrix[data.numberOfCustomers][customerID];
                    lastCustomerID = customerID;
                    customerCounter++;

                }
                else if (customerCounter == trip.size()-1){
                    vehicleTravelingTimes[vehicleAssigment.get(tripCounter)] +=
                            data.distanceMatrix[customerID][data.numberOfCustomers];
                }
                else {
                    vehicleTravelingTimes[vehicleAssigment.get(tripCounter)] +=
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

    public ArrayList<Integer> getVehicleAssignmentList(){
        return vehicleAssigment;
    }






}
