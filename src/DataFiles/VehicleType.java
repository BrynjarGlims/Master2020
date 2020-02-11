package DataFiles;

import DataFiles.Data;

public class VehicleType {


    public int vehicleTypeID;
    public int capacity;
    public int costPerHour;
    public int costPerHourOvertime;
    public int costPerDay;
    public int costPerKm;

    public VehicleType( int capacity,  int costPerHour, int costPerHourOvertime,
                       int costPerDay, int costPerKm){

        this.capacity = capacity;
        this.costPerHour = costPerHour;
        this.costPerHourOvertime = costPerHourOvertime;
        this.costPerDay = costPerDay;
        this.costPerKm = costPerKm;
    }

    public void setVehicleTypeID(int vehicleTypeID){
        this.vehicleTypeID = vehicleTypeID;
    }



}


/* Old vehicleType class
public class VehicleType {

    public Integer[][] customerAndPeriodToVehicleType;
    private Data data;

    public VehicleType(Data data){
        this.data = data;

    }

}

 */