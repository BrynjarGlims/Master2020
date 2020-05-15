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
import Master2020.StoringResults.SolutionStorer;

import java.io.IOException;

public class MIPController {

    public static void runMIPAFM(){
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        ArcFlowModel afm = new ArcFlowModel(dataMip);
        afm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual bestIndividual = afm.getIndividual();
        String modelName = "AFM";
        String folderName = SolutionStorer.getFolderName(modelName);

        Result result = new Result(bestIndividual, modelName, folderName, afm.feasible, afm.optimal);
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
        String modelName = "PFM";
        String folderName = SolutionStorer.getFolderName(modelName);
        Result result = new Result(bestIndividual, modelName, folderName, pfm.feasible, pfm.optimal);
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
        String modelName = "JBM";
        String folderName = SolutionStorer.getFolderName(modelName);
        Result result = new Result(bestIndividual, modelName, folderName, jbm.feasible, jbm.optimal);
        try{
            result.store(jbm.runTime, jbm.MIPGap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
