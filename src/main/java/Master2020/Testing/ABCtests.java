package Master2020.Testing;

import Master2020.DataFiles.Data;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;

import java.util.ArrayList;
import java.util.Arrays;

public class ABCtests {

    public static boolean allCustomersExists(ArrayList<Journey>[][] journeys, Data data){
        boolean found;
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int customer : data.customersInPeriod.get(p)){
                found = false;
                for (int vt  = 0 ; vt < data.numberOfVehicleTypes ; vt++) {
                    for (Journey journey : journeys[p][vt]) {
                        for (Trip trip : journey.trips) {
                            if (trip.customers.contains(customer)){
                                found = true;
                            }
                        }
                    }
                }
                if (!found){
                    return false;
                }
            }
        }
        return true;
    }
}
