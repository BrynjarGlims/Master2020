package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.Individual.AdSplit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Swarm {

    Data data;

    public Swarm(Data data){
        this.data = data;
    }

    public ArrayList<Integer>[] parsePosition(Bee bee){
        ArrayList[] customerVisits = new ArrayList[data.numberOfVehicleTypes];
        int[] sortedIndices = IntStream.range(0, bee.position.length)
                .boxed()
                .sorted(Comparator.comparingDouble(i -> bee.position[i]))
                .mapToInt(i -> i)
                .toArray();

        int numVisitsByVehicleType;
        int[] accumulatedValues = new int[data.numberOfVehicleTypes + 1];
        int accumulatedValue = 0;
        for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
            int finalVt = vt;
            numVisitsByVehicleType = (int) Arrays.stream(bee.position).filter(c -> c >= finalVt).filter(c -> c < (finalVt + 1)).count();
            customerVisits[vt] = new ArrayList<>(Arrays.asList(new Integer[numVisitsByVehicleType]));
            accumulatedValue += numVisitsByVehicleType;
            accumulatedValues[vt + 1] = accumulatedValue;
        }
        int index = 0;
        for (int vt = 1 ; vt < data.numberOfVehicleTypes + 1 ; vt++) {
            while (index < accumulatedValues[vt]) {
                customerVisits[vt - 1].set(index - accumulatedValues[vt - 1], data.customersInPeriod.get(bee.period)[sortedIndices[index]]);
                index++;
            }
        }
        return customerVisits;


        }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        Swarm swarm = new Swarm(data);
        Bee bee = new Bee(data, 1);
        bee.scout();
        ArrayList<Integer>[] p = swarm.parsePosition(bee);
        System.out.println(Arrays.stream(data.customers[0].requiredVisitPeriod).filter(x -> x==1).count());
        System.out.println(data.customers[0].nonDividableOrders.length);
    }

}
