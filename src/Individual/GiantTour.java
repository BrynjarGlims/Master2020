package Individual;

import DataFiles.Customer;
import DataFiles.Data;
import DataFiles.DataReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTour {

    public ArrayList<Integer>[][] chromosome;
    public Data data;


    public GiantTour(Data data){
        this.data = data;
        chromosome = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
    }


    public void initializeGiantTour() {
        constructChromosome(data.numberOfPeriods, data.numberOfVehicleTypes);
        for (Customer c : data.customers) {
            placeCustomer(c);
        }
        shuffleChromosome();
    }

    private void constructChromosome(int numPeriods, int numVehicleTypes){
        for (int p = 0 ; p < numPeriods ; p++){
            for (int vt = 0 ; vt < numVehicleTypes ; vt++){
                chromosome[p][vt] = new ArrayList<Integer>();
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
            }
        }
    }

    public String toString(){
        String out = "";
        for (ArrayList[] i : chromosome){
            out += "\nNEW PERIOD\n";
            for (ArrayList list : i){
                if (list != null){
                    out += list.toString() + "\n";
                }
            }
        }
        return out;
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        GiantTour gt = new GiantTour(data);
        gt.initializeGiantTour();

        System.out.println(Arrays.toString(gt.chromosome));
        for (int i = 0 ; i < data.numberOfVehicleTypes ; i++) {
            System.out.println("NEW DAY");
            for (ArrayList<Integer> vehicletypes : gt.chromosome[i]) {
                for (int j : vehicletypes){
                    System.out.println(j);
                }
            }
        }
    }
}
