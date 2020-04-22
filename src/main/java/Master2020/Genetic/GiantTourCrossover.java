package Master2020.Genetic;


import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Trip;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.IndividualTest;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTourCrossover {

    static Data data;
    static int numPeriodVehicleTypeCouples;
    public static int count = 0;



    public static Individual crossOver(Individual parent1, Individual parent2, OrderDistribution orderDistribution){
        data = parent1.data;
        numPeriodVehicleTypeCouples = parent1.numberOfPeriods * data.numberOfVehicleTypes;
        Individual child = new Individual(data, parent1.population, Parameters.isPeriodic, parent1.actualPeriod);
        child.orderDistribution = orderDistribution;
        HashSet<Integer>[] sets = initializeSets();
        HashMap<Integer, HashSet<Integer>> visitedCustomers = new HashMap<>();

        for (int i = 0 ; i < parent1.numberOfPeriods ; i++){
            visitedCustomers.put(i, new HashSet<Integer>());
        }

        inheritParent1(parent1, child, sets[0], sets[2], visitedCustomers);
        HashSet<Integer> combined = new HashSet<>();
        combined.addAll(sets[1]);
        combined.addAll(sets[2]);
        inheritParent2(parent2, child, combined, visitedCustomers);
        bestInsertion(child, orderDistribution, findMissingCustomers(visitedCustomers));
        AdSplit.adSplitPlural(child);
        child.updateFitness();
        return child;
    }


    private static void inheritParent1(Individual parent1, Individual child, HashSet<Integer> lambda1, HashSet<Integer> lambdaMix, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step one of vidal 2012
        ArrayList<Integer> copyArrayList;
        int period;
        int vehicleType;
        for (Integer i : lambda1){
            period = i / parent1.numberOfPeriods;
            vehicleType = i % data.numberOfVehicleTypes;
            copyArrayList = new ArrayList<>(getRoute(parent1, i));
            child.giantTour.chromosome[period][vehicleType] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(period).add(c);
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
                for (int j = alpha ; j < getRoute(parent1, i).size() ; j++){
                    copyArrayList.add(getRoute(parent1, i).get(j));
                }
            }
            child.giantTour.chromosome[period][vehicleType] = copyArrayList;
            for (int c : copyArrayList){
                visitedCustomers.get(period).add(c);
            }
        }
    }

    private static void inheritParent2(Individual parent2, Individual child, HashSet<Integer> combinedSet, HashMap<Integer, HashSet<Integer>> visitedCustomers){ //step 2 vidal
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
                    visitedCustomers.get(period).add(c);
                }
            }
        }
    }

    private static void bestInsertion(Individual child, OrderDistribution orderDistribution, HashMap<Integer, HashSet<Integer>> missingCustomers){
        double currentBestFitness;
        double journeyFitness;
        double tempJourneyFitness;
        int currentBestVehicleType = -1;
        Trip currentBestTrip;
        int currentBestIndex = -1;
        AdSplit.adSplitPlural(child);
        for (int p : missingCustomers.keySet()){
            for (int c : missingCustomers.get(p)){
                currentBestTrip = null;
                currentBestFitness = Double.MAX_VALUE;
                for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                    for (Journey journey : child.journeyList[p][vt]){
                        journeyFitness = FitnessCalculation.getTotalJourneyFitness(journey, orderDistribution);
                        for (Trip trip : journey.trips){
                            for (int i = 0 ; i < trip.customers.size() ; i++){
                                trip.addCustomer(c, i);
                                tempJourneyFitness = FitnessCalculation.getTotalJourneyFitness(journey, orderDistribution) - journeyFitness;
                                if(tempJourneyFitness < currentBestFitness){
                                    currentBestTrip = trip;
                                    currentBestIndex = i;
                                    currentBestFitness = tempJourneyFitness;
                                    currentBestVehicleType = vt;
                                }
                                trip.removeCustomer(c);
                            }
                        }
                    }
                }

                if(currentBestTrip == null){
                    Trip newTrip = makeNewTrip(p, c);
                    child.tripList[p][newTrip.vehicleType].add(newTrip);
                    currentBestVehicleType = newTrip.vehicleType;
                }
                else{
                    currentBestTrip.addCustomer(c, currentBestIndex);
                }

                child.setGiantTourFromTripsPerPeriodVehicleType(p, currentBestVehicleType, child.giantTour);
                AdSplit.adSplitSingular(child, p, currentBestVehicleType);
            }
        }
        // IndividualTest.isMissingCustomersAdded(missingCustomers, child); //todo: create new test
    }

    private static HashMap<Integer, HashSet<Integer>> findMissingCustomers(HashMap<Integer, HashSet<Integer>> visitedCustomers){
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


    private static Trip makeNewTrip(int period, int customer){
        int vt = ThreadLocalRandom.current().nextInt(data.numberOfVehicleTypes);
        int vehicleID = -1;
        for (int v : data.vehicleTypes[vt].vehicleSet){
            vehicleID = v;
            break;
        }
        Trip newTrip = new Trip();
        newTrip.initialize(period, vt, vehicleID);
        List<Integer> customers = new ArrayList<>();
        customers.add(customer);
        newTrip.setCustomers(customers, 0);
        return newTrip;
    }

    private static HashSet[] initializeSets(){
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

    private static ArrayList<Integer> getRoute(Individual individual, int index){
        return individual.giantTour.chromosome[index / data.numberOfVehicleTypes][index % data.numberOfVehicleTypes];
    }



    public static void main(String[] args){
        Data data = DataReader.loadData();
        int correctCustomers = 0;
        for (Customer customer : data.customers){
            if(customer.numberOfNonDividableOrders == customer.numberOfVisitPeriods){
                correctCustomers++;
            }
        }

        OrderDistribution od1 = new OrderDistribution(data);
        od1.makeInitialDistribution();
        Individual individual1 = new Individual(data);
        individual1.initializeIndividual(od1);
        AdSplit.adSplitPlural(individual1);

        OrderDistribution od2 = new OrderDistribution(data);
        od2.makeInitialDistribution();
        Individual individual2 = new Individual(data);
        individual2.initializeIndividual(od1);
        AdSplit.adSplitPlural(individual2);



        System.out.println(individual1.getFitness(true));
        System.out.println(individual2.getFitness(true));
        Individual child = GiantTourCrossover.crossOver(individual1, individual2, individual1.orderDistribution);
        System.out.println(child.getFitness(true));
    }

}
