package DataFiles;

public class Parameters {

    //File paths
    public static final String customersFilePath = "data/Not_in_use.csv";
    public static final String ordersFilePath = "data/Orders.csv";
    public static final String timeWindowsFilePath = "data/Time_windows.csv";
    public static final String vehicleFilePath = "data/Vehicles.csv";


    //Population parameters
    public static final int maximumSubPopulationSize = 100; // A subpopulation ie either the feasile or infeasible population
    public static final int minimumSubPopulationSize = 50;
    public static final int initialPopulationSize = 100;
    public static final int initialOrderDistributionPopulationSize = 10;

    public static final int maxNumberIterationsWithoutImprovement = 20;
    public static final int maxNumberOfIterations = 200;
    public static final double minimumFitnessDifferenceForClones = 10; //minimum fitness difference to encounter two individuals as clones

    //Decision parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;
    public static final int numberOfEliteIndividuals = 50; //TODO: find reasonable value
    public static final int maxPopulationSize = 200; //TODO: find reasonable value

    //Loading data parameters
    public static final int numberOfCustomers = 110;
    public static final int numberOfVehicles = 100;

    //Traveling parameters
    public static final double scalingDistanceParameter = 2;
    public static final double timeShift = 6;


    //Penalty parameters for genetic algorithm
    public static final double initialCapacityPenalty = 0.01;  // lambda
    public static final double initialOvertimePenalty = 0.01;  // theta
    public static final double initialTimeWarpPenalty = 0.01;  // zeta
    public static final double initialDrivingCostPenalty = 0.01; //used in weighted sum calculations of route costs in createTrips() in the AdSplit class.
    public static final double penaltyFactorForOverFilling = 1.1;
    public static final double penaltyFactorForUnderFilling = 1.5;



    //Period parameters
    //TODO: change to percentage values
    public static final double[] overtimeLimit = {100, 100, 100, 100, 100, 100};
    public static final double[] overtimeCost = {100, 100, 100, 100, 100, 100};

    //Time parameters
    public static final double maxJourneyDuration = 24; //changed to journey duration


    //TEMPORARY PARAMTERS
    public static final double loadingTimeAtDepotConstant = 0.03;
    public static final double loadaingTimeAtDepotVariable = 0.001;

    public static final double scalingUnloadingTimeAtCustomerConstant = 0.03;
    public static final double scalingUnloadingTimeAtCustomerVariable = 0.001;

    public static final double scalingVehicleCapacity = 0.01;


    //GUROBI PARAMETERS
    public static final String symmetry =  "car";      // // none, car, trips, customers, cost, duration
    public static final double modelTimeLimit = 10000;
    public static final double modelMipGap = 0.0001;
    public static final boolean plotArcFlow = false;
    public static final String instanceName = "testDataFromAsko";
    public static final int upperBoundQuantity = 100;  // upper bound q variable
    public static final int upperBoundOvertime = 1000;  // upper bound qO variable
    public static final double BigM = 1.5; // TODO: 24.11.2019 Change

    //PLOT PARAMETERS
    public static final boolean savePlots = false;

}
