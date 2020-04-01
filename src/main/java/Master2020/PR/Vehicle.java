package Master2020.PR;
public class Vehicle {

    public int vehicleID;
    public VehicleType vehicleType;


    public Vehicle(int vehicleID, VehicleType vehicleType){
        this.vehicleID = vehicleID;
        this.vehicleType = vehicleType;
    }

    public String toString(){
        String out = "";
        out += "vehicle ID: " + vehicleID;
        out += "\nvehicle type: " + vehicleType;
        return out;

    }
}
