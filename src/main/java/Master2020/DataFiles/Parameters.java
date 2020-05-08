package Master2020.DataFiles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Parameters {

    // File import parameters
    public static final String dataSet = "Trondelag";   // Trondelag or VestTele
    public static final String ordersFilePath = "data/" + dataSet + "/Orders.csv";
    public static final String timeWindowsFilePath = "data/" + dataSet + "/Time_windows.csv";
    public static final String vehicleFilePath = "data/" + dataSet + "/Vehicles.csv";
    public static final boolean doRandomSeed = true;
    public static final int samples = 1;
    public static long randomSeedValue = 10;

    // Population parameters
    public static final int maximumSubIndividualPopulationSize = 100; //TODO: find reasonable value
    public static final int minimumSubIndividualPopulationSize = 50;
    public static final int initialPopulationSize = 50;
    public static final int initialOrderDistributionPopulationSize = 50;
    public static final int maxNumberIterationsWithoutImprovement = 100;
    public static final int maxNumberOfGenerations = 100;

    // Loading data parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;
    public static int numberOfCustomers = 75; // A maximum of 75
    public static final int numberOfVehicles = 25;
    public static final double distanceCutOffFromDepot = 3.0;
    public static final boolean adjustTimeWindow = true;
    public static final double adjustTimeWindowReduction = 5;
    public static final double adjustTimeWindowLimit = 9.5;

    // GA specific parameters
    public static final int nearestNeighbors = 5; //Neighborhood size for each customer
    public static final int educationTabooSize = 2;
    public static final int tripOptimizerSizeLimit = 7;
    public static final double educationProbability = 0.5;
    public static final double tripOptimizerProbability = 0.3;
    public static final double repairProbability = 0.4;
    public static final double ODMIPProbability = 0.5;
    public static final double heuristicDominanceValue = 1;

    // ABC specific parameters
    public static final boolean runSingular = false;
    public static final boolean threaded = true;
    public static final int numberOfSwarms = 5;
    public static final int minimumIterations = 15;
    public static final int orderDistributionCutoff = 2;
    public static final int swarmIterationsWithoutImprovementLimit = 5;
    public static final int generationsPerOrderDistribution = 300;
    public static final int orderDistributionUpdates = 50;
    public static final int numberOfEmployees = 10;
    public static final int numberOfOnlookers = 40;
    public static final int numberOfScoutTrials = 10;
    public static final int maxNumberOfTrials = 10;
    public static final int maxBoundDimensionality = 4; //max amount of dimensions that can be changed, chosen randomly from 1-this
    public static final double weightNeighborEmployed = 1;
    public static final double weightNeighborOnlooker = 1.5;
    public static final double weightGlobalBest = 0.8;
    public static final double movementRange = 1; //both positive and negative, but only half in negative direction
    public static final double onlookerRandomAdjustment = 0.2; //a random number added when onlooker goes to employers foodsource
    public static final int numberOfEnhancements = 0; //number of enhancements that wil happen to onlookers
    public static final double[] weightsEnhancement = new double[]{33, 33, 34}; //probability distribution of enhancements, [reverse, swap, insert]
    public static final double globalTrialsCutoff = 1.1; //trials will not increment if solution is within this multiplier of global best

    //Penalty parameters for heuristics
    public static final double initialCapacityPenalty = 10000;  // lambda
    public static final double initialTimeWarpPenalty = 10000;  // theta
    public static final double initialDrivingCostPenalty = 1; //used in weighted sum calculations of route costs in createTrips() in the AdSplit class.
    public static final double penaltyFactorForOverFilling = 1.1;
    public static final double penaltyFactorForUnderFilling = 1.5;

    // Scaling parameters
    public static final double scalingDistanceParameter = 2.0; //set to 2.2
    public static final double timeShift = 6;
    public static final double maxJourneyDuration = 10; //changed to journey duration
    public static final double loadingTimeAtDepotConstant = 0.03;
    public static final double loadingTimeAtDepotVariable = 0.001;
    public static final double scalingUnloadingTimeAtCustomerConstant = 0.03;
    public static final double scalingUnloadingTimeAtCustomerVariable = 0.001;
    public static final double scalingVehicleCapacity = 0.01;
    public static final double indifferenceValue = 0.0001;
    public static final double scalingVolumeValue = 1.5;
    public static final double lowerVolumeFlexibility = 0.7;
    public static final double upperVolumeFlexibility = 1.3;

    //Cost parameters
    public static final double[] overtimeLimitPercentage = {0.19, 0.19, 0.19, 0.19, 0.19, 0.05};
    public static final double[] overtimeCost = {100, 100, 100, 100, 100, 100};
    public static final double scalingDrivingCost = 25;  // // TODO: 03/04/2020 Not sure if implemented correctly 

    //Tournament selection parameters
    public static final int nearestNeighborsDiversity = 5;  // Neighbours from calculating diversity
    public static final int diversityCalculationInterval = 100;
    public static final double bestIndividualProbability = 0.6;
    public static final int tournamentSize = 5;  // 2 or larger, size = 2 --> binary tournament selection
    public static final boolean binarySelection = true;  // if true, bestIndProp must be much larger than 0.5

    //GUROBI parameters
    public static final String symmetry =  "trips";      // // none, car, trips, customers, cost, duration
    public static final double modelTimeLimit = 36000;
    public static final double modelMipGap = 0.001;
    public static final boolean plotArcFlow = false;
    public static final String instanceName = "testDataFromAsko";
    public static final int upperBoundQuantity = 100;  // upper bound q variable
    public static final int upperBoundOvertime = 1000;  // upper bound qO variable
    public static final double MIPSafetyIndifference = 0.001;
    public static final boolean verboseODValidity = false;


    //Periodic parameters
    public static boolean isPeriodic = false;
    public static final boolean threadedPGA = true;
    public static int numberOfPeriodicRuns = 2;
    public static final int generationsPerOrderDistributionPeriodic = 5;
    public static final int generationsOfOrderDistributions = 10;
    public static final int newIndividualCombinationsGenerated = 3;
    public static final int minimumPeriodicSubPopulationSize = 20;
    public static final int maksimumPeriodicSubPopulationSize = 50;
    public static final double initialOrderDistributionScale = 1;
    public static final double incrementPerOrderDistributionScaling = 0.02;
    public static final int numberOfGenerationsBetweenODScaling = 2;
    public static final int numberOfGenerationBeforeODScalingStarts = 20;

    // Verbose parameters
    public static final boolean verboseArcFlow = false;
    public static final boolean verbosePathFlow = false;
    public static final boolean verboseJourneyBased = false;
    public static final boolean savePlots = false;
    public static final boolean verbose = false;
    public static final char separator = ';';
}
