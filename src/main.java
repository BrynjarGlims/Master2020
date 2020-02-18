import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Individual.Individual;
import Population.Population;
import ProductAllocation.OrderDistribution;


public class main {
    public static void main(String[] args){
        Data data = DataReader.loadData();
        Population population = new Population(data);
        population.initializePopulation();
        int numberOfIterations = 0;

        while ( population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                numberOfIterations < Parameters.maxNumberOfIterations){
            numberOfIterations++;
            System.out.println("hei");






        }

    }



}
