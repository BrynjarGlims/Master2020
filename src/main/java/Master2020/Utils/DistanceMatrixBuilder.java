package Master2020.Utils;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import java.io.*;
import java.util.HashMap;


public class DistanceMatrixBuilder {
    private static LatLng[] getCoordinates(Data data){
        LatLng[] coordinates = new LatLng[data.numberOfNodes];
        for (Customer c : data.customers){
            coordinates[c.customerID] = new LatLng(c.yCoordinate, c.xCoordinate);
        }
        coordinates[data.customers.length] = new LatLng(data.depot.yCoordinate, data.depot.xCoordinate);
        coordinates[data.customers.length + 1] = new LatLng(data.depot.yCoordinate, data.depot.xCoordinate);
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

    private static HashMap<Integer, HashMap<Integer, Long>> GoogleDistancesMap(Data data) throws InterruptedException, ApiException, IOException {
        LatLng[] coordinates = getCoordinates(data);

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .build();


        HashMap<Integer, HashMap<Integer, Long>> map = new HashMap<>();
        for (int from = 0 ; from < data.customers.length ; from++){
            System.out.println(from);
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix result = req.origins(new LatLng(data.customers[from].yCoordinate,data.customers[from].xCoordinate))
                    .destinations(coordinates)
                    .mode(TravelMode.DRIVING)
                    .language("en-US")
                    .await();
            HashMap<Integer, Long> mapEntry = new HashMap<>();
            map.put(data.customers[from].customerNumber, mapEntry);
            map.get(data.customers[from].customerNumber).put(0, result.rows[0].elements[data.numberOfCustomers].duration.inSeconds);
            for (int to = 0 ; to < data.customers.length ; to++){
                mapEntry.put(data.customers[to].customerNumber, result.rows[0].elements[to].duration.inSeconds);
            }
        }
        HashMap<Integer,Long> depot = new HashMap<>();
        map.put(0, depot);
        for (int from = 0 ; from < data.customers.length ; from++){
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix result = req.origins(new LatLng(data.depot.yCoordinate,data.depot.xCoordinate))
                    .destinations(coordinates)
                    .mode(TravelMode.DRIVING)
                    .language("en-US")
                    .await();
            map.get(0).put(from, result.rows[0].elements[from].duration.inSeconds);
        }
        context.shutdown();
        return map;
    }

    public static double[][] createDistanceMatrix(Data data, String name) throws IOException, ClassNotFoundException {
        HashMap<Integer, HashMap<Integer, Long>> distanceMap = loadDistanceMap("Google_Trondelag");
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

}
