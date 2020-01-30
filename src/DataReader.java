import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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

    public static Customer[] parseOrders(List<String[]> productData){
        List<Customer> customerList = new ArrayList<Customer>();
        List<Product> productList = new ArrayList<Product>();
        int customerCount = 0;
        int productCount = 0;
        int productID = 0;


        for(int line = 0; line < productData.size(); line++){
            if (line != 0){
                if (productData.get(line)[0] != productData.get(line)[0]) {
                    customerList.add(new Customer(Integer.parseInt(productData.get(line)[0]), productData.get(line)[1]));
                    customerList.get(customerCount).setProducts(convertProductList(productList));
                    productList = new ArrayList<Product>();
                    customerCount++;
                    productCount = 0;

                }
            }
            productList.add(new Product(productID,
                    Double.parseDouble(productData.get(line)[11]),
                    checkSplitAttribute(productData.get(line)[6]),
                    productData.get(line)[3],
                    Integer.parseInt(productData.get(line)[7]),
                    Integer.parseInt(productData.get(line)[8]),
                    Integer.parseInt(productData.get(line)[9])));
            productCount++;
            productID++;
        }
        return convertCustomerList(customerList);
    }

    public static boolean checkSplitAttribute( String flagg){
        if (flagg.equals("Volumsplitt")){
            return true;
        }
        else if (flagg.equals("Sone")){
            return false;
        }
        else{
            System.out.println("Unknown isDividable value");
            return false;
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

    public static Customer[] parseTimeWindows(Customer[] customers, List<String[]> timeWindowData){
        int customerCount = 0;
        double[][] timeWindow = new double[6][2];

        for (int line = 0; line < timeWindowData.size(); line++){
            System.out.println(line);
            for (String element : timeWindowData.get(line) ){
                System.out.println(element);
            }
            if (line != 0 && timeWindowData.get(line)[0] != timeWindowData.get(line)[0]) {
                double[] coordinates = getCoordinates(timeWindowData,line-1);
                double[] loadingTimes = getLoadingTime(timeWindowData, line-1);
                customers[customerCount].setTimeWindow(timeWindow);
                customers[customerCount].setCoordinates(coordinates);
                customers[customerCount].setLoadingTimes(loadingTimes);
                if(customers[customerCount].customerID == Integer.parseInt(timeWindowData.get(line)[0]))
                    System.out.println("Wrong order of customers, assignment need to be adjusted");
                customerCount++;
            }
            timeWindow = setTimeWindows(timeWindow, timeWindowData, line);
        }
        return customers;

    }

    public static double[] getCoordinates(List<String[]> timeWindowData, int line){
        double x_coordinate = Double.parseDouble(timeWindowData.get(line)[5]);
        double y_coordinate = Double.parseDouble(timeWindowData.get(line)[6]);
        double[] coordinates = {x_coordinate, y_coordinate};
        return coordinates;
    }

    public static double[] getLoadingTime(List<String[]> timeWindowData, int line){
        double fixedLoadingTime = Double.parseDouble(timeWindowData.get(line)[9]);
        double variableLoadingTime = Double.parseDouble(timeWindowData.get(line)[10]);
        double fixedUnloadingTime = Double.parseDouble(timeWindowData.get(line)[11]);
        double variableUnloadingTime = Double.parseDouble(timeWindowData.get(line)[9]);
        double[] loadingTimes = {fixedLoadingTime,variableLoadingTime,fixedUnloadingTime,variableUnloadingTime};
        return loadingTimes;


    }

    public static double[][] setTimeWindows (double[][] timeWindows, List<String[]> timeWindowData, int line){

        // TODO: 30.01.2020 Change this so the time is correctly implemented 
        if (!timeWindowData.get(line)[13].equals("")){
            timeWindows[0][0] = Double.parseDouble(timeWindowData.get(line)[13]);
            timeWindows[0][1] = Double.parseDouble(timeWindowData.get(line)[14]);
        }
        if (!timeWindowData.get(line)[15].equals("")){
            timeWindows[1][0] = Double.parseDouble(timeWindowData.get(line)[15]);
            timeWindows[1][1] = Double.parseDouble(timeWindowData.get(line)[16]);
        }
        if (!timeWindowData.get(line)[17].equals("")){
            timeWindows[2][0] = Double.parseDouble(timeWindowData.get(line)[17]);
            timeWindows[2][1] = Double.parseDouble(timeWindowData.get(line)[18]);
        }
        if (!timeWindowData.get(line)[19].equals("")){
            timeWindows[3][0] = Double.parseDouble(timeWindowData.get(line)[19]);
            timeWindows[3][1] = Double.parseDouble(timeWindowData.get(line)[20]);
        }
        if (!timeWindowData.get(line)[21].equals("")){
            timeWindows[4][0] = Double.parseDouble(timeWindowData.get(line)[21]);
            System.out.println(timeWindowData.get(line)[21]);
            timeWindows[4][1] = Double.parseDouble(timeWindowData.get(line)[22]);
        }
        if (!timeWindowData.get(line)[23].equals("")){
            timeWindows[5][0] = Double.parseDouble(timeWindowData.get(line)[23]);
            timeWindows[5][1] = Double.parseDouble(timeWindowData.get(line)[24]);
        }
        return timeWindows;
    }
    
    public static double convertTimeToDouble(String time){


        // TODO: 30.01.2020 Implement this 
    }




    public static Data loadData(){
        List<String[]> customerData = DataReader.readCSVFile(Parameters.customersFilePath);
        List<String[]> orderData = DataReader.readCSVFile(Parameters.ordersFilePath);
        List<String[]> timeWindowData = DataReader.readCSVFile(Parameters.timeWindowsFilePath);
        List<String[]> vehiclesData = DataReader.readCSVFile(Parameters.vehicleFilePath);

        Customer[] customers = parseOrders(orderData);
        customers = parseTimeWindows(customers, timeWindowData);
        Data data = new Data(customers);
        return data;





    }
    public static void main(String[] args){
        Data data = loadData();




    }

}
