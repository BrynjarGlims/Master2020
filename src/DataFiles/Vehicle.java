package DataFiles;

public class Vehicle {

    public int vehicleNumber;
    public String vehicleName;

    public String trailerNumberPlate;

    public VehicleType vehicleType;

    public Vehicle(int vehicleNumber, String vehicleName,  String trailerNumberPlate){
        this.vehicleNumber = vehicleNumber;
        this.vehicleName = vehicleName;

        this.trailerNumberPlate = trailerNumberPlate;

    }


    public void setVehicleType(VehicleType vehicleType){
        this.vehicleType = vehicleType;

    }


    }

