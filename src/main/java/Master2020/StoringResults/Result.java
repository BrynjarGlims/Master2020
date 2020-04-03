package Master2020.StoringResults;

import Master2020.DataFiles.*;
import Master2020.Genetic.OrderDistributionCrossover;
import Master2020.Individual.Individual;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDelivery;
import Master2020.ProductAllocation.OrderDistribution;
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
import java.util.Date;
import java.util.Scanner;

public class Result {
    Population population;
    Data data;
    Individual bestIndividual;
    OrderDistribution bestOD;
    String modelName;



    public Result(Population population){
        this.population = population;
        this.data = population.data;
        this.bestIndividual = population.returnBestIndividual();
        this.bestOD = bestIndividual.orderDistribution;
    }

    public Result(Individual individual){
        this.bestOD = individual.orderDistribution;
        this.data = individual.data;
        this.bestIndividual = individual;
    }

    public Result(Individual individual, String modelName){
        this(individual);
        this.modelName = modelName;
    }

    public Result(Population population, String modelName){
        this(population);
        this.modelName = modelName;
    }



    public void store() throws IOException {

        String fileName = getFileName();
        storeSummary(fileName);
        createDetailedDirectory(fileName);
        storeDetailed(fileName);
        System.out.println("Storing complete");

    }

    private String getFileName(){
        if (FileParameters.useDefaultFileName){
            SimpleDateFormat date_formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            return date_formatter.format(new Date());
        }
        else{
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            System.out.println("Specify detailed filename: ");
            return myObj.nextLine();  // Read user input
        }

    }

    private void createDetailedDirectory(String fileName){
        File file = new File(FileParameters.filePathDetailed + "/"+ fileName );
        boolean bool = file.mkdir();
        if (bool){
            System.out.println("Directory succesfully created");
        }
    };

    private void storeDetailed(String fileName) throws IOException {

        storeDetailedVehicle(fileName);
        storeDetailedCustomer(fileName);
        storeDetailedTrip(fileName);
        storeDetailedOrders(fileName);
    }

    private void storeDetailedVehicle(String fileName) throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/vehicle.csv";
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

