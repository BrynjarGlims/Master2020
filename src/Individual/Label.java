package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Arrays;

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

    //most likely not used

    //Intermediate values for calculation of cost
    public double[] vehicleTravelingTimes;
    public double travelValue;
    public double overtimeValue;
    public double loadValue;
    public double timeWarpValue;

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
        System.out.println("hei");
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
        for (int i = 0; i < this.labelEntries.length; i++){
            this.labelEntries[i] = new LabelEntry(i, vehicleTypeID, periodID, data, orderDistribution);
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

    public ArrayList<Integer> getVehicleAssignmentList(){
        return vehicleAssigment;
    }


}

/*
public class SortByCost implements Comparator<LabelEntry> {
    @Override
    public int compare(LabelEntry e1, LabelEntry e2){
        return e1.vehicleCost > e2.vehicleCost;
    }


}

 */
