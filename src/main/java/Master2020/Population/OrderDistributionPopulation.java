package Master2020.Population;


import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.PGA.PeriodicPopulation;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Genetic.FitnessCalculation;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


public class OrderDistributionPopulation {

    public Data data;
    public Set<OrderDistribution> setOfOrderDistributions;
    private Population population;
    private PeriodicPopulation periodicPopulation;
    private HashMap<Individual, HashMap<OrderDistribution, Double>> fillingLevelFitness;  //todo: maybe in addition to overTimeAtDepotFitness
    private double orderDistributionScalingFactor = 1;

    //private Set<Individual> currentGiantTourPopulation;

    public OrderDistributionPopulation(Data data) {
        this.data = data;
        this.setOfOrderDistributions = new HashSet<OrderDistribution>();
    }

    public void setOrderDistributionScalingFactor(double orderDistributionScalingFactor){
        this.orderDistributionScalingFactor = orderDistributionScalingFactor;
        for (OrderDistribution orderDistribution : setOfOrderDistributions){
            orderDistribution.setOrderScalingFactor(orderDistributionScalingFactor);
        }
    }

    public void initializeOrderDistributionPopulation(Population population) {
        this.population = population;
        for (int i = 0; i < Parameters.initialOrderDistributionPopulationSize; i++) {
            OrderDistribution od = new OrderDistribution(data);
            od.makeInitialDistribution();
            this.setOfOrderDistributions.add(od);
        }
    }

    public void initializeOrderDistributionPopulation(PeriodicPopulation periodicPopulation) {
        this.periodicPopulation  = periodicPopulation;
        for (int i = 0; i < Parameters.initialOrderDistributionPopulationSize; i++) {
            OrderDistribution od = new OrderDistribution(data);
            od.makeInitialDistribution();
            this.setOfOrderDistributions.add(od);
        }
    }

    public void calculateFillingLevelFitnessScoresPlural() {
        this.fillingLevelFitness = new HashMap<Individual, HashMap<OrderDistribution, Double>>();
        Set<Individual> currentGiantTourPopulation = new HashSet<Individual>();
        if (population.feasiblePopulation.size() > 0) {
            currentGiantTourPopulation.addAll(population.feasiblePopulation);
        }
        if (population.infeasiblePopulation.size() > 0) {
            currentGiantTourPopulation.addAll(population.infeasiblePopulation);
        }

        for (Individual ind : currentGiantTourPopulation) {
            fillingLevelFitness.put(ind, getFillingLevelFitnessSingular(ind));
        }
    }

    public OrderDistribution getBestOrderDistribution(Individual individual){
        HashMap<OrderDistribution, Double> orderHashMap = fillingLevelFitness.get(individual);
        OrderDistribution bestOD = null;
        double bestFitnessValue = Double.MAX_VALUE;
        for (Map.Entry<OrderDistribution,Double> entry : orderHashMap.entrySet()){
            if(bestFitnessValue > entry.getValue()){
                bestFitnessValue = entry.getValue();
                bestOD = entry.getKey();
            }
        }
        return bestOD;
    }

    private HashMap<OrderDistribution, Double> getFillingLevelFitnessSingular(Individual individual) {
        HashMap<OrderDistribution, Double> tempHashMap = new HashMap<OrderDistribution, Double>();
        for (OrderDistribution od : this.setOfOrderDistributions) {
            tempHashMap.put(od, FitnessCalculation.getFillLevelFitnessAnIndividualAndAnOrderDistribution(individual, od));
        }
        return tempHashMap;
    }
    public int getPopulationSize(){
        return this.setOfOrderDistributions.size();
    }

    public OrderDistribution getRandomOrderDistribution(){ // TODO: 04.03.2020 Must be a better way to do this
        int randomIndex = ThreadLocalRandom.current().nextInt(0, setOfOrderDistributions.size());
        int currentIndex = 0;
        for (OrderDistribution od : setOfOrderDistributions){
            if (randomIndex == currentIndex) {
                return od;
            }
            currentIndex++;
        }
        return null;
    }


    public void removeNoneUsedOrderDistributions(Population population){
        Set<OrderDistribution> orderDistributionsToKeep = new HashSet<OrderDistribution>();
        for (Individual individual : population.feasiblePopulation) {
            orderDistributionsToKeep.add(individual.getOrderDistribution());
        }
        for (Individual individual : population.infeasiblePopulation) {
            orderDistributionsToKeep.add(individual.getOrderDistribution());
        }
        this.setOfOrderDistributions = orderDistributionsToKeep;
    }

    public void addOrderDistribution(OrderDistribution od){
        setOfOrderDistributions.add(od);
    }


    public static void main(String[] args) {
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        population.initializePopulation(odp.getRandomOrderDistribution());
        odp.initializeOrderDistributionPopulation(population);

    }

}

