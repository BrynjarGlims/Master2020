package ProjectReport;
public class VehicleType {

    public int type;
    public int drivingCost;
    public int unitCost;
    public int capacity;
    public double loadingTime;
    public Vehicle[] vehicles;

    public VehicleType(int type, int drivingCost, int unitCost, int capacity, double loadingTime, int numVehicles) {
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