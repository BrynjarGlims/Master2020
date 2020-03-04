package Genetic;

import DataFiles.Customer;
import DataFiles.CustomerToTrip;
import DataFiles.Data;
import DataFiles.DataReader;
import Individual.*;
import ProductAllocation.OrderDistribution;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;

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
                insert1(individual, c, period);
            }
        }
    }



    private static boolean insert1(Individual individual, int customer, int period){
        //Tries to remove a single customer from its trip, and insert it after one of its nearest neighbors
        double improvementOfRemoval;
        double detoriorationOfInsertion;
        List<Integer> originalTrip1;
        List<Integer> originalTrip2;
        CustomerToTrip ctt1;
        CustomerToTrip ctt2;

        ctt1 = individual.customerToTrips[period][customer];
        originalTrip1 = new LinkedList<>(individual.giantTour.chromosome[period][ctt1.vehicletype].subList(ctt1.startIndex, ctt1.endIndex));
        improvementOfRemoval = fitnessDifferenceOfRemoval(originalTrip1, ctt1);

        for (Customer neighbor : data.customers[customer].nearestNeighbors){
            if (data.customers[neighbor.customerID].requiredVisitPeriod[period] == 0){
                continue;
            }
            ctt2 = individual.customerToTrips[period][neighbor.customerID];
            originalTrip2 = new LinkedList<>(individual.giantTour.chromosome[period][ctt2.vehicletype].subList(ctt2.startIndex, ctt2.endIndex));
            detoriorationOfInsertion = fitnessDifferenceOfInsertion(originalTrip2, ctt2, customer);
            if (improvementOfRemoval > detoriorationOfInsertion){
                doInsertion(individual, ctt1, ctt2);
                return true;

            }
        }
        return false;
    }

    private static boolean insert2(Individual individual, int customer){
        return true;
    }



    private static double fitnessDifferenceOfRemoval(List<Integer> customerSequence, CustomerToTrip ctt){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.remove(ctt.index);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return initialFitness - newFitness;
    }

    private static double fitnessDifferenceOfInsertion(List<Integer> customerSequence, CustomerToTrip ctt, int insertedCustomer){
        double initialFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        customerSequence.add(ctt.index + 1, insertedCustomer);
        double newFitness = FitnessCalculation.getTripFitness(customerSequence, ctt.vehicletype, ctt.period, orderDistribution.orderVolumeDistribution, data);
        return newFitness - initialFitness;
    }

    private static void doInsertion(Individual individual, CustomerToTrip removeCustomer, CustomerToTrip insertCustomer){
        int adjustIndex = removeCustomer.vehicletype == insertCustomer.vehicletype ? 0 : 1;
        individual.giantTour.chromosome[removeCustomer.period][removeCustomer.vehicletype].remove(removeCustomer.startIndex + removeCustomer.index);
        individual.giantTour.chromosome[insertCustomer.period][insertCustomer.vehicletype].add(insertCustomer.startIndex + insertCustomer.index + adjustIndex, removeCustomer.customer);
        AdSplit.adSplitSingular(individual, removeCustomer.period, removeCustomer.vehicletype);
        AdSplit.adSplitSingular(individual, insertCustomer.period, insertCustomer.vehicletype);
        individual.makeCustomerToTripMapSingular(removeCustomer.period, removeCustomer.vehicletype);
        individual.makeCustomerToTripMapSingular(insertCustomer.period, insertCustomer.vehicletype);
    }










    public static void main(String[] args){
        Data data = DataReader.loadData();
        Individual individual = new Individual(data);
        individual.initializeIndividual();

        AdSplit.adSplitPlural(individual);

        individual.makeCustomerToTripMap();
        improveRoutes(individual, individual.orderDistribution);
        List<Integer> a = new LinkedList<>();
        a.add(5);
        a.add(4);
        System.out.println(a);
        a.add(a.size(),1);
        System.out.println(a);
        a.remove(1);
        System.out.println(a);

//        System.out.println(individual.giantTour.chromosome[0][5]);
//        System.out.println(individual.giantTourSplit.chromosome[0][5]);
//        System.out.println(individual.customerToTrips[0].length);
//        System.out.println(Arrays.toString(individual.customerToTrips[0]));


    }



}
