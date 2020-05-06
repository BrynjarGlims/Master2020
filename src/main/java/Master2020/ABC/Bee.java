package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Journey;
import Master2020.Utils.Utils;
import Master2020.Utils.WeightedRandomSampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Bee {

    public Data data;
    public int period;
    public int numCustomers;
    public double[] position;
    public double fitness;
    public PeriodSwarm colony;
    protected ThreadLocalRandom random = ThreadLocalRandom.current();
    protected WeightedRandomSampler enhancementsSampler = new WeightedRandomSampler(Parameters.weightsEnhancement);


    public Bee(Data data, int period, PeriodSwarm colony){
        this.period = period;
        this.data = data;
        this.numCustomers = data.numberOfCustomerVisitsInPeriod[period];
        this.colony = colony;
        scout();
    }

    protected double[] scout(){
        //set random position of bee individual
        double[] position = new double[data.numberOfCustomerVisitsInPeriod[period]];
        for (int i = 0 ; i < position.length ; i++){
            position[i] = ThreadLocalRandom.current().nextDouble(0, data.numberOfVehicleTypes);
        }
        return position;
    }

    protected void updatePosition(double[] newPosition, double[] neighborPosition, boolean employee){
        int numDimensions = random.nextInt(1, Math.max( 2, Math.min(numCustomers, Parameters.maxBoundDimensionality + 1)));
        int[] dimensions  = new int[numDimensions];
        for (int d = 0 ; d < numDimensions ; d++){
            dimensions[d] = random.nextInt(0, numCustomers);
        }
        double weight = employee ? Parameters.weightNeighborEmployed : Parameters.weightNeighborOnlooker;
        for (int d : dimensions){
            newPosition[d] = (newPosition[d]
                    + weight * random.nextDouble(-(Parameters.movementRange/2), Parameters.movementRange) * (neighborPosition[d] - newPosition[d])
                    +  random.nextDouble(0, Parameters.weightGlobalBest) * (colony.globalBestPosition[d] - newPosition[d])
                    + data.numberOfVehicleTypes) % data.numberOfVehicleTypes;
        }
    }

    public double getFitness(){
        return getFitness(true);
    }

    public double getFitness(boolean update){
        if (update){
            this.fitness = getFitness(this.position);
        }
        return this.fitness;
    }

    protected double getFitness(double[] position){
        ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(data, period, position);
        double fitness = 0;
        for (int vt = 0 ; vt < giantTourEntry.length ; vt++){
            ArrayList<Journey> journeys = AdSplit.adSplitSingular(giantTourEntry[vt], data, colony.orderDistribution, period, vt);
            for (Journey journey : journeys){
                fitness += journey.getTotalFitness(colony.orderDistribution);
            }
        }
        return fitness;
    }

    protected void enhance(){
        PositionObject po = HelperFunctions.dividePosition(data, period, position);
        boolean foundBetterSolution;
        for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
            int count = 0;
            while (count < Parameters.numberOfEnhancements){
                foundBetterSolution = false;
                int action = enhancementsSampler.nextIndex();
                if (action == 0){
                    foundBetterSolution = operation(po, vt, Utils.reverse);
                }
                else if (action == 1){
                    foundBetterSolution = operation(po, vt, Utils.swap);
                }
                else if (action == 2){
                    foundBetterSolution = operation(po, vt, Utils.insert);
                }
                count = foundBetterSolution ? 0 : count + 1;
                for (double d : enhancementsSampler.weights){
                    if(d < 0){
                        System.out.println(Arrays.toString(enhancementsSampler.weights));
                    }
                }
            }
        }
        this.position = po.parsePosition(numCustomers);
    }


    private boolean operation(PositionObject po, int vt, Function<List<?>, Function<Integer, Consumer<Integer>>> function){
        ArrayList<Integer> customerVisits = po.customerVisits[vt];
        ArrayList<Integer> parsedIndices = po.parsedIndices[vt];
        if (parsedIndices.size() > 1){
            int[] positions = getPositions(parsedIndices);
            ArrayList<Journey> oldJourneys = AdSplit.adSplitSingular(customerVisits, data, colony.orderDistribution, period, vt);
            double oldFitness = 0;
            for (Journey journey : oldJourneys){
                oldFitness += FitnessCalculation.getTotalJourneyFitness(journey, colony.orderDistribution);
            }
            //update
            function.apply(customerVisits).apply(positions[0]).accept(positions[1]);
            //evaluate
            ArrayList<Journey> newJourneys = AdSplit.adSplitSingular(customerVisits, data, colony.orderDistribution, period, vt);
            double newFitness = 0;
            for (Journey journey : newJourneys){
                newFitness += FitnessCalculation.getTotalJourneyFitness(journey, colony.orderDistribution);
            }
            if (newFitness < oldFitness){
                //update other structures
                function.apply(parsedIndices).apply(positions[0]).accept(positions[1]);
                return true;
            }
            else {
                //reverse change
                function.apply(customerVisits).apply(positions[0]).accept(positions[1]);
            }
        }
        return false;
    }

    private <E> int[] getPositions(ArrayList<E> visits){
        int pos1 = random.nextInt(visits.size() - 1);
        int pos2 = random.nextInt(pos1 + 1, visits.size());
        return new int[]{pos1, pos2};
    }
}
