package Individual;

import java.util.List;

public class Trip {

    public int period;
    public int vehicleType;
    public int vehicleID;
    public int tripIndex;
    public List<Integer> customers;


    public Trip(int period, int vehicleType, int vehicleID){
        this.period = period;
        this.vehicleType = vehicleType;
        this.vehicleID = vehicleID;

    }

    public void setTripIndex(int tripIndex) {
        this.tripIndex = tripIndex;
    }

    public void setCustomers(List<Integer> customers) {
        this.customers = customers;
    }


    public String toString(){
        String out = "";
        out += "\nperiod: " + period;
        out += "\nvehicleType: " + vehicleType;
        out += "\ntrip index: " + tripIndex;
        out += "\ncustomers: " + customers;

        return out;
    }

}
