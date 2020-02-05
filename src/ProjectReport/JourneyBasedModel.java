package ProjectReport;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;


public class JourneyBasedModel {

    public GRBEnv env;
    public GRBModel model;
    public Data data;
    public PathGenerator pg;
    public Result result;
    public String dataPath;
    public int optimstatus;
    public double objval;
    public String symmetry;

    // derived variables
    public int numVehiclesUsed = 0;
    public double volumeOvertime = 0;
    public int numTripsUsed = 0;
    public int numDivCommodity;
    public int numNondivCommodity;
    public int numTripVariables = 0;
    public int numArcsUsed = 0;
    public int numArcVariables = 0;
    public int numJourneysUsed = 0;
    public int numJourneyVariables = 0;
    public double preProcessTime;
    public int numGeneratedTrips = 0;
    public int numGeneratedJourneys = 0;

    //variables
    public GRBVar[][][] gamma;
    public GRBVar[] k;
    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[] qO;

    public ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed;


    public JourneyBasedModel(String filePath){
        this.dataPath = filePath;
    }


    public void initializeModel() throws GRBException, FileNotFoundException {
        env = new GRBEnv(true);
        //this.env.set("logFile",  "JourneyBasedModel_" + dataPath.substring(5, dataPath.length()-4) +".log");
        this.env.start();
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "JourneyBasedModel");
        this.data = DataReader.readFile(dataPath);
        this.pg = new PathGenerator(data);
        double time = System.currentTimeMillis();
        data.setPathMap(pg.generateAllPaths());
        JourneyGenerator jg  = new JourneyGenerator(data);
        data.setJourneyMap(jg.generateAllJourneys());
        this.preProcessTime = (System.currentTimeMillis() - time)/1000;
    }



    public void initializeParameters() throws GRBException {
        this.gamma = new GRBVar[data.numPeriods][data.numVehicles][];
        this.k = new GRBVar[data.numVehicles];
        this.u = new GRBVar[data.numPeriods][data.numCustomers][data.numProducts];
        this.q = new GRBVar[data.numPeriods][data.numVehicles][data.numTrips][data.numCustomers][data.numProducts];
        this.qO = new GRBVar[data.numPeriods];


        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                gamma[d][v] = new GRBVar[data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type).size()];
                for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    numJourneyVariables++;
                    String variable_name = String.format("gamma[%d][%d][%d]", d, v, r.journeyId);
                    gamma[d][v][r.journeyId] = model.addVar(0.0, 1.0, r.cost, GRB.BINARY, variable_name);
                }
            }
        }
        for (int v = 0; v < data.numVehicles; v++) {
            String variable_name = String.format("v[%d]", v);
            k[v] = model.addVar(0.0, 1.0, data.costVehicle[v], GRB.BINARY, variable_name);
        }


        for (int m = 0; m < data.numProducts; m++) {
            for (int i = 0; i < data.numCustomers; i++) {
                if (data.productQuantity[i][m] == 0){
                    continue;
                }
                for (int d = 0; d < data.numPeriods; d++){
                    String variable_name = String.format("u[%d][%d][%d]", d, i, m);
                    u[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }

        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                if (data.productQuantity[i][m] == 0){
                    continue;
                }
                for (int d = 0; d < data.numPeriods; d++) {
                    for (int v = 0; v < data.numVehicles; v++) {
                        for (int r  = 0; r < data.numTrips; r++){
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            q[d][v][r][i][m] = model.addVar(0.0, Data.upperBoundQuantity , 0, GRB.CONTINUOUS, variable_name);
                        }

                    }
                }
            }
        }
        for (int d = 0; d < data.numPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, Data.upperBoundOvertime, data.costOvertime, GRB.CONTINUOUS, variable_name);
        }
    }

    public void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // TODO: 20.11.2019 Change objective
    }

    public Result createAndStoreModelResults(boolean hasObjective, int isOptimal) throws IOException, GRBException {
        calculateResultValues();
        if (hasObjective){
            System.out.println("Success: "+optimstatus);
            result = new Result(model.get(GRB.DoubleAttr.Runtime), model.get(GRB.DoubleAttr.ObjVal), model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripVariables, numJourneyVariables,
                    volumeOvertime, model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs),
                    model.get(GRB.IntAttr.NumVars)-model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumQNZs),
                    model.get(GRB.IntAttr.SolCount), data.numVehicles, data.numCustomers, numDivCommodity, numNondivCommodity, data.numTrips, data.numPeriods, model.get(GRB.DoubleAttr.NodeCount),0,
                    preProcessTime, numGeneratedTrips, numGeneratedJourneys, pathsUsed,  symmetry);
        }
        else {
            result = new Result(model.get(GRB.DoubleAttr.Runtime), 1000000.00, model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripVariables, numJourneyVariables,
                    volumeOvertime, model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs),
                    model.get(GRB.IntAttr.NumVars)-model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumQNZs),
                    model.get(GRB.IntAttr.SolCount), data.numVehicles, data.numCustomers, numDivCommodity, numNondivCommodity, data.numTrips, data.numPeriods, model.get(GRB.DoubleAttr.NodeCount),0,
                    preProcessTime, numGeneratedTrips, numGeneratedJourneys, pathsUsed, symmetry);
        }
        result.store();
        System.out.println("Solution stored");
        return result;
    }

    public void terminateModel() throws GRBException {
        model.dispose();
        env.dispose();
    }


    public void constraint67() throws GRBException {
        // Constraint 5.67
        // Convexity of a journey
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(1.0, gamma[d][v][r.journeyId]);
                }
                String constraint_name = String.format("5.69 -At most one journey can be chosen for vehicle  %d period %d", v, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, 1, constraint_name);
            }
        }
    }

    public void constraint68() throws GRBException {
        // Constraint 5.68
        // if a journey is used, then the vehicle corresponding to that journey is considered used
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (Journey r :  data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(1, gamma[d][v][r.journeyId]);
                }
                lhs.addTerm(-1, k[v]);
                String constraint_name = String.format("5.70 -Use of vehicle %d, period %d", v, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
            }
        }
    }

    public void constraint69() throws GRBException {
        // Constraint 5.69
        // Allowable visits to customer on spesific day
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int v = 0; v < data.numVehicles; v++) {
                    for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                        for (Customer c : r.customers) {
                            if (c.customerID == i) {
                                lhs.addTerm(1, gamma[d][v][r.journeyId]);
                            }
                        }
                    }
                }
                String constraint_name = String.format("5.71 -Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, data.possibleDeliveryDays[d][i]);
                model.addConstr(lhs, GRB.EQUAL, data.possibleDeliveryDays[d][i], constraint_name);
            }
        }


    }

    public void constraint70() throws GRBException {
        // Constraint 5.70
        //Capacity constraint on each delivery
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                for (int i = 0; i < data.numCustomers; i++) {
                    for (int r = 0; r < data.numTrips; r++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int m = 0; m < data.numProducts; m++) {
                            if (data.productQuantity[i][m] > 0) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        for (Journey j : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                            if (r < j.paths.length) {
                                Path p = j.paths[r];
                                for (Customer c : p.customers) {
                                    if (c.customerID == i) {
                                        lhs.addTerm(-data.vehicleCapacity[v], gamma[d][v][j.journeyId]);
                                    }
                                }
                            }
                        }
                        String constraint_name = String.format("5.72 -Connection gamma and q for customer %d vehicle %d day %d. M = %f", i, v, d, data.vehicleCapacity[v]);
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint71() throws GRBException {
        // Constraint 5.73
        // Capacity for each vehicle
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                for (int r = 0; r < data.numTrips; r++){
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int i = 0; i < data.numCustomers; i++) {
                        for (int m = 0; m < data.numProducts; m++) {
                            if (data.productQuantity[i][m] > 0) {
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                            }
                        }
                    }
                    String constraint_name = String.format("5.73 -Capacity vehicle %d trip %d period %d. Capacity %f", v, r, d, data.vehicleCapacity[v]);
                    model.addConstr(lhs, GRB.LESS_EQUAL, data.vehicleCapacity[v], constraint_name);
                }
            }
        }
    }

    public void constraint72() throws GRBException {
        // Constraint 5.74:
        // Overitme at the depot
        for (int d = 0; d < data.numPeriods; d++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            for (int v = 0; v < data.numVehicles; v++) {
                for (int r = 0; r < data.numTrips; r++) {
                    for (int i = 0; i < data.numCustomers; i++) {
                        for (int m = 0; m < data.numProducts; m++) {
                            if (data.productQuantity[i][m] > 0)
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                        }
                    }
                }
            }
            lhs.addTerm(-1.0, qO[d]); // Add the over time variable for that day
            // Create name
            String constraint_name = String.format("5.74 -Overtime on day %d. OvertimeLimit %f ", d, data.overtimeLimit[d]);
            model.addConstr(lhs, GRB.LESS_EQUAL, data.overtimeLimitAveraged, constraint_name);
        }
    }

    public void constraint73() throws GRBException {
        // Constraint 5.75:
        // If one choose to deliver a non-div good, than a certain Q must be delivered
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                for (int m = 0; m < data.numProducts; m++) {
                    if (data.productTypes[i][m] == 0 && data.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(data.productQuantity[i][m], u[d][i][m]);
                        for (int v = 0; v < data.numVehicles; v++) {
                            for (int r = 0; r < data.numTrips; r++) {
                                lhs.addTerm(-1, q[d][v][r][i][m]);
                            }
                        }
                        String constraint_name = String.format("5.75 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, data.productQuantity[i][m], data.numProducts);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint74() throws GRBException {
        // Constraint 5.76 a):
        // Lower bound for delivery for non-div product. (same as before)
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                for (int m = 0; m < data.numProducts; m++) {
                    if (data.productTypes[i][m] == 1 && data.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < data.numVehicles; v++) {
                            for (int r = 0; r < data.numTrips; r++) {
                                lhs.addTerm(-1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(data.minAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 a) -Min delivery of dividable product %d customer %d on day %d. Min amount: %f", m, i, d, data.minAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
        // Constraint 5.76 b):
        // Upper bound for delivery for non-div product. (same as before)
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                for (int m = 0; m < data.numProducts; m++) {
                    if (data.productTypes[i][m] == 1 && data.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < data.numVehicles; v++) {
                            for (int r = 0; r < data.numTrips; r++) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(-data.maxAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, data.maxAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint75() throws GRBException {
        // Constraint 5.77
        // Demand of every product must be satisfied in the planning horizon (same as before)
        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                if (data.productQuantity[i][m] > 0) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int d = 0; d < data.numPeriods; d++) {
                        for (int v = 0; v < data.numVehicles; v++) {
                            for (int r = 0; r < data.numTrips; r++) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                    }
                    String constraint_name = String.format("5.77 -Delivery of product %d to customer %d. Quantity %f", m, i, data.productQuantity[i][m]);
                    // Activate the constraint
                    model.addConstr(lhs, GRB.EQUAL, data.productQuantity[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint76() throws GRBException {
        // Constraint 5.78: Only one non-div product is delivered to the store on day d.
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < data.numProducts; m++) {
                    if (data.productTypes[i][m] == 0 && data.productQuantity[i][m] > 0) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                }
                // Activate the constraint
                String constraint_name = String.format("5.78 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.EQUAL, data.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }
    public void constraint77() throws GRBException {
        // Constraint 5.79
        // Non-dividable good has to be delivered during t
        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.productTypes[i][m] == 0 && data.productQuantity[i][m] > 0) {
                    for (int d = 0; d < data.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.79 -Nondiv good %d must be delivered exactly once to customer %d", m, i);
                    model.addConstr(lhs, GRB.EQUAL, 1, constraint_name);
                }
            }
        }
    }

    public void constraint78() throws GRBException {
        // Constraint 5.80
        // Dividable good has to be delivered at least above the minimum frequenzy
        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.productTypes[i][m] == 1 && data.productQuantity[i][m] > 0) {
                    for (int d = 0; d < data.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.80 -Div good %d must be delivered at least %d to customer %d", m, data.minFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, data.minFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint79() throws GRBException {
        // Constraint 5.81
        // Dividable good has to be delivered at most the maximum number of times
        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.productTypes[i][m] == 1 && data.productQuantity[i][m] > 0) {
                    for (int d = 0; d < data.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.81 -Div good %d must be delivered at most %d to customer %d", m, data.maxFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.LESS_EQUAL, data.maxFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    public void symmetryCar() throws GRBException {
        for (int v = 0; v < data.numVehicles - 1; v++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            if (data.vehicles[v].vehicleType.type != data.vehicles[v + 1].vehicleType.type)
                continue;
            lhs.addTerm(1, k[v]);
            lhs.addTerm(-1, k[v + 1]);
            String constraint_name = String.format("5.65 Sym3- Vehicle %d must be used before vehicle %d over vehicle type %d", v, v + 1, data.vehicles[v + 1].vehicleType.type);
            model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
        }
    }

    public void symmetryCost() throws GRBException {
        System.out.println("------------------- Symmetri: Cost--------------------");
        // Constraint 5.64 //// CANNOT BE USED WITH 5.66 amd 5.63
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.type != data.vehicles[v + 1].vehicleType.type) {
                    continue;
                }
                for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type) ) {
                    lhs.addTerm(r.cost, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.cost, gamma[d][v + 1][r.journeyId]);
                String constraint_name = String.format("5.87 Sym5 - Length of jouney for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, data.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void symmetryCustomers() throws GRBException {
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        System.out.println("------------------- Symmetri: customers --------------------");
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.type != data.vehicles[v + 1].vehicleType.type)
                    continue;
                for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(r.numCustomers, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.numCustomers, gamma[d][v + 1][r.journeyId]);
                }
                String constraint_name = String.format("5.88 Sym4 - Number of customer visits for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, data.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryTrips() throws GRBException {
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        System.out.println("------------------- Symmetri: trip --------------------");
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.type != data.vehicles[v + 1].vehicleType.type)
                    continue;
                for (Journey r : data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(r.numTrips, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.numTrips, gamma[d][v + 1][r.journeyId]);
                }
                String constraint_name = String.format("5.88 Sym4 - Number of trips used for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, data.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }


    public void activateConstraints(String symmetry) throws GRBException {
        // -------- Add constraints -------------

        constraint68();
        constraint69();
        constraint70();
        constraint71();
        constraint72();

        // ------- Constraints form AFM ---------
        constraint73();
        constraint74();
        constraint75();
        constraint76();
        constraint77();
        constraint78();
        constraint79();

        // ----------------- Symmetry breaking constraints ------------

        // Four choices: none, car, cost, customers, trips
        if (!symmetry.equals("none")){
            symmetryCar();
            if (symmetry.equals("cost")) {
                symmetryCost();
            }
            else if (symmetry.equals("customers")){
                symmetryCustomers();
            }
            else if (symmetry.equals("trips")) {
                symmetryTrips();
            }
            else {
                System.out.println("------------------- Symmetri: " +symmetry + " (not standard) --------------------");
            }
        }
        else {
            System.out.println("No symmetri breaking constriant chosen");;
        }



    }

    public void optimizeModel(int timeLimit) throws GRBException {
        //model.set(GRB.DoubleParam.MIPGap, 0);
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);
        model.optimize();
        model.get(GRB.DoubleAttr.Runtime);
        System.out.println(GRB.Status.OPTIMAL);
        System.out.println(GRB.DoubleAttr.Runtime);
        this.optimstatus = model.get(GRB.IntAttr.Status);
    }

    public void displayResults(boolean ISS) throws GRBException {
        if (optimstatus == GRB.Status.OPTIMAL) {
            objval = model.get(GRB.DoubleAttr.ObjVal);
            System.out.println("Optimal objective: " + objval);
        }
        else if (optimstatus == GRB.Status.INF_OR_UNBD) {
            System.out.println("Model is infeasible or unbounded");
        }
        else if (optimstatus == GRB.Status.INFEASIBLE) {
            // Compute IIS
            System.out.println("The model is infeasible");
            if (ISS == true){
                System.out.println("computing IIS");
                model.computeIIS();
                model.write("model.ilp");
                System.out.println("\nThe following constraint(s) "
                        + "cannot be satisfied:");
                for (GRBConstr c : model.getConstrs()) {
                    if (c.get(GRB.IntAttr.IISConstr) == 1) {
                        System.out.println(c.get(GRB.StringAttr.ConstrName));
                    }
                }
            }
        }
        else if (optimstatus == GRB.Status.UNBOUNDED) {
            System.out.println("Model is unbounded");
        }
        else {
            System.out.println("Optimization was stopped with status = "
                    + optimstatus);
        }
    }

    public void printSolution() throws GRBException {
        // Print lambda variables: choosing a path //
        System.out.println("Print of gamma-variables: If a vehicle  uses a path");
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                for (Journey r:  data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    if (gamma[d][v][r.journeyId].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Vehicle " + v + " on period " + d + " uses journey " + r.journeyId);
                        System.out.println("And visits customers: ");
                        for (Customer c : r.customers){
                            System.out.print(" - " + c.customerID);
                        }
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        // Create k variables: vehicle v is used in the planning period
        System.out.println("Print of k-variables: If a vehicle is used in the planing period");
        for (int v = 0; v < data.numVehicles; v++) {
            if (k[v].get(GRB.DoubleAttr.X) == 1) {

                System.out.println("Vehicle " + v + " is used in the planning period with capacity: "  +data.vehicleCapacity[v]);
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        // Create u variables: if a product is delivered to customer m
        System.out.println("Print of u-variables: If a product m is delivered to customer i");
        for (int d = 0; d < data.numPeriods; d++) {
            for (int i = 0; i < data.numCustomers; i++) {
                for (int m = 0; m < data.numProducts; m++) {
                    if (data.productQuantity[i][m] == 0 )
                        continue;
                    if (u[d][i][m].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Product " + m + " in customer " + i + " is delivered on day " + d);
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        //Create q variables: Quantity of m delivered to store i
        System.out.println("Print of q-variables: Quantity of m delivered to store i");
        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicles; v++) {
                for (int r = 0; r < data.numTrips; r++) {
                    for (int i = 0; i < data.numCustomers; i++) {
                        for (int m = 0; m < data.numProducts; m++) {
                            if (data.productQuantity[i][m] == 0)
                                continue;
                            if (q[d][v][r][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                                System.out.println("Quantity " + (double) Math.round(q[d][v][r][i][m].
                                        get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " + m +
                                        " is delivered to " + "customer " + i + " with vehicle " + v +
                                        " on period " + d);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        //Create qO (overtime) variables
        System.out.println("Print of qO-variables: Overtime at the depot");
        for (int d = 0; d < data.numPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is "
                        + (double)Math.round(qO[d].get(GRB.DoubleAttr.X) * 1000d) / 1000d );
            }
        }

        System.out.println("  ");
        System.out.println("  ");

    }
    public void storePath() throws GRBException {
        pathsUsed = new ArrayList<>();
        for (int d = 0; d < data.numPeriods; d++) {
            ArrayList<ArrayList<ArrayList<Integer>>> arrayDays = new ArrayList<>();
            for (int v = 0; v < data.numVehicles; v++) {
                ArrayList<ArrayList<Integer>> arrayVehicles = new ArrayList<>();
                for (Journey r: data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    if (gamma[d][v][r.journeyId].get(GRB.DoubleAttr.X) == 1) {
                        for (Path p : r.paths){
                            ArrayList<Integer> arrayPaths= new ArrayList<>();
                            for (Customer c : r.customers) {
                                arrayPaths.add(c.customerID);
                            }
                            arrayVehicles.add(arrayPaths);
                        }
                    }
                }
                arrayDays.add(arrayVehicles);
            }
            pathsUsed.add(arrayDays);
        }
    }

    public void calculateResultValues() throws GRBException {
        if (optimstatus == 2){
            for (int v = 0; v < data.numVehicles; v++) {
                if (k[v].get(GRB.DoubleAttr.X) == 1){
                    numVehiclesUsed++;
                }
            }
            for (int d = 0; d < data.numPeriods; d++) {
                for (int v = 0; v < data.numVehicles; v++) {
                    for (Journey r: data.journeyMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                        if (gamma[d][v][r.journeyId].get(GRB.DoubleAttr.X) == 1) {
                                this.numJourneysUsed++;
                        }
                    }
                }
            }

            for (int d = 0; d < data.numPeriods; d++) {
                if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                    volumeOvertime += qO[d].get(GRB.DoubleAttr.X);
                }
            }
        }

        for (int d = 0; d < data.numPeriods; d++) {
            for (int v = 0; v < data.numVehicleTypes; v++) {
                for (Journey r : data.journeyMap.get(d).get(v)) {
                    numGeneratedJourneys++;
                }
            }
        }

        for (int i = 0; i < data.numCustomers; i++) {
            for (int m = 0; m < data.numProducts; m++) {
                if (data.productQuantity[i][m] >= 0.001) {
                    continue;
                }
                if (data.productTypes[i][m] == 1) {
                    numDivCommodity++;
                } else if (data.productTypes[i][m] == 0) {
                    numNondivCommodity++;
                }
            }
        }
    }

    public Result runModel(String symmetry) {
        try {
            this.symmetry = symmetry;
            System.out.println("Initalize model");
            initializeModel();
            System.out.println("Initalize parameters");
            initializeParameters();
            System.out.println("Set objective");
            setObjective();
            System.out.println("Activate constraints");
            activateConstraints(symmetry);
            System.out.println("Optimize model");
            optimizeModel(Parameters.timeOut);
            System.out.println("Print results:");
            displayResults(false);
            if (optimstatus == 3) {
                System.out.println("no solution found");
                Result res = createAndStoreModelResults( false,  0);
                System.out.println("Terminate model");
                terminateModel();
                return res;
            }
            else if (optimstatus == 2){
                System.out.println("Create and store results");
                storePath();
                Result res = createAndStoreModelResults(true,  1);
                printSolution();
                System.out.println("Terminate model");
                terminateModel();
                return res;
            }
            else{
                System.out.println("Create and store results");
                storePath();
                Result res = createAndStoreModelResults(true, 0);
                printSolution();
                System.out.println("Terminate model");
                terminateModel();
                return res;
            }

        } catch (GRBException | FileNotFoundException e) {
            System.out.println("ERROR: " + e);
            return null;
        } catch (Error e) {
            System.out.println(e);
            return null;
        } catch (IOException e) {
            System.out.println("File directory wrong" + e);
            return null;
        }
    }
}