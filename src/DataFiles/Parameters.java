package DataFiles;

public class Parameters {

    //File paths
    public static final String customersFilePath = "data/Not_in_use.csv";
    public static final String ordersFilePath = "data/Orders.csv";
    public static final String timeWindowsFilePath = "data/Time_windows.csv";
    public static final String vehicleFilePath = "data/Vehicles.csv";



    //Population parameters
    public static final int populationSize = 30;
    public static final int maxNumberIterationsWithoutImprovement = 20;
    public static final int maxNumberOfIterations = 200;

    //Decision parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;
    public static final int numberOfEliteIndividuals = 50; //TODO: find reasonable value
    public static final int  maxPopulationSize = 100; //TODO: find reasonable value

    //Loading data parameters
    public static final int numberOfCustomers = 10;
    public static final int numberOfVehicles = 30;


    //Traveling parameters
    public static final double scalingDistanceParameter = 10;


    //Penalty parameters for genetic algorithm
    public static final double initialCapacityPenalty = 1;  // lambda
    public static final double initialOvertimePenalty = 1;  // theta
    public static final double initialTimeWarpPenalty = 1;  // zeta
    public static final double initialDrivingCostPenalty = 1; //used in weighted sum calculations of route costs in createTrips() in the AdSplit class.


    //Period parameters
    public static final int[] overtimeLimit = {100, 100, 100, 100, 100, 100};  //needs to be equal to number of periods in length
    public static final int[] overtimeCost = {100, 100, 100, 100, 100, 100};

    //Time parameters
    public static final double maxJourneyDuration = 10; //changed to journey duration


    //TEMPORARY PARAMTERS
    public static final double loadingTimeAtDepotConstant = 0.3;
    public static final double loadaingTimeAtDepotVariable = 0.01;

    public static final double scalingUnloadingTimeAtCustomerConstant = 0.3;
    public static final double scalingUnloadingTimeAtCustomerVariable = 0.01;


    //GUROBI PARAMETERS
    public static final double modelTimeLimit = 10000;
    public static final double modelMipGap = 0.0001;
    public static final boolean plotArcFlow = false;
    public static final String instanceName = "testDataFromAsko";
    public static final int upperBoundQuantity = 100;  // upper bound q variable
    public static final int upperBoundOvertime = 1000;  // upper bound qO variable
    public static final double BigM = 1.5; // TODO: 24.11.2019 Change

}
