package Master2020.Utils;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class DistanceMatrixBuilder {
    private static LatLng[] getCoordinates(Data data, int from,  int to){
        LatLng[] coordinates = new LatLng[to - from + 2];
        System.out.println(data.customers.length);
        for (int c = from ; c < to ; c++){
            System.out.println("c: " + c);
            coordinates[c - from] = new LatLng(data.customers[c].yCoordinate, data.customers[c].xCoordinate);
        }
        coordinates[to - from] = new LatLng(data.depot.yCoordinate, data.depot.xCoordinate);
        coordinates[to - from + 1] = new LatLng(data.depot.yCoordinate, data.depot.xCoordinate);
        return coordinates;
    }

    private static void saveDistanceMap(Data data, String name) throws IOException, ApiException, InterruptedException {
        HashMap<Integer, HashMap<Integer, Long>> map = GoogleDistancesMap(data);
        String path = System.getProperty("user.dir") + "\\data\\Distances\\" + name + ".ser";
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
        out.writeObject(map);
        out.close();
        fileOutputStream.close();
        System.out.println("successfully written " + name + " to location: " + path);
    }

    private static HashMap<Integer, HashMap<Integer, Long>> loadDistanceMap(String name)  {
        HashMap<Integer, HashMap<Integer, Long>> map = null;
        try {
            String path = System.getProperty("user.dir") + "\\data\\Distances\\" + name + ".ser";
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            map = (HashMap<Integer, HashMap<Integer, Long>>) in.readObject();
            System.out.println("successfully loaded file: " + path);
            in.close();
            fileIn.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return map;
    }

    private static void fillMap(Data data, int from, int to, GeoApiContext context, HashMap<Integer, HashMap<Integer, Long>> map) throws InterruptedException, ApiException, IOException {
        LatLng[] coordinates = getCoordinates(data, from, to);
        for (int fromCustomer = 0 ; fromCustomer < data.customers.length ; fromCustomer++){
            System.out.println(fromCustomer);
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix result = req.origins(new LatLng(data.customers[fromCustomer].yCoordinate,data.customers[fromCustomer].xCoordinate))
                    .destinations(coordinates)
                    .mode(TravelMode.DRIVING)
                    .language("en-US")
                    .await();
            HashMap<Integer, Long> mapEntry = map.get(data.customers[fromCustomer].customerNumber);
            map.get(data.customers[fromCustomer].customerNumber).put(0, result.rows[0].elements[coordinates.length - 1].duration.inSeconds);
            for (int toCustomer = 0 ; toCustomer < coordinates.length - 2 ; toCustomer++){
                mapEntry.put(data.customers[toCustomer + from].customerNumber, result.rows[0].elements[toCustomer].duration.inSeconds);
            }
        }
        HashMap<Integer,Long> depot = map.get(0);
        for (int fromCustomer = 0 ; fromCustomer < coordinates.length - 2 ; fromCustomer++){
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix result = req.origins(new LatLng(data.depot.yCoordinate,data.depot.xCoordinate))
                    .destinations(coordinates)
                    .mode(TravelMode.DRIVING)
                    .language("en-US")
                    .await();
            map.get(0).put(data.customers[fromCustomer + from].customerNumber, result.rows[0].elements[fromCustomer].duration.inSeconds);
        }
    }

    private static HashMap<Integer, HashMap<Integer, Long>> GoogleDistancesMap(Data data) throws InterruptedException, ApiException, IOException {

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .build();


        HashMap<Integer, HashMap<Integer, Long>> map = new HashMap<>();
        for (Customer c : data.customers){
            map.put(c.customerNumber, new HashMap<>());
        }
        map.put(0, new HashMap<>());
        fillMap(data, 0, 50, context, map);
        fillMap(data, 50, data.numberOfCustomers, context, map);

        context.shutdown();
        return map;
    }

    public static double[][] createDistanceMatrix(Data data, String name) throws IOException, ClassNotFoundException {
        HashMap<Integer, HashMap<Integer, Long>> distanceMap = loadDistanceMap(name);
        double[][] distanceMatrix = new double[data.numberOfNodes][data.numberOfNodes];
        for  (int from = 0 ; from < data.numberOfCustomers ; from++){
            for (int to = 0 ; to < data.numberOfCustomers ; to++){
                distanceMatrix[from][to] = distanceMap.get(data.customers[from].customerNumber).get(data.customers[to].customerNumber) * 0.000277777778; //seconds to hours
            }
            distanceMatrix[from][data.numberOfCustomers] = distanceMap.get(data.customers[from].customerNumber).get(0) * 0.000277777778;
            distanceMatrix[from][data.numberOfCustomers + 1] = distanceMap.get(data.customers[from].customerNumber).get(0) * 0.000277777778;
            distanceMatrix[data.numberOfCustomers][from] = distanceMap.get(data.customers[from].customerNumber).get(0) * 0.000277777778;
            distanceMatrix[data.numberOfCustomers + 1][from] = distanceMap.get(data.customers[from].customerNumber).get(0) * 0.000277777778;
        }
        return distanceMatrix;
    }

    public static void main(String[] args) throws InterruptedException, ApiException, IOException, ClassNotFoundException {
//        Data data = DataReader.loadData();
////        saveDistanceMap(data, Parameters.distancePath);
//        HashMap<Integer, HashMap<Integer, Long>> map = DistanceMatrixBuilder.loadDistanceMap(Parameters.distancePath);
//        System.out.println(map.get(data.customers[0].customerNumber).get(data.customers[1].customerNumber));
//        createDistanceMatrix(data, Parameters.distancePath);


        ArrayList<Integer> test = new ArrayList<>();
        test.add(1);
        test.add(2);
        test.add(3);
        test.add(4);
        test.subList(5,test.size()).clear();
        System.out.println(test);


    }

}
