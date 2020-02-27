package Individual;
import DataFiles.*;
import scala.util.regexp.WordExp;


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
    public Label(Label parentLabel, int vehicleIndex, double arcCost){


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
        this.labelEntries[vehicleIndex].updateArcCost(arcCost, listOfTrips.get(tripNumber));

        this.sortLabelEntries();
        this.deriveLabelCost();

        /*
        System.out.println(" --------------  New label  ---------------");
        System.out.print("TimeWarpCost: " + fleetTimeWarp + "\n");
        for(LabelEntry le : labelEntries){
            System.out.print(le.toString());
        }

         */
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

    //generate empty label
    public Label(Data data, int tripNumber, double[][] orderDistribution, int periodID,
                 int vehicleTypeID){
        this.vehicleTypeID = vehicleTypeID;
        this.periodID = periodID;
        this.data = data;
        this.parentLabel = null;
        this.tripNumber = tripNumber;
        this.orderDistribution = orderDistribution;
        this.isEmptyLabel = true;

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
        this.isEmptyLabel = false;


        //labelEntries generated
        this.labelEntries = new LabelEntry[numberOfVehicles];
        this.initializeLabelEntries(periodID, vehicleTypeID);
        this.labelEntries[0].updateArcCost(arcCost, listOfTrips.get(tripNumber));





        //derive cost
        this.deriveLabelCost();

        /*
        System.out.println(" --------------  New label (first label)  ---------------");
        System.out.print("TimeWarpCost: " + fleetTimeWarp + "\n");
        for(LabelEntry le : labelEntries){
            System.out.print(le.toString());
        }

         */
    }

    private void initializeLabelEntries(int periodID, int vehicleTypeID){
        int i = 0;
        for ( int vehicleID : data.vehicleTypes[this.vehicleTypeID].vehicleSet ){
            this.labelEntries[i] = new LabelEntry(vehicleID, vehicleTypeID, periodID, data, orderDistribution);
            i++;
        }
    }


    public void deriveLabelCost() {  //todo: implement for new structure
        calculateTravelValue();  //must be used before calculateOvertimeValue
        calculateOvertimeValue();
        calculateLoadValue();
        calculateTimeWarp();
        this.costOfLabel = fleetTravelTime + fleetOvertime + fleetOverLoad + fleetTimeWarp;
    }

    public void calculateTimeWarp(){
        fleetTimeWarp = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetTimeWarp += labelEntry.getTimeWarpInfeasibility();
        }

        fleetTimeWarp *= Parameters.initialTimeWarpPenalty;

    }


    public void calculateTravelValue(){ //implement this with overtime calculation

        fleetTravelTime = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetTravelTime += labelEntry.getTravelTimeValue();
        }

    }


    public void calculateOvertimeValue(){

        fleetOvertime = 0;
        for (LabelEntry labelEntry : this.labelEntries){
            fleetOvertime += labelEntry.getOvertimeValue();
        }
        fleetOvertime *= Parameters.initialOvertimePenalty;


    }

    public void calculateLoadValue(){

        fleetOverLoad = 0;

        for (LabelEntry labelEntry : this.labelEntries){
            fleetOverLoad += labelEntry.getLoadInfeasibility();
        }

        fleetOverLoad *= Parameters.initialCapacityPenalty;

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

    public double getLabelDrivingDistance(){
        double drivingDistance = 0;
        for (LabelEntry labelEntry : labelEntries){
            drivingDistance += labelEntry.getDrivingDistance();
        }
        return drivingDistance;
    }

    public int getNumberOfVehicles() {
        int numberOfVehicles = 0;
        for (LabelEntry labelEntry : labelEntries) {
            if (labelEntry.inUse)
                numberOfVehicles += 1;
        }
        return numberOfVehicles;
    }


    public double getTimeWarpInfeasibility() { return fleetTimeWarp; }

    public double getLoadInfeasibility() { return fleetOverLoad; }

    public double getOvertimeInfeasibility(){ return fleetOvertime; }
}




