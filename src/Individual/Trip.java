package Individual;

import DataFiles.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Trip {

    public Data data;

    public int period;
    public int vehicleType;
    public int vehicleID;
    public int tripIndex;
    public int tripStartIndex = -1;
    public int tripEndIndex = -1;
    public List<Integer> customers;
    public HashMap<Integer, Integer> customerToTripIndexMap;  // customerID to location


    public Trip(Data data){
        this.data = data;
    }

    public void initialize(int period, int vehicleType, int vehicleID){
        this.period = period;
        this.vehicleType = vehicleType;
        this.vehicleID = vehicleID;
    }

    public void setTripIndex(int tripIndex) {
        this.tripIndex = tripIndex;
    }

    public void setCustomers(List<Integer> customers, int tripStartIndex) {
        if (!(tripStartIndex == -1)){
            this.tripStartIndex = tripStartIndex;
        }
        this.customers = customers;
        this.tripEndIndex = this.tripStartIndex + customers.size();
        this.setCustomerMaps();

    }


    public void setCustomers(List<Integer> customers) {
        this.setCustomers(customers, -1);
    }


    public void setCustomerMaps(){
        customerToTripIndexMap = new HashMap<>();
        for (int c = 0; c < customers.size(); c++){
            customerToTripIndexMap.put(customers.get(c), c );
        }
    }

    public List<Integer> getCustomers(){
        return customers;
    }


}
