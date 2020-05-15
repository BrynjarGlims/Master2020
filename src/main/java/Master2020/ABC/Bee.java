package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.FitnessCalculation;
import Master2020.Genetic.PenaltyControl;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Journey;
import Master2020.Utils.Utils;
import Master2020.Utils.WeightedRandomSampler;

import java.util.ArrayList;
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
    private PenaltyControl penaltyControl;


    public Bee(Data data, int period, PeriodSwarm colony, PenaltyControl penaltyControl){
        this.period = period;
        this.data = data;
        this.numCustomers = data.numberOfCustomerVisitsInPeriod[period];
        this.colony = colony;
        this.penaltyControl = penaltyControl;
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
        double[] fitnesses = new double[4];
        for (int vt = 0 ; vt < giantTourEntry.length ; vt++){
            ArrayList<Journey> journeys = AdSplit.adSplitSingular(giantTourEntry[vt], data, colony.orderDistribution, period, vt, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
            for (Journey journey : journeys){
                fitnesses = FitnessCalculation.getJourneyFitness(journey, colony.orderDistribution);
                for (double d : fitnesses){
                    fitness += d;
                }
                penaltyControl.adjust(fitnesses[1] > 0, fitnesses[3] > 0);
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
            }
        }
        this.position = po.parsePosition(numCustomers);
    }


    private boolean operation(PositionObject po, int vt, Function<List<?>, Function<Integer, Consumer<Integer>>> function){
        ArrayList<Integer> customerVisits = po.customerVisits[vt];
        ArrayList<Integer> parsedIndices = po.parsedIndices[vt];
        if (parsedIndices.size() > 1){
            int[] positions = getPositions(parsedIndices);
            ArrayList<Journey> oldJourneys = AdSplit.adSplitSingular(customerVisits, data, colony.orderDistribution, period, vt, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
            double oldFitness = FitnessCalculation.getTotalPeriodVehicleTypeFitness(oldJourneys, colony.orderDistribution, 1);
            //update
            function.apply(customerVisits).apply(positions[0]).accept(positions[1]);
            //evaluate
            ArrayList<Journey> newJourneys = AdSplit.adSplitSingular(customerVisits, data, colony.orderDistribution, period, vt, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
            double newFitness = FitnessCalculation.getTotalPeriodVehicleTypeFitness(newJourneys, colony.orderDistribution, 1);
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
