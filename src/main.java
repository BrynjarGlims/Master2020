import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Genetic.GiantTourCrossover;
import Individual.Individual;
import Population.Population;
import ProductAllocation.OrderDistribution;


public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        population.initializePopulation();
        GiantTourCrossover GTC = new GiantTourCrossover(data);
        int numberOfIterations = 0;

        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfIterations){


            //crossover to obtain a new child
            //for the obtained child:
            //adsplit
            //getIndividualFitnessScore()
            //educate (with probability P_ls)
            //if (child infeasible):
                //repair
            //insert child into population
            //if (population.getSize() > maxSize): select survivors
            //adjust penalty parameters for overtimeInfeasibility, loadInfeasibility and timeWarpInfeasibility

            numberOfIterations++;
        }




    }



}
