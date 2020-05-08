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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    private static JSONObject createJSON(Data data){
        JSONObject obj = new JSONObject();
        JSONArray row;
        for (int i = 0 ; i < data.numberOfNodes ; i++){
            row = new JSONArray();
            for (double d : data.distanceMatrix[i]){
                row.add(d);
            }
            row.get(0);
            obj.put(i, row);
        }
        return obj;
    }

    private static void writeJSON(JSONObject obj, String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path);
        fileWriter.write(obj.toJSONString());
        fileWriter.close();
    }

    public static JSONObject readJSON(String path) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(path);
        JSONObject obj = (JSONObject) parser.parse(reader);
        reader.close();
        return obj;
    }

    public static void main(String[] args) throws InterruptedException, ApiException, IOException, ParseException {

        Data data = DataReader.loadData();
        LatLng[] coordinates = getCoordinates(data);
        System.out.println(coordinates[1]);
        JSONObject obj = createJSON(data);
        System.out.println(obj.get(1));
        String path = System.getProperty("user.dir") + "\\data\\Distances\\DistanceMatrix.json";

        //writeJSON(obj, path);
        JSONObject readobj = readJSON(path);
        System.out.println(readobj.get("1").getClass());
        for (Object s : (JSONArray) readobj.get("1")){
            System.out.println(s);
        }

//        GeoApiContext context = new GeoApiContext.Builder()
//                .apiKey(System.getenv("GOOGLE_API_KEY"))
//                .build();
//        DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
//
//        LatLng[] coordinates1 = new LatLng[]{new LatLng(data.customers[0].yCoordinate, data.customers[0].xCoordinate),new LatLng(data.customers[2].yCoordinate, data.customers[2].xCoordinate)};
//
//        DistanceMatrix result = req.origins(coordinates1)
//                .destinations(new LatLng(data.customers[1].yCoordinate, data.customers[1].xCoordinate))
//                .mode(TravelMode.DRIVING)
//                .language("en-US")
//                .await();
//
//        String distApart = result.rows[1].elements[0].distance.humanReadable;
//        long meters = result.rows[1].elements[0].distance.inMeters;
//        System.out.println("dist km: " + distApart);
//        System.out.println("dist meters: " + meters);
//        context.shutdown();
//

    }
}
