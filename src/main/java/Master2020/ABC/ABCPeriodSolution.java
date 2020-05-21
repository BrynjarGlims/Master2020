package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;

public class ABCPeriodSolution implements Comparable<ABCPeriodSolution>{


    public double[] position;
    public ArrayList<Journey>[] journeys;
    public int period;
    public Data data;
    public double fitness;

    public double timeWarpPenalty;
    public double overLoadPenalty;

    public ABCPeriodSolution(Data data, int period, double[] position, OrderDistribution orderDistribution){
        this.position = position;
        this.period = period;
        this.data = data;
        this.timeWarpPenalty = Parameters.initialTimeWarpPenalty;
        this.overLoadPenalty = Parameters.initialOverLoadPenalty;
        createJourneysAndFitness(orderDistribution);
    }

    private void createJourneysAndFitness(OrderDistribution orderDistribution){
        ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, period, position);
        ArrayList<Journey>[] allJourneys = new ArrayList[data.numberOfVehicleTypes];
        double fitness = 0;
        double[] fitnesses;
        for (int vt = 0 ; vt < giantTourEntry.length ; vt++){
            allJourneys[vt] = new ArrayList<>();
            ArrayList<Journey> journeys = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, period, vt, timeWarpPenalty, overLoadPenalty);
            for (Journey journey : journeys){
                fitnesses = FitnessCalculation.getJourneyFitness(journey, orderDistribution);
                fitness += fitnesses[0] + fitnesses[1] + fitnesses[2] + fitnesses[3];
                if (fitnesses[1] + fitnesses[2] == 0){
                    allJourneys[vt].add(journey);
                }
            }
        }
        this.fitness = fitness;
        this.journeys  = allJourneys;
    }

    @Override
    public int compareTo(ABCPeriodSolution o) {
        if (fitness < o.fitness){
            return -1;
        }
        else if (fitness > o.fitness){
            return 1;
        }
        return 0;
    }

    public String toString(){
        return "fitness: " + fitness;
    }


}
