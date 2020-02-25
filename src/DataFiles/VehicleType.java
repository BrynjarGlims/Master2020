package DataFiles;

import DataFiles.Data;
import scala.collection.immutable.HashMap;
import java.util.HashSet;

import javax.swing.text.StyleConstants;

public class VehicleType {

    public int vehicleTypeID;
    public double capacity;
    public String capacityString;
    public int costPerHour;
    public int costPerHourOvertime;
    public int costPerDay;
    public int costPerKm;
    public int travelCost;
    public int usageCost;
    public HashSet<Integer> vehicleSet;

    public double loadingTimeAtDepot;

    public VehicleType( String capacity,  int costPerHour, int costPerHourOvertime,
                       int costPerDay, int costPerKm){

        this.capacity = Double.parseDouble(capacity) * Parameters.scalingVehicleCapacity ;
        this.capacityString = capacity;
        this.costPerHour = costPerHour;
        this.costPerHourOvertime = costPerHourOvertime;
        this.costPerDay = costPerDay;
        this.costPerKm = costPerKm;

        this.loadingTimeAtDepot = Parameters.loadingTimeAtDepotConstant +
                Parameters.loadaingTimeAtDepotVariable*this.capacity;

        this.travelCost = costPerHour + costPerKm; //todo: implement a better way to determine this
        this.usageCost = costPerDay*6; //todo: implement a better way to determine this
        this.vehicleSet = new HashSet<>();
    }

    public void setVehicleTypeID(int vehicleTypeID){
        this.vehicleTypeID = vehicleTypeID;
    }

    public void addVehicleToSet(int vehicleID){
        this.vehicleSet.add(vehicleID);
    }



}

