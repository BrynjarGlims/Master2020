package Genetic;

import DataFiles.Customer;
import DataFiles.CustomerToTrip;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.*;
import ProductAllocation.OrderDistribution;

import java.util.*;

public class RouteImprovements {


    public static Data data;
    public static OrderDistribution orderDistribution;

    public static void improveRoutes(Individual individual, OrderDistribution od){
        data = individual.data;
        orderDistribution = od;
        HashSet<Integer> customers = new HashSet<>();
        for (int i = 0 ; i < individual.data.numberOfCustomers ; i++){
            customers.add(i);
        }
        for (int c : customers){
            for (int period = 0 ; period < data.numberOfPeriods ; period++){
                if (data.customers[c].requiredVisitPeriod[period] == 0){
                    continue;
                }
                swap3(individual, c, period);
            }
        }
    }



    private static boolean insert1(Individual individual, int customer, int period){
        //Tries to remove a single customer from its trip, and insert it after one of its nearest neighbors
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        CustomerToTrip ctt2;

        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        improvementOfRemoval = fitnessDifferenceOfRemoval(originalTrip1, ctt1);

        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0){
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            detoriorationOfInsertion = fitnessDifferenceOfInsertion(originalTrip2, ctt2, customer);
            if (improvementOfRemoval > detoriorationOfInsertion){
                doRemovalInsertion(individual, ctt1, ctt2);
                return true;
            }
        }
        return false;
    }

    private static boolean insert2(Individual individual, int customer, int period){
        return doubleInsertion(individual, customer, period, false);
    }

    private static boolean insert3(Individual individual, int customer, int period){
        return doubleInsertion(individual, customer, period, true);
    }

    private static boolean swap1(Individual individual, int customer, int period) {
        //swaps customer with neighbor if improvement
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];

        originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        CustomerToTrip ctt2;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new ArrayList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            if (fitnessDifferenceOfSwap(originalTrip1, originalTrip2, ctt1, ctt2) > 0){
                performSwap(individual, ctt1, ctt2);
                return true;
            }
        }
        return false;
    }

    private static boolean swap2(Individual individual, int customer, int period){
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        int succeedingCustomer = getSucceedingCustomer(individual, ctt1);
        CustomerToTrip cttSucceeding = individual.customerToTrips[period][succeedingCustomer];
        double improvementOfRemoval = fitnessDifferenceOfRemoval(originalTrip1, cttSucceeding);

        CustomerToTrip ctt2;
        double fitnessDifferenceOfSwap;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            originalTrip2.add(ctt2.index + 1, succeedingCustomer);
            fitnessDifferenceOfSwap = fitnessDifferenceOfSwap(originalTrip1, originalTrip2, ctt1, ctt2);
            if (improvementOfRemoval + fitnessDifferenceOfSwap > 0){
                performSwap(individual, ctt1, ctt2, false);
                doRemovalInsertion(individual, cttSucceeding, ctt2);
                return true;
            }
        }
        return false;
    }

    private static boolean swap3(Individual individual, int customer, int period){
        //swaps chosen customer and successor with neighbor and successor
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        List<Integer> originalTrip1;
        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        CustomerToTrip ctt1Successor = individual.customerToTrips[period][ getSucceedingCustomer(individual, ctt1)];

        List<Integer> originalTrip2;
        originalTrip1 = new ArrayList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        CustomerToTrip ctt2;
        CustomerToTrip ctt2Successor;
        for (Customer neighbor : data.customers[customer].nearestNeighbors) {
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == ctt1Successor.customer || ctt2.endIndex - (ctt2.startIndex + ctt2.index) < 2) {
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            ctt2Successor = individual.customerToTrips[period][ getSucceedingCustomer(individual, ctt2)];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            if (fitnessDifferenceOfDoubleSwap(originalTrip1, originalTrip2, ctt1, ctt1Successor, ctt2, ctt2Successor) > 0){
                performSwap(individual, ctt1, ctt2, false);
                performSwap(individual, ctt1Successor, ctt2Successor);
                return true;
            }
        }
        return false;
    }

    private static boolean doubleInsertion(Individual individual, int customer, int period, boolean reverse){
        //inserts  customer and its successor after customers nearest neighbor if it gives improvement
        //If reverse is true, successor is placed prior to customer
        CustomerToTrip ctt1 = individual.customerToTrips[period][customer];
        if (ctt1.endIndex - (ctt1.startIndex + ctt1.index) < 2){
            return false;
        }
        int succeedingCustomer = getSucceedingCustomer(individual, ctt1);
        CustomerToTrip ctt2 = individual.customerToTrips[period][succeedingCustomer];
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;

        int c1;
        int c2;

        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        improvementOfRemoval = fitnessDifferenceOfRemoval2Customers(originalTrip1, period, ctt1.vehicletype, ctt2.index, ctt1.index);
        CustomerToTrip ctt3;
        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (neighbor.requiredVisitPeriod[period] == 0 || neighbor.customerID == succeedingCustomer){
                continue;
            }
            ctt3 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt3.vehicletype].subList(ctt3.startIndex, ctt3.endIndex));
            c1 = reverse ? ctt2.customer : ctt1.customer;
            c2 = reverse ? ctt1.customer : ctt2.customer;
            detoriorationOfInsertion = fitnessDifferenceOfInsertion2Customers(originalTrip2, ctt3, c1, c2);
            if (improvementOfRemoval > detoriorationOfInsertion){
                doInsertion2Customers(individual, ctt1, ctt3, ctt2.customer, reverse);
                return true;
            }
        }
        return false;
    }

