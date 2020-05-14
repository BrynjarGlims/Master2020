package Master2020.Testing;

import Master2020.DataFiles.Data;
import Master2020.Individual.Individual;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;

public class AdsplitTest {

    public static void tripsWithOverload(Data data, int p, int vt, OrderDistribution orderDistribution, ArrayList<ArrayList<Integer>> matrixOfTrips){
        for (ArrayList<Integer> trips: matrixOfTrips){
            double load = 0;
            for (int customer : trips){
                load += orderDistribution.getOrderVolumeDistribution(p, customer);
            }
            if (load > data.vehicleTypes[vt].capacity){
                //System.out.println("Overload at period " + p + " vehicleType " + vt + "load: " + load );
            }
            else{
                //System.out.println("Underload with "+ load);
            }
        }
    }
}
