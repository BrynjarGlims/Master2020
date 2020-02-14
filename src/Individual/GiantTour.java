package Individual;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTour {


    ArrayList[][] chromosome;

    public GiantTour(Data data){ //save data?
        initializeGiantTour(data);
    }



    public void initializeGiantTour(Data data) {
        constructChromosome(data.numberOfPeriods, data.numberOfVehicleTypes);
        for (Customer c : data.customers) {
            placeCustomer(c);
        }
        shuffleChromosome();
    }

    private void constructChromosome(int numPeriods, int numVehicleTypes){
        chromosome = new ArrayList[numPeriods][numVehicleTypes];
        for (int p = 0 ; p < numPeriods ; p++){
            for (int vt = 0 ; vt < numVehicleTypes ; vt++){
                chromosome[p][vt] = new ArrayList();
            }
        }
    }

    private void placeCustomer(Customer customer){
        for (int i = 0 ; i < customer.requiredVisitPeriod.length ; i++){
            if (customer.requiredVisitPeriod[i] == 1) {
                chromosome[i][ThreadLocalRandom.current().nextInt(0, chromosome[0].length)].add(customer.customerID);
            }
        }
    }

    private void shuffleChromosome(){
        for (int p = 0; p < chromosome.length; p++) {
            for (int vt = 0; vt < chromosome[0].length; vt++) {
                Collections.shuffle(chromosome[p][vt]);
                System.out.println(chromosome[p][vt]);
            }
        }
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        GiantTour gt = new GiantTour(data);
        gt.initializeGiantTour(data);
    }
}
