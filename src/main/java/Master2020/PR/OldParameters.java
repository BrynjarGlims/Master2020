package Master2020.PR;
public class OldParameters {

    //Chose which models to run. Results are written to csv in folder results/result-files
    public static final boolean runArcFlow = false;
    public static final boolean runPathFlow = false;
    public static final boolean runJourneyBased = false;

    //Chose symmetry for path and journey model:
    public static final String arcSymmetry = "trips"; // none, car, trips, customers, cost, duration
    public static final String pathSymmetry = "trips"; //none, car, trips, customers, cost, duration
    public static final String journeySymmetry = "car"; //none, cost, customers, trips

    //Timeout for all models:
    public static final int timeOut = 3600; //given in seconds
    
    //Chose which files to run:
    public static final boolean runAllFiles = true; //if true all files in /data folder is run
    public static final String filepath = "filepath"; //specify specific file to only run this file

    //Chose to plot results
    public static final boolean plotArcFlow = false; //true for plot
    public static final boolean plotPathFlow = false;
}
