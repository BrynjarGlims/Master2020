import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.GiantTourCrossover;
import Individual.Individual;
import Population.Population;
import ProductAllocation.OrderDistribution;
import Population.OrderDistributionPopulation;

public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        OrderDistributionPopulation odp = new OrderDistributionPopulation(data);
        population.initializePopulation(odp);
        odp.initializeOrderDistributionPopulation(population);
        GiantTourCrossover GTC = new GiantTourCrossover(data);

        int numberOfIterations = 0;

        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfIterations){
            OrderDistribution targetOD = odp.getRandomOrderDistribution();

            while (population.getPopulationSize() < Parameters.maxPopulationSize){

            }

            //crossover to obtain a new child
            //for the obtained child:
            //adsplit
            //getIndividualFitnessScore()
            //educate (with probability P_ls)
            //if (child infeasible):
                //repair
            //insert child into relevant subpopulation

            //if (subpopulation.getSize() > maxSize): select survivors:
            //if child.isFeasible();
                //Population.selectFeasibleSurvivors();
            //else
                //population.selectInfeasibleSurvivors();



            //adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility
            numberOfIterations++;
        }




    }



}
