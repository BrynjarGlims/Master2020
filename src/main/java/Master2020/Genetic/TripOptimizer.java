package Master2020.Genetic;

import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.Individual.AdSplit;
import Master2020.DataFiles.Data;
import Master2020.Population.*;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TripOptimizer {

    public static void optimizeTrip(Trip trip, OrderDistribution orderDistribution){
        if (trip.customers.size() > Parameters.tripOptimizerSizeLimit){
            return;
        }
        List<List<Integer>> permutations = Utils.Permutate(new ArrayList<>(trip.customers));
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
