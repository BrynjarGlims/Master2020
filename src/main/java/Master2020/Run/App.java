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
    private static int[] journeys;


    public static void main(String[] args) throws Exception {
        //args = new String[]{"HYBRID", "debug"};
        switch (args[1]) {
            case "base":
                baseCase(args);
                break;
            case "parameter":
                parameterTuning(args);
                break;
            case "full":
                fullRun(args);
                break;
            case "debug":
                debugRun(args);
                break;
        }
    }


    private static void fullRun(String[] args) throws Exception {
        initialize();
        Parameters.customFileName = "fullRun" + args[1];
        Parameters.totalRuntime = 1800000;
        for (int iteration = 0 ; iteration < 1 ; iteration++){
            for (int dataset = 0 ; dataset < 2 ; dataset++){
                Parameters.useVestTeleDataset = dataset == 0;
                for (int instance = 0 ; instance < 5 ; instance++){
                    Parameters.numberOfCustomers = customers[instance];
                    Parameters.numberOfVehicles = vehicles[instance];
                    Parameters.numberOfIndividualJourneysInMIPPerPeriod = journeys[instance];
                    Parameters.timeLimitPerAlgorithm = Parameters.timeLimitPerAlgorithmConstant + Parameters.numberOfCustomers*Parameters.timeLimitPerAlgorithmVariable;
                    if (Parameters.timeLimitPerAlgorithm > Parameters.totalRuntime){
                        throw new IllegalArgumentException(" Time limit per algorithm is larger than totalRuntime");
                    }
                    for (int seed : seeds[dataset][instance]){
                        Parameters.randomSeedValue = seed;
                        System.out.println("running " + args[0] + " for dataset " + dataset + " for " + Parameters.numberOfCustomers + " customers, seed: " + seed);
                        System.out.println("Time limit per algorithm: " + Parameters.timeLimitPerAlgorithm/1000 + " seconds");
                        run(args);
                    }
                }
            }
        }
    }

    private static void debugRun(String[] args) throws Exception {
        Parameters.customFileName = "debugRun" + args[1];
        Parameters.totalRuntime = 1800000;
        for (int dataset = 0 ; dataset < 2 ; dataset++){
            Parameters.useVestTeleDataset = dataset == 0;
            for (int instance = 4 ; instance < 5 ; instance++){
                Parameters.numberOfCustomers = 100;
                Parameters.numberOfVehicles = 50;
                Parameters.removePresolve = true;
                Parameters.numberOfIndividualJourneysInMIPPerPeriod = 3;
                Parameters.timeLimitPerAlgorithm = Parameters.timeLimitPerAlgorithmConstant + Parameters.numberOfCustomers*Parameters.timeLimitPerAlgorithmVariable;
                if (Parameters.timeLimitPerAlgorithm > Parameters.totalRuntime){
                    throw new IllegalArgumentException(" Time limit per algorithm is larger than totalRuntime");
                }
                Parameters.randomSeedValue = 10;
                System.out.println("running " + args[0] + " for dataset " + dataset + " for " + Parameters.numberOfCustomers + " customers, seed: " + Parameters.randomSeedValue);
                System.out.println("Time limit per algorithm: " + Parameters.timeLimitPerAlgorithm/1000 + " seconds");
                run(args);

            }
        }
    }

    private static void parameterTuning(String[] args) throws Exception {
        Parameters.totalRuntime = 1200000;
        Parameters.numberOfCustomers = 50;
        Parameters.numberOfVehicles = 25;
        Parameters.timeLimitPerAlgorithm = Parameters.timeLimitPerAlgorithmConstant + Parameters.numberOfCustomers*Parameters.timeLimitPerAlgorithmVariable;
        for (int iteration = 0 ; iteration < 5 ; iteration++){
            for (int bool = 0 ; bool < 2 ; bool++){
                Parameters.useVestTeleDataset = bool == 0;
                int[] seeds = Parameters.useVestTeleDataset ? new int[]{15,84} : new int[]{69,85};
                for (int seed : seeds) {
                    Parameters.randomSeedValue = seed;

//                    MUST BE CHANGED DEPENDING ON WHAT WE WANT TO TEST!!!
//                    GENERATIONS / OD
                    double[] gens = new double[]{1};
                    for (double gen : gens){
                        Parameters.numberOfPGA = Integer.parseInt(args[2]);
                        Parameters.customFileName = "FinalBalancePGA" + Parameters.numberOfPGA;
                        System.out.println(Parameters.customFileName);
                        System.out.println("Using vestTele: " + Parameters.useVestTeleDataset + " for seed: " + Parameters.randomSeedValue);
                        run(args);
                    }
                }
            }
        }
    }

    private static void baseCase(String[] args) throws Exception {
        Parameters.numberOfCustomers = 10;
        Parameters.numberOfVehicles = 5;
        Parameters.customFileName = "baseCase" + args[1];
        Parameters.totalRuntime = 1800000;
        Parameters.timeLimitPerAlgorithm = Parameters.timeLimitPerAlgorithmConstant + Parameters.numberOfCustomers*Parameters.timeLimitPerAlgorithmVariable;
        for (int iteration = 0 ; iteration < 2 ; iteration++){
            for (int bool = 0 ; bool < 2 ; bool++){
                Parameters.useVestTeleDataset = bool == 0;
                int[] seeds = Parameters.useVestTeleDataset ? new int[]{89,1} : new int[]{57,97,80};
                for (int seed : seeds) {
                    Parameters.randomSeedValue = seed;
                    run(args);
                }
            }
        }
    }

    private static void run(String[] args) throws Exception {
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
            Parameters.useJCM = false;
            Parameters.numberOfPGA = Parameters.numberOfAlgorithms;
            Parameters.isPeriodic = true;
            HybridController hc = new HybridController();
            hc.run();
        }
        else if (args[0].equals("ABC")) {
            Parameters.numberOfPGA = 0;
            Parameters.useJCM = false;
            Parameters.isPeriodic = true;
            HybridController hc = new HybridController();
            hc.run();
        }
        else if (args[0].equals("HYBRID")) {
            Parameters.useJCM = true;
            Parameters.isPeriodic = true;
            HybridController hc = new HybridController();
            hc.run();
        }
    }

    private static void initialize(){
        seeds = new int[2][5][];
        customers = new int[5];
        vehicles = new int[5];
        journeys = new int[5];
        //25
        seeds[0][0] = new int[]{15, 84};
        seeds[1][0] = new int[]{69,85};
        customers[0] = 25;
        vehicles[0] = 12;
        journeys[0] = 3;
        //50
        seeds[0][1] = new int[]{94,41};
        seeds[1][1] = new int[]{20,60};
        customers[1] = 50;
        vehicles[1] = 25;
        journeys[1] = 3;
        //75
        seeds[0][2] = new int[]{32,18,37};
        seeds[1][2] = new int[]{1};
        customers[2] = 75;
        vehicles[2] = 32;
        journeys[2] = 2;
        //100
        seeds[0][3] = new int[]{97,46,35,76};
        seeds[1][3] = new int[]{};
        customers[3] = 100;
        vehicles[3] = 50;
        journeys[4] = 1;
        //115
        seeds[0][4] = new int[]{10,27};
        seeds[1][4] = new int[]{};
        customers[4] = 115;
        vehicles[4] = 62;
        journeys[4] = 1;
    }
}