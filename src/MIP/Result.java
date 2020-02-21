package MIP;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Result {
    //all attributes of the class must be defined
    //en variabel for model type --> indekseres med pathflow/arcflow
    //ta inn

    //main info
    public Double runTime;
    public Double objective;
    public Double MIPGap;
    public Double objBound;
    public Double objBoundC;
    public String modelType;
    public String instanceName;
    public String filename = "result_instances";
    public int isOptimal;
    public int optimistatus;
    public String symmetry;

    //solution info
    public int numVehicleUsed;
    public double volumeOvertime;
    public Date todaysDate = new Date();  //Todo; check if correct date

    public int numArcsUsed;
    public int numTripsUsed;
    public int numJourneysUsed;
    public int numTripVariables;
    public int numJourneyVariables;
    public int numArcsGenerated;
    public int numGeneratedTrips;
    public int numGeneratedJourneys;






    //Model specific info
    public int numRows;
    public int numCol;
    public int continuousVariables;
    public int binaryVariables;
    public int nonZeros;
    public int solCount;
    public double nodeCount;
    public double rootNodeSolution;

    // instance specific
    public int numVehicles;
    public int numCustomers;
    public int numDivCommodity;
    public int numNondivCommodity;
    public int numTrips;
    public int numPeriods;
    public double preProcessTime;


    public String filePath;
    public ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed;
    public String pathsString;



    // constructor
    public Result(Double runTime, Double objective, Double MIPGap, Double objBound, Double objBoundC, String modelType, String instanceName, int isOptimal,
                  int optimistatus,
                  int numVehicleUsed, int numArcsUsed, int numTripsUsed, int numJourneysUsed, int numArcsGenerated, int numTripVariables, int numJourneyVariables,
                  double volumeOvertime, int numRows,
                  int numCol, int continuousVariables, int binaryVariables, int nonZeros, int solCount, int numVehicles,
                  int numCustomers, int numDivCommodity, int numNondivCommodity, int numTrips, int numPeriods, double nodeCount, int rootNodeSolution, double preProcessTime,
                  int numGeneratedTrips, int numGeneratedJourneys,
                  ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed, String symmetry) {

        this.runTime  =runTime;
        this.objective = objective;
        this.MIPGap = MIPGap;
        this.objBound = objBound;
        this.objBoundC = objBoundC;
        this.symmetry = symmetry;

        this.modelType = modelType;
        this.instanceName = instanceName;
        this.isOptimal = isOptimal;
        this.numVehicleUsed = numVehicleUsed;
        this.numArcsUsed = numArcsUsed;
        this.numTripsUsed = numTripsUsed;
        this.numJourneysUsed = numJourneysUsed;
        this.numArcsGenerated = numArcsGenerated;
        this.numTripVariables = numTripVariables;
        this.numJourneyVariables = numJourneyVariables;
        this.numGeneratedTrips = numGeneratedTrips;
        this.numGeneratedJourneys = numGeneratedJourneys;
        this.volumeOvertime = volumeOvertime;
        this.numRows = numRows;
        this.numCol = numCol;
        this.continuousVariables = continuousVariables;
        this.binaryVariables = binaryVariables;
        this.nonZeros = nonZeros;
        this.solCount = solCount;
        this.numVehicles = numVehicles;
        this.numCustomers = numCustomers;
        this.numDivCommodity = numDivCommodity;
        this.numNondivCommodity = numNondivCommodity;
        this.numTrips = numTrips;
        this.numPeriods = numPeriods;
        this.filePath = "results/result-files/" + filename  + ".csv";
        this.pathsUsed = pathsUsed;
        this.optimistatus = optimistatus;
        this.nodeCount = nodeCount;
        this.preProcessTime = preProcessTime;
        this.rootNodeSolution = rootNodeSolution;
    }
    //write all attributes of the object to results to a csv file





    public void store() throws IOException {

        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Instance Description","Model Type", "Objective Value", "Runtime", "Preprocess time", "Total runtime", "Gap", "Objective Bound", "Objective Bound C",
                    "isOptimal", "OptimizationStatusCode", "Symmetry",
                    "Date", "vehiclesUsed", "arcsUsed",
                    "tripsUsed", "journeysUsed", "arcsTotal", "tripsTotal", "journeysTotal",
                    "overtimeDepot", "Rows", "Columns", "ContinuousVariables", "BinaryVarables", "NonZeros", "SolutionCount",
                    "Vehicles", "Customers", "DividableCommodity" ,"NonDividableCommodity", "Trips", "Periods", "NodeCount", "RootNodeSolution", "#TripsGenerated", "#JourneysGenerated","Paths"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }
        if (pathsUsed != null){
            pathsString = "Result:";
            for ( ArrayList<ArrayList<ArrayList<Integer>>> day: pathsUsed){
                pathsString += ";";
                int vehicleCounter = 0;
                for ( ArrayList<ArrayList<Integer>> vehicle: day){
                    pathsString += "V" + vehicleCounter + ": ";
                    vehicleCounter++;
                    for ( ArrayList<Integer> trip : vehicle  ){
                        for (int customer : trip ){
                            pathsString += customer + "-";
                        }
                        pathsString += "N-";
                    }
                    pathsString += " ";
                }
            }
        }
        else {
            pathsString = "";
        }

        String[] results = {instanceName, modelType, formatter.format(objective),
                formatter.format(runTime), formatter.format(preProcessTime),formatter.format(preProcessTime+ runTime),
                formatter.format(MIPGap), formatter.format(objBound), formatter.format(objBoundC),
                formatter.format(isOptimal), formatter.format(optimistatus),symmetry,
                date_formatter.format(todaysDate), formatter.format(numVehicleUsed) , formatter.format(numArcsUsed),
                formatter.format(numTripsUsed) ,  formatter.format(numJourneysUsed) , formatter.format(numArcsGenerated),
                formatter.format(numTripVariables) , formatter.format(numJourneyVariables),
                formatter.format(volumeOvertime), formatter.format(numRows),
                formatter.format(numCol), formatter.format(continuousVariables), formatter.format(binaryVariables),
                formatter.format(nonZeros),formatter.format(solCount),formatter.format(numVehicles),
                formatter.format(numCustomers),formatter.format(numDivCommodity),formatter.format(numNondivCommodity),
                formatter.format(numTrips),formatter.format(numPeriods), formatter.format(nodeCount),
                formatter.format(rootNodeSolution), formatter.format(numGeneratedTrips), formatter.format(numGeneratedJourneys)
                , pathsString};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();

    }


    public static void main(String[] args) throws FileNotFoundException {

    }





}
