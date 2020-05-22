package Master2020.StoringResults;

import Master2020.DataFiles.*;
import Master2020.Genetic.OrderDistributionCrossover;
import Master2020.Genetic.PenaltyControl;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDelivery;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;
import com.opencsv.CSVWriter;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Individual.Trip;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Result {


    Population population;
    Data data;
    Individual bestIndividual;
    OrderDistribution bestOD;
    String modelName;
    String fileName;
    boolean isFeasible;
    boolean isOptimal;
    double startTime;
    double MIPGap;
    double runTime;



    public Result(Population population, String modelName, String fileName){
        this.population = population;
        this.data = population.data;
        this.bestIndividual = population.returnBestIndividual();
        this.bestOD = bestIndividual.orderDistribution;
        this.isFeasible = bestIndividual.isFeasible();
        this.isOptimal = false;
        this.modelName = modelName;
        this.fileName = fileName;
    }

    public Result(Individual individual, String modelName, String fileName){
        this.bestOD = individual.orderDistribution;
        this.data = individual.data;
        this.bestIndividual = individual;
        this.isFeasible = bestIndividual.isFeasible();
        this.isOptimal = false;
        this.modelName = modelName;
        this.fileName = fileName;
    }

    public Result(Individual individual, String modelName, String fileName, boolean isFeasible, boolean isOptimal){
        this(individual, modelName, fileName);
        this.isFeasible = isFeasible;
        this.isOptimal = isOptimal;

    }

    public Result(Population population, String modelName, String fileName, boolean isFeasible, boolean isOptimal){
        this(population, modelName, fileName);
        this.isFeasible = isFeasible;
        this.isOptimal = isOptimal;
        this.modelName = modelName;
    }

    public void store() throws IOException {
        store(0, -1);
    }

    public void store(double startTime) throws IOException {
        store(startTime, -1);
    }

    public void store(double startTime, double MIPGap) throws IOException {
        this.startTime = startTime;
        this.runTime = (System.currentTimeMillis() - startTime);
        this.MIPGap = MIPGap;

        if (!this.isFeasible && (modelName == "PFM" || modelName != "AFM" || modelName != "JBM")) {
            createEmptyResult();
            System.out.println("Trolig feil....");
            System.out.println("Storing complete");
        }
        else{
            storeSummary();
            storeDetailed();
            System.out.println("Storing complete");
        }
    }


    private void createEmptyResult() throws IOException {
        String filePath  = FileParameters.filePathSummary + "/main_results.csv";
        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing summary file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name" ,"Objective Value", "Model", "Runtime", "Date", "Seed value", "Population Size ", "Generations",
                    "Customers", "Vehicles", "isFeasible", "isOptimal", "MIPGap"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }
        //todo: change to print other values
        String[] results = {fileName, "-", modelName, String.valueOf(this.runTime), date_formatter.format(new Date()),  String.valueOf(Parameters.randomSeedValue),
                String.valueOf(Parameters.populationSize),String.valueOf(Parameters.maxNumberOfGenerations), String.valueOf(Parameters.numberOfCustomers)
                , String.valueOf(Parameters.numberOfVehicles), "false", "false", "-100%"};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();





    }



    private void storeDetailed() throws IOException {
        storeSummaryDetailed();
        storeDetailedVehicle();
        storeDetailedCustomer();
        storeDetailedTrip();
        storeParameters();
        storeDetailedOrders();
    }

    private void storeDetailedVehicle() throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_vehicle.csv";
        File newFile = new File(filePath);
        System.out.println("Path : " + newFile.getAbsolutePath());
        Writer writer = Files.newBufferedWriter( Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Vehicle Name","VehicleID", "Vehicle Number", "Vehicle Type", "Trailer Plate",
                    "Capacity", "Traveling cost", "Usage cost", "Loading time [minutes]", "Trips to drive", "Days used"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        for (Vehicle v : data.vehicles) {
            String[] results = {v.vehicleName, String.valueOf(v.vehicleID), String.valueOf(v.vehicleNumber),
                    String.valueOf(v.vehicleType.vehicleTypeID), v.trailerNumberPlate, String.valueOf(v.vehicleType.capacity),
                    String.valueOf(v.vehicleType.travelCost), String.valueOf(v.vehicleType.usageCost),
                    String.format("%.3f", v.vehicleType.loadingTimeAtDepot*60), Converter.findNumberOfTrips(v, bestIndividual), Converter.findNumberOfDays(v, bestIndividual)};
            csvWriter.writeNext(results, false);
        }
        csvWriter.close();
        writer.close();

    }

    private void storeDetailedTrip() throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_trip.csv";
        File newFile = new File(filePath);
        System.out.println("Path : " + newFile.getAbsolutePath());
        Writer writer = Files.newBufferedWriter( Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"TripID", "Trip Number", "Day", "VehicleID", "Vehicle Name", "Capacity", "Departure time" ,
                    "Total Trip Time[min]","Traveling Time[min]","CustomerIDs", "Customers visted", "Time Windows"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }
        int tripNumber = 1;
        for (ArrayList<Journey>[] periodJourneys : bestIndividual.journeyList){
            for (ArrayList<Journey> vehicleJourneys : periodJourneys) {
                for (Journey journey : vehicleJourneys ){
                    int tripCounter = 0;
                    for (Trip t : journey.trips){
                        tripCounter += 1;
                        if(t.customers.size() == 0){
                            System.out.println("Empty trip");
                            continue;
                        }
                        String[] results = {String.valueOf(tripNumber), String.valueOf(tripCounter) , Converter.periodConverter(t.period),String.valueOf(t.vehicleID)
                                ,data.vehicles[t.vehicleID].vehicleName, String.valueOf(data.vehicleTypes[t.vehicleType].capacity),
                                Converter.getStartingTimeForTrip(t, data)
                                ,Converter.calculateTotalTripTime(t, data), Converter.calculateDrivingTime(t, data) ,Converter.formatList(t.customers)
                                ,Converter.findCustomersFromID((ArrayList) t.customers, data), Converter.findTimeWindowToCustomers((ArrayList) t.customers, data, t.period)};
                        csvWriter.writeNext(results, false);
                        tripNumber++;

                    }
                }
            }
        }

        for (ArrayList<Trip>[] periodTrips : bestIndividual.tripList ){
            for (ArrayList<Trip> vehicleTypeTrips : periodTrips ){
                for (Trip t : vehicleTypeTrips){


                }
            }
        }
        csvWriter.close();
        writer.close();
    }

    private void storeDetailedCustomer() throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_customer.csv";
        File newFile = new File(filePath);
        System.out.println("Path : " + newFile.getAbsolutePath());
        Writer writer = Files.newBufferedWriter( Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Customer Name", "CustomerID", "Customer number", "Orders", "Dividable Orders", "NonDividable Orders",
                    "Frequency", "Total Volume",  "Visit Monday", "Visit Tuesday", "Visit Wednesday" ,"Visit Thursday" ,"Visit Friday" ,"Visit Saturday",
                    "Unloading time [minutes]"
            };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }


        for (Customer c : data.customers){
            String[] results = {c.customerName, String.valueOf(c.customerID), String.valueOf(c.customerNumber),
                    String.valueOf(c.numberOfOrders), String.valueOf(c.numberOfDividableOrders), String.valueOf(c.numberOfNonDividableOrders),
                    String.valueOf(c.numberOfVisitPeriods), Converter.calculateTotalOrderVolume(c),
                    Converter.convertTimeWindow(c.timeWindow[0][0], c.timeWindow[0][1]),
                    Converter.convertTimeWindow(c.timeWindow[1][0], c.timeWindow[1][1]), Converter.convertTimeWindow(c.timeWindow[2][0], c.timeWindow[2][1]),
                    Converter.convertTimeWindow(c.timeWindow[3][0], c.timeWindow[3][1]), Converter.convertTimeWindow(c.timeWindow[4][0], c.timeWindow[4][1]),
                    Converter.convertTimeWindow(c.timeWindow[5][0], c.timeWindow[5][1]),
                    String.format("%.0f", c.totalUnloadingTime*60)};
            csvWriter.writeNext(results, false);
        }
        csvWriter.close();
        writer.close();
    }

    private void storeDetailedOrders() throws IOException {
        System.out.println("complete?: " + IndividualTest.checkIfIndividualIsComplete(bestIndividual));
        System.out.println("complete OD?: " + IndividualTest.testValidOrderDistribution(data, bestOD));
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_orders.csv";
        File newFile = new File(filePath);
        System.out.println("Path : " + newFile.getAbsolutePath());
        Writer writer = Files.newBufferedWriter( Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.0000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Order Id", "Type", "Product" ,"Volume", "Day", "CustomerID", "Customer Name", "VehicleID", "Vehicle Name", "Vehicle capacity" };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }
        int vehicleID;

        for (OrderDelivery orderDelivery : bestOD.orderDeliveries){
            if (!orderDelivery.dividable){
                if (bestIndividual.tripMap.get(orderDelivery.getPeriod()).containsKey(orderDelivery.order.customerID)){ //todo: change when fixed
                    vehicleID = bestIndividual.tripMap.get(orderDelivery.getPeriod()).get(orderDelivery.order.customerID).vehicleID;
                    int period = orderDelivery.getPeriod();
                    String[] results = {String.valueOf(orderDelivery.order.orderID), Converter.dividableConverter(orderDelivery.dividable),
                            orderDelivery.order.commodityFlow, formatter.format(orderDelivery.orderVolumes[period]), Converter.periodConverter(period),
                            String.valueOf(orderDelivery.order.customerID), data.customers[orderDelivery.order.customerID].customerName,
                            String.valueOf(bestIndividual.tripMap.get(orderDelivery.getPeriod()).get(orderDelivery.order.customerID).vehicleID),
                            String.valueOf(data.vehicles[vehicleID].vehicleName),String.valueOf(data.vehicles[vehicleID].vehicleType.capacity)};
                    csvWriter.writeNext(results, false);


                }
            }
            else{
                for (int period = 0; period < data.numberOfPeriods; period++ ){
                    if (orderDelivery.orderPeriods[period] == 0){
                        continue;
                    }
                    else{
                        if (bestIndividual.tripMap.get(period).size() == 0){
                            if (!bestIndividual.tripMap.get(period).containsKey(orderDelivery.order.customerID)){
                                System.out.println("-------Wrong delivery-------- Find this message in result.java, storing results");
                                System.out.println("OrderID: " + orderDelivery.order.orderID+  " Period: " + period + " customer: " + orderDelivery.order.customerID + " required visit: " + orderDelivery.orderPeriods[period]);
                                continue;
                            }
                        }

                        vehicleID = bestIndividual.tripMap.get(period).get(orderDelivery.order.customerID).vehicleID;
                        String[] results = {String.valueOf(orderDelivery.order.orderID), Converter.dividableConverter(orderDelivery.dividable),
                                orderDelivery.order.commodityFlow, formatter.format(orderDelivery.orderVolumes[period]), Converter.periodConverter(period),
                                String.valueOf(orderDelivery.order.customerID), data.customers[orderDelivery.order.customerID].customerName,
                                String.valueOf(bestIndividual.tripMap.get(period).get(orderDelivery.order.customerID).vehicleID),
                                String.valueOf(data.vehicles[vehicleID].vehicleName),
                                String.valueOf(data.vehicles[vehicleID].vehicleType.capacity)};
                        csvWriter.writeNext(results, false);
                    }
                }
            }
        }
        csvWriter.close();
        writer.close();
    }

    private void storeParameters() throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_parameters.csv";
        File newFile = new File(filePath);
        System.out.println("Path : " + newFile.getAbsolutePath());
        Writer writer = Files.newBufferedWriter( Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.0000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Parameter",  "Value" };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }
        //Add parameters
        String[] results = {"Seed value", String.valueOf(Parameters.randomSeedValue)};
        csvWriter.writeNext(results, false);
        String dataSet = Parameters.useVestTeleDataset ? Parameters.dataSet2 : Parameters.dataSet1;
        results = new String[]{"Dataset:", dataSet};
        csvWriter.writeNext(results, false);
        results = new String[]{"Minimum vehicle size:", String.valueOf(Parameters.minimumVehicleSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Total runtime", String.valueOf(Parameters.totalRuntime)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Custom file name:", String.valueOf(Parameters.customFileName)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Population Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial population size", String.valueOf(Parameters.populationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"New Individuals Each Generation", String.valueOf(Parameters.numberOfOffspring)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Fraction of elite individuals", String.valueOf(Parameters.fractionEliteIndividuals)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Fraction of feasible individuals", String.valueOf(Parameters.fractionOfFeasibleIndividualsFromAdsplit)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial OD population size", String.valueOf(Parameters.initialOrderDistributionPopulationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Maximum number of generations", String.valueOf(Parameters.maxNumberOfGenerations)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Generations without improvement", String.valueOf(Parameters.maxNumberIterationsWithoutImprovement)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Frequency penalty update PGA", String.valueOf(Parameters.frequencyOfPenaltyUpdatesPGA)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Frequency penalty update PGA", String.valueOf(Parameters.frequencyOfPenaltyUpdatesABC)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Loading Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of periods", String.valueOf(Parameters.numberOfPeriods)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of trips", String.valueOf(Parameters.numberOfTrips)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of customers", String.valueOf(Parameters.numberOfCustomers)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of vehicles", String.valueOf(Parameters.numberOfVehicles)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Distance cutoff from depot", String.valueOf(Parameters.distanceCutOffFromDepot)};
        csvWriter.writeNext(results, false);
        results = new String[]{"GA specific Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Nearest neighbours education", String.valueOf(Parameters.nearestNeighbors)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Education taboo size", String.valueOf(Parameters.educationTabooSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Trip optimizer size limit ", String.valueOf(Parameters.tripOptimizerSizeLimit)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Education probability", String.valueOf(Parameters.educationProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Repair probability", String.valueOf(Parameters.repairProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"ODMip probability ", String.valueOf(Parameters.ODMIPProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Heuristic domimance value", String.valueOf(Parameters.heuristicDominanceValue)};
        csvWriter.writeNext(results, false);
        results = new String[]{"ABC parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Generations per order distribution", String.valueOf(Parameters.generationsPerOrderDistribution)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of employees", String.valueOf(Parameters.numberOfEmployees)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of onlockers", String.valueOf(Parameters.numberOfOnlookers)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of scout trials", String.valueOf(Parameters.numberOfScoutTrials)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Max number of trials", String.valueOf(Parameters.maxNumberOfTrials)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Max bound dimentionality", String.valueOf(Parameters.maxBoundDimensionality)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Wigthed Neighbour Onlocker", String.valueOf(Parameters.weightNeighborOnlooker)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Wigthed Neighbour Employed", String.valueOf(Parameters.weightNeighborEmployed)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Weight global best", String.valueOf(Parameters.weightGlobalBest)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Movement Range" , String.valueOf(Parameters.movementRange)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Onlooker Random Adjustment", String.valueOf(Parameters.onlookerRandomAdjustment)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of Enhancements" , String.valueOf(Parameters.numberOfEnhancements)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Weights Enhancement", String.valueOf(Arrays.toString(Parameters.weightsEnhancement))};
        csvWriter.writeNext(results, false);
        results = new String[]{"Global trials cutoff", String.valueOf(Parameters.globalTrialsCutoff)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Penalty parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial capacity penalty", String.valueOf(Parameters.initialOverLoadPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial time warp penalty", String.valueOf(Parameters.initialTimeWarpPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial driving cost penalty", String.valueOf(Parameters.initialDrivingCostPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor for overfilling", String.valueOf(Parameters.penaltyFactorForOverFilling)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor fro underfilling", String.valueOf(Parameters.penaltyFactorForUnderFilling)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Scaling Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Eucledian distance", String.valueOf(Parameters.euclidianDistance)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling distance", String.valueOf(Parameters.scalingDistanceParameter)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Timeshift", String.valueOf(Parameters.timeShift)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Max journey duration", String.valueOf(Parameters.maxJourneyDuration)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Loading time at depot constant", String.valueOf(Parameters.loadingTimeAtDepotConstant)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Loading time at depot variable", String.valueOf(Parameters.loadingTimeAtDepotVariable)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling unloading time constant ", String.valueOf(Parameters.scalingUnloadingTimeAtCustomerConstant)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling unloading time variable", String.valueOf(Parameters.scalingUnloadingTimeAtCustomerVariable)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling vehicle capacity", String.valueOf(Parameters.scalingVehicleCapacity)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Indifference value", String.valueOf(Parameters.indifferenceValue)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling volume value", String.valueOf(Parameters.scalingVolumeValue)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Lower volume flexibility", String.valueOf(Parameters.lowerVolumeFlexibility)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Upper volume flexibility", String.valueOf(Parameters.upperVolumeFlexibility)};
        csvWriter.writeNext(results, false);


        results = new String[]{"Cost parameters:", "------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Overtime of:", " "};
        csvWriter.writeNext(results, false);
        double[] overtimeLimit = Converter.roundArray(Data.overtimeLimit, 2);
        for (int p = 0; p < data.numberOfPeriods; p++){
            results = new String[]{Converter.periodConverter(p) + " [percentage limit cost]",
                    String.valueOf(Parameters.overtimeLimitPercentage[p])+ " " + String.valueOf(overtimeLimit[p]) + " " + String.valueOf(Parameters.overtimeCost[p]) };
            csvWriter.writeNext(results, false);
        }
        results = new String[]{"Scaling driving cost parameter", String.valueOf(Parameters.scalingDrivingCost)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Tournament Selection Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Nearest neighbour diversity", String.valueOf(Parameters.nearestNeighborsDiversity)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Diversity calculation intervall", String.valueOf(Parameters.diversityCalculationInterval)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Best individual probability", String.valueOf(Parameters.bestIndividualProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Tournament size", String.valueOf(Parameters.tournamentSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Binary tournament", String.valueOf(Parameters.binarySelection)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Gurobi Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Symmetry", Parameters.symmetry};
        csvWriter.writeNext(results, false);
        results = new String[]{"Model time limit", String.valueOf(Parameters.modelTimeLimit)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Model mip gap", String.valueOf(Parameters.modelMipGap)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Upper bound on q", String.valueOf(Parameters.upperBoundQuantity)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Upper bound on q0", String.valueOf(Parameters.upperBoundOvertime)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Periodic Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Is periodic", String.valueOf(Parameters.isPeriodic)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Use odMip Between iterations", String.valueOf(Parameters.useODMIPBetweenIterations)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Generations Per Order Distribution Periodic", String.valueOf(Parameters.generationsPerOrderDistributionPeriodic)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial order distribution scale", String.valueOf(Parameters.initialOrderDistributionScale)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Joureny Combination Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Use JCM", String.valueOf(Parameters.useJCM)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of individualjourneys in mip per period", String.valueOf(Parameters.useODMIPBetweenIterations)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Symmetry of JCM", String.valueOf(Parameters.symmetryOFJCM)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Common Parameters for abc, pga and hybrid:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of algorithms", String.valueOf(Parameters.numberOfAlgorithms)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of PGA", String.valueOf(Parameters.numberOfPGA)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Number of ABC", String.valueOf(Parameters.numberOfABC)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Minimum iteration per OD", String.valueOf(Parameters.minimumIterationsPerOD)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Hybrid iterations without improvement limit", String.valueOf(Parameters.hybridIterationsWithoutImprovementLimit)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Order distribution cutoff", String.valueOf(Parameters.orderDistributionCutoff)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Diversified OD generated", String.valueOf(Parameters.diversifiedODsGenerated)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Time run Parameters:", "---------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Time limit per algorithm", String.valueOf(Parameters.timeLimitPerAlgorithm)};
        csvWriter.writeNext(results, false);
        results = new String[]{"odUpdateTime", String.valueOf(Parameters.odUpdateTime)};
        csvWriter.writeNext(results, false);

        csvWriter.close();
        writer.close();
    }

    private void storeSummaryDetailed() throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_summary.csv";
        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing summary file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name" ,"Objective Value", "Model", "Runtime", "Date", "Seed value", "Population Size ", "Generations",
                    "Customers", "Vehicles", "isFeasible", "isOptimal", "MIPGap"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        if (modelName == "GA") {
            this.isFeasible = bestIndividual.isFeasible();
        }

        if (modelName == "GA"){
            this.isOptimal = false;
        }


        String[] results = {fileName, String.format("%.4f",bestIndividual.getFitness(false)), modelName, String.valueOf(this.runTime), date_formatter.format(new Date()), String.valueOf(Parameters.randomSeedValue),
                String.valueOf(Parameters.populationSize),String.valueOf(Parameters.maxNumberOfGenerations), String.valueOf(Parameters.numberOfCustomers)
                , String.valueOf(Parameters.numberOfVehicles), String.valueOf(this.isFeasible), String.valueOf(this.isOptimal), String.valueOf(Math.round(this.MIPGap*1000000)/10000) + "%"};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();
    }


    private void storeSummary() throws IOException {
        String filePath  = FileParameters.filePathSummary + "/main_results.csv";
        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing summary file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name" ,"Objective Value", "Model", "Runtime", "Time limit", "VestTeleDataset", "Date", "Seed value", "UseJCM", "PGAs", "ABCs" , "Population Size ", "Generations",
                    "Customers", "Vehicles","isFeasible", "isOptimal", "MIPGap"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }


        String[] results = {fileName, String.format("%.4f",bestIndividual.getFitness(false)), modelName,  String.valueOf(this.runTime), String.valueOf(Parameters.timeLimitPerAlgorithm),
                String.valueOf(Parameters.useVestTeleDataset),
                date_formatter.format(new Date()), String.valueOf(Parameters.useJCM) ,String.valueOf(Parameters.numberOfPGA), String.valueOf(Parameters.numberOfABC) ,String.valueOf(Parameters.randomSeedValue),
                String.valueOf(Parameters.populationSize),String.valueOf(Parameters.maxNumberOfGenerations), String.valueOf(Parameters.numberOfCustomers)
                , String.valueOf(Parameters.numberOfVehicles), String.valueOf(this.isFeasible), String.valueOf(this.isOptimal), String.valueOf(Math.round(this.MIPGap*1000000)/10000) + "%"};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        Data data = DataReader.loadData();
        PenaltyControl penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty, Parameters.frequencyOfPenaltyUpdatesPGA);
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD, penaltyControl);
        String modelName = "GA";
        String folderName = SolutionStorer.getFolderName(modelName);
        Result res = new Result(population, modelName, folderName);
        res.store();
    }

}
