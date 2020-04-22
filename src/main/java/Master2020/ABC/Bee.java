package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.AdSplit;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Bee {

    public Data data;
    public int period;
    public OrderDistribution orderDistribution;
    public int numCustomers;
    public double[] position;
    public double fitness;
    public PeriodSwarm colony;
    protected ThreadLocalRandom random = ThreadLocalRandom.current();


    public Bee(Data data, int period, OrderDistribution orderDistribution, PeriodSwarm colony){
        this.period = period;
        this.data = data;
        this.numCustomers = data.numberOfCustomerVisitsInPeriod[period];
        this.orderDistribution = orderDistribution;
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
        int numDimensions = random.nextInt(1, Math.min(numCustomers, Parameters.maxBoundDimensionality + 1));
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
        ArrayList<Integer>[] giantTourEntry = HelperFunctions.parsePosition(this, position);
        double fitness = 0;
        for (int vt = 0 ; vt < giantTourEntry.length ; vt++){
            ArrayList<Journey> journeys = AdSplit.adSplitSingular(giantTourEntry[vt], data, orderDistribution, period, vt);
            for (Journey journey : journeys){
                fitness += journey.getTotalFitness(orderDistribution);
            }
        }
        return fitness;
    }






    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
        System.out.println(Arrays.toString(data.customersInPeriod.get(1)));


    }


    
}
