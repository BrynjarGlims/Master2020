package Master2020.Utils;

import Master2020.DataFiles.*;
import Master2020.Individual.Journey;
import Master2020.Individual.Origin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {



    public static double averageDistanceToNeighbor(Data data){
        double distance = 0;
        double neighborDistance;
        for (Customer c : data.customers){
            neighborDistance = 0;
            for (Customer neighbor : c.nearestNeighbors){
                neighborDistance += data.distanceMatrix[c.customerID][neighbor.customerID];
            }
            distance += neighborDistance / c.nearestNeighbors.size();
        }
        return distance / data.numberOfCustomers;
    }

    public static double averageOrderVolume(Data data){
        double volume = 0;
        for (Customer c : data.customers){
            for (Order o : c.orders){
                volume += o.volume;
            }
        }
        return volume /data.numberOfCustomers;
    }

    public static Function<List<?>, Function<Integer, Consumer<Integer>>> reverse = list -> int1 -> int2 -> {
        reverse(list, int1, int2);
    };

    public static Function<List<?>, Function<Integer, Consumer<Integer>>> insert = list -> int1 -> int2 -> {
        insert(list, int1, int2);
    };

    public static Function<List<?>, Function<Integer, Consumer<Integer>>> swap = list -> int1 -> int2 -> {
        Collections.swap(list, int1, int2);
    };

    public static <E> void insert(List<E> list, int pos1, int pos2){
        E insertElement = list.get(pos1);
        if (pos1 < pos2){
            for (int i = pos1 ; i < pos2 ; i++){
                list.set(i, list.get(i + 1));
            }
        }
        else{
            for (int i = pos1 ; i > pos2 ; i--){
                list.set(i, list.get(i - 1));
            }
        }
        list.set(pos2, insertElement);
    }

    public static <E> void reverse(List<E> list, int pos1, int pos2){
        int from = Math.min(pos1, pos2);
        int to = Math.max(pos1, pos2);
        List<E> sublist = list.subList(from, to + 1);
        Collections.reverse(sublist);
        int count = 0;
        for (int i = from ; i < to + 1; i++){
            list.set(i, sublist.get(count));
            count++;

        }
    }

    public static <T> List<List<T>> Permutate(List<T> original) {
        if (original.isEmpty()) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        T firstElement = original.remove(0);
        List<List<T>> returnValue = new ArrayList<>();
        List<List<T>> permutations = Permutate(original);
        for (List<T> smallerPermutated : permutations) {
            for (int index=0; index <= smallerPermutated.size(); index++) {
                List<T> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }

    public static int[] getJourneyTags(ArrayList<Journey>[][] journeys, Data data){
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
        int[] result = {count[0], count[1]};
        return result;
    }



    public static void main(String[] args){

        double distances = 0;
        double volumes = 0;
        for (int i = 0 ; i < 100 ; i++){
            System.out.println(" --- SEED: " + i + " ---");
            Parameters.randomSeedValue = i;
            Data data = DataReader.loadData();
            double averageDistance = averageDistanceToNeighbor(data);
            double averageVolume = averageOrderVolume(data);
            volumes += averageVolume;
            distances += averageDistance;

            System.out.println(averageDistance);
            System.out.println(averageVolume);

        }
        System.out.println("average distance: " + distances/100);
        System.out.println("average volume: " + volumes/100);
    }

}


