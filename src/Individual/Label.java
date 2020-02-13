package Individual;
import DataFiles.*;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

public class Label {

    //Lable values
    public double[] vehicleTravelCost;
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
    public Label(Label parentNode){

        this.vehicleTravelCost = parentNode.vehicleTravelCost;
        this.loadInfeasibility = parentNode.loadInfeasibility;
        this.parentNode = parentNode;
        this.data = parentNode.data;
        this.listOfTrips = parentNode.listOfTrips;
        this.tripNumber = parentNode.tripNumber + 1;
        this.deriveCost();
    }


    //create first label
    public Label(int numberOfVehicles, int arcCost, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution ){
        this.createInitialVehicleArray(numberOfVehicles, arcCost);
        this.loadInfeasibility = 0;
        this.parentNode = null;
        this.data = data;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.vehicleAssigment = new VehicleAssigment();
        this.giantTourSplit = new GiantTourSplit();

        this.travelValue = 0;
        this.overtimeValue = 0;
        this.loadValue = 0;



        this.deriveCost();
    }

    public void createInitialVehicleArray(int numberOfVehicles, int arcCost){
        this.vehicleTravelCost = new double[numberOfVehicles];
        this.vehicleTravelCost[0] = arcCost;
    }

    public double[] getVehicleTravelCost() {
        return vehicleTravelCost;
    }

    public void deriveCost() {
        calculateTravelValue();  //must be used before calculateOvertimeValue
        calculateOvertimeValue();
        calculateLoadValue();
        this.cost = travelValue + overtimeValue + loadValue;
    }

    public void calculateTravelValue(){ //implement this with overtime calculation

        vehicleTravelingTimes = new double[Parameters.numberOfVehicles];

        int lastCustomerID = -1;
        int counter = 0;

        for (ArrayList<Integer> trip : listOfTrips){
            for(int customerID : trip){
                if (counter == 0){
                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(counter)] +=
                            data.distanceMatrix[data.numberOfCustomers][customerID];
                    lastCustomerID = customerID;
                    counter++;
                }
                else if (counter == trip.size()-1){
                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(counter)] +=
                            data.distanceMatrix[customerID][data.numberOfCustomers ];
                }
                else {
                    vehicleTravelingTimes[vehicleAssigment.chromosome.get(counter)] +=
                            data.distanceMatrix[lastCustomerID][customerID];
                    lastCustomerID = customerID;
                    counter++;
                }

            }

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
