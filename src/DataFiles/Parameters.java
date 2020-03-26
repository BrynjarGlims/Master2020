package DataFiles;

public class Parameters {

    //File paths
    public static final String customersFilePath = "data/Not_in_use.csv";
    public static final String ordersFilePath = "data/Orders.csv";
    public static final String timeWindowsFilePath = "data/Time_windows.csv";
    public static final String vehicleFilePath = "data/Vehicles.csv";

    //Population parameters

    public static final int maximumSubIndividualPopulationSize = 100; //TODO: find reasonable value
    public static final int minimumSubIndividualPopulationSize = 50;
    public static final int numberOfElitismSurvivorsPerGeneration = 5;

    public static final int initialPopulationSize = 50;
    public static final int initialOrderDistributionPopulationSize = 10;
    public static final int maxNumberIterationsWithoutImprovement = 10;
    public static final int maxNumberOfGenerations = 100;

    public static final double minimumFitnessDifferenceForClones = 10; //minimum fitness difference to encounter two individuals as clones

    //Decision parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;

    //Loading data parameters
    public static final int numberOfCustomers = 7;
    public static final int numberOfVehicles = 5;


    public static final int nearestNeighbors = 5; //Neighborhood size for each customer
    public static final int EducationTabooSize = 2;

    //Runtime parameters
    public static final double educationProbability = 0.7;
    public static final double repairProbability = 0;
    public static final double greedyMIPValue = 1;


    //Traveling parameters
    public static final double scalingDistanceParameter = 2.2;
    public static final double timeShift = 2;

    //Penalty parameters for genetic algorithm
    public static final double initialCapacityPenalty = 1;  // lambda
    public static final double initialOvertimePenalty = 1;  // theta
    public static final double initialTimeWarpPenalty = 1000;  // zeta
    public static final double initialDrivingCostPenalty = 1; //used in weighted sum calculations of route costs in createTrips() in the AdSplit class.


    public static final double penaltyFactorForOverFilling = 1.1;
    public static final double penaltyFactorForUnderFilling = 1.5;

    //Tournament selection parameters
    public static final int nearestNeighborsDiversity = 5;  // Neighbours from calculating diversity
    public static final double besIndividualProbability = 0.6;
    public static final int tournamentSize = 10;  // 2 or larger, size = 2 --> binary tournament selection
    public static final boolean binarySelection = false;  // if true, bestIndProp must be much larger than 0.5


    //Period parameters
    //TODO: change to percentage values
    public static final double[] overtimeLimit = {10, 20, 30, 40, 50, 60};
    public static final double[] overtimeCost = {100, 100, 100, 100, 100, 100};

    //Time parameters
    public static final double maxJourneyDuration = 20; //changed to journey duration

    //TEMPORARY PARAMTERS
    public static final double loadingTimeAtDepotConstant = 0.03;
    public static final double loadaingTimeAtDepotVariable = 0.001;
    public static final double scalingUnloadingTimeAtCustomerConstant = 0.03;
    public static final double scalingUnloadingTimeAtCustomerVariable = 0.001;
    public static final double scalingVehicleCapacity = 0.01;
    public static final double indifferenceValue = 0.0001;


    //GUROBI PARAMETERS

    public static final String symmetry =  "car";      // // none, car, trips, customers, cost, duration
    public static final double modelTimeLimit = 3600;
    public static final double modelMipGap = 0.001;
    public static final boolean plotArcFlow = false;
    public static final String instanceName = "testDataFromAsko";
    public static final int upperBoundQuantity = 100;  // upper bound q variable
    public static final int upperBoundOvertime = 1000;  // upper bound qO variable

    public static final boolean verboseArcFlow = true;
    public static final boolean verbosePathFlow = false;
    public static final boolean verboseJourneyBased = false;

    //PLOT PARAMETERS
    public static final boolean savePlots = false;
    public static final boolean verbose = false;
    public static final char separator = ',';
}
