package Master2020.PR;
public class VehicleType {

    public int type;
    public double drivingCost;
    public double unitCost;
    public double capacity;
    public double loadingTime;
    public Vehicle[] vehicles;

    public VehicleType(int type, double drivingCost, double unitCost, double capacity, double loadingTime, int numVehicles) {
        this.type = type;
        this.drivingCost = drivingCost;
        this.unitCost = unitCost;
        this.capacity = capacity;
        this.loadingTime = loadingTime;
        this.vehicles = new Vehicle[numVehicles];
    }

    public void addVehicle(Vehicle vehicle, int index){
        vehicles[index] = vehicle;
    }
}