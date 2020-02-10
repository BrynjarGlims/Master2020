package ProjectReport;
public class Journey {


    public Customer[] customers;
    public int numCustomers;
    public double distance;
    public double cost;
    public int period;
    public int numTrips;
    public Path[] paths;
    public VehicleType vehicleType;
    public int journeyId;

    public Journey(int journeyId, Customer[] customers, int numTrips, Path[] paths, double distance, double cost, int period, VehicleType vehicleType){
        this.journeyId = journeyId;
        this.customers = customers;
        this.numCustomers = customers.length;
        this.numTrips = numTrips;
        this.paths = paths;
        this.distance = distance;
        this.cost = cost;
        this.period = period;
        this.vehicleType = vehicleType;
    }


    public String toString(){
        String out = "Journey " + journeyId + ":\n";
        for (Path path : paths){
            out += "using path: " + path.pathId + " with customers:\n";
            for (Customer c : path.customers){
                out += c.customerID + "-";
            }
            out += "\n";
        }
        return out;
    }
}
