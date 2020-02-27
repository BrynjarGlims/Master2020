package Population;
import DataFiles.*;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import scala.xml.PrettyPrinter;

import java.util.*;

public class Population {
    private int totalPopulationSize;
    private Data data;
    private Set<Individual> feasiblePopulation;
    private Set<Individual> infeasiblePopulation;
    private OrderDistribution currentOrderDistribution;

    int iterationsWithoutImprovement = 0;

    public Population(Data data) {
        this.data = data;
        this.currentOrderDistribution = new OrderDistribution(this.data);
        this.currentOrderDistribution.makeDistribution();
        this.feasiblePopulation = new HashSet<Individual>();
        this.infeasiblePopulation = new HashSet<Individual>();
    }


    public void initializePopulation() {
        for (int i = 0; i < 4*Parameters.minimumSubPopulationSize; i++) {
            Individual individual = new Individual(this.data, this.currentOrderDistribution, this);
            //individual.educate();
            Random r = new Random();
            double repairProbability = r.nextDouble();
            if (repairProbability >= 0.5) {
                //individual.repair()
            }
            //
            if (individual.isFeasible()) {
                feasiblePopulation.add(individual);
                if (getSizeOfFeasiblePopulation() > Parameters.maximumSubPopulationSize) {
                    //survivorSelection();
                }
            }
            else {
                infeasiblePopulation.add(individual);
                if (getSizeOfInfeasiblePopulation() > Parameters.maximumSubPopulationSize) {
                    //activate survivor selection
                }
            }
        }
    }


    private static double getFitnessDifference(Individual i1, Individual i2) {
        return (Math.abs(i1.fitness - i2.fitness));
    }


    private Set<Individual> getFeasibleClonesForAnIndividual(Individual individual) {
        Set<Individual> setOfClones = new HashSet<Individual>();
        for (Individual ind: feasiblePopulation)
            if (getFitnessDifference(individual, ind) <= Parameters.minimumFitnessDifferenceForClones) {
                setOfClones.add(ind);
            }
        return setOfClones;
    }



    public void selectFeasibleSurvivors() {
        Set<Individual> setOfAllClones = new HashSet<Individual>();
        //select #Individuals (size = minimumSubPopulationSize) to keep both diversity and low-cost individuals
        for (Individual individual: feasiblePopulation) {
            for (Individual ind: getFeasibleClonesForAnIndividual(individual)) {
                setOfAllClones.add(ind);
            }
        }

        for (int i = 0; i < (getSizeOfFeasiblePopulation() - Parameters.minimumSubPopulationSize); i++) {
            setOfAllClones.sort(); //TODO: find a way to sort individuals based on fitness
        }
        //X = set of individuals with clones
        //if X not empty:
        //remove individuals in X with max biased fitness
        //else:
        //remove P in the whole subpop with maximum biased fitness
        //update distance and biased fitness measures
    }

    public void selectInfeasibleSurvivors() {
        for (int i = 0; i < (getSizeOfInfeasiblePopulation() - Parameters.minimumSubPopulationSize); i++) {

        }
    }




    public Set<Individual> getFeasiblePopulation() {
        return feasiblePopulation;
    }

    public Set<Individual> getInfeasiblePopulation() {
        return infeasiblePopulation;
    }

    public int getTotalPopulationSize() {
        return totalPopulationSize;
    }

    public int getSizeOfInfeasiblePopulation() {
        int sizeOfInfeasiblePopulation = 50;
        return sizeOfInfeasiblePopulation;
    }

    public int getSizeOfFeasiblePopulation() {
        int sizeOfFeasiblePopulation = 50;
        return sizeOfFeasiblePopulation;
    }

    public int getIterationsWithoutImprovement(){
        return iterationsWithoutImprovement;
    }




}