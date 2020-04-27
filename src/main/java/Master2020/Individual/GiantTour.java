package Master2020.Individual;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class GiantTour {

    public ArrayList<Integer>[][] chromosome;
    public Data data;
    public boolean isPeriodic;
    public int periods;
    public int actualPeriod;



    public GiantTour(Data data){
       this(data, false, -1);
    }


    public GiantTour(Data data, boolean isPeriodic, int actualPeriod){
        this.data = data;
        this.isPeriodic = isPeriodic;
        this.periods = (isPeriodic ? 1 : data.numberOfPeriods);
        if (isPeriodic){
            this.actualPeriod = actualPeriod;
        }
        this.chromosome = new ArrayList[this.periods][data.numberOfVehicleTypes];

    }


    public void setChromosome(ArrayList<Integer>[][] chromosome) {
        this.chromosome = chromosome;
    }

    public void initializeGiantTour() {
        constructChromosome(this.periods, data.numberOfVehicleTypes);
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

    private void placeCustomer(Customer customer) {
        if (isPeriodic) {
            if (customer.requiredVisitPeriod[actualPeriod] == 1) {  //// TODO: 22/04/2020 Not sure if this is implemented correctly
                    chromosome[0][ThreadLocalRandom.current().nextInt(0, chromosome[0].length)].add(customer.customerID); //indexed with 0 for period, regardless of period
            }
        } else {
            for (int i = 0; i < customer.requiredVisitPeriod.length; i++) {
                if (customer.requiredVisitPeriod[i] == 1) {
                    chromosome[i][ThreadLocalRandom.current().nextInt(0, chromosome[0].length)].add(customer.customerID);
                }
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
        /*
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

         */
    }
}
