package Master2020.Utils;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.TravelMode;

import java.io.IOException;

public class DistanceMatrix {

    public static void main(String[] args) throws InterruptedException, ApiException, IOException {


        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .build();
        DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
        com.google.maps.model.DistanceMatrix result = req.origins("Oslo")
                .destinations("Trondheim")
                .mode(TravelMode.DRIVING)
                .language("en-US")
                .await();

        String distApart = result.rows[0].elements[0].distance.humanReadable;
        System.out.println(distApart);
        context.shutdown();


    }
}
