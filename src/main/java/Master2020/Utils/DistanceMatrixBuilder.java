package Master2020.Utils;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import javax.annotation.processing.Filer;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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


    private static HashMap<String, HashMap<String, Double>> createHashMap(Data data){
        HashMap<String, HashMap<String, Double>> outMap = new HashMap<>();
        for (int from = 0 ; from < data.numberOfCustomers ; from++){
            HashMap<String, Double> internalMap = new HashMap<>();
            outMap.put(data.customers[from].customerName, internalMap);
            for (int to = 0 ; to < data.numberOfCustomers ; to++){
                internalMap.put(data.customers[to].customerName, data.distanceMatrix[from][to]);
            }
            internalMap.put("Depot", data.distanceMatrix[from][data.numberOfCustomers]);
        }
        return outMap;
    }

    private static void saveDistanceMap(Data data, String name) throws IOException {
        HashMap<String, HashMap<String, Double>> map = createHashMap(data);
        String path = System.getProperty("user.dir") + "\\data\\Distances\\" + name + ".ser";
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
        out.writeObject(map);
        out.close();
        fileOutputStream.close();
    }

    private static HashMap<String, HashMap<String, Double>> loadDistanceMap(String name) throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir") + "\\data\\Distances\\" + name + ".ser";
        FileInputStream fileIn = new FileInputStream(path);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        HashMap<String, HashMap<String, Double>> map = (HashMap<String, HashMap<String, Double>>) in.readObject();
        in.close();
        fileIn.close();
        return map;
    }

    private static HashMap<String, HashMap<String, Double>> GoogleDistancesMap(Data data) throws InterruptedException, ApiException, IOException {
        LatLng[] coordinates = getCoordinates(data);

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .build();
        DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);


        DistanceMatrix result = req.origins(coordinates)
                .destinations(coordinates)
                .mode(TravelMode.DRIVING)
                .language("en-US")
                .await();

        String distApart = result.rows[1].elements[0].distance.humanReadable;
        long meters = result.rows[1].elements[0].distance.inMeters;
        System.out.println("dist km: " + distApart);
        System.out.println("dist meters: " + meters);
        context.shutdown();

    }


    public static void main(String[] args) throws InterruptedException, ApiException, IOException, ParseException, ClassNotFoundException {
//
        Data data = DataReader.loadData();
//        HashMap<String, HashMap<String, Double>> map = createHashMap(data);
        String path = System.getProperty("user.dir") + "\\data\\Distances\\distanceMap.ser";
//        FileOutputStream fileOutputStream = new FileOutputStream(path);
//        ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
//        out.writeObject(map);
//        out.close();
//        fileOutputStream.close();

        FileInputStream fileIn = new FileInputStream(path);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        HashMap<String, HashMap<String, Double>> map = (HashMap<String, HashMap<String, Double>>) in.readObject();
        System.out.println(map.get(data.customers[0].customerName).get("Depot"));
//
//

    }
}
