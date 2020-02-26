package Genetic;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.GiantTour;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import org.w3c.dom.ls.LSOutput;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTourCrossover {

    static Data data;
    static int numPeriodVehicleTypeCouples;

    public GiantTourCrossover(Data data){
        this.data = data;
        numPeriodVehicleTypeCouples = data.numberOfVehicleTypes * data.numberOfPeriods;

    }

    public GiantTour crossOver(Individual parent1, Individual parent2){
        GiantTour child = new GiantTour(data, false);
        HashSet<Integer>[] sets = initializeSets();
        HashMap<Integer, HashSet<Integer>> visitedCustomers = new HashMap<>();
        for (int i = 0 ; i < data.numberOfPeriods ; i++){
            visitedCustomers.put(i, new HashSet<Integer>());
        }

        inheritParent1(parent1, child, sets[0], sets[2], visitedCustomers);
        HashSet<Integer> combined = new HashSet<>();
        combined.addAll(sets[1]);
        combined.addAll(sets[2]);
        inheritParent2(parent2, child, combined, visitedCustomers);
        findMissingCustomers(visitedCustomers);

        return child;
    }


    private void inheritParent1(Individual parent1, GiantTour child, HashSet<Integer> lambda1, HashSet<Integer> lambdaMix, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step one of vidal 2012
        ArrayList<Integer> copyArrayList;
        for (Integer i : lambda1){
            copyArrayList = new ArrayList<>(getRoute(parent1, i));
            child.chromosome[i / data.numberOfVehicleTypes][i % data.numberOfVehicleTypes] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(i / data.numberOfVehicleTypes).add(c);
            }
        }
        int alpha;
        int beta;
        for (Integer i : lambdaMix){
            if (getRoute(parent1, i).isEmpty()){
                child.chromosome[i / data.numberOfVehicleTypes][i % data.numberOfVehicleTypes] = new ArrayList<>();
                continue;
            }
            alpha = ThreadLocalRandom.current().nextInt(0, (getRoute(parent1, i).size() + 1));
            beta = ThreadLocalRandom.current().nextInt(0, (getRoute(parent1, i).size() + 1));

            copyArrayList = new ArrayList<>();
            if (alpha <= beta){
                for (int j = alpha ; j < beta ; j++){
                    copyArrayList.add(getRoute(parent1, i).get(j));
                }
            }
            else{
                for (int j = 0; j < beta ; j++){
                    copyArrayList.add(getRoute(parent1, i).get(j));
                }
                for (int j = alpha ; j <getRoute(parent1, i).size() ; j++){
                    copyArrayList.add(getRoute(parent1, i).get(j));
                }
            }
            child.chromosome[i / data.numberOfVehicleTypes][i % data.numberOfVehicleTypes] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(i / data.numberOfVehicleTypes).add(c);
            }
        }
    }

    private void inheritParent2(Individual parent2, GiantTour child, HashSet<Integer> combinedSet, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step 2 vidal
        ArrayList<Integer> copyArrayList;
        int period;
        int vehicleType;
        for (Integer i : combinedSet){
            period = i / data.numberOfVehicleTypes;
            vehicleType = i % data.numberOfVehicleTypes;
            if (child.chromosome[period][vehicleType] == null){
                child.chromosome[period][vehicleType] = new ArrayList<>();
                }
            copyArrayList = child.chromosome[period][vehicleType];
            for (int j : getRoute(parent2, i)){
                if (!visitedCustomers.get(period).contains(j)){
                    copyArrayList.add(j);
                    visitedCustomers.get(period).add(j);
                }
            }
        }
    }

    private HashMap<Integer, HashSet<Integer>> findMissingCustomers(HashMap<Integer, HashSet<Integer>> visitedCustomers){
        HashMap<Integer, HashSet<Integer>> missingCustomers = new HashMap<>();
        for (int i = 0 ; i < data.numberOfPeriods ; i++){
            missingCustomers.put(i, new HashSet<Integer>());
        }
        for (Customer c : data.customers){
            for (int i = 0 ; i < data.numberOfPeriods ; i++){
                if (c.requiredVisitPeriod[i] == 1 && !visitedCustomers.get(i).contains(c.customerID)){
                    missingCustomers.get(i).add(c.customerID);
                    System.out.println("found missing customer: " + c.customerID);
                }
            }
        }

        return missingCustomers;
    }



    private HashSet[] initializeSets(){
        HashSet[] sets = new HashSet[3];
        int num1 = ThreadLocalRandom.current().nextInt(0, numPeriodVehicleTypeCouples);
        int num2 = ThreadLocalRandom.current().nextInt(0, numPeriodVehicleTypeCouples);
        int n1 = Math.min(num1, num2);
        int n2 = Math.max(num1, num2);

        List<Integer> allPeriodVehicleTypeCouples = new LinkedList<>();
        for (int i = 0; i < numPeriodVehicleTypeCouples ; i++){
            allPeriodVehicleTypeCouples.add(i);
        }
        Collections.shuffle(allPeriodVehicleTypeCouples);

        HashSet<Integer> lambda1 = new HashSet<>();
        HashSet<Integer> lambda2 = new HashSet<>();
        HashSet<Integer> lambdaMix = new HashSet<>();

        for (int i = 0 ; i < n1 ; i++){
            lambda1.add(allPeriodVehicleTypeCouples.remove(0));
        }
        for (int i = 0 ; i < (n2 - n1) ; i++){
            lambda2.add(allPeriodVehicleTypeCouples.remove(0));
        }
        for (int i = 0 ; i < numPeriodVehicleTypeCouples - n2 ; i++){
            lambdaMix.add(allPeriodVehicleTypeCouples.remove(0));
        }

        sets[0] = lambda1;
        sets[1] = lambda2;
        sets[2] = lambdaMix;
        return sets;
    }

    private ArrayList<Integer> getRoute(Individual individual, int index){
        return individual.giantTour.chromosome[index / data.numberOfVehicleTypes][index % data.numberOfVehicleTypes];
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        GiantTourCrossover GTC = new GiantTourCrossover(data);


        OrderDistribution orderDistribution1 = new OrderDistribution(data);
        orderDistribution1.makeDistribution();
        Individual parent1 = new Individual(data, orderDistribution1);
        parent1.adSplit();

        OrderDistribution orderDistribution2 = new OrderDistribution(data);
        orderDistribution2.makeDistribution();
        Individual parent2 = new Individual(data, orderDistribution2);
        parent2.adSplit();
        GiantTour child = GTC.crossOver(parent1, parent2);

    }


}
