package DataFiles;

public class Parameters {

    //File paths
    public static final String customersFilePath = "data/Not_in_use.csv";
    public static final String ordersFilePath = "data/Orders.csv";
    public static final String timeWindowsFilePath = "data/Time_windows.csv";
    public static final String vehicleFilePath = "data/Vehicles.csv";


    //Decision parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;

    //Traveling parameters
    public static final double scalingDistanceParameter = 10;

    //Penalty parameters
    public static final int initialCapacityPenalty = 1;
    public static final int initialOvertimePenalty = 1;

    //Other parameters
    public static final int maxTripDuration = 10;



}
