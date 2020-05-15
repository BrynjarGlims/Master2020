package Master2020.StoringResults;

import Master2020.DataFiles.Customer;
import Master2020.DataFiles.Parameters;
import Master2020.Interfaces.PeriodicSolution;
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
import java.util.Date;
import java.util.Scanner;

public class SolutionStorer {

    public static String modelName;

    public static void store(PeriodicSolution periodicSolution, double startTime, String folderName){
        try {
            String filePath = FileParameters.filePathDetailed + "/" + folderName + "/" + folderName + "_solutions.csv";
            File newFile = new File(filePath);
            System.out.println("Storing result at path : " + newFile.getAbsolutePath());
            Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            NumberFormat formatter = new DecimalFormat("#0.000000");
            SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            double runtime = (System.currentTimeMillis() - startTime)/1000;
            double fitnesses[] = periodicSolution.getFitnesses();

            if (newFile.length() == 0) {
                String[] CSV_COLUMNS = {"Instance Name",
                        "Time",
                        "Objective",
                        "TravelCost",
                        "VehicleUsage",
                        "OverTimeCost",
                        "TimeWarp",
                        "OverLoad",
                        "Feasible" };
                csvWriter.writeNext(CSV_COLUMNS, false);
            }
            String[] results = {folderName,
                    formatter.format(runtime),
                    formatter.format(periodicSolution.getFitness()),
                    formatter.format(fitnesses[0]),
                    formatter.format(fitnesses[3]),
                    formatter.format(periodicSolution.getOrderDistribution().getFitness()),
                    formatter.format(fitnesses[1]),
                    formatter.format(fitnesses[2]),
                    Boolean.toString(periodicSolution.isFeasible())};
            csvWriter.writeNext(results, false);

            csvWriter.close();
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }





    }




    public static String getFolderName(String modelName){
        if (FileParameters.useDefaultFileName){
            SimpleDateFormat date_formatter = new SimpleDateFormat("dd_MM_yyyy");
            String dateString = date_formatter.format(new Date());
            return modelName + "_S" + Parameters.randomSeedValue + "_C" + Parameters.numberOfCustomers +
                    "_V" + Parameters.numberOfVehicles + "_" + dateString;
        }
        else if (FileParameters.specifyFileNameWhenRunning){
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            System.out.println("Specify detailed filename: ");
            return myObj.nextLine();  // Read user input
        }
        createDirectory();
        createDetailedDirectory(modelName);
        return modelName;
    }

    public static void createDirectory(){
        System.out.println("Directory Create");
        File f = new File(FileParameters.filePathSummary);
        if (!f.exists()) {
            f.mkdir();
        }
        f = new File(FileParameters.filePathDetailed);
        if (!f.exists()) {
            f.mkdir();
        }
        else {
            f.delete();
            if (!f.exists()){
                f.mkdir();
            }
        }

    }

    public static void createDetailedDirectory(String modelName){
        File file = new File(FileParameters.filePathDetailed + "/"+ modelName  );
        boolean bool = file.mkdir();
        if (bool){
            System.out.println("Directory succesfully created");
        }
    };
}
