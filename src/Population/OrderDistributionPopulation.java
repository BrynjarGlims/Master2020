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
import java.util.concurrent.ThreadLocalRandom;


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

    private HashMap<OrderDistribution, Double> getFillingLevelFitnessSingular(Individual individual) {
        HashMap<OrderDistribution, Double> tempHashMap = new HashMap<OrderDistribution, Double>();
        for (OrderDistribution od : this.setOfOrderDistributionIndividuals) {
            tempHashMap.put(od, FitnessCalculation.getFitnessForAnIndividualAndAnOrderDistribution(individual, od));
        }
        return tempHashMap;
    }

    public OrderDistribution getRandomOrderDistribution(){ // TODO: 04.03.2020 Must be a better way to do this
        int randomIndex = ThreadLocalRandom.current().nextInt(0,setOfOrderDistributionIndividuals.size());
        int currentIndex = 0;
        for (OrderDistribution od : setOfOrderDistributionIndividuals){
            if (randomIndex == currentIndex) {
                return od;
            }
            currentIndex++;
        }

        return null;
    }

    public void addOrderDistribution(OrderDistribution od){
        setOfOrderDistributionIndividuals.add(od);
    }



    public static void main(String[] args) {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        population.initializePopulation(odp.getRandomOrderDistribution());
        odp.initializeOrderDistributionPopulation(population);
        odp.getFillingLevelFitnessScoresPlural();
    }

}

