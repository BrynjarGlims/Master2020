package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;

public class PeriodSolution implements Comparable<PeriodSolution>{


    public double[] position;
    public ArrayList<Journey>[] journeys;
    public int period;
    public Data data;
    public double fitness;

    public PeriodSolution(Data data, int period, double[] position, OrderDistribution orderDistribution){
        this.position = position;
        this.period = period;
        this.data = data;
        createJourneysAndFitness(orderDistribution);
    }

    private void createJourneysAndFitness(OrderDistribution orderDistribution){
        ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, period, position);
        ArrayList<Journey>[] allJourneys = new ArrayList[data.numberOfVehicleTypes];
        double fitness = 0;
        for (int vt = 0 ; vt < giantTourEntry.length ; vt++){
            allJourneys[vt] = new ArrayList<>();
            ArrayList<Journey> journeys = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, period, vt);
            for (Journey journey : journeys){
                fitness += journey.getTotalFitness(orderDistribution);
            }
            allJourneys[vt].addAll(journeys);

        }
        this.fitness = fitness;
        this.journeys  = allJourneys;
    }

    @Override
    public int compareTo(PeriodSolution o) {
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
