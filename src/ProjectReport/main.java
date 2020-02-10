package ProjectReport;
import java.io.File;

public class main {


    private static final Object GRB = 0;


    public static String[] findFiles() {
        File folder = new File("data");
        String[] files = folder.list();
        for (String file : files){
            System.out.println(file);
        }
        return files;
    }

    public static void main(String[] args) {

        if (Parameters.runAllFiles) {
            String[] fileLocations = findFiles();
            int numDataSet = 0;
            for (String file : fileLocations) {

                numDataSet++;
                System.out.println("");
                System.out.println("Running data set number: " + numDataSet);
                System.out.println("Named: " + file);
                System.out.println("");

                if (Parameters.runArcFlow) {
                    ArcFlowModel afm = new ArcFlowModel("data\\" + file);
                    afm.runModel(Parameters.arcSymmetry);
                }
                if (Parameters.runPathFlow) {
                    PathFlowModel pfm = new PathFlowModel("data\\" + file);
                    pfm.runModel(Parameters.pathSymmetry);
                }
                if (Parameters.runJourneyBased) {
                    JourneyBasedModel jbm = new JourneyBasedModel("data\\" + file);
                    jbm.runModel(Parameters.journeySymmetry);
                }
            }
        }
        else if(!Parameters.filepath.equals("")){
            if (Parameters.runArcFlow) {
                ArcFlowModel afm = new ArcFlowModel(Parameters.filepath);
                afm.runModel(Parameters.arcSymmetry);
            }
            if (Parameters.runPathFlow) {
                PathFlowModel pfm2 = new PathFlowModel(Parameters.filepath);
                pfm2.runModel(Parameters.pathSymmetry);
            }
            if (Parameters.runJourneyBased) {
                JourneyBasedModel jbm = new JourneyBasedModel(Parameters.filepath);
                jbm.runModel(Parameters.journeySymmetry);
            }
        }
    }
}