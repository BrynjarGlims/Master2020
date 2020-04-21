package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import scala.xml.PrettyPrinter;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Bee {

    public Data data;
    public int period;
    public int numCustomers;
    public double[] position;
    public boolean employed;
    public int trials;
    private ThreadLocalRandom random = ThreadLocalRandom.current();

    public Bee(Data data, int period){
        this.period = period;
        this.data = data;
        scout();
    }


    public void scout(){
        //set random position of bee individual
        position = new double[data.numberOfCustomerVisitsInPeriod[period]];
        for (int i = 0 ; i < position.length ; i++){
            position[i] = ThreadLocalRandom.current().nextDouble(0, data.numberOfVehicleTypes);
        }
    }


    public void search(Bee neighbor, double[] globalBest){
        int numDimensions = random.nextInt(1, Math.min(numCustomers, Parameters.maxBoundDimensionality + 1));
        int[] dimensions  = new int[numDimensions];
        for (int d = 0 ; d < numDimensions ; d++){
            dimensions[d] = random.nextInt(0, numCustomers);
        }
        double weight = employed ? Parameters.weightNeighborEmployed : Parameters.weightNeighborOnlooker;
        for (int d : dimensions){
            position[d] = (position[d]
                    + weight * random.nextDouble(-Parameters.movementRange, Parameters.movementRange) * neighbor.position[d]
                    +  random.nextDouble(0, Parameters.weightGlobalBest) * globalBest[d]
                    + data.numberOfVehicleTypes) % data.numberOfVehicleTypes;
        }



    }

    public double getFitness(){
        // TODO: 21.04.2020 to be implemented
        return -1;
    }




    public static void main(String[] args){
        Data data = DataReader.loadData();
        System.out.println(Arrays.toString(data.customersInPeriod.get(0)));
        Bee bee = new Bee(data, 0);
        System.out.println(data.numberOfVehicleTypes);
        System.out.println(Arrays.toString(bee.position));
        double a = (-2 + 3) % 5;
        System.out.println(a);
    }


    
}