//  ------------------------------HELPER FUNCTIONS-------------------------------------------------------

    private static int getSucceedingCustomer(Individual individual, CustomerToTrip ctt){
        return individual.giantTour.chromosome[ctt.period][ctt.vehicletype].get(ctt.startIndex + ctt.index + 1);
    }

    private static double fitnessDifferenceOfRemoval(List<Integer> customerSequence, CustomerToTrip ctt){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(ctt.index);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index, ctt.customer);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfRemoval2Customers(List<Integer> customerSequence, int period, int vehicleType, int maxIndex, int minIndex){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(maxIndex);
        customerSequence.remove(minIndex);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, vehicleType, period, orderDistribution.orderVolumeDistribution, data);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfInsertion(List<Integer> customerSequence, CustomerToTrip ctt, int insertedCustomer){
        //evaluates fitness difference of adding customer (insertedCustomer) after ctt.customer
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index + 1, insertedCustomer);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfInsertion2Customers(List<Integer> customerSequence, CustomerToTrip ctt, int insertedCustomer1, int insertedCustomer2){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index + 1, insertedCustomer2);
        customerSequence.add(ctt.index + 1, insertedCustomer1);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static double fitnessDifferenceOfSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt2){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //perform swap and check new fitness
        customerSequence1.set(ctt1.index, ctt2.customer);
        customerSequence2.set(ctt2.index, ctt1.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //swap back to original
        customerSequence1.set(ctt1.index, ctt1.customer);
        customerSequence2.set(ctt2.index, ctt2.customer);
        return newFitness - oldFitness;
    }

    private static double fitnessDifferenceOfDoubleSwap(List<Integer> customerSequence1, List<Integer> customerSequence2, CustomerToTrip ctt1, CustomerToTrip ctt1Successor, CustomerToTrip ctt2, CustomerToTrip ctt2Successor){
        double oldFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        customerSequence1.set(ctt1.index, ctt2.customer);
        customerSequence2.set(ctt2.index, ctt1.customer);
        customerSequence1.set(ctt1Successor.index, ctt2Successor.customer);
        customerSequence2.set(ctt2Successor.index, ctt1Successor.customer);
        double newFitness = combinedFitnessOf2Sequences(customerSequence1, customerSequence2, ctt1.vehicletype, ctt1.period, ctt2.vehicletype, ctt2.period);
        //swap back to original
        customerSequence1.set(ctt1.index, ctt1.customer);
        customerSequence2.set(ctt2.index, ctt2.customer);
        customerSequence1.set(ctt1Successor.index, ctt1Successor.customer);
        customerSequence2.set(ctt2Successor.index, ctt2Successor.customer);
        return newFitness - oldFitness;

    }

    private static double combinedFitnessOf2Sequences(List<Integer> customerSequence1, List<Integer> customerSequence2, int vt1, int period1, int vt2, int period2){
        return FitnessCalculation.getTripFitness(customerSequence1, vt1, period1, orderDistribution.orderVolumeDistribution, data)
                + FitnessCalculation.getTripFitness(customerSequence2, vt2, period2, orderDistribution.orderVolumeDistribution, data);
    }

    private static void doRemovalInsertion(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer, boolean update){
        int adjustIndex = removeCustomer.vehicletype == insertCustomer.vehicletype && removeCustomer.startIndex + removeCustomer.index < insertCustomer.startIndex + insertCustomer.index ? 0 : 1;
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
        if(update){
            updateAdsplit(individual, removeCustomer);
            updateAdsplit(individual, insertCustomer);
        }
    }

    private static void doRemovalInsertion(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer){
        doRemovalInsertion(individual, removeCustomer, insertCustomer, true);
    }

    private static void doInsertion2Customers(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer, int succeedingCustomer, boolean reverse){
        int adjustIndex = removeCustomer.vehicletype == insertCustomer.vehicletype && removeCustomer.startIndex + removeCustomer.index < insertCustomer.startIndex + insertCustomer.index ? -1 : 1;
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        if (reverse){
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, succeedingCustomer);
        }
        else{
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, succeedingCustomer);
            individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
        }
        updateAdsplit(individual, removeCustomer);
        updateAdsplit(individual, insertCustomer);
    }

    private static void performSwap(Individual individual, CustomerToTrip ctt1, CustomerToTrip ctt2, boolean update){
        individual.giantTour.chromosome[ctt1.period][ctt1.vehicletype].set(ctt1.startIndex + ctt1.index, ctt2.customer);
        individual.giantTour.chromosome[ctt2.period][ctt2.vehicletype].set(ctt2.startIndex + ctt2.index, ctt1.customer);
        if (update){
            updateAdsplit(individual, ctt1);
            updateAdsplit(individual, ctt2);
        }
    }

    private static void performSwap(Individual individual, CustomerToTrip ctt1, CustomerToTrip ctt2){
        performSwap(individual, ctt1, ctt2, true);
    }

    private static void updateAdsplit(Individual individual, CustomerToTrip customerToTrip){
        AdSplit.adSplitSingular(individual, customerToTrip.period, customerToTrip.vehicletype);
        individual.makeCustomerToTripMapSingular(customerToTrip.period, customerToTrip.vehicletype);
    }










    public static void main(String[] args){
        Data data = DataReader.loadData();

        for (int i = 0 ; i < 1 ; i++){
            System.out.println(i);

            Individual individual = new Individual(data);
            individual.initializeIndividual();

            AdSplit.adSplitPlural(individual);

            individual.makeCustomerToTripMap();
            improveRoutes(individual, individual.orderDistribution);
        }
//        List<Integer> a = new LinkedList<>();
//        a.add(5);
//        a.add(4);
//        System.out.println(a);
//        a.add(a.size(),1);
//        a.set(1, 100);
//
//        List<Integer> b = new ArrayList<>(a);
//
//        System.out.println("b: " + b);
//        b.add(4);
//        b.add(123);
//        System.out.println("b: " + b);
//        System.out.println(a);

//        System.out.println(individual.giantTour.chromosome[0][5]);
//        System.out.println(individual.giantTourSplit.chromosome[0][5]);
//        System.out.println(individual.customerToTrips[0].length);
//        System.out.println(Arrays.toString(individual.customerToTrips[0]));


    }



}
