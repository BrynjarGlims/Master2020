package Master2020.Run;


import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import gurobi.GRBException;

import java.io.IOException;

import static Master2020.Run.MIPController.*;

public class App {

    private static int[][][] seeds;//dataset, instance size, seed
    private static int[] customers;//instance size
    private static int[] vehicles;//instance size


    public static void main(String[] args) throws Exception {
        parameterTuning(args);
        //fullRun(args);
    }


    private static void fullRun(String[] args) throws Exception {
        initialize();
        for (int dataset = 0 ; dataset < 2 ; dataset++){
            for (int instance = 0 ; instance < 5 ; instance++){
                Parameters.numberOfCustomers = customers[instance];
                Parameters.numberOfVehicles = vehicles[instance];
                for (int seed : seeds[dataset][instance]){
                    Parameters.randomSeedValue = seed;
                    System.out.println("running " + args[0] + " for dataset " + dataset + " for " + Parameters.numberOfCustomers + " customers, seed: " + seed);
                    if (args[0].equals("AFM"))
                        runMIPAFM();
                    else if (args[0].equals("PFM"))
                        runMIPPFM();
                    else if (args[0].equals("JBM"))
                        runMIPJBM();
                    else if (args[0].equals("GA")){
                        GAController ga = new GAController();
                        ga.runGA();
                    }
                    else if (args[0].equals("PGA")) {
                        Parameters.numberOfPGA = Parameters.numberOfAlgorithms;
                        Parameters.isPeriodic = true;
                        HybridController hc = new HybridController();
                        hc.run();
                    }
                    else if (args[0].equals("ABC")) {
                        Parameters.numberOfPGA = 0;
                        HybridController hc = new HybridController();
                        hc.run();
                    }
                    else if (args[0].equals("HYBRID")) {
                        HybridController hc = new HybridController();
                        hc.run();
                    }
                }
            }
        }
    }

    private static void parameterTuning(String[] args) throws Exception {
        for (int iteration = 0 ; iteration < 5 ; iteration++){
            for (int bool = 0 ; bool < 2 ; bool++){
                Parameters.useVestTeleDataset = bool == 0;
                int[] seeds = Parameters.useVestTeleDataset ? new int[]{15,84} : new int[]{69,85};
                for (int seed : seeds) {
                    Parameters.randomSeedValue = seed;

                    //MUST BE CHANGED DEPENDING ON WHAT WE WANT TO TEST!!!
                    //GENERATIONS / OD
                    double[] gens = new double[]{1,2,3,4,5};
                    for (double gen : gens){
                        Parameters.heuristicDominanceValue = gen;
                        Parameters.customFileName = "HeuristicDominance" + gen;
                        System.out.println(Parameters.customFileName);
                        System.out.println("Using vestTele: " + Parameters.useVestTeleDataset + " for seed: " + Parameters.randomSeedValue);


                        if (args[0].equals("AFM"))
                            runMIPAFM();
                        else if (args[0].equals("PFM"))
                            runMIPPFM();
                        else if (args[0].equals("JBM"))
                            runMIPJBM();
                        else if (args[0].equals("GA")){
                            GAController ga = new GAController();
                            ga.runGA();
                        }
                        else if (args[0].equals("PGA")) {
                            Parameters.numberOfPGA = Parameters.numberOfAlgorithms;
                            Parameters.isPeriodic = true;
                            HybridController hc = new HybridController();
                            hc.run();
                        }
                        else if (args[0].equals("ABC")) {
                            Parameters.numberOfPGA = 0;
                            HybridController hc = new HybridController();
                            hc.run();
                        }
                        else if (args[0].equals("HYBRID")) {
                            HybridController hc = new HybridController();
                            hc.run();
                        }
                    }
                }
            }
        }
    }

    private static void initialize(){
        seeds = new int[2][5][];
        customers = new int[5];
        vehicles = new int[5];
        //25
        seeds[0][0] = new int[]{}; //15, 84};
        seeds[1][0] = new int[]{};//69,85};
        customers[0] = 25;
        vehicles[0] = 12;
        //50
        seeds[0][1] = new int[]{};//94,41};
        seeds[1][1] = new int[]{};//20,60};
        customers[1] = 50;
        vehicles[1] = 25;
        //75
        seeds[0][2] = new int[]{};//32,18,37};
        seeds[1][2] = new int[]{};//1};
        customers[2] = 75;
        vehicles[2] = 32;
        //100
        seeds[0][3] = new int[]{};//97,46,35,76};
        seeds[1][3] = new int[]{};
        customers[3] = 100;
        vehicles[3] = 50;
        //115
        seeds[0][4] = new int[]{10,27};
        seeds[1][4] = new int[]{};
        customers[4] = 115;
        vehicles[4] = 62;
    }
}