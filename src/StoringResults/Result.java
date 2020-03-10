package StoringResults;

import DataFiles.Parameters;
import Individual.Individual;
import ProductAllocation.OrderDistribution;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class Result {
    OrderDistribution orderDistribution;
    ArrayList<VehicleResult> vehicleResults;



    public Result(Individual individual){
        //vehicleResults = Extractor.giantTourResult(individual.data, individual.giantTour, individual.giantTourSplit, individual.vehicleAssigment)


    }

    public void storeDetailed() throws IOException {

        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Specify detailed filename: ");

        String fileName = myObj.nextLine();  // Read user inpu
        File newFile = new File(Parameters.filePathDetailed + fileName);
        Writer writer = Files.newBufferedWriter(null, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name","Model Type", "Objective Value", "Runtime", "Preprocess time", "Total runtime", "Gap", "Objective Bound", "Objective Bound C",
                    "isOptimal", "OptimizationStatusCode", "Symmetry",
                    "Date", "vehiclesUsed", "arcsUsed",
                    "tripsUsed", "journeysUsed", "arcsTotal", "tripsTotal", "journeysTotal",
                    "overtimeDepot", "Rows", "Columns", "ContinuousVariables", "BinaryVarables", "NonZeros", "SolutionCount",
                    "Vehicles", "Customers", "DividableCommodity" ,"NonDividableCommodity", "Trips", "Periods", "NodeCount", "RootNodeSolution", "#TripsGenerated", "#JourneysGenerated","Paths"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }


        String[] results = {};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();

    }

    public void storeSummary() throws IOException {

        File newFile = new File(Parameters.filePathDetailed);
        Writer writer = Files.newBufferedWriter(null, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"File name","Model Type", "Objective Value", "Runtime", "Preprocess time", "Total runtime", "Gap", "Objective Bound", "Objective Bound C",
                    "isOptimal", "OptimizationStatusCode", "Symmetry",
                    "Date", "vehiclesUsed", "arcsUsed",
                    "tripsUsed", "journeysUsed", "arcsTotal", "tripsTotal", "journeysTotal",
                    "overtimeDepot", "Rows", "Columns", "ContinuousVariables", "BinaryVarables", "NonZeros", "SolutionCount",
                    "Vehicles", "Customers", "DividableCommodity" ,"NonDividableCommodity", "Trips", "Periods", "NodeCount", "RootNodeSolution", "#TripsGenerated", "#JourneysGenerated","Paths"};
            csvWriter.writeNext(CSV_COLUMNS, false);
        }


        String[] results = {};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();
    }



}
