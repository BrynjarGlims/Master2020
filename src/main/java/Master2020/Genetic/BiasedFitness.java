package Master2020.Genetic;

import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.Population.Population;
import Master2020.DataFiles.Data;

import java.util.*;
import java.util.stream.Collectors;

public class BiasedFitness {

    public static void setBiasedFitnessScore(Population population){
        calculateSimplePopulationDiversity(population);
        calculateFitnessAndDiversityRank(population);
    }

    private static void calculateSimplePopulationDiversity(Population population){
        if (population.feasiblePopulation.size() > Parameters.minimumSubIndividualPopulationSize){
            calculateDiversityForPopulation(population.feasiblePopulation);
        }
        if (population.infeasiblePopulation.size() > Parameters.minimumSubIndividualPopulationSize){
            calculateDiversityForPopulation(population.infeasiblePopulation);
        }

    }


    private static void calculateFitnessAndDiversityRank(Population population){
        if (population.feasiblePopulation.size() > Parameters.minimumSubIndividualPopulationSize ){
            calculateFitnessAndDiversityRankForSubPopulation(population.feasiblePopulation);
        }
        if (population.infeasiblePopulation.size() > Parameters.minimumSubIndividualPopulationSize){
            calculateFitnessAndDiversityRankForSubPopulation(population.infeasiblePopulation);
        }
    }

    public static void setBiasedFitnessScoreForInfeasibleIndividuals(Population population){
        calculateDiversityForPopulation(population.infeasiblePopulation);
        calculateFitnessAndDiversityRankForSubPopulation(population.infeasiblePopulation);
    }

    public static void setBiasedFitnessScoreForFeasibleIndividuals(Population population){
        calculateDiversityForPopulation(population.feasiblePopulation);
        calculateFitnessAndDiversityRankForSubPopulation(population.feasiblePopulation);
    }



    private static void calculateFitnessAndDiversityRankForSubPopulation(Set<Individual> subPopulation){
        Comparator<Individual> sortByFitness = new SortByFitness();
        Comparator<Individual> sortByDiversity = new SortByDiversity();
        ArrayList<Individual> diversityRank = new ArrayList<Individual>(subPopulation);
        ArrayList<Individual> fitnessRank = new ArrayList<Individual>(subPopulation);
        Collections.sort(diversityRank,sortByDiversity);
        Collections.sort(fitnessRank, sortByFitness);
        for (int i = 0; i < diversityRank.size(); i++){
            diversityRank.get(i).setDiversityRank(i+1);
            fitnessRank.get(i).setFitnessRank(i+1);
        }
        for (Individual individual: subPopulation){
            individual.calculateBiasedFitness();
        }
    }



    private static void calculateDiversityForPopulation(Set<Individual> subPopulation){
        for (Individual ind1 : subPopulation){
            ArrayList<Double> diversityList = new ArrayList<>();
            for (Individual ind2 : subPopulation){
                if (ind1.equals(ind2)){
                    continue;
                }
                diversityList.add(calculateSimpleDiversityBetweenIndividuals(ind1, ind2)); //todo: implement
            }
            Collections.sort(diversityList);
            ArrayList<Double> nearestDiversity = new ArrayList<>(diversityList.subList(0,Math.min(Parameters.nearestNeighborsDiversity,diversityList.size())));
            double diversity = nearestDiversity.stream().collect(Collectors.summingDouble(Double::doubleValue));
            diversity /= nearestDiversity.size();
            ind1.setDiversity(diversity);
        }

    }

    private static double calculateSimpleDiversityBetweenIndividuals(Individual ind1, Individual ind2){
        double diversity = 0;
        for (int p = 0; p < ind1.data.numberOfPeriods; p++){
            diversity += calculateSimpleSimilarityBetweenGiantTours(ind1.giantTour.chromosome[p], ind2.giantTour.chromosome[p], ind1.data);
        }
        return diversity;
    }

    private static double calculateSimpleSimilarityBetweenGiantTours(ArrayList<Integer>[] gt1, ArrayList<Integer>[] gt2, Data data){
        double diversity = 0;
        HashMap<Integer, Integer> gt1Map = new HashMap<>();
        HashMap<Integer, Integer> gt2Map = new HashMap<>();
        int prevCustomer;

        for (int vt = 0; vt < data.numberOfVehicleTypes; vt ++){
            //gt1
            prevCustomer = -1;
            for (int i : gt1[vt]){
                if (prevCustomer == -1){
                    prevCustomer = i;
                    continue;
                }
                gt1Map.put(prevCustomer, i);
                prevCustomer = i;
            }
            gt1Map.put(prevCustomer, -1);

            //gt2
            prevCustomer = -1;
            for (int i : gt2[vt]){
                if (prevCustomer == -1){
                    prevCustomer = i;
                    continue;
                }
                gt2Map.put(prevCustomer, i);
                prevCustomer = i;
            }
            gt2Map.put(prevCustomer, -1);
        }

        for (int key : gt1Map.keySet())
            if (gt1Map.get(key) != gt2Map.get(key)) {
                diversity += 1;
            }
        return diversity;
    }
}

class SortByFitness implements Comparator<Individual>{

    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getFitness(false) == o2.getFitness(false)){ // if tie, make a consistent choice
            return (o1.hashCode() < o2.hashCode()) ? -1 : 1;
        }
        return (o1.getFitness(false) - o2.getFitness(false) <= 0) ? -1 : 1;
    }
}

class SortByDiversity implements Comparator<Individual>{


    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getDiversity() == o2.getDiversity()){ // if tie, make a consistent choice
            return (o1.hashCode() < o2.hashCode()) ? -1 : 1;
        }
        return (o1.getDiversity() - o2.getDiversity() <= 0) ? 1 : -1;

    }
}


