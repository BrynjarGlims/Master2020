package Master2020.Genetic;

import Master2020.Individual.Individual;

import java.util.Comparator;

public class SortByFitness implements Comparator<Individual>{

    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getFitness(true) == o2.getFitness(true)){ // if tie, make a consistent choice
            return (o1.hashCode() < o2.hashCode()) ? -1 : 1;
        }
        return (o1.getFitness(true) - o2.getFitness(true) <= 0) ? -1 : 1;
        //todo: legg inn null
    }

}
