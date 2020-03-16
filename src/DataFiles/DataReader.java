package DataFiles;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
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
                content.add(line.split(";"));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found exception in readCSVFile");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private static Customer[] parseOrdersFileData(List<String[]> productData){
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

    private static double[] getCoordinates(List<String[]> timeWindowData, int line){
        double x_coordinate = Double.parseDouble(timeWindowData.get(line)[7]);
        double y_coordinate = Double.parseDouble(timeWindowData.get(line)[8]);
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
        else if (number > Parameters.maxJourneyDuration){
            return Parameters.maxJourneyDuration;
        }
        else{
            return number;
        }
    }

    private static Vehicle[] parseVehicleFileDataToVehicle(List<String[]> vehiclesData){
        List<Vehicle> vehicleList = new ArrayList<>();

        HashMap<String, VehicleType> vehicleTypeHashMap = new HashMap<String, VehicleType>();
        int vehicleCounter = 0;

        for (int line = 0; line < vehiclesData.size(); line++){
            //error check
            if (vehiclesData.get(line)[18].equals("") || Integer.parseInt(vehiclesData.get(line)[24]) < 10000){

                //current capacity
                String tempCapacity = vehiclesData.get(line)[24];

                if ( !vehicleTypeHashMap.containsKey(tempCapacity)){
                    //create new vehicle type
                    vehicleTypeHashMap.put(tempCapacity, new VehicleType(vehiclesData.get(line)[24],
                            Integer.parseInt(vehiclesData.get(line)[18]),
                            Integer.parseInt(vehiclesData.get(line)[19]),
                            Integer.parseInt(vehiclesData.get(line)[20]),
                            Integer.parseInt(vehiclesData.get(line)[21])));

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



    private static Customer[] removeInvalidCustomers(Customer[] customers){
        //Function to remove invalid data
        customers = removeInvalidNonDivOrderCombination(customers);
        return customers;
    }

    private static Customer[] removeInvalidNonDivOrderCombination(Customer[] customers){
        List<Integer> indexes = new ArrayList<Integer>() ;
        for (int i = 0; i < customers.length; i++){
            if (customers[i].numberOfNonDividableOrders > customers[i].numberOfVisitPeriods){
                indexes.add(i);
            }
        }
        Customer[] newCustomers = removeElementsInArray(customers, indexes);
        return newCustomers;
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

        List<String[]> orderData = DataReader.readCSVFile(Parameters.ordersFilePath);
        List<String[]> timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath);
        List<String[]> vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath);

        Customer[] customers = parseOrdersFileData(orderData);
        customers = parseTimeWindowFileData(customers, timeWindowData);
        customers = removeInvalidCustomers(customers);
        Vehicle[] vehicles = parseVehicleFileDataToVehicle(vehiclesData);
        Depot depot = parseVehicleFileDataToDepot(vehiclesData);
        Customer[] customersSubset = Arrays.copyOfRange(customers, 0, Parameters.numberOfCustomers);
        Vehicle[] vehiclesSubset = Arrays.copyOfRange(vehicles, 0, Parameters.numberOfVehicles);
        VehicleType[] vehicleTypes = getAndOrderVehicleTypes(vehiclesSubset);


        Data data = new Data(customersSubset, vehiclesSubset, depot, vehicleTypes);
        if(verbose){
            displayData(data);
        }

        return data;

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
    }

}
