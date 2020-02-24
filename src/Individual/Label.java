package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Label {

    //Lable values
    public double[] arcTraversalCost;
    public double loadInfeasibility;
    public double timeWarpInfeasibility;
    public Label parentLabel;
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


    // LabelEntry

    public LabelEntry[] labelEntries;


    //create non-first labels
    public Label(Label parentLabel, int vehicleIndex, double arcCost){


        //Information attributes
        this.periodID = parentLabel.periodID;
        this.vehicleTypeID = parentLabel.vehicleTypeID;
        this.parentLabel = parentLabel;
        this.data = parentLabel.data;
        this.orderDistribution = parentLabel.orderDistribution;
        this.tripNumber = parentLabel.tripNumber + 1;
        this.listOfTrips = parentLabel.listOfTrips;


        // Cost of choosing arcs from the SPA
        this.labelEntries = parentLabel.labelEntries.clone();
        this.labelEntries[vehicleIndex].updateArcCost(arcCost, listOfTrips.get(tripNumber));
        this.sortLabelEntries();

        this.deriveLabelCost();
    }


    private void sortLabelEntries(){
        Arrays.sort(labelEntries);
    }




    //create first label
    public Label(int numberOfVehicles, double arcCost, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution, int periodID,
                 int vehicleTypeID){



        //information variables for each label
        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.data = data;
        this.parentLabel = null;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;


        //labelEntries generated
        this.labelEntries = new LabelEntry[numberOfVehicles];
        this.initializeLabelEntries(periodID, vehicleTypeID);
        this.labelEntries[0].updateArcCost(arcCost, listOfTrips.get(tripNumber));

        //derive cost
        this.deriveLabelCost();
    }

    private void initializeLabelEntries(int periodID, int vehicleTypeID){
        int i = 0;
        for ( int vehicleID : data.vehicleTypes[this.vehicleTypeID].vehicleSet ){
            this.labelEntries[i] = new LabelEntry(vehicleID, vehicleTypeID, periodID, data, orderDistribution);
            i++;
        }
    }


    public void deriveLabelCost() {  //todo: implement for new structure
        double fleetTravelTime = calculateTravelValue();  //must be used before calculateOvertimeValue
        double fleetOvertime = calculateOvertimeValue();
        double fleetOverLoad = calculateLoadValue();
        double fleetTimeWarp = calculateTimeWarp();
        this.cost = fleetTravelTime + fleetOvertime + fleetOverLoad + fleetTimeWarp;
    }

    public double calculateTimeWarp(){
        double fleetTimeWarpValue = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetTimeWarpValue += labelEntry.getTimeWarpInfeasibility();
        }

        return fleetTimeWarpValue;
    }


    public double calculateTravelValue(){ //implement this with overtime calculation

        double fleetTravelTime = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetTravelTime += labelEntry.getTravelTimeValue();
        }

        return fleetTravelTime;
    }


    public double calculateOvertimeValue(){

        double fleetOvertime = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetOvertime += labelEntry.getOvertimeValue();
        }

        return fleetOvertime;

    }

    public double calculateLoadValue(){

        double fleetOverloadValue = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetOverloadValue += labelEntry.getLoadInfeasibility();
        }

        return fleetOverloadValue;

    }


    public HashMap<Integer, Integer> getVehicleAssignmentChromosome(){
        //number of trips could be calculated here if needed

        HashMap<Integer, Integer> vehicleAssignment = new HashMap<Integer, Integer>();
        for (LabelEntry labelEntry : labelEntries) {
            for (ArrayList<Integer> customerList : labelEntry.tripAssigment){
                for (int customerID : customerList) {
                    vehicleAssignment.put(customerID, labelEntry.vehicleID);  // TODO: 24.02.2020 Change to correct vehicle id
                }
           }
        }
        return vehicleAssignment;
    }


}


