package Master2020.DataFiles;

import scala.xml.PrettyPrinter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class DataReader {



    private static List<String[]> readCSVFile( String file) {
        List<String[]> content = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                content.add(line.split("\t"));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found exception in readCSVFile");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private static Customer[] parseOrdersFileDataOrdinary(List<String[]> productData){
        List<Customer> customerList = new ArrayList<Customer>();
        List<Order> productList = new ArrayList<Order>();
        int customerID = 0;
        int productID = 0;

        for(int line = 0; line < productData.size(); line++){
            if (line != 0){
                if (!productData.get(line-1)[0].equals(productData.get(line)[0]) || line == (productData.size()-1)) {
                    customerList.add(new Customer(customerID, Integer.parseInt(productData.get(line-1)[0]), productData.get(line-1)[1]));
                    customerList.get(customerID).setOrders(convertProductList(productList));
                    productList = new ArrayList<>();
                    customerID++;
                }
            }
            productList.add(new Order(productID, customerID,
                    Double.parseDouble(productData.get(line)[12]),
                    checkSplitAttribute(productData.get(line)[6], line),
                    productData.get(line)[3],
                    Integer.parseInt(productData.get(line)[7]),
                    Integer.parseInt(productData.get(line)[8]),
                    Integer.parseInt(productData.get(line)[9])));
            productID++;
        }
        return convertCustomerList(customerList);
    }

    private static Customer[] parseOrdersFileDataSpecial(List<String[]> productData){
        List<Customer> customerList = new ArrayList<Customer>();
        List<Order> productList = new ArrayList<Order>();
        int customerID = 0;
        int productID = 0;

        for(int line = 0; line < productData.size(); line++){
            if (line != 0){
                if (!productData.get(line-1)[0].equals(productData.get(line)[0]) || line == (productData.size()-1)) {
                    customerList.add(new Customer(customerID, Integer.parseInt(productData.get(line-1)[0]), productData.get(line-1)[1]));
                    customerList.get(customerID).setOrders(convertProductList(productList));
                    productList = new ArrayList<>();
                    customerID++;
                }
            }
            productList.add(new Order(productID, customerID,
                    Double.parseDouble(productData.get(line)[11]),
                    checkSplitAttribute(productData.get(line)[4], line),
                    productData.get(line)[3],
                    Integer.parseInt(productData.get(line)[6]),
                    Integer.parseInt(productData.get(line)[7]),
                    Integer.parseInt(productData.get(line)[8])));
            productID++;
        }
        return convertCustomerList(customerList);
    }

    private static boolean checkSplitAttribute( String flagg, int line){
        if (flagg.equals("Volumsplitt")){
            return true;
        }
        else if (flagg.equals("Sone")){
            return false;
        }
        else{
            throw new IllegalArgumentException("Unknown isDividable value at line " + (line + 2));
        }
    }

    private static Order[] convertProductList(List<Order> productList ){
        Order[] products = new Order[productList.size()];
        for (int i = 0; i < productList.size(); i++){
            products[i] = productList.get(i);
        }
        return products;
    }

    private static Customer[] convertCustomerList( List<Customer> customerList ){
        Customer[] customers = new Customer[customerList.size()];
        for (int i = 0; i < customerList.size(); i++){
            customers[i] = customerList.get(i);
        }
        return customers;
    }

    private static Customer[] parseTimeWindowFileData(Customer[] customers, List<String[]> timeWindowData){
        int customerCount = 0;
        double[][] timeWindow = new double[6][2];

        for (int line = 0; line < timeWindowData.size(); line++){
            if (line != 0 && !timeWindowData.get(line-1)[0].equals(timeWindowData.get(line)[0]) || line == timeWindowData.size()-1) {
                if(customers[customerCount].customerNumber != Integer.parseInt(timeWindowData.get(line-1)[0])) {
                    //System.out.println("Missing customer on line: " + (line+2));
                    continue;
                }
                customers[customerCount].setTimeWindow(timeWindow);
                customers[customerCount].setCoordinates(getCoordinates(timeWindowData,line-1));
                customers[customerCount].setLoadingTimes(getLoadingTime(timeWindowData, line-1));
                timeWindow = new double[6][2];
                customerCount++;
            }
            timeWindow = setTimeWindows(timeWindow, timeWindowData, line);
        }
        return customers;
    }

    private static Customer[] parseTimeWindowFileDataSpecial(Customer[] customers, List<String[]> timeWindowData){
        int customerCount = 0;
        double[][] timeWindow = new double[6][2];

        for (int line = 0; line < timeWindowData.size(); line++){
            if (line != 0 && !timeWindowData.get(line-1)[0].equals(timeWindowData.get(line)[0]) || line == timeWindowData.size()-1) {
                if(customers[customerCount].customerNumber != Integer.parseInt(timeWindowData.get(line-1)[0])) {
                    //System.out.println("Missing customer on line: " + (line+2));
                    continue;
                }
                customers[customerCount].setTimeWindow(timeWindow);
                customers[customerCount].setCoordinates(getCoordinates(timeWindowData,line-1, true), true);
                customers[customerCount].setLoadingTimes(getCustomLoadingTime());
                timeWindow = new double[6][2];
                customerCount++;
            }
            timeWindow = setTimeWindowsSpecial(timeWindow, timeWindowData, line);
        }
        return customers;
    }

    private static double[] getCoordinates(List<String[]> timeWindowData, int line){
        return getCoordinates( timeWindowData,  line, false);
    }

    private static double[] getCoordinates(List<String[]> timeWindowData, int line, boolean special){
        double x_coordinate;
        double y_coordinate;
        if (special){
            x_coordinate = Double.parseDouble(timeWindowData.get(line)[19]);
            y_coordinate = Double.parseDouble(timeWindowData.get(line)[18]);
        } else {
            x_coordinate = Double.parseDouble(timeWindowData.get(line)[7]);
            y_coordinate = Double.parseDouble(timeWindowData.get(line)[8]);
        }
            double[] coordinates = {x_coordinate, y_coordinate};
            return coordinates;
    }

    private static double[] getLoadingTime(List<String[]> timeWindowData, int line){
        double fixedLoadingTime = Double.parseDouble(timeWindowData.get(line)[9]);
        double variableLoadingTime = Double.parseDouble(timeWindowData.get(line)[10]);
        double fixedUnloadingTime = Double.parseDouble(timeWindowData.get(line)[11]);
        double variableUnloadingTime = Double.parseDouble(timeWindowData.get(line)[12]);
        double[] loadingTimes = {fixedLoadingTime,variableLoadingTime,fixedUnloadingTime,variableUnloadingTime};
        return loadingTimes;
    }

    private static double[] getCustomLoadingTime(){
        double fixedLoadingTime = 5;
        double variableLoadingTime = 0.04;
        double fixedUnloadingTime = 10;
        double variableUnloadingTime = 0.04;
        double[] loadingTimes = {fixedLoadingTime,variableLoadingTime,fixedUnloadingTime,variableUnloadingTime};
        return loadingTimes;
    }

    private static double[][] setTimeWindowsSpecial (double[][] timeWindows, List<String[]> timeWindowData, int line){
        int p = Integer.parseInt(timeWindowData.get(line)[5]) - 1;
        if (timeWindows[p][0] == 0) {
            timeWindows[p][0] = convertTimeToDouble(timeWindowData.get(line)[6]);
            timeWindows[p][1] = convertTimeToDouble(timeWindowData.get(line)[8]);
            if (Parameters.adjustTimeWindow){
                if ((timeWindows[p][0] > Parameters.adjustTimeWindowLimit) || (timeWindows[p][1] > Parameters.adjustTimeWindowLimit)){
                    timeWindows[p][0] -= Parameters.adjustTimeWindowReduction;
                    timeWindows[p][1] -= Parameters.adjustTimeWindowReduction;
                    if (timeWindows[p][0] < 0){
                        timeWindows[p][0] = 0;
                    }
                    if (timeWindows[p][1] < 0){
                        timeWindows[p][1] = 0;
                    }
                }
            }
        }

        return timeWindows;
    }


    private static double[][] setTimeWindows (double[][] timeWindows, List<String[]> timeWindowData, int line){

        if (!timeWindowData.get(line)[13].equals("")){
            timeWindows[0][0] = convertTimeToDouble(timeWindowData.get(line)[13]);
            timeWindows[0][1] = convertTimeToDouble(timeWindowData.get(line)[14]);
        }
        if (!timeWindowData.get(line)[15].equals("")){
            timeWindows[1][0] = convertTimeToDouble(timeWindowData.get(line)[15]);
            timeWindows[1][1] = convertTimeToDouble(timeWindowData.get(line)[16]);
        }
        if (!timeWindowData.get(line)[17].equals("")){
            timeWindows[2][0] = convertTimeToDouble(timeWindowData.get(line)[17]);
            timeWindows[2][1] = convertTimeToDouble(timeWindowData.get(line)[18]);
        }
        if (!timeWindowData.get(line)[19].equals("")){
            timeWindows[3][0] = convertTimeToDouble(timeWindowData.get(line)[19]);
            timeWindows[3][1] = convertTimeToDouble(timeWindowData.get(line)[20]);
        }
        if (!timeWindowData.get(line)[21].equals("")){
            timeWindows[4][0] = convertTimeToDouble(timeWindowData.get(line)[21]);
            timeWindows[4][1] = convertTimeToDouble(timeWindowData.get(line)[22]);
        }
        if (!timeWindowData.get(line)[23].equals("")){
            timeWindows[5][0] = convertTimeToDouble(timeWindowData.get(line)[23]);
            timeWindows[5][1] = convertTimeToDouble(timeWindowData.get(line)[24]);
        }
        if (Parameters.adjustTimeWindow){
            for (int p = 0; p < Parameters.numberOfPeriods; p++){
                if ((timeWindows[p][0] > Parameters.adjustTimeWindowLimit) || (timeWindows[p][1] > Parameters.adjustTimeWindowLimit)){
                    timeWindows[p][0] -= Parameters.adjustTimeWindowReduction;
                    timeWindows[p][1] -= Parameters.adjustTimeWindowReduction;
                    if (timeWindows[p][0] < 0){
                        timeWindows[p][0] = 0;
                    }
                    if (timeWindows[p][1] < 0){
                        timeWindows[p][1] = 0;
                    }
                }

            }

        }
        return timeWindows;
    }

    private static double convertTimeToDouble(String time){
        String[] convertedTime = time.split(":");
        double number = Double.parseDouble(convertedTime[0]) + Double.parseDouble(convertedTime[1])/60 - Parameters.timeShift;
        number = inConsistencyCheck(number);
        return number;


    }
    private static double inConsistencyCheck(double number){
        if (number < 0){
            return 0;
        }
        //else if (number > Parameters.maxJourneyDuration){
        //    return Parameters.maxJourneyDuration;
        //}
        else{
            return number;
        }
    }

    private static Vehicle[] parseVehicleFileDataToVehicle(List<String[]> vehiclesData){
        List<Vehicle> vehicleList = new ArrayList<>();

        HashMap<String, VehicleType> vehicleTypeHashMap = new HashMap<String, VehicleType>();
        int vehicleCounter = 0;

        for (int line = 0; line < vehiclesData.size(); line++){
            //error check  //todo: explain what it removes vehiclesData.get(line)[18].equals("")
            if ( Double.parseDouble(vehiclesData.get(line)[24]) < 10000 &&
            Integer.parseInt(vehiclesData.get(line)[23]) >= 3000){

                //current capacity
                String tempCapacity = vehiclesData.get(line)[24];

                if ( !vehicleTypeHashMap.containsKey(tempCapacity)){
                    //create new vehicle type
                    vehicleTypeHashMap.put(tempCapacity, new VehicleType(vehiclesData.get(line)[24],
                            Integer.parseInt(vehiclesData.get(line)[20]),
                            Integer.parseInt(vehiclesData.get(line)[21]),
                            Integer.parseInt(vehiclesData.get(line)[18]),
                            Integer.parseInt(vehiclesData.get(line)[19])));

                }
                //create new vehicle object
                vehicleList.add(new Vehicle(vehicleCounter, Integer.parseInt(vehiclesData.get(line)[0]),
                        vehiclesData.get(line)[2], vehiclesData.get(line)[28]));
                vehicleCounter++;

                //assign vehicle type to object
                vehicleList.get(vehicleList.size()-1).setVehicleType(vehicleTypeHashMap.get(tempCapacity));

            }


        }

        Vehicle[] vehicles = convertVehicleList(vehicleList);
        return vehicles;
    }

    private static Depot parseVehicleFileDataToDepot(List<String[]> vehiclesData)  {

        for (int line = 0; line < vehiclesData.size(); line++) {
            if (vehiclesData.get(line)[4].equals("TRONDHEIM")) {
                double xCoordinate = Double.parseDouble(vehiclesData.get(line)[38]);
                double yCoordinate = Double.parseDouble(vehiclesData.get(line)[39]);
                return new Depot(xCoordinate, yCoordinate);
            }
        }
        System.out.println("Depot cannot be found");
        return new Depot(0,0);
    }



    private static Vehicle[] convertVehicleList( List<Vehicle> vehicleList){
        Vehicle[] vehicles = new Vehicle[vehicleList.size()];
        for (int i = 0; i < vehicleList.size(); i++){
            vehicles[i] = vehicleList.get(i);
        }
        return vehicles;

    }



    private static Customer[] removeInvalidCustomers(Customer[] customers, Depot depot){
        //Function to remove invalid data
        List<Integer> indexes = new ArrayList<Integer>() ;
        for (int i = 0; i < customers.length; i++){
            if (customers[i].numberOfNonDividableOrders > customers[i].numberOfVisitPeriods ){
                //System.out.println(" too many number of visit days");
                indexes.add(i);
            }
            else if (distanceFromDepot(customers[i], depot) > Parameters.distanceCutOffFromDepot){
                /*
                System.out.println(distanceFromDepot(customers[i], depot));
                System.out.println("too far to drive");

                 */
                indexes.add(i);
            }


        }
        Customer[] newCustomers = removeElementsInArray(customers, indexes);
        return newCustomers;
    }

    private static double distanceFromDepot(Customer customer, Depot depot){
        return Math.sqrt(Math.pow(customer.xCoordinate - depot.xCoordinate, 2)
                + Math.pow(customer.yCoordinate - depot.yCoordinate, 2)) * Parameters.scalingDistanceParameter;
    }

    private static Customer[] removeElementsInArray(Customer[] customers, List<Integer> indexes ){
        // Check if something is wrong
        if (customers == null || indexes.size() == 0 || indexes.size() > customers.length){
            return customers;
        }
        Customer[] newCustomers = new Customer[customers.length-indexes.size()];
        int cusomterCounter = 0;
        int orderCounter = 0;
        for (int index = 0; index < customers.length; index++){
            if (!indexes.contains(index)) {
                newCustomers[cusomterCounter] = customers[index];
                newCustomers[cusomterCounter].setCustomerID(cusomterCounter);
                newCustomers[cusomterCounter].setOrderId(orderCounter);
                orderCounter += newCustomers[cusomterCounter].orders.length;
                cusomterCounter++;
            }
        }

        return newCustomers;
    };


    private static VehicleType[] getAndOrderVehicleTypes( Vehicle[] vehicles){
        int vehicleTypeCounter = 0;
        HashMap<String, VehicleType> vehicleTypeHashMap = new HashMap<String, VehicleType>();

        for(Vehicle vehicle : vehicles){
            if(!vehicleTypeHashMap.containsKey(vehicle.vehicleType.capacityString)) {
                vehicleTypeHashMap.put(vehicle.vehicleType.capacityString, vehicle.vehicleType);
                vehicle.vehicleType.setVehicleTypeID(vehicleTypeCounter);
                vehicleTypeCounter++;
            }
        }

        VehicleType[] vehicleTypes = convertHashMapToArray(vehicleTypeHashMap);
        return vehicleTypes;
    }

    private static VehicleType[] convertHashMapToArray(HashMap<String, VehicleType> vehicleTypeHashMap) {
        VehicleType[] vehicleTypes = new VehicleType[vehicleTypeHashMap.size()];
        for (HashMap.Entry<String, VehicleType> entry : vehicleTypeHashMap.entrySet()){
            vehicleTypes[entry.getValue().vehicleTypeID] = entry.getValue();
        }
        return vehicleTypes;
    }

    public static Data loadData(){
        return loadData(false);
    }

    public static Data loadData(boolean verbose)  {
        // Master function
        List<String[]> orderData;
        List<String[]> timeWindowData;
        List<String[]> vehiclesData;
        if (Parameters.useLargeDataset){
            orderData = DataReader.readCSVFile(Parameters.ordersFilePath2);
            timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath2);
            vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath2);
        }
        else{
            orderData = DataReader.readCSVFile(Parameters.ordersFilePath1);
            timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath1);
            vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath1);
        }

        Depot depot;
        Customer[] customers;
        Vehicle[] vehicles;
        if (Parameters.useLargeDataset){
            depot = setCustomDepot();
            customers = parseOrdersFileDataSpecial(orderData);
            customers = parseTimeWindowFileDataSpecial(customers, timeWindowData);
            customers = removeInvalidCustomers(customers, depot);
            vehicles = parseVehicleFileDataToVehicle(vehiclesData);
        }
        else{
            depot = parseVehicleFileDataToDepot(vehiclesData);
            customers = parseOrdersFileDataOrdinary(orderData);
            customers = parseTimeWindowFileData(customers, timeWindowData);
            customers = removeInvalidCustomers(customers, depot);
            vehicles = parseVehicleFileDataToVehicle(vehiclesData);
        }

        Customer[] customersSubset;
        Vehicle[] vehiclesSubset;
        VehicleType[] vehicleTypes;
        if (!Parameters.doRandomSeed){
            customersSubset = Arrays.copyOfRange(customers, 0, Parameters.numberOfCustomers);;
            vehiclesSubset = Arrays.copyOfRange(vehicles, 0, Parameters.numberOfVehicles);
            vehicleTypes = getAndOrderVehicleTypes(vehiclesSubset);
        } else{
            customersSubset = getRandomSeedFromCustomers(customers);
            vehiclesSubset = getRandomSeedFromVehicles(vehicles);
            vehicleTypes = getAndOrderVehicleTypes(vehiclesSubset);
        }


        Data data = new Data(customersSubset, vehiclesSubset, depot, vehicleTypes);
        if(verbose){
            displayData(data);
        }

        return data;

    }

    private static Customer[] getRandomSeedFromCustomers(Customer[] customers){
        Random generator = new Random(Parameters.randomSeedValue);
        Customer[] newCustomers = new Customer[Parameters.numberOfCustomers];
        HashSet<Integer> usedNumbers = new HashSet<>();
        int customerCounter = 0;
        int orderCounter = 0;
        for (int i = 0; i < Parameters.numberOfCustomers; i++){
            int number = generator.nextInt(customers.length);
            number = number % customers.length;
            while (usedNumbers.contains(number)){
                number = generator.nextInt(Parameters.numberOfCustomers);
                number = number % customers.length;
            }
            newCustomers[customerCounter] = customers[number];
            newCustomers[customerCounter].setCustomerID(customerCounter);
            newCustomers[customerCounter].setOrderId(orderCounter);
            orderCounter += newCustomers[customerCounter].orders.length;
            usedNumbers.add(number);
            customerCounter++;
        }
        return newCustomers;
    }

    private static Depot setCustomDepot(){
        double xCoordinate = 10.0747022851127;
        double yCoordinate = 59.05655053185615;
        return new Depot(xCoordinate, yCoordinate);
    }


    private static Vehicle[] getRandomSeedFromVehicles(Vehicle[] vehicles){
        Random generator = new Random(Parameters.randomSeedValue);
        Vehicle[] newVehicles = new Vehicle[Parameters.numberOfVehicles];
        HashSet<Integer> usedNumbers = new HashSet<>();
        int counter = 0;
        for (int i = 0; i < Parameters.numberOfVehicles; i++){
            int number = generator.nextInt(vehicles.length);
            number = number % vehicles.length;
            while (usedNumbers.contains(number)){
                number = generator.nextInt(Parameters.numberOfVehicles);
                number = number % vehicles.length;
            }
            newVehicles[counter] = vehicles[number];
            newVehicles[counter].setVehicleID(counter);
            usedNumbers.add(number);
            counter++;
        }
        return newVehicles;
    }

    public static void displayData(Data data){
        for (Customer customer :data.customers){
            System.out.println(customer.toString());
        }
        for (Vehicle vehicle : data.vehicles){
            System.out.println(vehicle.toString());
        }
    }

    public static void main(String[] args){
        Data data = loadData();
        System.out.println("test");
    }
}
