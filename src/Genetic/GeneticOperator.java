package Genetic;

import DataFiles.Data;
import Individual.Individual;
import Individual.GiantTour;
import ProductAllocation.OrderDistribution;

import javax.xml.transform.sax.SAXSource;
import java.util.Random;

public class GeneticOperator {


    //CROSSOVER--------------------------------------------------------------------------------
    public void crossoverOX (Individual parent1, Individual parent2, int p, int vt) {
        Random r = new Random();
        int startSplitIndex = r.nextInt(parent1.giantTour.chromosome[p][vt].size());
        int endSplitIndex = r.nextInt(parent1.giantTour.chromosome[p][vt].size());
    }

    public void getRandomParent() {
        //draw 2 random individual from current population
        //choose the one with the lowest fitness
    }

    public void selectChildToAppendPopulation(Individual child1, Individual child2) {
        //selection criteria
    }

    public void crossoverMain() {
        // parent1 = getRandomParent();
        //parent2 = getRandomParent();
        //child1 = crossover(parent1, parent2)
        //child2 = crossover(parent2, parent1)
        //newChild = selectChildToAppendPopulation(child1, child2)
        //Update population(newChild)
    }
    //CROSSOVER--------------------------------------------------------------------------------






}
