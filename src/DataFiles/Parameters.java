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

    //Penalty parameters for genetic algorithm
    public static final int initialCapacityPenalty = 1;  // lambda
    public static final int initialOvertimePenalty = 1;  // theta


    //Period parameters
    public static final int[] overtimeLimit = {100, 100, 100, 100, 100, 100};
    public static final int[] overtimeCost = {100, 100, 100, 100, 100, 100};

    //Trip parameters
    public static final int maxTripDuration = 10;

    //Other parameters
    public int maxPeriodLength;



}
