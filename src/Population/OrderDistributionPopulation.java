package Population;


import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import Population.Population;
import Genetic.FitnessCalculation;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class OrderDistributionPopulation {

    public Data data;
    public Set<OrderDistribution> setOfOrderDistributionIndividuals;
    private Population currentPopulation;
    //private Set<Individual> currentGiantTourPopulation;

    public OrderDistributionPopulation(Data data) {
        this.data = data;
        this.setOfOrderDistributionIndividuals = new HashSet<OrderDistribution>();
    }

    public void initializeOrderDistributionPopulation(Population population) {
        this.currentPopulation = population;
        for (int i = 0; i < Parameters.initialOrderDistributionPopulationSize; i++) {
            OrderDistribution od = new OrderDistribution(data);
            od.makeInitialDistribution();
            this.setOfOrderDistributionIndividuals.add(od);
        }
    }

    public HashMap<Individual, HashMap<OrderDistribution, Double>> getFillingLevelFitnessScoresPlural() {
        HashMap<Individual, HashMap<OrderDistribution, Double>> fillingLevelFitness = new HashMap<Individual, HashMap<OrderDistribution, Double>>();

        Set<Individual> currentGiantTourPopulation = new HashSet<Individual>();
        if (currentPopulation.feasiblePopulation.size() > 0) {
            currentGiantTourPopulation.addAll(currentPopulation.feasiblePopulation);
        }
        if (currentPopulation.infeasiblePopulation.size() > 0) {
            currentGiantTourPopulation.addAll(currentPopulation.infeasiblePopulation);
        }

        for (Individual ind : currentGiantTourPopulation) {
            fillingLevelFitness.put(ind, getFillingLevelFitnessSingular(ind));
        }

        return fillingLevelFitness;
    }

    public HashMap<OrderDistribution, Double> getFillingLevelFitnessSingular(Individual individual) {
        HashMap<OrderDistribution, Double> tempHashMap = new HashMap<OrderDistribution, Double>();
        for (OrderDistribution od : this.setOfOrderDistributionIndividuals) {
            tempHashMap.put(od, FitnessCalculation.getFitnessForAnIndividualAndAnOrderDistribution(individual, od));
        }
        return tempHashMap;
    }



    public static void main(String[] args) {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        population.initializePopulation(odp);
        odp.initializeOrderDistributionPopulation(population);
        System.out.println(odp.getFillingLevelFitnessScoresPlural());
    }

}

