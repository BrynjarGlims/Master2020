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

public class ArcFlowModel {


    public GRBEnv env;
    public GRBModel model;
    public DataMIP dataMIP;
    public PathGenerator pg;
    public Result result;
    public String dataPath;
    public int optimstatus;
    public double objval;
    public String symmetry;
    public boolean infeasible;

    //NEW RESULT VARIABLES:
    public Individual individual;
    public OrderDistribution orderDistribution;

    // derived variables
    public int numVehiclesUsed = 0;
    public double volumeOvertime = 0;
    public int numTripsUsed = 0;
    public int numDivCommodity;
    public int numNondivCommodity;
    public int numTripsGenerated = 0;
    public int numArcsUsed = 0;
    public int numArcVariables = 0;
    public int numJourneysUsed = 0;
    public int numJourneyVariables = 0;
    public double preProcessTime = 0;
    public int numGeneratedTrips = 0;
    public int numGeneratedJourneys = 0;

    //variables
    public GRBVar[][][][][] x;
    public GRBVar[][][][]y;
    public GRBVar[][][]z;
    //public GRBVar[] k;
    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[][][][] t;
    public GRBVar[] qO;

    public ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed; // TODO: 23.11.2019 Remove

    public ArcFlowModel(DataMIP dataMIP){
        this.dataMIP = dataMIP;
    }

