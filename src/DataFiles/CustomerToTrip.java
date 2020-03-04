package DataFiles;

public class CustomerToTrip {



    public int period;
    public int vehicletype;
    public int customer;
    public int index; //index of customer in the trip, not for the entire [period][vehicletype]
    public int startIndex;
    public int endIndex;

    public CustomerToTrip(int period, int vehicletype, int customer, int index, int startIndex, int endIndex){
        this.customer = customer;
        this.period = period;
        this.vehicletype = vehicletype;
        this.index = index;
        this.startIndex = startIndex;
        this. endIndex = endIndex;
    }



    public String toString(){
        String out = "\n";
        out += "period: " + period;
        out += "\ncustomer: " + customer;
        out += "\nVehicleType: " + vehicletype;
        out += "\nindex of customer" + index;
        out += "\nStart index: " + startIndex;
        out += "\nEnd index: " + endIndex;
        return out;
    }
}
