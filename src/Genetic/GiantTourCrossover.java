package Genetic;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.Individual;
import Individual.AdSplit;
import ProductAllocation.OrderDelivery;
import ProductAllocation.OrderDistribution;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTourCrossover {

    static Data data;
    static int numPeriodVehicleTypeCouples;
    public static int count = 0;

    public GiantTourCrossover(Data data){
        this.data = data;
        numPeriodVehicleTypeCouples = data.numberOfVehicleTypes * data.numberOfPeriods;

    }

    public Individual crossOver(Individual parent1, Individual parent2){
        Individual child = new Individual(data);
        OrderDistribution orderDistribution = new OrderDistribution(data);
        HashSet<Integer>[] sets = initializeSets();
        HashMap<Integer, HashSet<Integer>> visitedCustomers = new HashMap<>();

        for (int i = 0 ; i < data.numberOfPeriods ; i++){
            visitedCustomers.put(i, new HashSet<Integer>());
        }

        inheritParent1(parent1, child, orderDistribution, sets[0], sets[2], visitedCustomers);
        HashSet<Integer> combined = new HashSet<>();
        combined.addAll(sets[1]);
        combined.addAll(sets[2]);
        inheritParent2(parent2, child, combined, visitedCustomers);
        bestInsertion(child, parent1, parent2, findMissingCustomers(visitedCustomers));
        AdSplit.adSplitPlural(child);
        return child;
    }


    private void inheritParent1(Individual parent1, Individual child, OrderDistribution orderDistribution, HashSet<Integer> lambda1, HashSet<Integer> lambdaMix, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step one of vidal 2012
        ArrayList<Integer> copyArrayList;
        int period;
        int vehicleType;
        for (Integer i : lambda1){
            period = i / data.numberOfVehicleTypes;
            vehicleType = i % data.numberOfVehicleTypes;
            copyArrayList = new ArrayList<>(getRoute(parent1, i));
            child.giantTour.chromosome[period][vehicleType] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(period).add(c);
                updateOrder(parent1.orderDistribution, child.orderDistribution, period, c);
            }
        }
        int alpha;
        int beta;
        for (Integer i : lambdaMix){
            period = i / data.numberOfVehicleTypes;
            vehicleType = i % data.numberOfVehicleTypes;
            if (getRoute(parent1, i).isEmpty()){
                child.giantTour.chromosome[period][vehicleType] = new ArrayList<>();
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
            child.giantTour.chromosome[period][vehicleType] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(period).add(c);
                updateOrder(parent1.orderDistribution, child.orderDistribution, period, c);
            }
        }
    }

    private void inheritParent2(Individual parent2, Individual child, HashSet<Integer> combinedSet, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step 2 vidal
        ArrayList<Integer> copyArrayList;
        int period;
        int vehicleType;
        for (Integer i : combinedSet){
            period = i / data.numberOfVehicleTypes;
            vehicleType = i % data.numberOfVehicleTypes;
            if (child.giantTour.chromosome[period][vehicleType] == null){
                child.giantTour.chromosome[period][vehicleType] = new ArrayList<>();
                }
            copyArrayList = child.giantTour.chromosome[period][vehicleType];
            for (int c : getRoute(parent2, i)){
                if (!visitedCustomers.get(period).contains(c)){
                    copyArrayList.add(c);
                    updateOrder(parent2.orderDistribution, child.orderDistribution, period, c);
                    visitedCustomers.get(period).add(c);
                }
            }
        }
    }

    private void bestInsertion(Individual child, Individual parent1, Individual parent2, HashMap<Integer, HashSet<Integer>> missingCustomers){
        double currentBestFitness;
        List<Integer> customerSequence;
        Individual currentParent;
        int from;
        double tripFitness;
        double tempTripFitness;
        int currentBestVehicleType = 0;
        List<Integer> currentBestSequence = null;
        for (int p : missingCustomers.keySet()){
            for (int c : missingCustomers.get(p)){
                currentBestFitness = Double.MAX_VALUE;
                customerSequence = new ArrayList<>();
                currentParent = ThreadLocalRandom.current().nextInt(0,2) == 0 ? parent1 : parent2;
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    customerSequence.add(c);
                    tempTripFitness = FitnessCalculation.getTripFitness(customerSequence, vt, p, currentParent.orderDistribution.orderVolumeDistribution, data);
                    if (tempTripFitness < currentBestFitness){
                        currentBestFitness = tempTripFitness;
                        currentBestSequence = customerSequence;
                        currentBestVehicleType = vt;
                    }
                    count++;
                    AdSplit.adSplitSingular(child, p, vt);
                    for (int trip = 0 ; trip < child.giantTourSplit.chromosome[p][vt].size() ; trip++){
                        from = trip - 1 == -1 ? 0 : child.giantTourSplit.chromosome[p][vt].get(trip - 1);
                        customerSequence = new LinkedList<>(child.giantTour.chromosome[p][vt].subList(from, child.giantTourSplit.chromosome[p][vt].get(trip)));
                        tripFitness = FitnessCalculation.getTripFitness(customerSequence, vt, p, child.orderDistribution.orderVolumeDistribution, data);
                        for (int i = 0 ; i <= customerSequence.size() ; i++){
                            customerSequence.add(i, c);
                            tempTripFitness = FitnessCalculation.getTripFitness(customerSequence, vt, p, child.orderDistribution.orderVolumeDistribution, data);
                            if(tempTripFitness - tripFitness < currentBestFitness){
                                currentBestSequence = new ArrayList<>(customerSequence);
                                currentBestFitness = tripFitness - currentBestFitness;
                                currentBestVehicleType = vt;
                            }
                            customerSequence.remove(i);
                        }
                    }
                }
                if (currentBestSequence.size() == 1){
                    child.giantTour.chromosome[p][currentBestVehicleType].add(currentBestSequence.get(0));
                    updateOrder(currentParent.orderDistribution, child.orderDistribution, p, c);
                }
                else{
                    child.giantTour.chromosome[p][currentBestVehicleType] = (ArrayList<Integer>) currentBestSequence;
                    updateOrder(currentParent.orderDistribution, child.orderDistribution, p, c);
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

    private void updateOrder(OrderDistribution source, OrderDistribution target, int period, int customer){
        target.orderVolumeDistribution[period][customer] = source.orderVolumeDistribution[period][customer];
        for (int i : source.orderIdDistribution[period][customer]){
            target.orderIdDistribution[period][customer].add(i);
            if (target.orderDeliveries[i] == null){
                target.orderDeliveries[i] = new OrderDelivery(data.numberOfPeriods, data.orders[i], period, source.orderDeliveries[i].orderVolumes[period], source.orderDeliveries[i].dividable);
            }
            else {
                target.orderDeliveries[i].addDelivery(period, source.orderDeliveries[i].orderVolumes[period]);
            }

        }


    }

    private ArrayList<Integer> getRoute(Individual individual, int index){
        return individual.giantTour.chromosome[index / data.numberOfVehicleTypes][index % data.numberOfVehicleTypes];
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        GiantTourCrossover GTC = new GiantTourCrossover(data);


        OrderDistribution orderDistribution1 = new OrderDistribution(data);
        orderDistribution1.makeInitialDistribution();
        Individual parent1 = new Individual(data);
        parent1.initializeIndividual();
        AdSplit.adSplitPlural(parent1);

        OrderDistribution orderDistribution2 = new OrderDistribution(data);
        orderDistribution2.makeInitialDistribution();
        Individual parent2 = new Individual(data);
        parent2.initializeIndividual();
        AdSplit.adSplitPlural(parent2);
        



    }

}
