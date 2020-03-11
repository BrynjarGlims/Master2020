package DataFiles;

import Individual.Trip;

public class CustomerToTrip {



    public int period;
    public int vehicletype;
    public int customer;
    public int index; //index of customer in the trip, not for the entire [period][vehicletype]
    public Trip trip;
    public int tripIndex;

    public CustomerToTrip(int period, int vehicletype, int customer, int index, Trip trip, int tripIndex){
        this.customer = customer;
        this.period = period;
        this.vehicletype = vehicletype;
        this.index = index;
        this.tripIndex = tripIndex;
        this.trip = trip;
    }



    public String toString(){
        String out = "\n";
        out += "period: " + period;
        out += "\ncustomer: " + customer;
        out += "\nVehicleType: " + vehicletype;
        out += "\nindex of customer" + index;
        return out;
    }
}
