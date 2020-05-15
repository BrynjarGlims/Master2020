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

    public static void store(PeriodicSolution periodicSolution, String modelName){
        try {
            String fileName = getFileName(modelName);


            String filePath = FileParameters.filePathDetailed + "/" + fileName + "/" + fileName + "_customer.csv";
            File newFile = new File(filePath);
            System.out.println("Path : " + newFile.getAbsolutePath());
            Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            CSVWriter csvWriter = new CSVWriter(writer, Parameters.separator, CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            NumberFormat formatter = new DecimalFormat("#0.00000000");
            SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            System.out.println("Changing detailed file...");
            if (newFile.length() == 0) {
                String[] CSV_COLUMNS = {"Customer Name", "CustomerID", "Customer number", "Orders", "Dividable Orders", "NonDividable Orders",
                        "Frequency", "Total Volume", "Visit Monday", "Visit Tuesday", "Visit Wednesday", "Visit Thursday", "Visit Friday", "Visit Saturday",
                        "Unloading time [minutes]"
                };
                csvWriter.writeNext(CSV_COLUMNS, false);
            }
            for (Customer c : data.customers) {
                String[] results = {c.customerName, String.valueOf(c.customerID), String.valueOf(c.customerNumber),
                        String.valueOf(c.numberOfOrders), String.valueOf(c.numberOfDividableOrders), String.valueOf(c.numberOfNonDividableOrders),
                        String.valueOf(c.numberOfVisitPeriods), Converter.calculateTotalOrderVolume(c),
                        Converter.convertTimeWindow(c.timeWindow[0][0], c.timeWindow[0][1]),
                        Converter.convertTimeWindow(c.timeWindow[1][0], c.timeWindow[1][1]), Converter.convertTimeWindow(c.timeWindow[2][0], c.timeWindow[2][1]),
                        Converter.convertTimeWindow(c.timeWindow[3][0], c.timeWindow[3][1]), Converter.convertTimeWindow(c.timeWindow[4][0], c.timeWindow[4][1]),
                        Converter.convertTimeWindow(c.timeWindow[5][0], c.timeWindow[5][1]),
                        String.format("%.0f", c.totalUnloadingTime * 60)};
                csvWriter.writeNext(results, false);
            }
            csvWriter.close();
            writer.close();
        }
        catch () catch (IOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static String getFileName(String modelName){
        if (FileParameters.useDefaultFileName){
            SimpleDateFormat date_formatter = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
            String dateString = date_formatter.format(new Date());
            return modelName + "_S" + Parameters.randomSeedValue + "_C" + Parameters.numberOfCustomers +
                    "_V" + Parameters.numberOfVehicles + "_" + dateString;
        }
        else if (FileParameters.specifyFileNameWhenRunning){
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            System.out.println("Specify detailed filename: ");
            return myObj.nextLine();  // Read user input
        }
        return modelName;
    }
}
