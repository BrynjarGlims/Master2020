package Genetic;

import DataFiles.Parameters;
import Individual.Individual;
import Population.Population;
import Individual.Journey;
import Individual.Arc;
import Individual.SimpleArc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class BiasedFitness {

    public void setBiasedFitnessScore(Population population){
        calculateSimplePopulationDiversity(population);
        Comparator<Individual> sortByFitness = new SortByFitness();
        Comparator<Individual> sortByDiversity = new SortByDiversity();
        ArrayList<Individual> diversityRank = new ArrayList<Individual>(population.getTotalPopulation());
        ArrayList<Individual> fitnessRank = new ArrayList<Individual>(population.getTotalPopulation());
        Collections.sort(diversityRank,sortByDiversity);
        Collections.sort(fitnessRank, sortByFitness);
        for (int i = 0; i < diversityRank.size(); i++){
            diversityRank.get(i).setDiversityRank(i+1);
            fitnessRank.get(i).setFitnessRank(i+1);
        }

    }

    private void calculateSimplePopulationDiversity(Population population){

        for (Individual ind1 : population.getTotalPopulation()){
            ArrayList<Double> diversityList = new ArrayList<>();
            for (Individual ind2 : population.getTotalPopulation()){
                diversityList.add(calculateSimpleDiversityBetweenIndividuals(ind1, ind2)); //todo: implement
            }
            Collections.sort(diversityList);
            ArrayList<Double> nearestDiversity = (ArrayList) diversityList.subList(0, Parameters.nearestNeighborsDiversity);
            double diversity = nearestDiversity.stream().collect(Collectors.summingDouble(Double::doubleValue));
            diversity /= nearestDiversity.size();
            ind1.setDiversity(diversity);
        }
    }



    private HashSet<SimpleArc> getSimpleArc(ArrayList<Integer> giantTour){
        HashSet<SimpleArc> arcs = new HashSet<>();
        int previousCustomer = -1;  //depot
        for (int customerID : giantTour){
            arcs.add(new SimpleArc(previousCustomer, customerID));
            previousCustomer = customerID;
        }
        arcs.add(new SimpleArc(previousCustomer, -1));
        return arcs;
    }

    private ArrayList<Individual> getNearestIndividuals(Individual individual, Population population){
        return new ArrayList<Individual>();
    }

    private double calculateSimpleDiversityBetweenIndividuals(Individual ind1, Individual ind2){
        double diversity = 0;
        for (int p = 0; p < ind1.data.numberOfPeriods; p++){
            ArrayList<Integer> gt1 = new ArrayList<>();
            ArrayList<Integer> gt2 = new ArrayList<>();
            for (int vt = 0; vt < ind1.data.numberOfVehicleTypes; vt++){
                gt1.addAll(ind1.giantTour.chromosome[p][vt]);
                gt2.addAll(ind2.giantTour.chromosome[p][vt]);
            }
            diversity += calculateSimpleSimilarityBetweenGiantTours(gt1, gt2);
        }
        return diversity;
    }

    private double calculateSimpleSimilarityBetweenGiantTours(ArrayList<Integer> gt1, ArrayList<Integer> gt2){
        double diversity = 0;
        HashMap<Integer, Integer> gt1Map = new HashMap<>();
        HashMap<Integer, Integer> gt2Map = new HashMap<>();

        for (int i = 0; i < gt1.size(); i++){
            if (i != gt1.size()-1){
                gt1Map.put(gt1.get(i), gt1.get(i+1));
                gt2Map.put(gt2.get(i), gt2.get(i+1));
            }
            else {
                gt1Map.put(gt1.get(i), -1);
                gt2Map.put(gt2.get(i), -1);
            }
        }
        for (int customerID: gt1){
            if (gt1Map.get(customerID) != gt2Map.get(customerID)){
                diversity += 1;
            }
        }
        return diversity;
    }
}

class SortByFitness implements Comparator<Individual>{

    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getFitness(false) - o2.getFitness(false) < 0){
            return 1;
        }
        else{
            return -1;
        }
    }
}

class SortByDiversity implements Comparator<Individual>{


    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getDiversity() - o2.getDiversity() < 0){
            return -1;
        }
        else{
            return 1;
        }
    }
}

