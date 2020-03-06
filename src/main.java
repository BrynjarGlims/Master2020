import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.GiantTourCrossover;
import Genetic.OrderDistributionCrossover;
import Individual.Individual;
import MIP.OrderAllocationModel;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;
import Visualization.PlotIndividual;

public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        OrderDistribution firstOD = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(firstOD);

        int numberOfIterations = 0;
        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfGenerations){
            population.setSurvivorsForNextGeneration();
            System.out.println("Start generation: " + numberOfIterations);

            //Find best OD for the distribution
            odp.calculateFillingLevelFitnessScoresPlural();
            for (Individual individual : population.infeasiblePopulation){
                individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // TODO: 05.03.2020 Uncomment
            }

            //Generate new population
            while (population.getPopulationSize() < Parameters.maximumSubIndividualPopulationSize){
                Individual parent1 = population.getRandomIndividual();
                Individual parent2 = population.getRandomIndividual();  //todo:base this on crossfitnesscstore
                OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
                for (OrderDistribution od : crossoverOD){
                    odp.addOrderDistribution(od);
                }
                Individual newIndividual = GTC.crossOver(parent1, parent2, crossoverOD[0]);


                // TODO: 04.03.2020 Brynjar: Add education
                // TODO: 04.03.2020 Add repair:

                if (Math.random() < Parameters.greedyMIPValue){
                    //System.out.println("--------------------");
                    //System.out.println("Current fintness: " + newIndividual.getBiasedFitness());
                    OrderDistribution optimalOD = OrderAllocationModel.createOptimalOrderDistribution(newIndividual, data);
                    if (newIndividual.infeasibilityCost == 0){
                        newIndividual.setOptimalOrderDistribution(optimalOD, true);
                    }
                    else{
                        newIndividual.setOptimalOrderDistribution(optimalOD, false);
                    }

                    odp.addOrderDistribution(optimalOD);  // todo: do not remove adsplit
                    //System.out.println("New fitness: " + newIndividual.getBiasedFitness());
                    // TODO: 04.03.2020 Implement safe trap in case no solution is found in gurobi
                }
                population.addChildToPopulation(newIndividual);
            }

            //reduce size of both populations
            population.reduceSizeToMin();
            odp.removeNoneUsedOrderDistributions();

            // TODO: 04.03.2020 Implement adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility

            numberOfIterations++;
            Individual bestIndividual = population.returnBestIndividual();
            Individual bestFeasibleIndividual = population.returnBestFeasibleIndividual();
            Individual bestInfeasibleIndividual = population.returnBestInfeasibleIndividual();
            if(bestIndividual.isFeasible()){
                System.out.println("Best feasible individual: " + bestFeasibleIndividual.fitness);
            }
            System.out.println("Best infeasible individual: " + bestInfeasibleIndividual.fitness);
        }
        numberOfIterations++;
        Individual bestIndividual = population.returnBestIndividual();
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));
        PlotIndividual visualizer = new PlotIndividual(data);
        visualizer.visualize(bestIndividual);

    }
}
