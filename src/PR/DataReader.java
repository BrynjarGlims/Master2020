package PR;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;


public class DataReader {


    public static double[] concat(double[] array1, double[] array2) {
        int aLen = array1.length;
        int bLen = array2.length;
        double[] result = new double[aLen + bLen];
        System.arraycopy(array1, 0, result, 0, aLen);
        System.arraycopy(array2, 0, result, aLen, bLen);
        return result;
    }
    private static double[][] timeWindows(String windows){
        ArrayList<double[]> timeWindows = new ArrayList<>();
        String[] stringWindow = windows.replaceAll("[\\s|\\[|\\]]","").split(",");

        for (int i = 0 ; i < stringWindow.length ; i+= 2){
            timeWindows.add(new double[]{Double.parseDouble(stringWindow[i]), Double.parseDouble(stringWindow[i+1])});
        }
        double[][] output = new double[timeWindows.size()][2];
        for (int i = 0 ; i < timeWindows.size() ; i++){
            output[i] = timeWindows.get(i);
        }
        return output;
    }
    public static void readParameters(String line, DataMIP dataMIP){
        String[] tokens = line.split("\t");
        dataMIP.numCustomers = Integer.parseInt(tokens[0]);
        dataMIP.customers = new Customer[dataMIP.numCustomers];
        dataMIP.numVehicles = Integer.parseInt(tokens[1]);
        dataMIP.vehicles = new Vehicle[dataMIP.numVehicles];
        dataMIP.numVehicleTypes = Integer.parseInt(tokens[2]);
        dataMIP.vehicleTypes = new VehicleType[dataMIP.numVehicleTypes];
        dataMIP.numPeriods = Integer.parseInt(tokens[3]);
        dataMIP.latestInTime = Integer.parseInt(tokens[4]);
        dataMIP.numTrips =Integer.parseInt(tokens[5]);
        //dataMIP.costOvertime = Integer.parseInt(tokens[6]);
        double[] overtimes = new double[dataMIP.numPeriods];
        Arrays.fill(overtimes, Double.parseDouble(tokens[7]));
        dataMIP.overtimeLimit = overtimes;
        dataMIP.xCoordinateDepot = Double.parseDouble(tokens[8]);
        dataMIP.yCoordinateDepot = Double.parseDouble(tokens[9]);

    }

    public static void readCustomer(String line, DataMIP dataMIP){
        String[] tokens = line.split("\t");
        int ID = Integer.parseInt(tokens[0]);

        double[] nonDivProducts = Arrays.stream(tokens[1].
                replaceAll("[\\[\\]]","").trim().
                split("\\s+")).
                mapToDouble(Double::parseDouble).toArray();

        double[] divProducts = Arrays.stream(tokens[2].
                replaceAll("[\\[\\]]","").trim().
                split("\\s+")).
                mapToDouble(Double::parseDouble).toArray();


        double[] products = concat(divProducts, nonDivProducts);
        int[] types = new int[divProducts.length + nonDivProducts.length];
        Arrays.fill(types, 0, divProducts.length, 1);

        //Insert minFrequency
        int[] minFrequencyProduct = Arrays.stream(tokens[3].replaceAll("[\\s|\\[|\\]]","").
                split(",")).mapToInt(Integer::parseInt).toArray();

        //Insert maxFrequency
        int[] maxFrequencyProduct = Arrays.stream(tokens[4].replaceAll("[\\s|\\[|\\]]","").
                split(",")).mapToInt(Integer::parseInt).toArray();

        //Insert minQuantity
        double[] minQuantityProduct = Arrays.stream(tokens[5].replaceAll("[\\s|\\[|\\]]","").
                split(",")).mapToDouble(Double::parseDouble).toArray();
        //Insert maxQuantity
        double[] maxQuantityProduct = Arrays.stream(tokens[6].replaceAll("[\\s|\\[|\\]]","").
                split(",")).mapToDouble(Double::parseDouble).toArray();


        double[][] timeWindows = timeWindows(tokens[7]);
        int[] visitDays = Arrays.stream(tokens[8].
                replaceAll("\\s|\\[|\\]","").
                split("")).
                mapToInt(Integer::parseInt).toArray();
        double fixedUnloading = Double.parseDouble(tokens[9]);
        double xCoordinate = Double.parseDouble(tokens[10]);
        double yCoordinate = Double.parseDouble(tokens[11]);
        double distanceToDepot = Math.sqrt(Math.pow(xCoordinate - dataMIP.xCoordinateDepot, 2) + Math.pow(yCoordinate - dataMIP.yCoordinateDepot, 2));
        dataMIP.addCustomer(new Customer(ID, products, types, timeWindows, visitDays, minFrequencyProduct, maxFrequencyProduct, minQuantityProduct, maxQuantityProduct, fixedUnloading, xCoordinate, yCoordinate, distanceToDepot));
    }

