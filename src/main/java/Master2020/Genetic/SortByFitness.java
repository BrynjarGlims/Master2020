package Master2020.Genetic;

import Master2020.Individual.Individual;

import java.util.Comparator;

public class SortByFitness implements Comparator<Individual>{

    @Override
    public int compare(Individual o1, Individual o2) {
        if (o1.getFitness(false) == o2.getFitness(false)){ // if tie, make a consistent choice
            return 0;
        }
        return (o1.getFitness(false) - o2.getFitness(false) <= 0) ? -1 : 1;
    }

}
