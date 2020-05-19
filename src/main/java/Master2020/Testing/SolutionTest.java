package Master2020.Testing;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;
import Master2020.Interfaces.PeriodicAlgorithm;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.PR.DataMIP;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;

public class SolutionTest {

    public static void checkForInfeasibility(PeriodicSolution periodicSolution, Data data){
        ArrayList<Journey>[][] journeys = periodicSolution.getJourneys();
        OrderDistribution orderDistribution = periodicSolution.getOrderDistribution();

        for (int p = 0; p < journeys.length; p++){
            for (int vt = 0; vt < journeys[p].length; vt++ ){
                for (Journey journey : journeys[p][vt]){
                    for (Trip t : journey.trips){
                        double load = 0;
                        for (int i : t.customers){
                            load += orderDistribution.getOrderVolumeDistribution(p,i);
                        }
                        if (load > data.vehicleTypes[journey.vehicleType].capacity){
                            System.out.println("Overload: " +  load + " with capacity: "
                                    + data.vehicleTypes[journey.vehicleType].capacity + " in period "
                                    + p + " vehicle type " + vt  );
                        }

                    }
                }
            }
        }
    }

    public static void checkJourneyLength ( ArrayList<Journey>[][] journeys, DataMIP data){
        for (int p = 0; p < data.numPeriods; p++){
            for (int vt = 0; vt < data.numVehicleTypes; vt ++){
                for (Journey journey : journeys[p][vt]){
                    if (journey.trips.size() > data.numTrips){
                        throw new IllegalArgumentException("Too many trips in journey");
                    }
                }
            }
        }


    }
}
