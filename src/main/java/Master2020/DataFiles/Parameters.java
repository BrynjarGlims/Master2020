package Master2020.DataFiles;

import scala.Int;

import java.util.*;

public class Parameters {

    // File import parameters
    public static final String dataSet1 = "Trondelag";   // Trondelag or VestTele
    public static final String dataSet2 = "VestTele";   // Trondelag or VestTele
    public static final String ordersFilePath1 = "data/" + dataSet1 + "/Orders.txt";
    public static final String timeWindowsFilePath1 = "data/" + dataSet1 + "/Time_windows.txt";
    public static final String vehicleFilePath1 = "data/" + dataSet1 + "/Vehicles.txt";
    public static final String ordersFilePath2 = "data/" + dataSet2 + "/Orders.txt";
    public static final String timeWindowsFilePath2 = "data/" + dataSet2 + "/Time_windows.txt";
    public static final String vehicleFilePath2 = "data/" + dataSet2 + "/Vehicles.txt";
    public static final String distancePathVestTele = "Google_VestTele";
    public static final String distancePathTrondelag = "Google_Trondelag";
    public static boolean useVestTeleDataset = false;
    public static final boolean doRandomSeed = true;
    public static List<Integer> seeds = Arrays.asList(57, 97, 80, 89, 1);
    public static long randomSeedValue = 69;
    public static final int minimumVehicleSize = 2000;   // removes 1400 and 1800;

    public static long totalRuntime = 3 * 600000;


    //Misc
    public static String customFileName = "";

    // Population parameters
    public static final int populationSize = 25;    //my
    public static final int initialOrderDistributionPopulationSize = 50;
    public static final int maxNumberIterationsWithoutImprovement = 20;
    public static final int maxNumberOfGenerations = 10; //NOT IN USE
    public static int numberOfOffspring = 80;    //lambda FINAL
    public static final double fractionEliteIndividuals = 0.4;     //el
    public static final double fractionOfFeasibleIndividualsFromAdsplit = 0.2;
    public static final int frequencyOfPenaltyUpdatesPGA = 50;
    public static final int frequencyOfPenaltyUpdatesABC = 1000;

    // Loading data parameters
    public static final int numberOfPeriods = 6;
    public static final int numberOfTrips = 5;
    public static int numberOfCustomers = 10;        //A maximum of 75 trøndelag, 118 for vestfold/telemark
    public static int numberOfVehicles = 5;

    public static final double distanceCutOffFromDepot = 3.0;   //default 3
    public static final boolean adjustTimeWindow = true;
    public static final double adjustTimeWindowReduction = 5;
    public static final double adjustTimeWindowLimit = 9.5;

    // GA specific parameters
    public static final int nearestNeighbors = (int) Math.round(0.4 * numberOfCustomers); //Neighborhood size for each customer
    public static final int educationTabooSize = 1;
    public static final int tripOptimizerSizeLimit = 7;
    public static final double educationProbability = 1; //FINAL 1.0
    public static double tripOptimizerProbability = 0.25; //FINAL 0.25
    public static final double repairProbability = 0.4; //FINAL 0.4
    public static double ODMIPProbability = 0.75; //FINAL 0.75
    public static double heuristicDominanceValue = 1; //FINAL


    // ABC specific parameters
    public static final boolean ABCPenaltyAdjustment = false;
    public static int generationsPerOrderDistribution = 4 * numberOfCustomers; //FINAL
    public static int numberOfEmployees = 20; //FINAL
    public static int numberOfOnlookers = 8 * numberOfEmployees; //FINAL
    public static int numberOfScoutTrials = 50; //FINAL
    public static final int maxNumberOfTrials = 15;
    public static int maxBoundDimensionality = (int) Math.round(0.4 * numberOfCustomers); //FINAL //max amount of dimensions that can be changed, chosen randomly from 1-this
    public static double weightNeighborOnlooker = 1; //FINAL
    public static double weightNeighborEmployed = 0.5 * weightNeighborOnlooker; //FINAL
    public static double weightGlobalBest = 0.75; //FINAL
    public static final double movementRange = 1; //FINAL //both positive and negative, but only half in negative direction
    public static double onlookerRandomAdjustment = 0.01; //FINAL //a random number added when onlooker goes to employers foodsource
    public static int numberOfEnhancements = 3; //FINAL //number of enhancements for employees
    public static final double[] weightsEnhancement = new double[]{33, 33, 34}; //probability distribution of enhancements, [reverse, swap, insert]
    public static double globalTrialsCutoff = 1.3; //FINAL//trials will not increment if solution is within this multiplier of global best
    public static int ABCIterationsWithoutImprovementLimit = 4;



