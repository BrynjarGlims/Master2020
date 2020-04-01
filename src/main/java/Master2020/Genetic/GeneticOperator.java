package Master2020.Genetic;

import Master2020.DataFiles.Data;
import Master2020.Individual.Individual;
import Master2020.Individual.GiantTour;

import java.util.Random;

public class GeneticOperator {


    //CROSSOVER--------------------------------------------------------------------------------
    public static void crossoverOX (Individual parent1, Individual parent2, int p, int vt) {
        Random r = new Random();
        int startSplitIndex = r.nextInt(parent1.giantTour.chromosome[p][vt].size());
        int endSplitIndex = r.nextInt(parent1.giantTour.chromosome[p][vt].size());
    }

    public static void getRandomParent() {
        //draw 2 random individual from current population
        //choose the one with the lowest fitness
    }

    public static void selectChildToAppendPopulation(Individual child1, Individual child2) {
        //selection criteria
    }

    public static void crossoverMain() {
        // parent1 = getRandomParent();
        //parent2 = getRandomParent();
        //child1 = crossover(parent1, parent2)
        //child2 = crossover(parent2, parent1)
        //newChild = selectChildToAppendPopulation(child1, child2)
        //Update population(newChild)
    }
    //----------------------------------------------------------------------------------------

    public static void educate() {
        //call Local search
    }





}