    public void initializeModel() throws GRBException, FileNotFoundException {
        env = new GRBEnv(true);
        this.env.set("logFile",  "ArcFlowModel.log");
        this.env.start();
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "ArcFlowModel");
        this.pg = new PathGenerator(dataMIP);
        dataMIP.setPathMap(pg.generateAllPaths());
    }


    public void initializeParameters() throws GRBException {

        this.x = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips][dataMIP.numNodes][dataMIP.numNodes];
        this.y = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips][dataMIP.numCustomers];
        this.z = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips];
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
        this.t = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][dataMIP.numTrips][dataMIP.numNodes];
        this.qO = new GRBVar[dataMIP.numPeriods];


        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        for (int j = 0; j < dataMIP.numNodes; j++) {
                            numArcVariables++;  //Todo: maybe remove the zero values
                            String variable_name = String.format("x[%d][%d][%d][%d][%d]", d, v, r, i, j);
                            x[d][v][r][i][j] = model.addVar(0, 1, dataMIP.travelTime[i][j] * dataMIP.travelCost[v], GRB.BINARY, variable_name);
                        }
                    }
                }
            }
        }
        dataMIP.arcs = this.x;

        // Create y variables:
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        String variable_name = String.format("y[%d][%d][%d][%d]", d, v, r, i);
                        y[d][v][r][i] = model.addVar(0, 1, 0, GRB.BINARY, variable_name);
                    }
                }
            }
        }

        // Create z variables:
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    if (r == 0){
                        String variable_name = String.format("z[%d][%d][%d]", d, v, r);
                        z[d][v][r] = model.addVar(0, 1, dataMIP.vehicles[v].vehicleType.unitCost, GRB.BINARY, variable_name);
                    }
                    else{
                        String variable_name = String.format("z[%d][%d][%d]", d, v, r);
                        z[d][v][r] = model.addVar(0, 1, 0, GRB.BINARY, variable_name);
                    }
                }
            }
        }


        // Create u variables:
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                for (int d = 0; d < dataMIP.numPeriods; d++) {
                    String variable_name = String.format("u[%d][%d][%d]", d, i, m);
                    u[d][i][m] = model.addVar(0, 1, 0, GRB.BINARY, variable_name);
                }
            }
        }


        //Create q variables
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                if (dataMIP.productQuantity[i][m] == 0){
                    continue;
                }
                for (int d = 0; d < dataMIP.numPeriods; d++) {
                    for (int v = 0; v < dataMIP.numVehicles; v++) {
                        for (int r = 0; r < dataMIP.numTrips; r++) {
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            q[d][v][r][i][m] = model.addVar(0.0, Master2020.DataFiles.Parameters.upperBoundQuantity, 0, GRB.CONTINUOUS, variable_name);
                        }
                    }
                }
            }
        }

        //Create t variables
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        //System.out.println("TimeWindow: P:"+d +", V:" + v + ", R:" + r + ", i:" + i );
                        if (i == dataMIP.numCustomers || i == dataMIP.numCustomers + 1) {   // Constraint 5.12
                            String variable_name = String.format("t[%d][%d][%d][%d]", d, v, r, i);
                            t[d][v][r][i] = model.addVar(dataMIP.earliestDepartureTime, dataMIP.latestInTime, 0, GRB.CONTINUOUS, variable_name);
                            //System.out.println("Time window start: " + dataMIP.earliestDepartureTime);
                            //System.out.println("Time window end: " + dataMIP.latestInTime);
                        } else {
                            String variable_name = String.format("t[%d][%d][%d][%d]", d, v, r, i);
                            t[d][v][r][i] = model.addVar(dataMIP.timeWindowStart[d][i], dataMIP.timeWindowEnd[d][i], 0, GRB.CONTINUOUS, variable_name);
                            //System.out.println("Time window start: " + dataMIP.timeWindowStart[d][i]);
                            //System.out.println("Time window end: " + dataMIP.timeWindowEnd[d][i]);
                        }
                        //System.out.println("------------------------------");

                    }
                }
            }
        }

        //Create qO (overtime) variables
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, dataMIP.upperBoundOvertime, dataMIP.costOvertime[d], GRB.CONTINUOUS, variable_name);
        }
    }

    public void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // TODO: 20.11.2019 Change objective
    }

    public Result createAndStoreModelResults(boolean hasObjective, int isOptimal) throws IOException, GRBException {
        calculateResultValues();
        if (hasObjective){
            System.out.println("Success: "+optimstatus);
            this.result = new Result(model.get(GRB.DoubleAttr.Runtime), model.get(GRB.DoubleAttr.ObjVal), model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripsGenerated, numJourneyVariables,
                    volumeOvertime, model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs),
                    model.get(GRB.IntAttr.NumVars)-model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumBinVars), model.get(GRB.IntAttr.NumQNZs),
                    model.get(GRB.IntAttr.SolCount), dataMIP.numVehicles, dataMIP.numCustomers, numDivCommodity, numNondivCommodity, dataMIP.numTrips, dataMIP.numPeriods, model.get(GRB.DoubleAttr.NodeCount),0,
                    preProcessTime, numGeneratedTrips, numGeneratedJourneys, pathsUsed,  symmetry);
        }
        else {
            this.result = new Result(model.get(GRB.DoubleAttr.Runtime), 1000000.00, model.get(GRB.DoubleAttr.MIPGap),model.get(GRB.DoubleAttr.ObjBound), model.get(GRB.DoubleAttr.ObjBoundC),
                    model.get(GRB.StringAttr.ModelName), dataPath, isOptimal, optimstatus, numVehiclesUsed, numArcsUsed,numTripsUsed, numJourneysUsed, numArcVariables, numTripsGenerated, numJourneyVariables,
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

    public void constraint53() throws GRBException {
        // Constraint 5.9 - linear:
        // If a car drives from i to j, then customer i is visited and unloaded at.
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int j = 0; j < dataMIP.numNodes; j++) {
                            if (i == j || j == dataMIP.numCustomers)
                                continue;
                            GRBLinExpr lhs = new GRBLinExpr();    //Create the left hand side of the equation
                            lhs.addTerm(1, t[d][v][r][i]);
                            lhs.addTerm(-1, t[d][v][r][j]);
                            String constraint_name = String.format("5.9 -Time between customer %d and " +
                                    "customer %d for vehicle %d, trip %d, period %d. Travel time: %f, Fixed unloading time: %f.", i, j, v, r, d, dataMIP.travelTime[i][j], dataMIP.fixedUnloadingTime[i], dataMIP.latestInTime);
                            if (j == dataMIP.numCustomers+1) {
                                lhs.addTerm( dataMIP.timeWindowEnd[d][i] + dataMIP.travelTime[i][dataMIP.numCustomers+1] + dataMIP.fixedUnloadingTime[i], x[d][v][r][i][j]);
                                model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.timeWindowEnd[d][i], constraint_name);
                            }
                            else {
                                lhs.addTerm( dataMIP.timeWindowEnd[d][i] + dataMIP.travelTime[i][j] + dataMIP.fixedUnloadingTime[i] - dataMIP.timeWindowStart[d][j], x[d][v][r][i][j]);
                                model.addConstr(lhs, GRB.LESS_EQUAL,  dataMIP.timeWindowEnd[d][i] - dataMIP.timeWindowStart[d][j], constraint_name);
                            }
                        }
                    }
                }
            }
        }
    }

    public void constraint54() throws GRBException {
        // Constraint 5.10:
        // If a car drives from depot to customer i
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int j = 0; j < dataMIP.numNodes; j++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        if (j == dataMIP.numCustomers)
                            continue;
                        lhs.addTerm(1, t[d][v][r][dataMIP.numCustomers]);
                        lhs.addTerm(-1, t[d][v][r][j]);
                        if (j == dataMIP.numCustomers+1) {
                            String constraint_name = String.format("5.10 -Time between depot %d to depot %d for vehicle %d, trip %d, day %d. ", dataMIP.numCustomers, j, v, r, d);
                            model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                        }
                        else {
                            String constraint_name = String.format("5.10 -Time between depot %d and customer %d for vehicle %d, trip %d, day %d. ", dataMIP.numCustomers, j, v, r, d);
                            lhs.addTerm( dataMIP.latestInTime + dataMIP.travelTime[dataMIP.numCustomers][j] - dataMIP.timeWindowStart[d][j], x[d][v][r][dataMIP.numCustomers][j]);
                            model.addConstr(lhs, GRB.LESS_EQUAL,  dataMIP.latestInTime - dataMIP.timeWindowStart[d][j], constraint_name); //todo: Implement correct M
                        }
                    }
                }
            }
        }
    }

    public void constraint55() throws GRBException {
        // Constraint 5.11:
        // The trip constraint
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < (dataMIP.numTrips - 1); r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, t[d][v][r][dataMIP.numCustomers + 1]);
                    lhs.addTerm(dataMIP.loadingTime[dataMIP.vehicles[v].vehicleType.type], z[d][v][r + 1]);
                    lhs.addTerm(-1, t[d][v][r + 1][dataMIP.numCustomers]);
                    String constraint_name = String.format("5.11 -Loading time at docking area for vehicle %d, trip %d, day %d. Loading time: %f", v, r, d, dataMIP.loadingTime[dataMIP.vehicles[v].vehicleType.type]);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint57() throws GRBException {
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    //loop through all customers and products
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0) {
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                            }
                        }
                    }
                    // Create name
                    String constraint_name = String.format("5.3 -Capacity vehicle %d trip %d period %d. Capacity %f", v, r, d, dataMIP.vehicleCapacity[v]);
                    // Create constraint and defind RHS
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.vehicleCapacity[v], constraint_name);
                }
            }
        }
    }

    public void constraint58() throws GRBException {
        // Constraint 5.4: Overtime constraint at the warehouse if the goods delivered is higher
        // than the overtime limit
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
            String constraint_name = String.format("5.4 -Overtime on day %d. OvertimeLimit %f ", d, dataMIP.overtimeLimit[d]);
            // Create constraint and defind RHS
            model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.overtimeLimit[d], constraint_name);
        }
    }

    /*
    public void constraint59() throws GRBException {
        // Constraint 5.17
        // If vehicle visits a customer in the planing period, its considered used
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, k[v]);
                    String constraint_name = String.format("5.17 -Use of vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

     */


    public void constraint510() throws GRBException {
        //  Constraint 5.13
        // All trips must start at the depot
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int j = 0; j < dataMIP.numCustomers; j++) {  // need to include the depot,  //TODO: LOOK AT THE SUM
                        lhs.addTerm(1, x[d][v][r][dataMIP.numCustomers][j]);
                    }
                    lhs.addTerm(-1, z[d][v][r]);
                    String constraint_name = String.format("5.13 -Vehicle needs to start at the depot for vehicle %d, on trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0 , constraint_name);

                }
            }
        }

    }
    public void constraint511() throws GRBException {
        //  Constraint 5.14
        // All trips must end at the depot
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int i = 0; i < dataMIP.numCustomers; i++) {  // need to include the depot,   //TODO: LOOK AT THE SUM
                        lhs.addTerm(1, x[d][v][r][i][dataMIP.numCustomers + 1]);

                    }
                    lhs.addTerm(-1, z[d][v][r]);
                    String constraint_name = String.format("5.14 -Vehicle needs to end at depot for vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }


    public void constraint512() throws GRBException {
        //  Constraint 5.15
        //  If a vehicle enters an node, then the node is visited
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int j = 0; j < dataMIP.numCustomers; j++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int i = 0; i < dataMIP.numCustomers; i++) {  // need to include the start depot but not the end depot
                            lhs.addTerm(1, x[d][v][r][i][j]);
                        }
                        lhs.addTerm(1, x[d][v][r][dataMIP.numCustomers][j]);
                        lhs.addTerm(-1, y[d][v][r][j]);
                        String constraint_name = String.format("5.15 -Flow into customer node %d for vehicle %d, trip %d, day %d", j, v, r, d);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint513() throws GRBException {
        //  Constraint 5.16
        // If a vehicle exits an node, then the node is visited.
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int j = 0; j < dataMIP.numCustomers ; j++) {  // need to include the depot, //TODO: Maybe a problem with including the depot
                            lhs.addTerm(1, x[d][v][r][i][j]);
                        }
                        lhs.addTerm(1, x[d][v][r][i][dataMIP.numCustomers+1]);
                        lhs.addTerm(-1, y[d][v][r][i]);
                        String constraint_name = String.format("5.16 -Flow out from node %d for vehicle %d, trip %d, day %d", i, v, r, d);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint514() throws GRBException {
        // Constraint 5.18
        // Allowable visits to customer on spesific day
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    for (int r = 0; r < dataMIP.numTrips; r++) {
                        lhs.addTerm(1, y[d][v][r][i]);
                    }
                }
                String constraint_name = String.format("5.18 -Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, dataMIP.possibleDeliveryDays[d][i]);
                model.addConstr(lhs, GRB.EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }


    public void constraint515() throws GRBException {
        // Constraint 5.19
        // Presedence constriant of trips
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < (dataMIP.numTrips - 1); r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, z[d][v][r + 1]);
                    String constraint_name = String.format("5.19 -Precedence for trip %d vehicle %d, day %d", r, v, d);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint516() throws GRBException {
        // Constraint 5.20
        // No trip, no delivery
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        lhs.addTerm(1, y[d][v][r][i]);
                    }
                    lhs.addTerm(-dataMIP.numCustomers, z[d][v][r]);
                    String constraint_name = String.format("5.20 -No trip, no delivery for vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void constraint517() throws GRBException {
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
                        lhs.addTerm(-dataMIP.maxVehicleCapacity, y[d][v][r][i]);
                        String constraint_name = String.format("5.25 -Connection q and y for customer %d vehicle %d trip %d day %d. M = %f", i, v, r, d, dataMIP.maxVehicleCapacity);
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint518() throws GRBException {
        // Constraint 5.5: If one choose to deliver a non-div good, than a certain Q must be delivered
        // than the overtime limit
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
                        String constraint_name = String.format("5.5 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, dataMIP.productQuantity[i][m], dataMIP.numProductsPrCustomer[i]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }


    public void constraint519() throws GRBException {
        // Constraint 5.7 a): Lower bound for delivery for non-div product.
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
                        String constraint_name = String.format("5.7 a) -Min delivery of dividable product %d customer %d on day %d. Min amount: %f", m, i, d, dataMIP.minAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }

        // Constraint 5.7 b): Upper bound for delivery for non-div product.
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
                        String constraint_name = String.format("5.7 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, dataMIP.maxAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint520() throws GRBException {
        // Constraint 5.8: Demand of every product must be satisfied in the planning horizon
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
                    String constraint_name = String.format("5.8 -Delivery of div product %d to customer %d. Quantity %f", m, i, dataMIP.productQuantity[i][m] );
                    // Activate the constraint
                    model.addConstr(lhs, GRB.EQUAL, dataMIP.productQuantity[i][m], constraint_name);
                }
            }
        }
    }



    public void constraint521() throws GRBException {
        // Constraint 5.21: Only one non-div product is delivered to the store. // TODO: 23.11.2019 Change numbering to correct
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                }
                // Activate the constraint
                String constraint_name = String.format("5.6 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, 1, constraint_name);
            }
        }
    }

    public void constraint522() throws GRBException {
        // Constraint 5.21
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


    public void constraint523() throws GRBException {
        // Constraint 5.22
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
    public void constraint524() throws GRBException {
        // Constraint 5.23
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



    public void fixation1() throws GRBException {
        //fix: have all arcs from i to j where i == j equal to 0
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, x[d][v][r][i][i]);
                        String constraint_name = String.format("QF1 -Vehicle %d can drive from %d to %d on trip %d, day %d", v, i ,i, r, d);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void fixation2() throws GRBException {
        //fix 2: cannot drive from the end depot to the start depot
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, x[d][v][r][dataMIP.numCustomers +1][dataMIP.numCustomers]);
                    String constraint_name = String.format("QF2 -Vehicle %d can not drive from end-depot %d to start-depot %d on trip %d, day %d", v, dataMIP.numCustomers+1 , dataMIP.numCustomers, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void fixation3() throws GRBException {
        //fix 3: cannot drive from the start depot to the end depot directly
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, x[d][v][r][dataMIP.numCustomers][dataMIP.numCustomers +1]);
                    String constraint_name = String.format("QF3 -Vehicle %d can not drive from start-depot %d to end-depot %d on trip %d, day %d", v, dataMIP.numCustomers , dataMIP.numCustomers +1, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }

    /*
    public void symmetryCar() throws GRBException {
        // Constraint 5.65
        for (int v = 0; v < dataMIP.numVehicles - 1; v++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v + 1].vehicleType.type)
                continue;
            lhs.addTerm(1, k[v]);
            lhs.addTerm(-1, k[v + 1]);
            String constraint_name = String.format("5.65 Sym3- Vehicle %d must be used before vehicle %d over vehicle type %d", v, v + 1, dataMIP.vehicles[v + 1].vehicleType.type);
            model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
        }
    }

     */

    public void symmetryTrip() throws GRBException {
        // Vehicle trip number decreasing
        System.out.println("-----------------------Using symmetry : trip -------------------------");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles-1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v+1].vehicleType.type)
                    continue;
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, z[d][v+1][r]);

                }
                String constraint_name = String.format("5.XX Sym1 - Number of trips used for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
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
                    for (int i = 0; i < dataMIP.numNodes; i++){
                        for(int j = 0; j < dataMIP.numNodes; j++){
                            lhs.addTerm(dataMIP.travelTime[i][j] * dataMIP.travelCost[v], x[d][v][r][i][j]);
                            lhs.addTerm(dataMIP.travelTime[i][j] * dataMIP.travelCost[v], x[d][v+1][r][i][j]);
                        }
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
                    for (int i = 0; i < dataMIP.numCustomers; i++){
                        lhs.addTerm(1, y[d][v][r][i]);
                        lhs.addTerm(1, y[d][v+1][r][i]);
                    }
                }
                String constraint_name = String.format("5.66 Sym4 - Number of customer visits for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d , dataMIP.vehicles[v+1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }



    public void activateConstraints(String symmetry) throws GRBException {
        // -------- Add constraints -------------
        // 5.2 is implemented in variable declaration
        constraint53();
        constraint54();
        constraint55();

        // 5.6 is implemented in variable declaration
        constraint57();
        constraint58();
        //constraint59();
        constraint510();
        constraint511();
        constraint512();
        constraint513();
        constraint514();
        constraint515();
        constraint516();
        constraint517();
        constraint518();
        constraint519();
        constraint520();
        constraint521();
        constraint522();
        constraint523();
        constraint524();

        //Fixation of variables
        fixation1();
        fixation2();
        fixation3();

        //symmetry breaking constraints
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
            //} else if (symmetry.equals("duration")) {
            //    symmetryDuration();
            } else {
                System.out.println("-----------------------Using symmetry : " + symmetry + " (not standard) -------------------------");
                System.out.println("Only simple symmetry breaking chosen");
            }
        }

    }

    public void optimizeModel() throws GRBException {
        //if gap is needed to be changed:
        model.set(GRB.DoubleParam.MIPGap, Parameters.modelMipGap);
        model.set(GRB.DoubleParam.TimeLimit, Parameters.modelTimeLimit);
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
        } else if (optimstatus == GRB.Status.INF_OR_UNBD) {
            System.out.println("Model is infeasible or unbounded");

        } else if (optimstatus == GRB.Status.INFEASIBLE) {
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
        // Print x variables: car driving from i to j
        System.out.println("Print of x-variables: If a vehicle  uses an arc");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        for (int j = 0; j < dataMIP.numNodes; j++) {  // litt usikker på om dette er rett siden Aij er litt spess
                            if (Math.round(x[d][v][r][i][j].get(GRB.DoubleAttr.X)) == 1) {
                                if (i == dataMIP.numCustomers)
                                    System.out.println("Vehicle " + v + " on period " + d + " trip " + r + " drives from start-depot to customer " + j);
                                else if(j == dataMIP.numCustomers + 1)
                                    System.out.println("Vehicle " + v + " on period " + d + " trip " + r + " drives from customer" + i + " to end-depot");
                                else
                                    System.out.println("Vehicle " + v + " on period " + d + " trip " + r + " drives from customer" + i + " to customer" + j);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Print y variables: visiting customer i with vehicle v
        System.out.println("Print of y-variables: If a car visits a customer");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        if (Math.round(y[d][v][r][i].get(GRB.DoubleAttr.X)) == 1) {
                            System.out.println("Vehicle " + v + " on period " + d + " trip " + r +
                                    " visits customer " + i);
                        }
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Print z variables: if a vehicle uses a trip or not
        System.out.println("Print of z-variables: If a vehicle is uses a trip");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    if (Math.round(z[d][v][r].get(GRB.DoubleAttr.X)) == 1) {
                        System.out.println("Vehicle " + v + " on day " + d + " uses trip " + r);
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Create k variables: vehicle v is used in the planning period
        /*
        System.out.println("Print of k-variables: If a vehicle is used in the planing period");
        for (int v = 0; v < dataMIP.numVehicles; v++) {
            if (k[v].get(GRB.DoubleAttr.X) == 1) {
                System.out.println("Vehicle " + v + " is used in the planning period with capacity: "  + dataMIP.vehicleCapacity[v]);
            }
        }

         */

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
        System.out.println("Print of t-variables: Visiting time for customer i");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        for (int j = 0; j < dataMIP.numNodes; j++) {
                            if (Math.round(x[d][v][r][i][j].get(GRB.DoubleAttr.X)) == 1) {
                                if (i == dataMIP.numCustomers)
                                    System.out.println("Departure from start-depot on time " +
                                            t[d][v][r][i].get(GRB.DoubleAttr.X)  + " by vehicle "
                                            + v + " trip " + r + " on day " + d +
                                            " and will be visiting customer " + j + " next");
                                else if (j == dataMIP.numCustomers + 1)
                                    System.out.println("Customer " + i + " is visted on time " +
                                            t[d][v][r][i].get(GRB.DoubleAttr.X)  + " by vehicle "
                                            + v + " trip " + r + " on day " + d +
                                            " and will be visiting end-depot next");
                                else
                                    System.out.println("Customer " + i + " is visted on time " +
                                            t[d][v][r][i].get(GRB.DoubleAttr.X) + " by vehicle "
                                            + v + " trip " + r + " on day " + d +
                                            " and will be visiting customer " + j + " next");
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
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is "
                        + (double)Math.round(qO[d].get(GRB.DoubleAttr.X) * 1000d) / 1000d );
            }
        }
    }


    public void storePath() throws GRBException {
        pathsUsed = new ArrayList<>();
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            ArrayList<ArrayList<ArrayList<Integer>>> arrayDays = new ArrayList<>();
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                ArrayList<ArrayList<Integer>> arrayVehicles = new ArrayList<>();
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    if (Math.round(z[d][v][r].get(GRB.DoubleAttr.X)) == 1) {
                        ArrayList<Integer> arrayPaths = new ArrayList<>();
                        for (int i = 0; i < dataMIP.numCustomers; i++) {

                            if (Math.round(y[d][v][r][i].get(GRB.DoubleAttr.X)) == 1){// TODO: 23.11.2019 Få dette i rekkefølge
                                arrayPaths.add(i);
                            }
                        }
                        arrayVehicles.add(arrayPaths);
                    }
                }
                arrayDays.add(arrayVehicles);
            }
            pathsUsed.add(arrayDays);
        }
    }


    public void calculateResultValues() throws GRBException {
        if (optimstatus == 2){
            /*
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                if (k[v].get(GRB.DoubleAttr.X) == 1){
                    numVehiclesUsed++;
                }
            }

             */
            for (int d = 0; d < dataMIP.numPeriods; d++) {
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    for (int r = 0; r < dataMIP.numTrips; r++) {
                        for (int i = 0; i < dataMIP.numNodes; i++) {
                            for (int j = 0; j < dataMIP.numNodes; j++) {  // litt usikker på om dette er rett siden Aij er litt spess
                                if (Math.round(x[d][v][r][i][j].get(GRB.DoubleAttr.X)) == 1) {
                                    this.numArcsUsed++;
                                }
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

            // Master2020.Testing session
            //Master2020.Testing.MIPTest.printZSolutions(this);
            //Master2020.Testing.MIPTest.checkSpesificCase(this);
            Master2020.Testing.MIPTest.getDetailedResult(this);


            if (optimstatus == 3) {
                this.infeasible = true;
                System.out.println("No solution found");
                createEmptyIndividualAndOrderDistribution();
                System.out.println("Terminate model");
                terminateModel();
            }
            else if (optimstatus == 2){
                this.infeasible = false;

                if (Parameters.plotArcFlow){
                    GraphPlot plotter = new GraphPlot(dataMIP);
                    plotter.visualize(true);
                }

                System.out.println("Create and store results");
                storePath();
                createIndividualAndOrderDistributionObject();
                if (Parameters.verboseArcFlow)
                    printSolution();
                System.out.println("Terminate model");
                terminateModel();
            }
            else{
                System.out.println("Create and store results");
                storePath();
                if (Parameters.verboseArcFlow)
                    printSolution();
                createEmptyIndividualAndOrderDistribution();
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

    //arc flow converter
    public void createIndividualAndOrderDistributionObject() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        ModelConverter.initializeIndividualFromArcFlowModel(this);
        this.individual.setFitness(this.model.get(GRB.DoubleAttr.ObjVal));
    }

    public void createEmptyIndividualAndOrderDistribution() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        this.individual.setOrderDistribution(orderDistribution);
    }



    public OrderDistribution getOrderDistribution(){
        return orderDistribution;
    }

    public Individual getIndividual(){
        return individual;
    }

    public static void main(String[] args) throws IOException {
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        ArcFlowModel afm = new ArcFlowModel(dataMip);
        afm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual individual = afm.getIndividual();
        Master2020.StoringResults.Result res = new Master2020.StoringResults.Result(individual, "AFM");
        res.store();
        //PlotIndividual visualizer = new PlotIndividual(data);
        //visualizer.visualize(individual);
        System.out.println();
    }
}
