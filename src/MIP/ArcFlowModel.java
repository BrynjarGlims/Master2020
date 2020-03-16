package MIP;
import DataFiles.*;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class ArcFlowModel {


    public GRBEnv env;
    public GRBModel model;
    public Data data;
    public int optimstatus;
    public double objval;
    public String symmetry;

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
    public GRBVar[]k;
    public GRBVar[][][] uND;
    public GRBVar[][][] uD;
    public GRBVar[][][][][] qND;
    public GRBVar[][][][][] qD;
    public GRBVar[][][][] t;
    public GRBVar[] qO;

    public ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> pathsUsed; // TODO: 23.11.2019 Remove



    public void initializeModel() throws GRBException, FileNotFoundException {
        env = new GRBEnv(true);
        this.env.set("logFile",  "ArcFlowModel.log");
        this.env.start();
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "ArcFlowModel");
        this.data = DataReader.loadData();
    }


    public void initializeParameters() throws GRBException {
        createParameters();
        initializeXVariables();
        initializeYVariables();
        initializeZVariables();
        initializeKVariables();
        initializeUVariables();
        initializeQVariables();
        initializeTVariables();
        initializeQOVariables();

    }


    public void createParameters() {
        this.x = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfNodes][data.numberOfNodes];
        this.y = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers];
        this.z = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips];
        this.k = new GRBVar[data.numberOfVehicles];
        this.uD = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.uND = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.qD = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers][];
        this.qND = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers][];
        this.t = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfNodes];
        this.qO = new GRBVar[data.numberOfPeriods];

        for (int period = 0; period < data.numberOfPeriods; period++) {
            for (int customerID = 0; customerID < data.numberOfCustomers; customerID++) {
                this.uND[period][customerID] = new GRBVar[data.customers[customerID].numberOfNonDividableOrders];
                this.uD[period][customerID] = new GRBVar[data.customers[customerID].numberOfDividableOrders];
            }
        }

        for (int period = 0; period < data.numberOfPeriods; period++) {
            for (int vehicleID = 0; vehicleID < data.numberOfVehicles; vehicleID++) {
                for (int tripCounter = 0; tripCounter < data.numberOfTrips; tripCounter++) {
                    for (int customerID = 0; customerID < data.numberOfCustomers; customerID++) {
                        this.qD[period][vehicleID][tripCounter][customerID] = new GRBVar[data.customers[customerID].numberOfDividableOrders];
                        this.qND[period][vehicleID][tripCounter][customerID] = new GRBVar[data.customers[customerID].numberOfNonDividableOrders];
                    }
                }
            }
        }
    }

    public void initializeXVariables() throws GRBException {

        //Create x variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++) {
                        for (int j = 0; j < data.numberOfNodes; j++) {
                            numArcVariables++;
                            String variable_name = String.format("x[%d][%d][%d][%d][%d]", d, v, r, i, j);
                            x[d][v][r][i][j] = model.addVar(0.0, 1.0, data.distanceMatrix[i][j] *
                                    data.vehicles[v].vehicleType.travelCost, GRB.BINARY, variable_name);
                        }
                    }
                }
            }
        }


        //store in data files
        data.arcs = this.x;
    }

    public void initializeYVariables() throws GRBException {
        // Create y variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        String variable_name = String.format("y[%d][%d][%d][%d]", d, v, r, i);
                        y[d][v][r][i] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                    }
                }
            }
        }
    }

    public void initializeZVariables() throws GRBException {
        // Create z variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    String variable_name = String.format("z[%d][%d][%d]", d, v, r);
                    z[d][v][r] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }
    }

    public void initializeKVariables() throws GRBException {

        // Create k variables:
        for (int v = 0; v < data.numberOfVehicles; v++) {
            String variable_name = String.format("v[%d]", v);
            k[v] = model.addVar(0.0, 1.0, data.vehicles[v].vehicleType.usageCost, GRB.BINARY, variable_name);
        }
    }

    public void initializeUVariables() throws GRBException {

        // Create uND variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    String variable_name = String.format("u[%d][%d][%d]", d, i, m);
                    uND[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }

        // Create uD variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    String variable_name = String.format("u[%d][%d][%d]", d, i, m);
                    uD[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }
    }

    public void initializeQVariables() throws GRBException {

        //Create qND variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            qND[d][v][r][i][m] = model.addVar(0.0, Parameters.upperBoundQuantity, 0, GRB.CONTINUOUS, variable_name);
                            //todo: set upper bound quantity correctly
                        }
                    }
                }
            }
        }


        //Create qD variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            qD[d][v][r][i][m] = model.addVar(0.0, Parameters.upperBoundQuantity, 0, GRB.CONTINUOUS, variable_name);
                            //todo: set upper bound quantity correctly
                        }
                    }
                }
            }
        }
    }

    public void initializeTVariables() throws GRBException {

        //Create t variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++) {
                        if (i == data.numberOfCustomers || i == data.numberOfCustomers + 1) {   // Constraint 5.12
                            String variable_name = String.format("t[%d][%d][%d][%d]", d, v, r, i);
                            t[d][v][r][i] = model.addVar(0, Parameters.maxJourneyDuration,
                                    0, GRB.CONTINUOUS, variable_name);
                        } else {
                            String variable_name = String.format("t[%d][%d][%d][%d]", d, v, r, i);
                            t[d][v][r][i] = model.addVar(data.customers[i].timeWindow[d][0],
                                    data.customers[i].timeWindow[d][1], 0, GRB.CONTINUOUS, variable_name);
                        }
                    }
                }
            }
        }
    }

    public void initializeQOVariables() throws GRBException {

        //Create qO (overtime) variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, Parameters.upperBoundOvertime, Parameters.overtimeCost[d], GRB.CONTINUOUS, variable_name);
        }


    }

    public void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // TODO: 20.11.2019 Change objective
    }



    public void terminateModel() throws GRBException {
        model.dispose();
        env.dispose();
    }

    public void timeBetweenCustomers() throws GRBException {
        // Constraint 5.9 - linear:
        // If a car drives from i to j, then customer i is visited and unloaded at.
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        for (int j = 0; j < data.numberOfNodes; j++) {
                            if (i == j || j == data.numberOfCustomers)
                                continue;
                            GRBLinExpr lhs = new GRBLinExpr();    //Create the left hand side of the equation
                            lhs.addTerm(1, t[d][v][r][i]);
                            lhs.addTerm(-1, t[d][v][r][j]);

                            if (j == data.numberOfCustomers+1) {
                                String constraint_name = String.format("5.9 -Time between customer %d and " +
                                                "customer %d for vehicle %d, trip %d, period %d. Travel time: %f, Fixed unloading time: %f",
                                        i, j, v, r, d, data.distanceMatrix[i][j], data.customers[i].totalUnloadingTime, Parameters.maxJourneyDuration);
                                lhs.addTerm( data.customers[i].timeWindow[d][1] + data.distanceMatrix[i][data.numberOfCustomers+1]
                                        + data.customers[i].totalUnloadingTime, x[d][v][r][i][j]);
                                model.addConstr(lhs, GRB.LESS_EQUAL, data.customers[i].timeWindow[d][1], constraint_name);
                            }
                            else {

                                String constraint_name = String.format("5.9 -Time between customer %d and " +
                                                "customer %d for vehicle %d, trip %d, period %d. Travel time: %f, Fixed unloading time: %f. EndTimeWindowFrom: %f. StartTimeWindowTo: %f",
                                        i, j, v, r, d, data.distanceMatrix[i][j], data.customers[i].totalUnloadingTime,
                                        Parameters.maxJourneyDuration, data.customers[i].timeWindow[d][1], data.customers[j].timeWindow[d][0]);
                                //System.out.println("values: " + (data.customers[i].timeWindow[d][1] + data.distanceMatrix[i][j] +
                                    //    data.customers[i].totalUnloadingTime - data.customers[j].timeWindow[d][0]));

                                lhs.addTerm( data.customers[i].timeWindow[d][1] + data.distanceMatrix[i][j] +
                                        data.customers[i].totalUnloadingTime - data.customers[j].timeWindow[d][0], x[d][v][r][i][j]);
                                model.addConstr(lhs, GRB.LESS_EQUAL, data.customers[i].timeWindow[d][1] -
                                        data.customers[j].timeWindow[d][0], constraint_name);
                            }
                        }
                    }
                }
            }
        }
    }

    public void timeFromCustomerToDepot() throws GRBException {
        // Constraint 5.10:
        // If a car drives from depot to customer i
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int j = 0; j < data.numberOfNodes; j++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        if (j == data.numberOfCustomers)
                            continue;
                        lhs.addTerm(1, t[d][v][r][data.numberOfCustomers]);
                        lhs.addTerm(-1, t[d][v][r][j]);
                        if (j == data.numberOfCustomers+1) {
                            String constraint_name = String.format("5.10 -Time between depot %d to depot %d for vehicle %d, trip %d, day %d. ", data.numberOfCustomers, j, v, r, d);
                            model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                        }
                        else {
                            String constraint_name = String.format("5.10 -Time between depot %d and customer %d for vehicle %d, trip %d, day %d. ", data.numberOfCustomers, j, v, r, d);
                            lhs.addTerm( Parameters.maxJourneyDuration + data.distanceMatrix[data.numberOfCustomers][j] - data.customers[j].timeWindow[d][0] , x[d][v][r][data.numberOfCustomers][j]);
                            model.addConstr(lhs, GRB.LESS_EQUAL,  Parameters.maxJourneyDuration - data.customers[j].timeWindow[d][0], constraint_name);   //todo: Implement correct M  // TODO: 21/02/2020 Check if this is correct

                        }
                    }
                }
            }
        }
    }

    public void timeLoadingAtDepot() throws GRBException {
        // Constraint 5.11:
        // The trip constraint
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < (data.numberOfTrips - 1); r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, t[d][v][r][data.numberOfCustomers + 1]);
                    lhs.addTerm(data.vehicles[v].vehicleType.loadingTimeAtDepot , z[d][v][r + 1]);
                    lhs.addTerm(-1, t[d][v][r + 1][data.numberOfCustomers]);
                    String constraint_name = String.format("5.11 -Loading time at docking area for vehicle %d, trip %d, day %d. Loading time: %f", v, r, d, data.vehicles[v].vehicleType.loadingTimeAtDepot );
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void capacityVehicle() throws GRBException {
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    //loop through all customers and products
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                            lhs.addTerm(1.0, qND[d][v][r][i][m]);
                        }
                        for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                            lhs.addTerm(1.0, qD[d][v][r][i][m]);
                        }
                    }
                    // Create name
                    String constraint_name = String.format("5.3 -Capacity vehicle %d trip %d period %d. Capacity %f", v, r, d, data.vehicles[v].vehicleType.capacity);
                    // Create constraint and defind RHS
                    model.addConstr(lhs, GRB.LESS_EQUAL, data.vehicles[v].vehicleType.capacity, constraint_name);
                }
            }
        }
    }

    public void capacityOvertimeAtDepot() throws GRBException {
        // Constraint 5.4: Overtime constraint at the warehouse if the goods delivered is higher
        // than the overtime limit
        for (int d = 0; d < data.numberOfPeriods; d++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                            lhs.addTerm(1.0, qD[d][v][r][i][m]);
                        }
                        for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                            lhs.addTerm(1.0, qND[d][v][r][i][m]);
                        }
                    }
                }
            }
            lhs.addTerm(-1.0, qO[d]); // Add the over time variable for that day
            // Create name
            String constraint_name = String.format("5.4 -Overtime on day %d. OvertimeLimit %f ", d, Parameters.overtimeLimit[d]);
            // Create constraint and defind RHS
            model.addConstr(lhs, GRB.LESS_EQUAL, Parameters.overtimeLimit[d], constraint_name);
        }
    }


    public void capacityUseOfVehicle() throws GRBException {
        // Constraint 5.17
        // If vehicle visits a customer in the planing period, its considered used
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, k[v]);
                    String constraint_name = String.format("5.17 -Use of vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }


    public void flowEnforceStartAtDepot() throws GRBException {
        //  Constraint 5.13
        // All trips must start at the depot
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int j = 0; j < data.numberOfCustomers; j++) {  // need to include the depot,  //TODO: LOOK AT THE SUM
                        lhs.addTerm(1, x[d][v][r][data.numberOfCustomers][j]);
                    }
                    lhs.addTerm(-1, z[d][v][r]);
                    String constraint_name = String.format("5.13 -Vehicle needs to start at the depot for vehicle %d, on trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0 , constraint_name);
                }
            }
        }

    }
    public void flowEnforceEndAtDepot() throws GRBException {
        //  Constraint 5.14
        // All trips must end at the depot
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int i = 0; i < data.numberOfCustomers; i++) {  // need to include the depot,   //TODO: LOOK AT THE SUM
                        lhs.addTerm(1, x[d][v][r][i][data.numberOfCustomers + 1]);
                    }
                    lhs.addTerm(-1, z[d][v][r]);
                    String constraint_name = String.format("5.14 -Vehicle needs to end at depot for vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }


    public void flowEnteringNodeBalance() throws GRBException {
        //  Constraint 5.15
        //  If a vehicle enters an node, then the node is visited
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int j = 0; j < data.numberOfCustomers; j++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int i = 0; i < data.numberOfCustomers; i++) {  // need to include the start depot but not the end depot
                            lhs.addTerm(1, x[d][v][r][i][j]);
                        }
                        lhs.addTerm(1, x[d][v][r][data.numberOfCustomers][j]);
                        lhs.addTerm(-1, y[d][v][r][j]);
                        String constraint_name = String.format("5.15 -Flow into customer node %d for vehicle %d, trip %d, day %d", j, v, r, d);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void flowExitingNodeBalance() throws GRBException {
        //  Constraint 5.16
        // If a vehicle exits an node, then the node is visited.
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int j = 0; j < data.numberOfCustomers ; j++) {  // need to include the depot, //TODO: Maybe a problem with including the depot
                            lhs.addTerm(1, x[d][v][r][i][j]);
                        }
                        lhs.addTerm(1, x[d][v][r][i][data.numberOfCustomers+1]);
                        lhs.addTerm(-1, y[d][v][r][i]);
                        String constraint_name = String.format("5.16 -Flow out from node %d for vehicle %d, trip %d, day %d", i, v, r, d);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void flowAllowableVisitPeriods() throws GRBException {
        // Constraint 5.18
        // Allowable visits to customer on spesific day
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int v = 0; v < data.numberOfVehicles; v++) {
                    for (int r = 0; r < data.numberOfTrips; r++) {
                        lhs.addTerm(1, y[d][v][r][i]);
                    }
                }
                String constraint_name = String.format("5.18 -Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, data.customers[i].requiredVisitPeriod[d]);
                model.addConstr(lhs, GRB.EQUAL, data.customers[i].requiredVisitPeriod[d], constraint_name);
            }
        }
    }


    public void flowPrecedenceOfTrips() throws GRBException {
        // Constraint 5.19
        // Presedence constriant of trips
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < (data.numberOfTrips - 1); r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, z[d][v][r + 1]);
                    String constraint_name = String.format("5.19 -Precedence for trip %d vehicle %d, day %d", r, v, d);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void flowDeliveryVisitRequirement() throws GRBException {
        // Constraint 5.20
        // No trip, no delivery
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        lhs.addTerm(1, y[d][v][r][i]);
                    }
                    lhs.addTerm(-data.numberOfCustomers, z[d][v][r]);
                    String constraint_name = String.format("5.20 -No trip, no delivery for vehicle %d, trip %d, day %d", v, r, d);
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void flowDeliveryQuantityRestriction() throws GRBException {
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                            lhs.addTerm(1, qND[d][v][r][i][m]);

                        }
                        for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                            lhs.addTerm(1, qD[d][v][r][i][m]);

                        }

                        lhs.addTerm(data.vehicles[v].vehicleType.capacity, y[d][v][r][i]);
                        String constraint_name = String.format("5.25 -Connection q and y for customer %d vehicle %d trip %d day %d. M = %f", i, v, r, d, data.vehicles[v].vehicleType.capacity);
                        model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void quantityNonDividableDelivery() throws GRBException {
        // Constraint 5.5: If one choose to deliver a non-div good, than a certain Q must be delivered
        // than the overtime limit
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(data.customers[i].nonDividableOrders[m].volume, uND[d][i][m]);
                    for (int v = 0; v < data.numberOfVehicles; v++) {
                        for (int r = 0; r < data.numberOfTrips; r++) {
                            lhs.addTerm(-1, qND[d][v][r][i][m]);
                        }
                    }
                    String constraint_name = String.format("5.5 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, data.customers[i].nonDividableOrders[m].volume, data.customers[i].numberOfNonDividableOrders);
                    // Activate the constraint
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }



    public void quantityDividableBounds() throws GRBException {
        // Constraint 5.7 a): Lower bound for delivery for div product.
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int v = 0; v < data.numberOfVehicles; v++) {
                        for (int r = 0; r < data.numberOfTrips; r++) {
                            lhs.addTerm(-1, qD[d][v][r][i][m]);
                        }
                    }
                    lhs.addTerm(data.customers[i].dividableOrders[m].minVolume, uD[d][i][m]);
                    String constraint_name = String.format("5.7 a) -Min delivery of dividable product %d customer %d on " +
                            "day %d. Min amount: %f", m, i, d, data.customers[i].dividableOrders[m].minVolume);
                    // Activate the constraint
                    model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);

                }
            }
        }


        // Constraint 5.7 b): Upper bound for delivery for non-div product.
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int v = 0; v < data.numberOfVehicles; v++) {
                        for (int r = 0; r < data.numberOfTrips; r++) {
                            lhs.addTerm(-1, qD[d][v][r][i][m]);
                        }
                    }
                    lhs.addTerm(data.customers[i].dividableOrders[m].maxVolume, uD[d][i][m]);
                    String constraint_name = String.format("5.7 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, data.customers[i].dividableOrders[m].maxVolume);
                    // Activate the constraint
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);

                }
            }
        }
    }


    public void quantityTotalDemandDelivered() throws GRBException {
        // Constraint 5.8: Demand of every product must be satisfied in the planning horizon
        for (int i = 0; i < data.numberOfCustomers; i++) {
            for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int d = 0; d < data.numberOfPeriods; d++) {
                    for (int v = 0; v < data.numberOfVehicles; v++) {
                        for (int r = 0; r < data.numberOfTrips; r++) {
                            lhs.addTerm(1, qD[d][v][r][i][m]);
                        }
                    }
                }
                String constraint_name = String.format("5.8 -Delivery of div product %d to customer %d. Quantity %f", m, i, data.customers[i].dividableOrders[m].volume);
                // Activate the constraint
                model.addConstr(lhs, GRB.EQUAL, data.customers[i].dividableOrders[m].volume, constraint_name);
                //// TODO: 21/02/2020 Check if this needs to be implemented for non-div product as well
            }
        }
    }

    public void quantitySingleDeliveryOfNonDivCommodity() throws GRBException {
        // Constraint 5.21: Only one non-div product is delivered to the store. // TODO: 23.11.2019 Change numbering to correct
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    lhs.addTerm(1, uND[d][i][m]);
                }
                // Activate the constraint
                String constraint_name = String.format("5.6 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, 1, constraint_name);
            }
        }
    }

    public void quantityDeliveryRequirementNonDivCommodity() throws GRBException {
        // Constraint 5.21
        // Non-dividable good has to be delivered during t

        for (int i = 0; i < data.numberOfCustomers; i++) {
            for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int d = 0; d < data.numberOfPeriods; d++) {
                    lhs.addTerm(1, uND[d][i][m]);
                }
                String constraint_name = String.format("5.21 -Nondiv good %d must be delivered exactly once to customer %d", m, i);
                model.addConstr(lhs, GRB.EQUAL, 1, constraint_name);
            }
        }
    }

    public void quantityMinFrequencyDivCommodity() throws GRBException {
        // Constraint 5.22
        // Dividable good has to be delivered at least above the minimum frequenzy
        for (int i = 0; i < data.numberOfCustomers; i++) {
            for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int d = 0; d < data.numberOfPeriods; d++) {
                    lhs.addTerm(1, uD[d][i][m]);
                }
                String constraint_name = String.format("5.22 -Div good %d must be delivered at least %d to customer %d", m, data.customers[i].dividableOrders[m].minFrequency, i);
                model.addConstr(lhs, GRB.GREATER_EQUAL, data.customers[i].dividableOrders[m].minFrequency, constraint_name);

            }
        }
    }

    public void quantityMaxFrequencyDivCommodity() throws GRBException {
        // Constraint 5.23
        // Dividable good has to be delivered at most the maximum number of times

        for (int i = 0; i < data.numberOfCustomers; i++) {
            for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int d = 0; d < data.numberOfPeriods; d++) {
                    lhs.addTerm(1, uD[d][i][m]);
                }
                String constraint_name = String.format("5.23 -Div good %d must be delivered at most %d to customer %d", m, data.customers[i].dividableOrders[m].maxFrequency, i);
                model.addConstr(lhs, GRB.LESS_EQUAL, data.customers[i].dividableOrders[m].maxFrequency, constraint_name);
                
            }
        }
    }

    public void fixation1() throws GRBException {
        //fix: have all arcs from i to j where i == j equal to 0
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++) {
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
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, x[d][v][r][data.numberOfCustomers +1][data.numberOfCustomers]);
                    String constraint_name = String.format("QF2 -Vehicle %d can not drive from end-depot %d to start-depot" +
                            " %d on trip %d, day %d", v, data.numberOfCustomers +1 ,data.numberOfCustomers, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void fixation3() throws GRBException { // TODO: 21/02/2020 Could be combined with fixation 2 
        //fix 3: cannot drive from the start depot to the end depot directly
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    lhs.addTerm(1, x[d][v][r][data.numberOfCustomers][data.numberOfCustomers +1]);
                    String constraint_name = String.format("QF3 -Vehicle %d can not drive from start-depot %d to end-depot " +
                            "%d on trip %d, day %d", v, data.numberOfCustomers,data.numberOfCustomers +1, r, d);
                    model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void symmetryCar() throws GRBException {
        // Constraint 5.65
        for (int v = 0; v < data.numberOfVehicles - 1; v++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            if (data.vehicles[v].vehicleType.vehicleTypeID != data.vehicles[v + 1].vehicleType.vehicleTypeID)
                continue;
            lhs.addTerm(1, k[v]);
            lhs.addTerm(-1, k[v + 1]);
            String constraint_name = String.format("5.65 Sym3- Vehicle %d must be used before vehicle %d over vehicle " +
                    "type %d", v, v + 1, data.vehicles[v + 1].vehicleType.vehicleTypeID);
            model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
        }
    }

    public void symmetryTrip() throws GRBException {
        // Vehicle trip number decreasing
        System.out.println("-----------------------Using symmetry : trip -------------------------");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles -1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.vehicleTypeID != data.vehicles[v + 1].vehicleType.vehicleTypeID)
                    continue;
                for (int r = 0; r < data.numberOfTrips; r++) {
                    lhs.addTerm(1, z[d][v][r]);
                    lhs.addTerm(-1, z[d][v+1][r]);

                }
                String constraint_name = String.format("5.XX Sym1 - Number of trips used for vehicle %d must be larger " +
                        "than vehicle %d in period %d and vehicle type %d", v, v+1 ,d ,data.vehicles[v+1].vehicleType.vehicleTypeID);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryCost() throws GRBException {
        System.out.println("-----------------------Using symmetry : Cost  -------------------------");
        // Constraint 5.64 //// CANNOT BE USED WITH 5.66 amd 5.63
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles -1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.vehicleTypeID != data.vehicles[v + 1].vehicleType.vehicleTypeID)
                    continue;
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++){
                        for(int j = 0; j < data.numberOfNodes; j++){
                            lhs.addTerm(data.distanceMatrix[i][j] * data.vehicles[v].vehicleType.travelCost, x[d][v][r][i][j]);
                            lhs.addTerm(data.distanceMatrix[i][j] * data.vehicles[v].vehicleType.travelCost, x[d][v+1][r][i][j]);
                        }
                    }
                }
                String constraint_name = String.format("5.67 Sym5 - Length of jouney for vehicle %d must be larger than " +
                        "vehicle %d in period %d and vehicle type %d", v, v+1 ,d ,data.vehicles[v+1].vehicleType.vehicleTypeID);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryCustomers() throws GRBException {
        System.out.println("-----------------------Using symmetry : Customers -------------------------");
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles -1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (data.vehicles[v].vehicleType.vehicleTypeID != data.vehicles[v+1].vehicleType.vehicleTypeID)
                    continue;
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++){
                        lhs.addTerm(1, y[d][v][r][i]);
                        lhs.addTerm(1, y[d][v+1][r][i]);
                    }
                }
                String constraint_name = String.format("5.66 Sym4 - Number of customer visits for vehicle %d must be " +
                        "larger than vehicle %d in period %d and vehicle type %d", v, v+1 ,d ,data.vehicles[v+1].vehicleType.vehicleTypeID);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }



    public void activateConstraints(String symmetry) throws GRBException {
        // -------- Add constraints -------------
        // 5.2 is implemented in variable declaration
        // 5.6 is implemented in variable declaration

        //timeConstraints
        timeBetweenCustomers();
        timeFromCustomerToDepot();
        timeLoadingAtDepot();

        //capacityConstraint
        capacityVehicle();
        capacityOvertimeAtDepot();
        capacityUseOfVehicle();

        //flowConstraints
        flowEnforceStartAtDepot();
        flowEnforceEndAtDepot();
        flowEnteringNodeBalance();
        flowExitingNodeBalance();
        flowAllowableVisitPeriods();
        flowPrecedenceOfTrips();
        flowDeliveryVisitRequirement();
        flowDeliveryQuantityRestriction();

        //quantityConstraints
        quantityNonDividableDelivery();
        quantityDividableBounds();
        quantityTotalDemandDelivered();
        quantitySingleDeliveryOfNonDivCommodity();
        quantityDeliveryRequirementNonDivCommodity();
        quantityMinFrequencyDivCommodity();
        quantityMaxFrequencyDivCommodity();

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
            symmetryCar();
            if (symmetry.equals("trips")) {
                symmetryTrip();
            } else if (symmetry.equals("cost")) {
                symmetryCost();
            } else if (symmetry.equals("customers")) {
                symmetryCustomers();
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
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++) {
                        for (int j = 0; j < data.numberOfNodes; j++) {  // litt usikker på om dette er rett siden Aij er litt spess
                            if (x[d][v][r][i][j].get(GRB.DoubleAttr.X) == 1) {
                                if (i == data.numberOfCustomers)
                                    System.out.println("Vehicle " + v + " on period " + d + " trip " + r + " drives from start-depot to customer " + j);
                                else if(j == data.numberOfCustomers + 1)
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
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        if (y[d][v][r][i].get(GRB.DoubleAttr.X) == 1) {
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
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    if (z[d][v][r].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Vehicle " + v + " on day " + d + " uses trip " + r);
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Create k variables: vehicle v is used in the planning period
        System.out.println("Print of k-variables: If a vehicle is used in the planing period");
        for (int v = 0; v < data.numberOfVehicles; v++) {
            if (k[v].get(GRB.DoubleAttr.X) == 1) {
                System.out.println("Vehicle " + v + " is used in the planning period with capacity: "  +data.vehicles[v].vehicleType.capacity);
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Create uND variables: if a product is delivered to customer m
        System.out.println("Print of uND-variables: If a product m is delivered to customer i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    if (uND[d][i][m].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Non-diviable Product " + m + " in customer " + i + " is delivered on day " + d);
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        // Create uD variables: if a product is delivered to customer m
        System.out.println("Print of uD-variables: If a product m is delivered to customer i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    if (uD[d][i][m].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Diviable Product " + m + " in customer " + i + " is delivered on day " + d);
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        //Create q variables: Quantity of m delivered to store i
        System.out.println("Print of q-variables: Quantity of m delivered to store i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            double quantityday = 0;
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    double quantitiyTrip = 0;
                    for (int i = 0; i < data.numberOfCustomers; i++) {
                        double quantitiyCust = 0 ;
                        for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                            if (qD[d][v][r][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                                System.out.println("Dividable Quantity " + (double) Math.round(qD[d][v][r][i][m].
                                        get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " + m +
                                        " is delivered to " + "customer " + i + " with vehicle " + v +
                                        " trip " + r + " on day " + d);
                                quantityday += qD[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyTrip += qD[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyCust += qD[d][v][r][i][m].get(GRB.DoubleAttr.X);
                            }
                        }
                        for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                            if (qND[d][v][r][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                                System.out.println("Nondividable Quantity " + (double) Math.round(qND[d][v][r][i][m].
                                        get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " + m +
                                        " is delivered to " + "customer " + i + " with vehicle " + v +
                                        " trip " + r + " on day " + d);
                                quantityday += qND[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyTrip += qND[d][v][r][i][m].get(GRB.DoubleAttr.X);
                                quantitiyCust += qND[d][v][r][i][m].get(GRB.DoubleAttr.X);
                            }
                        }
                        if (quantitiyCust >= 0.0001 ){
                            System.out.println("Quantity for customer "+ i + " is :" + quantitiyCust);
                        }
                    }
                    if (quantitiyTrip >= 0.0001) {
                        System.out.println("Quantity for vehicle " + v + "for trip " + r + "is equal: " + quantitiyTrip);
                    }
                }
            }
            System.out.println("Quantity on day " + d + " is equal to:" + quantityday);
        }

        System.out.println("   ");
        System.out.println("   ");

        //Create t variables: Time of visit, customer i
        System.out.println("Print of t-variables: Visiting time for customer i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int v = 0; v < data.numberOfVehicles; v++) {
                for (int r = 0; r < data.numberOfTrips; r++) {
                    for (int i = 0; i < data.numberOfNodes; i++) {
                        for (int j = 0; j < data.numberOfNodes; j++) {
                            if (x[d][v][r][i][j].get(GRB.DoubleAttr.X) == 1) {
                                if (i == data.numberOfCustomers)
                                    System.out.println("Departure from start-depot on time " +
                                            t[d][v][r][i].get(GRB.DoubleAttr.X)  + " by vehicle "
                                            + v + " trip " + r + " on day " + d +
                                            " and will be visiting customer " + j + " next");
                                else if (j == data.numberOfCustomers + 1)
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
        for (int d = 0; d < data.numberOfPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is "
                        + (double)Math.round(qO[d].get(GRB.DoubleAttr.X) * 1000d) / 1000d );
            }
        }
    }


    public void storePath() throws GRBException {
        pathsUsed = new ArrayList<>();
        for (int d = 0; d < data.numberOfPeriods; d++) {
            ArrayList<ArrayList<ArrayList<Integer>>> arrayDays = new ArrayList<>();
            for (int v = 0; v < data.numberOfVehicles; v++) {
                ArrayList<ArrayList<Integer>> arrayVehicles = new ArrayList<>();
                for (int r = 0; r < data.numberOfTrips; r++) {
                    if (z[d][v][r].get(GRB.DoubleAttr.X) == 1) {
                        ArrayList<Integer> arrayPaths = new ArrayList<>();
                        for (int i = 0; i < data.numberOfCustomers; i++) {

                            if (y[d][v][r][i].get(GRB.DoubleAttr.X) == 1){// TODO: 23.11.2019 Få dette i rekkefølge
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
            for (int v = 0; v < data.numberOfVehicles; v++) {
                if (k[v].get(GRB.DoubleAttr.X) == 1){
                    numVehiclesUsed++;
                }
            }
            for (int d = 0; d < data.numberOfPeriods; d++) {
                for (int v = 0; v < data.numberOfVehicles; v++) {
                    for (int r = 0; r < data.numberOfTrips; r++) {
                        for (int i = 0; i < data.numberOfNodes; i++) {
                            for (int j = 0; j < data.numberOfNodes; j++) {  // litt usikker på om dette er rett siden Aij er litt spess
                                if (x[d][v][r][i][j].get(GRB.DoubleAttr.X) == 1) {
                                    this.numArcsUsed++;
                                }
                            }
                        }
                    }
                }
            }
            for (int d = 0; d < data.numberOfPeriods; d++) {
                if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                    volumeOvertime += qO[d].get(GRB.DoubleAttr.X);
                }
            }
        }

        
    }

    public void runModel() {
        try {
            this.symmetry = Parameters.symmetry;
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
                System.out.println("Terminate model");
                terminateModel();
            }
            else if (optimstatus == 2){
                if (Parameters.plotArcFlow){
                    GraphPlot plotter = new GraphPlot(data);
                    plotter.visualize(true);
                }

                System.out.println("Create and store results");
                storePath();
                printSolution();
                System.out.println("Terminate model");
                terminateModel();
            }
            else{
                System.out.println("Create and store results");
                storePath();
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

    public static void main(String[] args) {
        ArcFlowModel arcFlowModel = new ArcFlowModel();
        arcFlowModel.runModel();

    }

}
