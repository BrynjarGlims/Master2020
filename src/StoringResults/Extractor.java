package StoringResults;

import DataFiles.Data;
import DataFiles.Vehicle;
import Individual.Trip;

import java.util.*;

public class Extractor {

    public static ArrayList<VehicleResult> giantTourResult(Data data, ArrayList<Integer>[][] giantTour , HashMap<Integer, HashMap<Integer, Trip>> tripMap, ArrayList<Trip>[][] tripList){
        HashMap<Integer, VehicleResult> vehicleResultHashMap;
        VehicleResult tempVehicleResult;
        ArrayList<TripResult> tripResults;
        for(int p = 0; p < data.numberOfPeriods; p++){

            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                tripResults = extractTrips(giantTour[p][vt], tripMap.get(p), tripList[p][vt], p , vt);
                for (TripResult tr : tripResults){
                    return new ArrayList<VehicleResult>(); //todo: make feasible
                }

            }
        }
        return new ArrayList<VehicleResult>(); //todo not finished
    }

    private static ArrayList<TripResult> extractTrips(ArrayList<Integer> giantTour, ArrayList<Integer> giantTourSplit, HashMap<Integer, Integer> vehicleAssignment, int p, int vt) {
        ArrayList<TripResult> tripList = new ArrayList<>();
        TripResult tempTripResult = new TripResult();
        Iterator iterator = giantTourSplit.iterator();
        int split = (Integer) iterator.next();
        for (int i = 0; i < giantTour.size(); i++) {
            tempTripResult.addCustomer(giantTour.get(i));
            tempTripResult.setVehicle(vehicleAssignment.get(giantTour.get(i)));  //todo: inefficient
            if (split - 1 == i) {
                tripList.add(tempTripResult);
                tempTripResult = new TripResult();
                if (i != giantTour.size() - 1)
                    split = (Integer) iterator.next();
            }
        }
        return new ArrayList<TripResult>();
    }
}
