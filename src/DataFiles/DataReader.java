package DataFiles;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataReader {


    public static List<String[]> readCSVFile( String file) {
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

    public static Customer[] parseOrdersFileData(List<String[]> productData){
        List<Customer> customerList = new ArrayList<Customer>();
        List<Product> productList = new ArrayList<Product>();
        int customerCount = 0;
        int productID = 0;

        for(int line = 0; line < productData.size(); line++){
            if (line != 0){
                if (!productData.get(line-1)[0].equals(productData.get(line)[0]) || line == (productData.size()-1)) {
                    customerList.add(new Customer(customerCount, Integer.parseInt(productData.get(line-1)[0]), productData.get(line-1)[1]));
                    customerList.get(customerCount).setProducts(convertProductList(productList));
                    productList = new ArrayList<>();
                    customerCount++;
                }
            }
            productList.add(new Product(productID,
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

    public static boolean checkSplitAttribute( String flagg, int line){
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

    public static Product[] convertProductList( List<Product> productList ){
        Product[] products = new Product[productList.size()];
        for (int i = 0; i < productList.size(); i++){
            products[i] = productList.get(i);
        }
        return products;
    }

    public static Customer[] convertCustomerList( List<Customer> customerList ){
        Customer[] customers = new Customer[customerList.size()];
        for (int i = 0; i < customerList.size(); i++){
            customers[i] = customerList.get(i);
        }
        return customers;
    }

    public static Customer[] parseTimeWindowFileData(Customer[] customers, List<String[]> timeWindowData){
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

    public static double[] getCoordinates(List<String[]> timeWindowData, int line){
        double x_coordinate = Double.parseDouble(timeWindowData.get(line)[7]);
        double y_coordinate = Double.parseDouble(timeWindowData.get(line)[8]);
        double[] coordinates = {x_coordinate, y_coordinate};
        return coordinates;
    }

    public static double[] getLoadingTime(List<String[]> timeWindowData, int line){
        double fixedLoadingTime = Double.parseDouble(timeWindowData.get(line)[9]);
        double variableLoadingTime = Double.parseDouble(timeWindowData.get(line)[10]);
        double fixedUnloadingTime = Double.parseDouble(timeWindowData.get(line)[11]);
        double variableUnloadingTime = Double.parseDouble(timeWindowData.get(line)[12]);
        double[] loadingTimes = {fixedLoadingTime,variableLoadingTime,fixedUnloadingTime,variableUnloadingTime};
        return loadingTimes;
    }

    public static double[][] setTimeWindows (double[][] timeWindows, List<String[]> timeWindowData, int line){

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
    
    public static double convertTimeToDouble(String time){
        String[] convertedTime = time.split(":");
        return Double.parseDouble(convertedTime[0]) + Double.parseDouble(convertedTime[1])/60;
    }

    public static Vehicle[] parseVehicleFileData(List<String[]> vehiclesData){
        List<Vehicle> vehicleList = new ArrayList<>();

        for (int line = 0; line < vehiclesData.size(); line++){
            if (vehiclesData.get(line)[18].equals("") || Integer.parseInt(vehiclesData.get(line)[24]) < 10000){
                //todo: chech if more removal of invalid data is needed
                vehicleList.add(new Vehicle(Integer.parseInt(vehiclesData.get(line)[0]),
                        vehiclesData.get(line)[2],
                        Integer.parseInt(vehiclesData.get(line)[24]),
                                vehiclesData.get(line)[28],
                        Integer.parseInt(vehiclesData.get(line)[18]),
                        Integer.parseInt(vehiclesData.get(line)[19]),
                        Integer.parseInt(vehiclesData.get(line)[20]),
                        Integer.parseInt(vehiclesData.get(line)[21])));
            }
        }

        Vehicle[] vehicles = convertVehicleList(vehicleList);
        return vehicles;
    }

    public static Vehicle[] convertVehicleList( List<Vehicle> vehicleList){
        Vehicle[] vehicles = new Vehicle[vehicleList.size()];
        for (int i = 0; i < vehicleList.size(); i++){
            vehicles[i] = vehicleList.get(i);
        }
        return vehicles;

    }

    public static Data loadData(){
        // Master function

        // List<String[]> customerData = Data.DataReader.readCSVFile(Data.Parameters.customersFilePath);
        List<String[]> orderData = DataReader.readCSVFile(Parameters.ordersFilePath);
        List<String[]> timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath);
        List<String[]> vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath);

        Customer[] customers = parseOrdersFileData(orderData);
        customers = parseTimeWindowFileData(customers, timeWindowData);
        Vehicle[] vehicles = parseVehicleFileData(vehiclesData);
        Data data = new Data(customers, vehicles);
        return data;

    }

    public static Data loadSubsetData(int numberOfCustomer, int numberOfVehicles){

        List<String[]> orderData = DataReader.readCSVFile(Parameters.ordersFilePath);
        List<String[]> timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath);
        List<String[]> vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath);

        Customer[] customers = parseOrdersFileData(orderData);
        customers = parseTimeWindowFileData(customers, timeWindowData);
        Vehicle[] vehicles = parseVehicleFileData(vehiclesData);
        Customer[] customersSubset = Arrays.copyOfRange(customers, 0, numberOfCustomer);
        Vehicle[] vehiclesSubset = Arrays.copyOfRange(vehicles, 0, numberOfVehicles);
        Data data = new Data(customersSubset, vehiclesSubset);
        return data;

        // TODO: 31.01.2020 Implement random data extraction if needed
    }


    public static void main(String[] args){
        // Temporary main function
        Data dataSubset = loadSubsetData(10,5);

    }

}
