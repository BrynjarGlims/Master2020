package Genetic;

import DataFiles.DataReader;
import DataFiles.Parameters;
import Individual.Individual;
import Individual.Trip;
import Individual.AdSplit;
import DataFiles.Data;
import Population.*;
import ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.List;

public class TripOptimizer {

    public static void optimizeTrip(Trip trip, OrderDistribution orderDistribution){
        if (trip.customers.size() > Parameters.tripOptimizerSizeLimit){
            return;
        }
        List<List<Integer>> permutations = generatePermutations(new ArrayList<>(trip.customers));
        List<Integer> bestSequence = trip.customers;
        double bestFitness = FitnessCalculation.getTotalJourneyFitness(trip.journey, orderDistribution);;

        double tempFitness;
        for (List<Integer> perm: permutations){
            trip.customers = perm;
            tempFitness = FitnessCalculation.getTotalJourneyFitness(trip.journey, orderDistribution);
            if (tempFitness < bestFitness){
                bestSequence = perm;
                bestFitness = tempFitness;
            }
        }
        trip.customers = bestSequence;
    }

    private static <T> List<List<T>> generatePermutations(List<T> original) {
        if (original.isEmpty()) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        T firstElement = original.remove(0);
        List<List<T>> returnValue = new ArrayList<>();
        List<List<T>> permutations = generatePermutations(original);
        for (List<T> smallerPermutated : permutations) {
            for (int index=0; index <= smallerPermutated.size(); index++) {
                List<T> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);
        Individual individual = population.getRandomIndividual();
        AdSplit.adSplitPlural(individual);
        System.out.println(individual.getFitness(true));
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Trip trip : individual.tripList[p][vt]){
                    TripOptimizer.optimizeTrip(trip, individual.orderDistribution);
                }
            }
        }
        individual.setGiantTourFromTrips();
        System.out.println(individual.getFitness(true));
    }


}
