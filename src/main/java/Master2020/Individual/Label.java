package Master2020.Individual;
import Master2020.DataFiles.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Label {

    //Lable values

    public Label parentLabel;
    public double costOfLabel;
    public double fleetTravelTime;
    public double fleetOvertime;
    public double fleetOverLoad;
    public double fleetTimeWarp;
    public double fleetUsageCost;
    public int numberOfVehicles;

    //calculational value:
    public int periodID;
    public int vehicleTypeID;

    //Derived from first section of adSplit
    public ArrayList<ArrayList<Integer>> listOfTrips;
    public int tripNumber;

    //Data
    public Data data;
    public double[][] orderDistribution;  //period, customer

    // LabelEntry
    public LabelEntry[] labelEntries;
    public boolean isEmptyLabel;

    //create non-first labels

    public Label(Label parentLabel, int vehicleIndex, double penaltyMultiplier){

        //Information attributes
        this.periodID = parentLabel.periodID;
        this.vehicleTypeID = parentLabel.vehicleTypeID;
        this.data = parentLabel.data;
        this.orderDistribution = parentLabel.orderDistribution;
        this.tripNumber = parentLabel.tripNumber + 1;
        this.listOfTrips = parentLabel.listOfTrips;
        this.isEmptyLabel = false;

        // Cost of choosing arcs from the SPA
        this.cloneParentLabelEntries(parentLabel.labelEntries);
        this.labelEntries[vehicleIndex].updateLabelEntryValues(listOfTrips.get(tripNumber), tripNumber);

        this.sortLabelEntries();
        this.deriveLabelCost(penaltyMultiplier);

    }


    //generate empty label
    public Label(Data data, int tripNumber, double[][] orderDistribution, int periodID, int vehicleTypeID){
        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.data = data;
        this.parentLabel = null;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.isEmptyLabel = true;
        this.labelEntries = new LabelEntry[numberOfVehicles];
//        initializeLabelEntries(periodID, vehicleTypeID);
    }


    //create first label

    public Label(int numberOfVehicles, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution, int periodID,
                 int vehicleTypeID){
        this(numberOfVehicles, data, listOfTrips, tripNumber, orderDistribution, periodID, vehicleTypeID, 1);
    }

    public Label(int numberOfVehicles, Data data,
                 ArrayList<ArrayList<Integer>> listOfTrips, int tripNumber, double[][] orderDistribution, int periodID,
                 int vehicleTypeID, double penaltyMultiplier){

        //information variables for each label
        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.data = data;
        this.parentLabel = null;
        this.listOfTrips = listOfTrips;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.isEmptyLabel = false;
        this.numberOfVehicles = 1;

        //labelEntries generated
        this.labelEntries = new LabelEntry[numberOfVehicles];
        this.initializeLabelEntries(periodID, vehicleTypeID);
        this.labelEntries[0].updateLabelEntryValues( listOfTrips.get(tripNumber), tripNumber);

        //derive cost
        this.deriveLabelCost(penaltyMultiplier);
    }

    private void initializeLabelEntries(int periodID, int vehicleTypeID){
        int i = 0;
        for ( int vehicleID : data.vehicleTypes[this.vehicleTypeID].vehicleSet ){
            this.labelEntries[i] = new LabelEntry(vehicleID, vehicleTypeID, periodID, data, orderDistribution);
            i++;
        }
    }

    private void sortLabelEntries(){
        Arrays.sort(labelEntries);
    }

    private void cloneParentLabelEntries( LabelEntry[] parentLabelEntries){
        labelEntries = new LabelEntry[parentLabelEntries.length];
        for (int i = 0; i < parentLabelEntries.length; i++){
            labelEntries[i] = parentLabelEntries[i].copyLabelEntry();
        }
    }


    private void deriveLabelCost(double penaltyMulitplier) {  //todo: implement for new structure
        calculateDrivingCost();  //must be used before calculateOvertimeValue
        calculateLoadValue(penaltyMulitplier);
        calculateTimeWarp(penaltyMulitplier);
        calculateVehicleUseValue();
        this.costOfLabel = fleetTravelTime + fleetOvertime + fleetOverLoad + fleetTimeWarp + fleetUsageCost;
    }

    private void calculateVehicleUseValue(){
        numberOfVehicles = 0;

        for (LabelEntry labelEntry : labelEntries) {
            if (labelEntry.inUse)
                numberOfVehicles += 1;
        }

        fleetUsageCost = numberOfVehicles * data.vehicleTypes[vehicleTypeID].usageCost;

    }

    public void calculateTimeWarp(double penaltyMultiplier){
        fleetTimeWarp = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetTimeWarp += labelEntry.getTimeWarpInfeasibility();
        }

        fleetTimeWarp *= penaltyMultiplier*Parameters.initialTimeWarpPenalty;

    }


    private void calculateDrivingCost(){ //implement this with overtime calculation

        fleetTravelTime = 0;
        for (LabelEntry labelEntry : this.labelEntries){
            fleetTravelTime += labelEntry.getDrivingDistance();
        }
        fleetTravelTime *= data.vehicleTypes[vehicleTypeID].travelCost;
    }



    private void calculateLoadValue(double penaltyMultiplier){

        fleetOverLoad = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetOverLoad += labelEntry.getLoadInfeasibility();
        }

        fleetOverLoad *= penaltyMultiplier*Parameters.initialCapacityPenalty;

    }


    public double getLabelDrivingDistance(){
        double drivingDistance = 0;
        for (LabelEntry labelEntry : labelEntries){
            drivingDistance += labelEntry.getDrivingDistance();
        }
        return drivingDistance;
    }

    public int getNumberOfVehicles() {
        if (numberOfVehicles == 0) {
            numberOfVehicles = 0;
            for (LabelEntry labelEntry : labelEntries) {
                if (labelEntry.inUse)
                    numberOfVehicles += 1;
            }
        }
        return numberOfVehicles;
    }


    public double getTimeWarpInfeasibility() { return fleetTimeWarp; }

    public double getLoadInfeasibility() { return fleetOverLoad; }

    public double getOvertimeInfeasibility(){ return fleetOvertime; }
}




