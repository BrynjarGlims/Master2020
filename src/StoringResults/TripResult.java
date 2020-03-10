package StoringResults;

import java.util.ArrayList;

public class TripResult {

    ArrayList<Integer> customerSequence;
    int vehicleID;


    public TripResult(){
        customerSequence = new ArrayList<>();

    }

    public void addCustomer(int customerID){
        this.customerSequence.add(customerID);
    }

    public void setVehicle(int vehicleID){
        this.vehicleID = vehicleID;
    }
}
