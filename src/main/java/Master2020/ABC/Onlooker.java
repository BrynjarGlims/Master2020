package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.ProductAllocation.OrderDistribution;

public class Onlooker extends Bee {


    public Onlooker(Data data, int period, OrderDistribution orderDistribution, PeriodSwarm colony) {
        super(data, period, orderDistribution, colony);
    }


    public double[] scout(){
        this.position = super.scout();
        return position;
    }


    public void search(Bee neighbor, Employee employer){
        position = employer.position.clone();
        for (int d = 0 ; d < position.length ; d++){
            position[d] = ((position[d] + random.nextDouble(-Parameters.onlookerRandomAdjustment, Parameters.onlookerRandomAdjustment)) + data.numberOfVehicleTypes) % data.numberOfVehicleTypes;
        }
        updatePosition(position, neighbor.position, false);
        double fitness = getFitness();
        if (fitness < employer.fitness){
            employer.onlookerFitnesses.put(this, fitness);
        }
    }

}