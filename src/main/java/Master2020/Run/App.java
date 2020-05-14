package Master2020.Run;


import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;

import static Master2020.Run.MIPController.*;

public class App {


    public static void main(String[] args) throws Exception {


        for (int seed : Parameters.seeds) {
            Parameters.randomSeedValue = seed;
            Data data = DataReader.loadData();
            System.out.println(Parameters.randomSeedValue);
            HybridController hybridController = new HybridController(data);
            hybridController.initialize();
            hybridController.run();


//        if (args[0].equals("AFM"))
//            runMIPAFM();
//        else if (args[0].equals("PFM"))
//            runMIPPFM();
//        else if (args[0].equals("JBM"))
//            runMIPJBM();
//        else if (args[0].equals("GA")) {
//            Data data = DataReader.loadData();
//            GAController gaController = new GAController(data);
//            gaController.run();
//        } else if (args[0].equals("PGA")) {
//            Parameters.isPeriodic = true;
//            Data data = DataReader.loadData();
//            HybridController hybridController = new HybridController(data);
//            hybridController.initialize();
//            hybridController.run();
//        }

        }
    }

}