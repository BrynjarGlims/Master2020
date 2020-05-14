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
        }
    }
}