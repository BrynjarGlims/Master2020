package Master2020.Genetic;

import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.Population.Population;

import java.util.*;

public class TournamentSelection {


    public static Individual performSelection(Population population){
        Random rand = new Random();
        int randomIndex;
        ArrayList<Individual> individuals = new ArrayList<Individual>(population.feasiblePopulation);
        individuals.addAll(population.infeasiblePopulation);
        ArrayList<Individual> tournament = new ArrayList<>();
        Collections.shuffle(individuals);
        int tournamentSize = Parameters.binarySelection ? 2 : Parameters.tournamentSize;
        for (int i = 0; i < tournamentSize; i++){
            randomIndex = rand.nextInt(individuals.size());
            tournament.add(individuals.get(randomIndex));
            individuals.remove(randomIndex);
        }
        return performProbabilisticSelection(tournament);
    }


    private static Individual performProbabilisticSelection(ArrayList<Individual> tournament){
        Collections.sort(tournament);
        if (Parameters.binarySelection){
            return tournament.get(0);
        }
        Random r = new Random();
        double sumProbability = 0;
        for (int i = 0; i < tournament.size(); i++){
            sumProbability = Parameters.bestIndividualProbability *Math.pow(1-Parameters.bestIndividualProbability, i);
        }

        double randomValue = r.nextDouble()*sumProbability;
        
        for (int i = 0; i < tournament.size(); i++){
            randomValue -= Parameters.bestIndividualProbability *Math.pow(1-Parameters.bestIndividualProbability, i);
            if (randomValue <= 0){
                return tournament.get(i);
            }
        }
        System.out.println("Least likely individual returned");
        return tournament.get(tournament.size()-1);

    }


}
