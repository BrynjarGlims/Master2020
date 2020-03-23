package Genetic;

import DataFiles.Parameters;
import Individual.Individual;
import Population.Population;

import java.util.*;

public class TournamentSelection {



    public static Individual performSelection(Population population){
        Random rand = new Random();
        int randomIndex;
        int individualIndex;
        HashSet<Individual> individuals = new HashSet<Individual>(population.feasiblePopulation);
        individuals.addAll(population.infeasiblePopulation);
        ArrayList<Individual> tournament = new ArrayList<>();

        for (int i = 0; i < Parameters.tournamentSize; i++){
            randomIndex = rand.nextInt(individuals.size());
            individualIndex = 0;
            for (Individual individual : individuals){
                if (randomIndex == individualIndex){
                    tournament.add(individual);
                    individuals.remove(individual);
                    break;
                }
                individualIndex++;
            }
        }
        return selectBestIndividual(tournament);
    }

    private static Individual selectBestIndividual(ArrayList<Individual> tournament){
        if (Parameters.binarySelection){
            return performBinarySelection(tournament);
        }
        return performProbabilisticSelection(tournament);
    }

    private static Individual performBinarySelection(ArrayList<Individual> tournament){
         Collections.sort(tournament);
         return tournament.get(0);   //// TODO: 23/03/2020 Check if correct individual is returned
    }

    private static Individual performProbabilisticSelection(ArrayList<Individual> tournament){
        Collections.sort(tournament);
        Random r = new Random();
        double sumProbability = 0;
        for (int i = 0; i < tournament.size(); i++){
            sumProbability = Parameters.besIndividualProbability*Math.pow(1-Parameters.besIndividualProbability, i);
        }

        double randomValue = r.nextDouble()*sumProbability;
        
        for (int i = 0; i < tournament.size(); i++){
            randomValue -= Parameters.besIndividualProbability*Math.pow(1-Parameters.besIndividualProbability, i);
            if (randomValue <= 0){
                return tournament.get(i);
            }
        }
        System.out.println("Least likely individual returned");
        return tournament.get(tournament.size()-1);

    }


}
