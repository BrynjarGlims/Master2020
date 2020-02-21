package DataFiles;

import DataFiles.Data;

import javax.swing.text.StyleConstants;

public class VehicleType {

    public int vehicleTypeID;
    public int capacity;
    public int costPerHour;
    public int costPerHourOvertime;
    public int costPerDay;
    public int costPerKm;
    public int travelCost;
    public int usageCost;

    public double loadingTimeAtDepot;

    public VehicleType( int capacity,  int costPerHour, int costPerHourOvertime,
                       int costPerDay, int costPerKm){

        this.capacity = capacity;
        this.costPerHour = costPerHour;
        this.costPerHourOvertime = costPerHourOvertime;
        this.costPerDay = costPerDay;
        this.costPerKm = costPerKm;

        this.loadingTimeAtDepot = Parameters.loadingTimeAtDepotConstant +
                Parameters.loadaingTimeAtDepotVariable*capacity;

        this.travelCost = costPerHour + costPerKm; //todo: implement a better way to determine this
        this.usageCost = costPerDay*6; //todo: implement a better way to determine this
    }

    public void setVehicleTypeID(int vehicleTypeID){
        this.vehicleTypeID = vehicleTypeID;
    }



}

