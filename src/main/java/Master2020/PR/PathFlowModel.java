package Master2020.PR;
import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.MIP.DataConverter;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class PathFlowModel {

    public String modelName = "PathFlowModel";

    public GRBEnv env;
    public GRBModel model;
    public DataMIP dataMIP;
    public PathGenerator pg;
    public Result result;
    public String dataPath;
    public int optimstatus;
    public double objval;
    public double preProcessTime;
    public String symmetry;
    private Individual individual;
    private OrderDistribution orderDistribution;
    public boolean infeasible;


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
    public int numGeneratedTrips = 0;
    public int numGeneratedJourneys = 0;

    //variables
    public GRBVar[][][][] lambda;
    public GRBVar[][] k;
    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[][][] tS;
    public GRBVar[] qO;
    public ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed = null ;

    public PathFlowModel(DataMIP dataMIP){
        this.dataMIP = dataMIP;
    }

    public void initializeModel() throws GRBException, FileNotFoundException {
        env = new GRBEnv(true);
        this.env.set("logFile",  "PathFlowModel.log");
        this.env.start();
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "PathFlowModel");
        this.pg = new PathGenerator(dataMIP);
        double time = System.currentTimeMillis();
        dataMIP.setPathMap(pg.generateAllPaths());
        this.preProcessTime = (System.currentTimeMillis() - time)/1000;
    }

    public void initializeParameters() throws FileNotFoundException, GRBException {
        this.lambda = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips][];
        this.k = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles];
        this.u = new GRBVar[dataMIP.numPeriods][dataMIP.numCustomers][];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int c = 0; c < dataMIP.numCustomers; c++){
                this.u[p][c] = new GRBVar[dataMIP.numProductsPrCustomer[c]];
            }
        }
        this.q = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips][dataMIP.numCustomers][];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int v = 0; v < dataMIP.numVehicles; v++){
                for (int r = 0; r < dataMIP.numTrips; r++){
                    for (int c = 0; c < dataMIP.numCustomers; c++){
                        this.q[p][v][r][c] = new GRBVar[dataMIP.numProductsPrCustomer[c]];
                    }
                }
            }
        }
        this.tS = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips];
        this.qO = new GRBVar[dataMIP.numPeriods];

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    lambda[d][v][r] = new GRBVar[dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type).size()]; //creating a jagged array
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        numTripVariables++;
                        String variable_name = String.format("lambda[%d][%d][%d][%d]", d, v, r, p.pathId);
                        lambda[d][v][r][p.pathId] = model.addVar(0.0, 1.0, p.cost, GRB.BINARY, variable_name);
                    }
                }
            }
        }
        dataMIP.paths = this.lambda;
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                String variable_name = String.format("p[%d]v[%d]",p, v);
                k[p][v] = model.addVar(0.0, 1.0, dataMIP.costVehicle[v], GRB.BINARY, variable_name);
            }
        }


        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                for (int d = 0; d < dataMIP.numPeriods; d++){
                    String variable_name = String.format("u[%d][%d][%d]", d, i, m);
                    u[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }

        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                for (int d = 0; d < dataMIP.numPeriods; d++) {
                    for (int v = 0; v < dataMIP.numVehicles; v++) {
                        for (int r = 0; r < dataMIP.numTrips; r++) {
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            q[d][v][r][i][m] = model.addVar(0.0, DataMIP.upperBoundQuantity , 0, GRB.CONTINUOUS, variable_name);
                        }
                    }
                }
            }
        }

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    String variable_name = String.format("tS[%d][%d][%d]", d, v, r);
                    tS[d][v][r] = model.addVar(0, dataMIP.latestInTime, 0, GRB.CONTINUOUS, variable_name);
                }
            }
        }

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, DataMIP.upperBoundOvertime, dataMIP.costOvertime[d], GRB.CONTINUOUS, variable_name);
        }
    }

    public void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
    }

    public Result createAndStoreModelResults(boolean hasObjective, int isOptimal) throws IOException, GRBException {
        calculateResultValues();
        if (hasObjective){
            System.out.println("Success: "+optimstatus);
            result = new Result(model.get(GRB.DoubleAttr.Runtime), model.get(GRB.DoubleAttr.ObjVal), model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripVariables, numJourneyVariables,
                    volumeOvertime, model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs),
                    model.get(GRB.IntAttr.NumVars)-model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumQNZs),
                    model.get(GRB.IntAttr.SolCount), dataMIP.numVehicles, dataMIP.numCustomers, numDivCommodity, numNondivCommodity, dataMIP.numTrips, dataMIP.numPeriods, model.get(GRB.DoubleAttr.NodeCount),0,
                    preProcessTime, numGeneratedTrips, numGeneratedJourneys, pathsUsed, symmetry);
        }
        else {
            result = new Result(model.get(GRB.DoubleAttr.Runtime), 1000000.00, model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripVariables, numJourneyVariables,
                    volumeOvertime, model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs),
                    model.get(GRB.IntAttr.NumVars)-model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumQNZs),
                    model.get(GRB.IntAttr.SolCount), dataMIP.numVehicles, dataMIP.numCustomers, numDivCommodity, numNondivCommodity, dataMIP.numTrips, dataMIP.numPeriods, model.get(GRB.DoubleAttr.NodeCount),0,
                    preProcessTime, numGeneratedTrips, numGeneratedJourneys, pathsUsed, symmetry);
        }
        //result.store();
        System.out.println("Solution stored");
        return result;
    }

    public void terminateModel() throws GRBException {
        model.dispose();
        env.dispose();
    }

    public void constraint39() throws GRBException {
        // Contraint 5.40 a) Lower bound
        // Earliest starting time for a vehicle and trip
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips ; r++) {  // num trips cannot be
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        lhs.addTerm(p.earliestStartTime, lambda[d][v][r][p.pathId]);
                    }
                    lhs.addTerm(-1.0, tS[d][v][r]);
                    String constraint_name = String.format("5.40 a) - Earliest starting time for vehicle %d trip %d period %d",  v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }

        // Contraint 5.40 b) Upper bound
        // Upper bound on the volume delivered by a vehicle. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips ; r++) {  // num trips cannot be
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        lhs.addTerm(p.latestStartTime, lambda[d][v][r][p.pathId]);
                    }
                    lhs.addTerm(-1.0, tS[d][v][r]);
                    String constraint_name = String.format("5.40 b) - Latest starting time for vehicle %d trip %d period %d",  v, r, d);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint40() throws GRBException {
        // Constraint 5.41
        // Upper bound on the volume delivered by a vehicle. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips - 1; r++) {  // num trips cannot be
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1.0, tS[d][v][r]);
                    lhs.addTerm(-1.0, tS[d][v][(r + 1)]);
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        lhs.addTerm(p.duration, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(dataMIP.latestInTime + dataMIP.loadingTime[dataMIP.vehicles[v].vehicleType.type], lambda[d][v][r+1][p.pathId]);
                    }
                    String constraint_name = String.format("5.41 - Route cannot start before next route for period %d vehicle %d trip %d to trip %d", d, v, r, r + 1);
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.latestInTime, constraint_name);
                }
            }
        }
    }

    public void constraint41() throws GRBException {
        // Constraint 5.42
        // if a route is used, then the vehicle corresponding to that path is considered used.
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        lhs.addTerm(1, lambda[d][v][r][p.pathId]);
                    }
                    lhs.addTerm(-1, k[d][v]);
                    String constraint_name = String.format("5.42 -Use of vehicle %d, trip %d, period %d", v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint42() throws GRBException {
        // Constraint 5.43
        // Allowable visits to customer on spesific day
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {

                if (dataMIP.possibleDeliveryDays[d][i] == 0)  // TODO: Check if this is correct implemented
                    continue;
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    for (int r = 0; r < dataMIP.numTrips; r++) {
                        for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                            for (Customer c : p.customers) {
                                if (c.customerID == i) { // TODO: 19.11.2019 can be done quicker?
                                    lhs.addTerm(1, lambda[d][v][r][p.pathId]);
                                }
                            }
                        }
                    }
                }
                String constraint_name = String.format("5.43 -Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, dataMIP.possibleDeliveryDays[d][i]);
                model.addConstr(lhs, GRB.EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }

    public void constraint43() throws GRBException {
        // Constraint 5.44
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips-1; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        lhs.addTerm(1, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(-1, lambda[d][v][r+1][p.pathId]);
                    }
                    String constraint_name = String.format("5.45 - Cannot use trip %d before trip %d for vehicle %d, period %d", r+1,r, v, d);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint44() throws GRBException {
        // Constraint 5.45
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0){
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                            for (Customer c : p.customers) {
                                if (c.customerID == i) { // TODO: 19.11.2019 can be done quicker?
                                    lhs.addTerm(-dataMIP.vehicleCapacity[v], lambda[d][v][r][p.pathId]);
                                }
                            }
                        }
                        String constraint_name = String.format("5.45 -Connection q and lambda for customer %d vehicle %d trip %d day %d. M = %f", i, v, r, d, dataMIP.vehicleCapacity[v]);
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint45() throws GRBException {
        // ------- Constraints form AFM ---------
        // Constraint 5.46
        // Capacity for each vehicle
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0) {
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                            }
                        }
                    }
                    String constraint_name = String.format("5.46 -Capacity vehicle %d trip %d period %d. Capacity %f", v, r, d, dataMIP.vehicleCapacity[v] );
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.vehicleCapacity[v], constraint_name);
                }
            }
        }
    }

    public void constraint46() throws GRBException {
        // Constraint 5.47:
        // Overitme at the depot
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0)
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                        }
                    }
                }
            }
            lhs.addTerm(-1.0, qO[d]); // Add the over time variable for that day
            // Create name
            String constraint_name = String.format("5.43 -Overtime on day %d. OvertimeLimit %f ", d, dataMIP.overtimeLimit[d]);
            // Create constraint and defind RHS
            model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.overtimeLimit[d], constraint_name);
        }
    }

    public void constraint47() throws GRBException {
        // Constraint 5.48:
        // If one choose to deliver a non-div good, than a certain Q must be delivered
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(dataMIP.productQuantity[i][m], u[d][i][m]);
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(-1, q[d][v][r][i][m]);
                            }
                        }
                        String constraint_name = String.format("5.44 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, dataMIP.productQuantity[i][m], dataMIP.numProductsPrCustomer[i]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint48() throws GRBException {
        // Constraint 5.49 a):
        // Lower bound for delivery for non-div product. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 1  && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(-1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(dataMIP.minAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.45 a) -Min delivery of dividable product %d customer %d on day %d. Min amount: %f", m, i, d, dataMIP.minAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }

        // Constraint 5.49 b):
        // Upper bound for delivery for non-div product. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 1  && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(-dataMIP.maxAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.45 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, dataMIP.maxAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint49() throws GRBException {
        // Constraint 5.50
        // Demand of every product must be satisfied in the planning horizon (same as before)
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                if (dataMIP.productQuantity[i][m] > 0) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                    }
                    String constraint_name = String.format("5.46 -Delivery of product %d to customer %d. Quantity %f", m, i, dataMIP.productQuantity[i][m] );
                    // Activate the constraint
                    model.addConstr(lhs, GRB.EQUAL, dataMIP.productQuantity[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint50() throws GRBException {
        // Constraint 5.51: Only one non-div product is delivered to the store.
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                }
                // Activate the constraint
                String constraint_name = String.format("5.51 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }

    public void constraint51() throws GRBException {
        // Constraint 5.52
        // Non-dividable good has to be delivered during t
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.21 -Nondiv good %d must be delivered exactly once to customer %d", m, i);
                    model.addConstr(lhs, GRB.EQUAL, 1, constraint_name);
                }
            }
        }
    }

    public void constraint52() throws GRBException {
        // Constraint 5.53
        // Dividable good has to be delivered at least above the minimum frequenzy
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 1  && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.22 -Div good %d must be delivered at least %d to customer %d", m, dataMIP.minFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, dataMIP.minFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint53() throws GRBException {
        // Constraint 5.54
        // Dividable good has to be delivered at most the maximum number of times
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 1  && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.23 -Div good %d must be delivered at most %d to customer %d", m, dataMIP.maxFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.maxFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    public void symmetryTrip() throws GRBException {
        // Constraint 5.63  //CANNOT BE USED WITH 5.66 and 5.67
        // Vehicle trip number decreasing
        System.out.println("-----------------------Using symmetry : trip -------------------------");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles-1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v+1].vehicleType.type)
                    continue;
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)){
                        lhs.addTerm(1, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(-1, lambda[d][v+1][r][p.pathId]);
                    }
                }
                String constraint_name = String.format("5.63 Sym1 - Number of trips used for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryCost() throws GRBException {
        System.out.println("-----------------------Using symmetry : Cost  -------------------------");
        // Constraint 5.64 //// CANNOT BE USED WITH 5.66 amd 5.63
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles-1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v+1].vehicleType.type)
                    continue;
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)){
                        lhs.addTerm(p.cost, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(p.cost, lambda[d][v+1][r][p.pathId]);
                    }
                }
                String constraint_name = String.format("5.67 Sym5 - Length of jouney for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryCustomers() throws GRBException {
        System.out.println("-----------------------Using symmetry : Customers -------------------------");
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles-1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v+1].vehicleType.type)
                    continue;
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)){
                        lhs.addTerm(p.customers.length, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(p.customers.length, lambda[d][v+1][r][p.pathId]);
                    }
                }
                String constraint_name = String.format("5.66 Sym4 - Number of customer visits for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryDuration() throws GRBException {
        System.out.println("-----------------------Using symmetry : duration  -------------------------");
        // Constraint 5.67 //// CANNOT BE USED WITH 5.66 amd 5.63
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles-1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v+1].vehicleType.type)
                    continue;
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)){
                        lhs.addTerm(p.duration, lambda[d][v][r][p.pathId]);
                        lhs.addTerm(p.duration, lambda[d][v+1][r][p.pathId]);
                    }
                }
                String constraint_name = String.format("5.67 Sym5 - Length of jouney for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void activateConstraints(String symmetry) throws GRBException {

        // -------- Add constraints -------------
        constraint39();
        constraint40();
        constraint41();
        constraint42();
        constraint43();
        constraint44();
        constraint45();
        constraint46();
        constraint47();
        constraint48();
        constraint49();
        constraint50();
        constraint51();
        constraint52();
        constraint53();

        // ----------------- Symmetry breaking constraints ------------
        if (symmetry.equals("none")){
            System.out.println("----------------------------No symmetry chosen----------------------------------------");
        }
        else {
            //default symmetry
            //symmetryCar();
            if (symmetry.equals("trips")) {
                symmetryTrip();
            } else if (symmetry.equals("cost")) {
                symmetryCost();
            } else if (symmetry.equals("customers")) {
                symmetryCustomers();
            } else if (symmetry.equals("duration")) {
                symmetryDuration();
            } else {
                System.out.println("-----------------------Using symmetry : " + symmetry + " (not standard) -------------------------");
                System.out.println("Only simple symmetry breaking chosen");
            }
        }
    }

    public void optimizeModel() throws GRBException {
        model.set(GRB.DoubleParam.MIPGap, Parameters.modelMipGap);
        model.set(GRB.DoubleParam.TimeLimit,Parameters.modelTimeLimit);
        model.optimize();
        model.get(GRB.DoubleAttr.Runtime);
        System.out.println(GRB.Status.OPTIMAL);
        System.out.println(GRB.DoubleAttr.Runtime);
        this.optimstatus = model.get(GRB.IntAttr.Status);
    }

    public void displayResults( boolean ISS ) throws GRBException {
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
        System.out.println("Print of lambda-variables: If a vehicle  uses a path");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                        if (Math.round(lambda[d][v][r][p.pathId].get(GRB.DoubleAttr.X)) == 1) {
                            System.out.println("Vehicle " + v + " on period " + d + " trip " + r + " uses path " + p.pathId);
                            System.out.println("Start time for trip is " + tS[d][v][r].get(GRB.DoubleAttr.X) + "and end time " + (tS[d][v][r].get(GRB.DoubleAttr.X) + p.duration));
                        }
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        // Create k variables: vehicle v is used in the planning period
        System.out.println("Print of k-variables: If a vehicle is used in the planing period");
        for( int p = 0 ; p < dataMIP.numPeriods; p++){
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                if (Math.round(k[p][v].get(GRB.DoubleAttr.X)) == 1) {
                    System.out.println("Vehicle " + v + " is used in the planning period with capacity: "  + dataMIP.vehicleCapacity[v]);
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Create u variables: if a product is delivered to customer m
        System.out.println("Print of u-variables: If a product m is delivered to customer i");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productQuantity[i][m] == 0 )
                        continue;
                    if (Math.round(u[d][i][m].get(GRB.DoubleAttr.X)) == 1) {
                        System.out.println("Product " + m + " in customer " + i + " is delivered on day " + d);
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        //Create q variables: Quantity of m delivered to store i
        System.out.println("Print of q-variables: Quantity of m delivered to store i");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            double quantityday = 0;
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    double quantitiyTrip = 0;
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        double quantitiyCust = 0 ;
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] == 0)
                                continue;
                            if (q[d][v][r][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                                System.out.println("Quantity " + (double) Math.round(q[d][v][r][i][m].
                                        get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " + m +
                                        " is delivered to " + "customer " + i + " with vehicle " + v +
                                        " trip " + r + " on day " + d);
                                quantityday += q[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyTrip += q[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyCust += q[d][v][r][i][m].get(GRB.DoubleAttr.X);
                            }
                        }
                        if (quantitiyCust >= 0.0001 ){
                            System.out.println("quantity for customer "+ i + " is :" + quantitiyCust);
                        }
                    }
                    if (quantitiyTrip >= 0.0001) {
                        System.out.println("quantity for vehicle " + v + "for trip " + r + "is equal: " + quantitiyTrip);
                    }

                }


            }
            System.out.println("Quantity on day " + d + " is equal to:" + quantityday);
        }
        System.out.println("   ");
        System.out.println("   ");

        //Create t variables: Time of visit, customer i
        System.out.println("Print of t-variables: Visiting time for customer i"); // TODO: 24.11.2019 Print only times that are non-zero
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    double time = 0;
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)){
                        if (Math.round(lambda[d][v][r][p.pathId].get(GRB.DoubleAttr.X)) == 1)
                            System.out.println("Departure time veh:" + v+ ", per: " + d + ", tri: " + r+  " path: " + p.pathId +  " at time: " + tS[d][v][r].get(GRB.DoubleAttr.X) + " with duration " + p.duration);
                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");

        //Create qO (overtime) variables
        System.out.println("Print of qO-variables: Overtime at the depot");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
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
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            ArrayList<ArrayList<ArrayList<Integer>>> arrayDays = new ArrayList<>();
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                ArrayList<ArrayList<Integer>> arrayVehicles = new ArrayList<>();
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {  // TODO: 23.11.2019 Problem when less than 4 types
                        if (Math.round(lambda[d][v][r][p.pathId].get(GRB.DoubleAttr.X)) == 1) {
                            ArrayList<Integer> arrayPaths= new ArrayList<>();
                            for (Customer c : p.customers) {
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
            for (int p = 0; p < dataMIP.numPeriods; p++){
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    if (Math.round(k[p][v].get(GRB.DoubleAttr.X)) == 1){
                        numVehiclesUsed++;
                    }
                }
            }

            for (int d = 0; d < dataMIP.numPeriods; d++) {
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    for (int r = 0; r < dataMIP.numTrips; r++) {
                        for (Path p : dataMIP.pathMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                            if (Math.round(lambda[d][v][r][p.pathId].get(GRB.DoubleAttr.X)) == 1) {
                                this.numTripsUsed++;
                            }
                        }
                    }
                }
            }
            for (int d = 0; d < dataMIP.numPeriods; d++) {
                if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                    volumeOvertime += qO[d].get(GRB.DoubleAttr.X);
                }
            }
        }
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicleTypes; v++) {
                for (Path p : dataMIP.pathMap.get(d).get(v)) {
                    numGeneratedTrips++;
                }
            }
        }
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                if (dataMIP.productQuantity[i][m] >= 0.001) {
                    continue;
                }
                if (dataMIP.productTypes[i][m] == 1) {
                    numDivCommodity++;
                } else if (dataMIP.productTypes[i][m] == 0) {
                    numNondivCommodity++;
                }
            }
        }
    }

    public void runModel(String symmetry) {
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
            optimizeModel();
            System.out.println("Print results:");
            displayResults(true);
            if (optimstatus == 3) {
                System.out.println("no solution found");
                infeasible = true;
                System.out.println("Terminate model");
                terminateModel();
            }
            else if (optimstatus == 2){
                if (Parameters.plotArcFlow){
                    GraphPlot plotter = new GraphPlot(dataMIP);
                    plotter.visualize(false);
                }
                System.out.println("Create and store results");
                storePath();
                createIndividualAndOrderDistributionObject();
                infeasible = false;
                if (Parameters.verbosePathFlow)
                    printSolution();
                System.out.println("Terminate model");
                terminateModel();
            }
            else{
                System.out.println("Create and store results");
                storePath();
                infeasible = true;
                createIndividualAndOrderDistributionObject();
                if (Parameters.verbosePathFlow)
                    printSolution();
                System.out.println("Terminate model");
                terminateModel();
            }
        } catch (GRBException | FileNotFoundException e) {
            System.out.println("ERROR: " + e);
        } catch (Error e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println("File directory wrong" + e);
        }


    }


    public OrderDistribution getOrderDistribution(){
        return orderDistribution;
    }

    public Individual getIndividual(){
        return individual;
    }

    public void createIndividualAndOrderDistributionObject() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        ModelConverter.initializeIndividualFromPathFlowModel(this);
        this.individual.setFitness(this.model.get(GRB.DoubleAttr.ObjVal));
    }

    public void createEmptyIndividualAndOrderDistribution() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        this.individual.setOrderDistribution(orderDistribution);
    }


    public static void main(String[] args) throws IOException {

        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        PathFlowModel pfm = new PathFlowModel(dataMip);
        pfm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual individual = pfm.getIndividual();
        Master2020.StoringResults.Result res = new Master2020.StoringResults.Result(individual, "PFM");
        res.store();
        System.out.println(" ");

    }

}