    private void storeDetailedTrip(String fileName) throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/trip.csv";
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
        for (ArrayList<Trip>[] periodTrips : bestIndividual.tripList ){
            for (ArrayList<Trip> vehicleTypeTrips : periodTrips ){
                for (Trip t : vehicleTypeTrips){
                    String[] results = {String.valueOf(tripNumber), Converter.getTripNumber(t, bestIndividual) , Converter.periodConverter(t.period),String.valueOf(t.vehicleID)
                            ,data.vehicles[t.vehicleID].vehicleName, String.valueOf(data.vehicleTypes[t.vehicleType].capacity),
                            Converter.getStartingTimeForTrip(t, data)
                            ,Converter.calculateTotalTripTime(t, data), Converter.calculateDrivingTime(t, data) ,Converter.formatList(t.customers)
                            ,Converter.findCustomersFromID((ArrayList) t.customers, data), Converter.findTimeWindowToCustomers((ArrayList) t.customers, data, t.period)};
                    csvWriter.writeNext(results, false);
                    tripNumber++;
                }
            }
        }
        csvWriter.close();
        writer.close();
    }

    private void storeDetailedCustomer(String fileName) throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/customer.csv";
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

    private void storeDetailedOrders(String fileName) throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/orders.csv";
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
                else{
                    System.out.print("Impossible order found: ");
                    System.out.print(" orderID:" + orderDelivery.order.orderID);
                    System.out.print(" period:" + orderDelivery.getPeriod());
                    System.out.println(" customerID:" + orderDelivery.order.customerID);
                    System.out.println("----------------");
                }
            }
            else{
                for (int period = 0; period < data.numberOfPeriods; period++ ){
                    if (orderDelivery.orderPeriods[period] == 0){
                        continue;
                    }
                    else{
                        if (!bestIndividual.tripMap.get(period).containsKey(orderDelivery.order.customerID)){
                            System.out.println("-------Wrong delivery-------- Find this message in result.java, storing results");
                            System.out.println("OrderID: " + orderDelivery.order.orderID+  " Period: " + period + " customer: " + orderDelivery.order.customerID + " required visit: " + orderDelivery.orderPeriods[period]);
                            continue;
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

    private void storeParameters(String fileName) throws IOException {
        String filePath  = FileParameters.filePathDetailed + "/" + fileName + "/parameters.csv";
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
        results = new String[]{"Dataset:", Parameters.dataSet};
        csvWriter.writeNext(results, false);

        results = new String[]{"Population Parameters:", "------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Maximum sub population size", String.valueOf(Parameters.maximumSubIndividualPopulationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Minimum sub population size", String.valueOf(Parameters.minimumSubIndividualPopulationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial population size", String.valueOf(Parameters.initialPopulationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial order distribution population size", String.valueOf(Parameters.initialOrderDistributionPopulationSize)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Maximum number of generations", String.valueOf(Parameters.maxNumberOfGenerations)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Maximum number of generations without improvement", String.valueOf(Parameters.maxNumberIterationsWithoutImprovement)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Loading Parameters:", "------"};
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

        results = new String[]{"GA specific Parameters:", "------"};
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
        results = new String[]{"Repair probability", String.valueOf(Parameters.repairProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"ODMip probability ", String.valueOf(Parameters.ODMIPProbability)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Heuristic domimance value", String.valueOf(Parameters.heuristicDominanceValue)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Penalty parameters:", "------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial capacity penalty", String.valueOf(Parameters.initialCapacityPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial time warp penalty", String.valueOf(Parameters.initialTimeWarpPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial driving cost penalty", String.valueOf(Parameters.initialDrivingCostPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor for overfilling", String.valueOf(Parameters.penaltyFactorForOverFilling)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor fro underfilling", String.valueOf(Parameters.penaltyFactorForUnderFilling)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Scaling Parameters:", "------"};
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
        results = new String[]{"Scaling unloading time at customer constant ", String.valueOf(Parameters.scalingUnloadingTimeAtCustomerConstant)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling unloading time at customer variable", String.valueOf(Parameters.scalingUnloadingTimeAtCustomerVariable)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Scaling vehicle capacity", String.valueOf(Parameters.scalingVehicleCapacity)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Indifference value", String.valueOf(Parameters.indifferenceValue)};
        csvWriter.writeNext(results, false);

        results = new String[]{"Cost parameters:", "------"};
        csvWriter.writeNext(results, false);
        results = new String[]{"Overtime limit ", String.valueOf(Parameters.initialCapacityPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial time warp penalty", String.valueOf(Parameters.initialTimeWarpPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Initial driving cost penalty", String.valueOf(Parameters.initialDrivingCostPenalty)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor for overfilling", String.valueOf(Parameters.penaltyFactorForOverFilling)};
        csvWriter.writeNext(results, false);
        results = new String[]{"Penalty factor fro underfilling", String.valueOf(Parameters.penaltyFactorForUnderFilling)};
        csvWriter.writeNext(results, false);

        csvWriter.close();
        writer.close();
    }


    private void storeSummary(String fileName) throws IOException {
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
            String[] CSV_COLUMNS = {"File name" ,"Objective Value", "Model", "Runtime", "Date", "Population Size ", "Generations",
                    "Customers", "Vehicles" };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        String[] results = {fileName, String.format("%.4f",bestIndividual.getFitness(false)), modelName, "0", date_formatter.format(new Date()),
                String.valueOf(Parameters.maximumSubIndividualPopulationSize),String.valueOf(Parameters.maxNumberOfGenerations), String.valueOf(Parameters.numberOfCustomers)
        , String.valueOf(Parameters.numberOfVehicles)};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        Result res = new Result(population);
        res.store();
    }

}
