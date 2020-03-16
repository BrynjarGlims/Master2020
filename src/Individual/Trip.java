package Individual;

import DataFiles.Data;

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

    public void removeCustomer(int customer){
        int index = customerToTripIndexMap.get(customer);
        customers.remove(index);
        adjustIndices(customerToTripIndexMap.get(customer));
        customerToTripIndexMap.remove(customer);
    }

    public void addCustomer(int customer, int index){
        customers.add(index, customer);
        adjustIndices(index);
    }

    private void adjustIndices(int fromIndex){
        for (int i = fromIndex ; i < customers.size() ; i++){
            customerToTripIndexMap.put(customers.get(i), i);
        }
    }

    public void setCustomer(int customer, int index){
        customers.set(index, customer);
        customerToTripIndexMap.put(customer, index);
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


    public String toString(){
        String out = "";
        out += "\nperiod: " + period;
        out += "\nvehicleType: " + vehicleType;
        out += "\ntrip index: " + tripIndex;
        out += "\ncustomers: " + customers;

        return out;
    }

}
