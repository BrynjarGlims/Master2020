package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class HelperFunctions {


    public static ArrayList<Integer>[] parsePosition(Data data, int period, double[] position){
        ArrayList[] customerVisits = new ArrayList[data.numberOfVehicleTypes];
        int[] sortedIndices = IntStream.range(0, position.length)
                .boxed()
                .sorted(Comparator.comparingDouble(i -> position[i]))
                .mapToInt(i -> i)
                .toArray();

        int numVisitsByVehicleType;
        int[] accumulatedValues = new int[data.numberOfVehicleTypes + 1];
        int accumulatedValue = 0;
        for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
            int finalVt = vt;
            numVisitsByVehicleType = (int) Arrays.stream(position).filter(c -> c >= finalVt).filter(c -> c < (finalVt + 1)).count();
            customerVisits[vt] = new ArrayList<>(Arrays.asList(new Integer[numVisitsByVehicleType]));
            accumulatedValue += numVisitsByVehicleType;
            accumulatedValues[vt + 1] = accumulatedValue;
        }
        int index = 0;
        for (int vt = 1 ; vt < data.numberOfVehicleTypes + 1 ; vt++) {
            while (index < accumulatedValues[vt]) {
                customerVisits[vt - 1].set(index - accumulatedValues[vt - 1], data.customersInPeriod.get(period)[sortedIndices[index]]);
                index++;
            }
        }
        return customerVisits;
    }

    public static PositionObject dividePosition(Data data, int period, double[] position){
        ArrayList[] customerVisits = new ArrayList[data.numberOfVehicleTypes];
        ArrayList[] positionsPerVehicleType = new ArrayList[data.numberOfVehicleTypes];
        ArrayList[] indicesPerVehicleType = new ArrayList[data.numberOfVehicleTypes];
        int[] sortedIndices = IntStream.range(0, position.length)
                .boxed()
                .sorted(Comparator.comparingDouble(i -> position[i]))
                .mapToInt(i -> i)
                .toArray();

        int numVisitsByVehicleType;
        int[] accumulatedValues = new int[data.numberOfVehicleTypes + 1];
        int accumulatedValue = 0;
        for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
            int finalVt = vt;
            numVisitsByVehicleType = (int) Arrays.stream(position).filter(c -> c >= finalVt).filter(c -> c < (finalVt + 1)).count();
            positionsPerVehicleType[vt] = new ArrayList<>(Arrays.asList(new Integer[numVisitsByVehicleType]));
            indicesPerVehicleType[vt] = new ArrayList<>(Arrays.asList(new Integer[numVisitsByVehicleType]));
            customerVisits[vt] = new ArrayList<>(Arrays.asList(new Integer[numVisitsByVehicleType]));
            accumulatedValue += numVisitsByVehicleType;
            accumulatedValues[vt + 1] = accumulatedValue;
        }
        int index = 0;
        for (int vt = 1 ; vt < data.numberOfVehicleTypes + 1 ; vt++) {
            while (index < accumulatedValues[vt]) {
                customerVisits[vt - 1].set(index - accumulatedValues[vt - 1], data.customersInPeriod.get(period)[sortedIndices[index]]);
                positionsPerVehicleType[vt - 1].set(index - accumulatedValues[vt - 1], position[sortedIndices[index]]);
                indicesPerVehicleType[vt - 1].set(index - accumulatedValues[vt - 1], sortedIndices[index]);
                index++;
            }
        }
        return new PositionObject(customerVisits, positionsPerVehicleType, indicesPerVehicleType);
    }


    public static Individual createIndividual(Data data, ArrayList<Journey>[][] journeys, OrderDistribution orderDistribution){
        Individual individual = new Individual(data);
        individual.journeyList = journeys;
        individual.orderDistribution = orderDistribution;
        individual.setGiantTourFromJourneys();
        return individual;
    }

}
