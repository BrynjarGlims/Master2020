package PR;
public class Path implements Comparable<Path>{

    public int period;
    public int pathId;
    public VehicleType vehicleType;
    public Customer[] customers;
    public double latestStartTime;
    public double earliestStartTime;
    public double duration;
    public double distance;
    public double cost; //cost for using path

    public Path(int pathId, Customer[] customers, VehicleType vehicleType, int period, double earliestStartTime, double latestStartTime){
        this.pathId = pathId;
        this.period = period;
        this.customers = customers;
        this.vehicleType = vehicleType;
        this.latestStartTime = latestStartTime;
        this.earliestStartTime = earliestStartTime;
    }

    public void setDuration(double distance, double duration){
        this.duration = duration;
        this.distance = distance;
        this.cost = distance * vehicleType.drivingCost;
    }

    public String toString(){
        String out = "route ID: " + pathId + "\n";
        for (Customer c : customers) {
            out += c.customerID + "-";
        }
        out += "\nEarliest start time: " + earliestStartTime;
        out += "\nLatest start time: " + latestStartTime;
        out += "\nDuration: " + duration + "\n";
        return out;
    }

    public int compareTo(Path p){
        if (latestStartTime == p.latestStartTime){
            return 0;
        }
        else if (latestStartTime > p.latestStartTime){
            return 1;
        }
        else{
            return -1;
        }
    }
}

