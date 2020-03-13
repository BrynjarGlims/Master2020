package StoringResults;

import DataFiles.*;
import Genetic.GiantTourCrossover;
import Genetic.OrderDistributionCrossover;
import Individual.Individual;
import Population.Population;
import ProductAllocation.OrderDelivery;
import ProductAllocation.OrderDistribution;
import com.opencsv.CSVWriter;
import Population.OrderDistributionPopulation;
import Individual.Trip;

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
import java.util.Scanner;

public class Result {
    Population population;
    Data data;
    Individual bestIndividual;
    OrderDistribution bestOD;



    public Result(Population population){
        this.population = population;
        this.data = population.data;
        this.bestIndividual = population.returnBestIndividual();
        this.bestOD = bestIndividual.orderDistribution;
    }

    public void store() throws IOException {


        String fileName = getFileName();
        storeSummary(fileName);
        createDetailedDirectory(fileName);
        storeDetailed(fileName);

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
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Vehicle Name","VehicleID", "Vehicle Number", "Vehicle Type", "Trailer Plate",
                    "Capacity", "Traveling cost", "Usage cost", "Loading time [minutes]", };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        for (Vehicle v : data.vehicles) {
            String[] results = {v.vehicleName, String.valueOf(v.vehicleID), String.valueOf(v.vehicleNumber),
                    String.valueOf(v.vehicleType.vehicleTypeID), v.trailerNumberPlate, String.valueOf(v.vehicleType.capacity),
                    String.valueOf(v.vehicleType.travelCost), String.valueOf(v.vehicleType.usageCost),
                    String.format("%.3f", v.vehicleType.loadingTimeAtDepot*60)};
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
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Trip Number", "Day ", "VehicleID", "Vehicle Name", "Vehicle Type", "Total Trip Time[min]","Traveling Time[min]" ,"CustomerIDs", "Customers visted"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        int tripNumber = 1;
        for (ArrayList<Trip>[] periodTrips : bestIndividual.tripList ){
            for (ArrayList<Trip> vehicleTypeTrips : periodTrips ){
                for (Trip t : vehicleTypeTrips){
                    String[] results = {String.valueOf(tripNumber), Converter.periodConverter(t.period),String.valueOf(t.vehicleID), data.vehicles[t.vehicleID].vehicleName,
                            String.valueOf(data.vehicleTypes[t.vehicleType].capacity), Converter.calculateTotalTripTime(t, data), Converter.calculateDrivingTime(t, data), t.customers.toString()  ,Converter.findCustomersFromID((ArrayList) t.customers, data)};
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
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
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
                    String.valueOf(c.numberOfVisitPeriods), Converter.calculateTotalOrderVolume(c, data),
                    Converter.convertTimeWindow(c.timeWindow[0][0], c.timeWindow[0][1]),
                    Converter.convertTimeWindow(c.timeWindow[1][0], c.timeWindow[1][1]), Converter.convertTimeWindow(c.timeWindow[2][0], c.timeWindow[2][1]),
                    Converter.convertTimeWindow(c.timeWindow[3][0], c.timeWindow[3][1]), Converter.convertTimeWindow(c.timeWindow[4][0], c.timeWindow[4][1]),
                    Converter.convertTimeWindow(c.timeWindow[5][0], c.timeWindow[5][1]),
                    String.format("%.5f", c.totalUnloadingTime*60)};
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
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing detailed file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Order Id", "Type", "Product" ,"Volume", "Day", "CustomerID", "Customer Name", "VehicleID", "Vehicle capacity", };
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
                            String.valueOf(data.vehicles[vehicleID].vehicleType.capacity)};
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
                        break;
                    }
                    else{
                        vehicleID = bestIndividual.tripMap.get(period).get(orderDelivery.order.customerID).vehicleID;
                        String[] results = {String.valueOf(orderDelivery.order.orderID), Converter.dividableConverter(orderDelivery.dividable),
                        orderDelivery.order.commodityFlow, formatter.format(orderDelivery.orderVolumes[period]), Converter.periodConverter(period),
                                String.valueOf(orderDelivery.order.customerID), data.customers[orderDelivery.order.customerID].customerName,
                                String.valueOf(bestIndividual.tripMap.get(period).get(orderDelivery.order.customerID).vehicleID),
                                String.valueOf(data.vehicles[vehicleID].vehicleType.capacity)};
                        csvWriter.writeNext(results, false);

                    }
                }
            }


        }

        csvWriter.close();
        writer.close();

    }


    private void storeSummary(String fileName) throws IOException {
        String filePath  = FileParameters.filePathSummary + "/main_results.csv";
        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println("Changing summary file...");
        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name" , "Objective Value", "Runtime", "Date", "Population Size ", "Generations",
                    "Customers", "Vehicles" };
            csvWriter.writeNext(CSV_COLUMNS, false);
        }

        String[] results = {fileName, String.format("%.4f",bestIndividual.fitness), "0", date_formatter.format(new Date()),
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
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        Result res = new Result(population);
        res.store();
    }

}
