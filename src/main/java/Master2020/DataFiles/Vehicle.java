package Master2020.DataFiles;

public class Vehicle {

    public int vehicleID;
    public int vehicleNumber;
    public String vehicleName;
    public String trailerNumberPlate;

    public VehicleType vehicleType;


    public Vehicle(int vehicleID, int vehicleNumber, String vehicleName, String trailerNumberPlate) {
        this.vehicleID = vehicleID;
        this.vehicleNumber = vehicleNumber;
        this.vehicleName = vehicleName;
        this.trailerNumberPlate = trailerNumberPlate;
    }


    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }


    public String toString() {
        String string = "Vehicle id: " + this.vehicleID + " - " + this.vehicleName + " \n ";
        string += "Capacity: " + this.vehicleType.capacity + " \n";
        return string;
    }
}