    //Penalty parameters for heuristics - Tunable
    public static double initialOverLoadPenalty = 10000;  // lambda
    public static double initialTimeWarpPenalty = 10000;  // theta
    public static final double initialDrivingCostPenalty = 1; //used in weighted sum calculations of route costs in createTrips() in the AdSplit class.
    public static final double penaltyFactorForOverFilling = 1.1;
    public static final double penaltyFactorForUnderFilling = 1.5;

    // Scaling and cost parameters - Not tunable
    public static final boolean euclidianDistance = false;
    public static final double scalingDistanceParameter = 2.0; //set to 2.2
    public static final double timeShift = 6;
    public static final double maxJourneyDuration = 11; //changed to journey duration //todo: see if this should be mentioned....
    public static final double loadingTimeAtDepotConstant = 0.03;
    public static final double loadingTimeAtDepotVariable = 0.001;
    public static final double scalingUnloadingTimeAtCustomerConstant = 0.02;
    public static final double scalingUnloadingTimeAtCustomerVariable = 0.5;
    public static final double scalingVehicleCapacity = 0.02;
    public static final double indifferenceValue = 0.0001;
    public static final double scalingVolumeValue = 1.5;
    public static final double lowerVolumeFlexibility = 0.7;
    public static final double upperVolumeFlexibility = 1.3;
    public static final double[] overtimeLimitPercentage = {0.19, 0.19, 0.19, 0.19, 0.19, 0.05};
    public static final double[] overtimeCost = {100, 100, 100, 100, 100, 100};
    public static final double scalingDrivingCost = 25;  // // TODO: 03/04/2020 Not sure if implemented correctly 

    //Tournament selection parameters - Probably tunable
    public static final int nearestNeighborsDiversity = (int) Math.round(0.2 * populationSize);  // Neighbours from calculating diversity ... change!!!
    public static final int diversityCalculationInterval = 20;
    public static final double bestIndividualProbability = 0.6;
    public static int tournamentSize = 5;  // 2 or larger, size = 2 --> binary tournament selection
    public static final boolean binarySelection = true;  // if true, bestIndProp must be much larger than 0.5

    //GUROBI parameters - Not tunable
    public static final String symmetry =  "Trips";      // // none, car, trips, customers, cost, duration
    public static final double modelTimeLimit = 10800;  //seconds
    public static final double modelJCMTimeLimit = 180;  //seconds
    public static final double modelMipGap = 0.00001;
    public static final boolean plotArcFlow = false;
    public static final String instanceName = "testDataFromAsko";
    public static final int upperBoundQuantity = 100;  // upper bound q variable
    public static final int upperBoundOvertime = 1000;  // upper bound qO variable
    public static final double MIPSafetyIndifference = 0.1;
    public static final boolean verboseODValidity = false;


    //Periodic parameters
    public static boolean isPeriodic = false;  // should be set to true, but has default value false
    public static boolean useODMIPBetweenIterations = true;
    public static int maxGenerationsPerOrderDistributionUpdatePeriodic = 50; //FINAL
    public static final double initialOrderDistributionScale = 1;
    public static final int PHGAIterationsWithoutImprovementLimit = 2;

    //......

    // Journey Combination Model parameters
    public static boolean useJCM = false;
    public static final int numberOfIndividualJourneysInMIPPerPeriod = 1;
    public static final String symmetryOFJCM = "car";


    // Periodic Parameters, common for PGA, ABC, and HYBRID

    public static final int numberOfAlgorithms = 6;
    public static int numberOfPGA = 0;
    public static int numberOfABC = numberOfAlgorithms - numberOfPGA;
    public static final int minimumIterationsPerOD = 3;
    public static final int orderDistributionCutoff = 2;
    public static int diversifiedODsGenerated = 9;

    // Time run parameters
    public static long timeLimitPerAlgorithm = 15000 ;  // in milli
    public static final double odUpdateTime = 2.3;

    // Verbose parameters
    public static final boolean verboseArcFlow = false;
    public static final boolean verbosePathFlow = false;
    public static final boolean verboseJourneyBased = false;
    public static final boolean verboseJourneyCombination = false;
    public static final boolean savePlots = false;
    public static final boolean verbose = false;
    public static final char separator = ';';
}
