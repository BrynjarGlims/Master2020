package Master2020.Testing;

import Master2020.DataFiles.Data;
import Master2020.Individual.Journey;
import Master2020.Individual.Origin;
import org.nustaq.kson.KsonStringOutput;


import java.util.ArrayList;

public class HybridTest {

    public static void displayJourneyTags(ArrayList<Journey>[][] journeys, Data data){
        int[] count = new int[Origin.values().length];
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                for (Journey journey: journeys[p][vt]){
                    for (int o = 0; o < Origin.values().length; o++){
                        if (journey.ID == Origin.values()[o]){
                            count[o] += 1;
                        }
                    }
                }
            }
        }
        System.out.print("Journeys used: ");
        for (int o = 0; o < Origin.values().length; o++){
            System.out.print(Origin.values()[o] + " " + count[o] + " | ");
        }
        System.out.println(" ");
    }


    public static boolean checkIfJourneysExists(ArrayList<Journey>[][] journeys, Data data, int counter){
        boolean journeyExists = false;
        int count = 0;
        Origin id = null;
        for (int p = 0; p < data.numberOfPeriods; p++){
            if (journeys[p][0].size() > 0) {
                id = journeys[p][0].get(0).ID;
                journeyExists = true;
            }
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                for (Journey journey: journeys[p][vt]){
                    count += 1;
                }
            }
        }
        if(id == null){
            System.out.println("No id found for algorithm " + counter );
        }
        return journeyExists;
    }
}
