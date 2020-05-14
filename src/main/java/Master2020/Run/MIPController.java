package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.MIP.DataConverter;
import Master2020.PR.ArcFlowModel;
import Master2020.PR.DataMIP;
import Master2020.PR.JourneyBasedModel;
import Master2020.PR.PathFlowModel;
import Master2020.StoringResults.Result;

import java.io.IOException;

public class MIPController {

    public static void runMIPAFM(){
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        ArcFlowModel afm = new ArcFlowModel(dataMip);
        afm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual bestIndividual = afm.getIndividual();
        Result result = new Result(bestIndividual, "AFM", afm.feasible, afm.optimal);
        try{
            result.store(afm.runTime, afm.MIPGap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runMIPPFM(){
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        PathFlowModel pfm = new PathFlowModel(dataMip);
        pfm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual bestIndividual = pfm.getIndividual();
        Result result = new Result(bestIndividual, "PFM", pfm.feasible, pfm.optimal);
        try{
            result.store(pfm.runTime, pfm.MIPGap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parameters.randomSeedValue += 1;

    }

    public static void runMIPJBM(){
        System.out.println("RUNNING FOR SAMPLE: " + Parameters.randomSeedValue);
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        JourneyBasedModel jbm = new JourneyBasedModel(dataMip);
        jbm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual bestIndividual = jbm.getIndividual();
        Result result = new Result(bestIndividual, "JBM", jbm.feasible, jbm.optimal);
        try{
            result.store(jbm.runTime, jbm.MIPGap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
