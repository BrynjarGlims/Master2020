package Master2020.Run;


import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;

import static Master2020.Run.MIPController.*;

public class App {

    public static void main(String[] args) throws Exception {
        for (int bool = 0 ; bool < 2 ; bool++){
            Parameters.useVestTeleDataset = bool == 0;
            int[] seeds = Parameters.useVestTeleDataset ? new int[]{84} : new int[]{};
            for (int seed : seeds) {
                Parameters.randomSeedValue = seed;

                //MUST BE CHANGED DEPENDING ON WHAT WE WANT TO TEST!!!
                //OFFSPRING GENERATED
                double[] tripProbs = new double[]{0, 0.25, 0.5, 0.75, 1};
                for (double tripProb : tripProbs){
                    Parameters.tripOptimizerProbability = tripProb;
                    Parameters.customFileName = " TripOptimizer" + tripProb;
                    System.out.println(Parameters.ODMIPProbability);
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