    public static void readVehicle(String line, DataMIP dataMIP, AtomicInteger vehicleCounter){
        String[] tokens = line.split("\t");
        int type = Integer.parseInt(tokens[0]);
        int numVehicles = Integer.parseInt(tokens[1]);
        int drivingCost = Integer.parseInt(tokens[2]);
        int unitCost = Integer.parseInt(tokens[3]);
        int capacity = Integer.parseInt(tokens[4]);
        double loadingTime = Double.parseDouble(tokens[5]);
        VehicleType vehicleType = new VehicleType(type, drivingCost, unitCost, capacity, loadingTime, numVehicles);
        dataMIP.addVehicleType(vehicleType);
        Vehicle vehicle;
        for (int i = 0 ; i < numVehicles ; i++){
            int ID = vehicleCounter.getAndIncrement();
            vehicle = new Vehicle(ID, vehicleType);
            dataMIP.addVehicle(vehicle);
            vehicleType.addVehicle(vehicle, i);
        }

    }

    public static void readDistances(String line, ArrayList<double[]> distances){
        String[] tokens = line.replaceAll("[\\[|,\\]]","").strip().split("\\s+");
        double[] row = new double[tokens.length];
        for (int i = 0 ; i < tokens.length ; i++){
            row[i] = Double.parseDouble(tokens[i]);
        }
        distances.add(row);
    }
    public static void makeDistanceData(ArrayList<double[]> distances, DataMIP dataMIP){
        double[][] distanceMatrix = new double[distances.size()][distances.size()];
        for (int row = 0 ; row < distances.size() ; row++){
            for (int col = 0 ; col < distances.get(row).length ; col++){
                distanceMatrix[row][col] = distances.get(row)[col];
            }
        }
        dataMIP.setDistances(distanceMatrix);
    }

    public static DataMIP readFile(String inputPath) throws FileNotFoundException {
        DataMIP dataMIP = new DataMIP();
        dataMIP.instanceName = inputPath;
        ArrayList<double[]> distances = new ArrayList<>();
        File file = new File(inputPath);
        Scanner scanner = new Scanner(file);
        int mode = 0;
        AtomicInteger vehicleCounter = new AtomicInteger(0);
        while (scanner.hasNext()){
            String line = scanner.nextLine();
            if (line.equals("")){
                mode += 1;
                continue;
            }
            switch (mode){
                case 0:
                    readParameters(line, dataMIP);
                    break;
                case 1:
                    readCustomer(line, dataMIP);
                    break;
                case 2:
                    readVehicle(line, dataMIP, vehicleCounter);
                    break;
                case 3:
                    readDistances(line, distances);
                    break;
            }
        }
        makeDistanceData(distances, dataMIP);
        dataMIP.initialize();
        return dataMIP;
    }

    public static DataMIP initialieNewData(DataFiles.Data data ) {

        return new DataMIP();


    }


}
