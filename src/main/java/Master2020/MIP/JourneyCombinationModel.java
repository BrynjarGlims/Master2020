package Master2020.MIP;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Order;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.PR.*;
import Master2020.PR.Path;
import Master2020.PR.Model;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class JourneyCombinationModel extends Model{

    public GRBEnv env;
    public GRBModel model;
    public DataMIP dataMIP;
    public PathGenerator pg;
    public Result result;
    public String dataPath;
    public int optimstatus;
    public double objval;
    public String symmetry;
    public boolean feasible;
    public boolean optimal;
    public double runTime;
    public double MIPGap;

    // Master 2020 parameters
    private OrderDistribution globalOrderDistribution;
    private Individual individual;
    private OrderDistribution orderDistribution;

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
    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[] qO;
    public ArrayList<Master2020.Individual.Journey>[][] journeys;   //period, vehicleType


    public JourneyCombinationModel(DataMIP dataMIP) throws GRBException {
        super(dataMIP);
        this.dataMIP = dataMIP;
        env = new GRBEnv(true);
        this.env.set("logFile",  "JourneyBasedModel.log");
        this.env.start();
    }

    @Override
    public ArrayList<Master2020.Individual.Journey>[][] getJourneys() {
        return journeys;
    }

    public void initializeModel() throws GRBException, FileNotFoundException {
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "JourneyCombinationModel");
        this.pg = new PathGenerator(dataMIP);
        double time = System.currentTimeMillis();
        this.preProcessTime = (System.currentTimeMillis() - time) / 1000;
        System.out.println("stop");
    }



    public void initializeParameters() throws GRBException {
        this.gamma = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicles][];
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
        this.qO = new GRBVar[dataMIP.numPeriods];


        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                gamma[d][v] = new GRBVar[journeys[d][dataMIP.vehicles[v].vehicleType.type].size()];
                for (int r  =0 ; r <  journeys[d][dataMIP.vehicles[v].vehicleType.type].size();  r++) {
                    numJourneyVariables++;
                    String variable_name = String.format("gamma[%d][%d][%d]", d, v, r);
                    gamma[d][v][r] = model.addVar(0.0, 1.0, journeys[d][dataMIP.vehicles[v].vehicleType.type].get(r).getFitnessWithVehicleCost(globalOrderDistribution), GRB.BINARY, variable_name);
                }
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
                        for (int r = 0; r < dataMIP.numTrips; r++){
                            String variable_name = String.format("q[%d][%d][%d][%d][%d]", d, v, r, i, m);
                            q[d][v][r][i][m] = model.addVar(0.0, DataMIP.upperBoundQuantity , 0, GRB.CONTINUOUS, variable_name);
                        }

                    }
                }
            }
        }
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, Data.overtimeLimit[d], dataMIP.costOvertime[d], GRB.CONTINUOUS, variable_name);
        }
    }

    public void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // TODO: 20.11.2019 Change objective
    }



    public void terminateModel() throws GRBException {
        model.dispose();
        env.dispose();
    }



    public void useOfVehiclesForEachCapacity() throws GRBException {
        // Constraint 5.68
        // if a journey is used, then the vehicle corresponding to that journey is considered used
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++){
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int r = 0; r < journeys[d][vt].size(); r++) {
                    if (journeys[d][vt].get(r).vehicleType == vt)
                        lhs.addTerm(1, gamma[d][vt][r]);
                }
                String constraint_name = String.format("5.70 -Max number of vehicles to be used for period %d and vt %d", d, vt);
                model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.vehicleTypes[vt].vehicles.length, constraint_name);
            }
        }
    }

    public void useOfVehicles() throws GRBException {
        // Constraint 5.68
        // if a journey is used, then the vehicle corresponding to that journey is considered used
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++){
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int r = 0; r < journeys[d][dataMIP.vehicles[v].vehicleType.type].size(); r++) {
                    lhs.addTerm(1, gamma[d][v][r]);
                }
                String constraint_name = String.format("5.70 -Max number of vehicles to be used for period %d and v %d", d, v);
                model.addConstr(lhs, GRB.LESS_EQUAL, 1, constraint_name);
            }
        }
    }

    public void allowableVisits() throws GRBException {
        // Constraint 5.69
        // Allowable visits to customer on spesific day
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    for (int r = 0; r < journeys[d][dataMIP.vehicles[v].vehicleType.type].size(); r++){
                        for (Master2020.Individual.Trip t : journeys[d][dataMIP.vehicles[v].vehicleType.type].get(r).trips) {
                            if (t.customers.contains(dataMIP.customers[i].customerID)){
                                lhs.addTerm(1, gamma[d][v][r]);
                            }
                        }
                    }
                }
                String constraint_name = String.format("5.71 -Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, dataMIP.possibleDeliveryDays[d][i]);
                model.addConstr(lhs, GRB.EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }


    public void capacityOfJourney() throws GRBException {  //evaluate this constraint
        // Constraint 5.70
        //Capacity constraint on each delivery
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicleTypes; v++) {
                for (int i = 0; i < dataMIP.numCustomers; i++) {
                    for (int r = 0; r < dataMIP.numTrips; r++) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        for (int j = 0; j < journeys[d][dataMIP.vehicles[v].vehicleType.type].size(); j++) {
                            for (Trip t : journeys[d][dataMIP.vehicles[v].vehicleType.type].get(j).trips ){
                                if (t.customers.contains(dataMIP.customers[i].customerID)) {
                                    lhs.addTerm(-dataMIP.vehicleCapacity[v], gamma[d][v][j]);
                                }
                            }
                        }
                        String constraint_name = String.format("5.72 -Connection gamma and q for customer %d vehicle %d day %d. M = %f", i, v, d, dataMIP.vehicleCapacity[v]);
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void constraint71() throws GRBException {
        // Constraint 5.73
        // Capacity for each vehicle
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++){
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] > 0) {
                                lhs.addTerm(1.0, q[d][v][r][i][m]);
                            }
                        }
                    }
                    String constraint_name = String.format("5.73 -Capacity vehicle %d trip %d period %d. Capacity %f", v, r, d, dataMIP.vehicleCapacity[v]);
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.vehicleCapacity[v], constraint_name);
                }
            }
        }
    }

    public void constraint72() throws GRBException {
        // Constraint 5.74:
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
            String constraint_name = String.format("5.74 -Overtime on day %d. OvertimeLimit %f ", d, dataMIP.overtimeLimit[d]);
            model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.overtimeLimit[d], constraint_name);
        }
    }

    public void constraint73() throws GRBException {
        // Constraint 5.75:
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
                        String constraint_name = String.format("5.75 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, dataMIP.productQuantity[i][m], dataMIP.numProductsPrCustomer[i]);
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
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 1 && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(-1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(dataMIP.minAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 a) -Min delivery of dividable product %d customer %d on day %d. Min amount: %f", m, i, d, dataMIP.minAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
        // Constraint 5.76 b):
        // Upper bound for delivery for non-div product. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 1 && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                lhs.addTerm(1, q[d][v][r][i][m]);
                            }
                        }
                        lhs.addTerm(-dataMIP.maxAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, dataMIP.maxAmountDivProduct[i][m]);
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
                    String constraint_name = String.format("5.77 -Delivery of product %d to customer %d. Quantity %f", m, i, dataMIP.productQuantity[i][m]);
                    // Activate the constraint
                    model.addConstr(lhs, GRB.EQUAL, dataMIP.productQuantity[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint76() throws GRBException {
        // Constraint 5.78: Only one non-div product is delivered to the store on day d.
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                }
                // Activate the constraint
                String constraint_name = String.format("5.78 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }
    public void constraint77() throws GRBException {
        // Constraint 5.79
        // Non-dividable good has to be delivered during t
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
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
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 1 && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.80 -Div good %d must be delivered at least %d to customer %d", m, dataMIP.minFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, dataMIP.minFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    public void constraint79() throws GRBException {
        // Constraint 5.81
        // Dividable good has to be delivered at most the maximum number of times
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.productTypes[i][m] == 1 && dataMIP.productQuantity[i][m] > 0) {
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, u[d][i][m]);
                    }
                    String constraint_name = String.format("5.81 -Div good %d must be delivered at most %d to customer %d", m, dataMIP.maxFrequencyProduct[i][m], i);
                    model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.maxFrequencyProduct[i][m], constraint_name);
                }
            }
        }
    }

    /*
    public void symmetryCar() throws GRBException {
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

    public void symmetryCost() throws GRBException {
        System.out.println("------------------- Symmetri: Cost--------------------");
        // Constraint 5.64 //// CANNOT BE USED WITH 5.66 amd 5.63
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v + 1].vehicleType.type) {
                    continue;
                }
                for (Master2020.PR.Journey r : dataMIP.journeyMap.get(d).get(dataMIP.vehicles[v].vehicleType.type) ) {
                    lhs.addTerm(r.cost, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.cost, gamma[d][v + 1][r.journeyId]);
                    String constraint_name = String.format("5.87 Sym5 - Length of jouney for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, dataMIP.vehicles[v + 1].vehicleType.type);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                }
            }
        }
    }

    public void symmetryCustomers() throws GRBException {
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        System.out.println("------------------- Symmetri: customers --------------------");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v + 1].vehicleType.type)
                    continue;
                for (Master2020.PR.Journey r : dataMIP.journeyMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(r.numCustomers, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.numCustomers, gamma[d][v + 1][r.journeyId]);
                }
                String constraint_name = String.format("5.88 Sym4 - Number of customer visits for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, dataMIP.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryTrips() throws GRBException {
        // Constrant 5.66 //// CANNOT BE USED WITH 5.67 and 5.63
        System.out.println("------------------- Symmetri: trip --------------------");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles - 1; v++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                if (dataMIP.vehicles[v].vehicleType.type != dataMIP.vehicles[v + 1].vehicleType.type)
                    continue;
                for (Master2020.PR.Journey r : dataMIP.journeyMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                    lhs.addTerm(r.numTrips, gamma[d][v][r.journeyId]);
                    lhs.addTerm(r.numTrips, gamma[d][v + 1][r.journeyId]);
                }
                String constraint_name = String.format("5.88 Sym4 - Number of trips used for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, dataMIP.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }


    public void activateConstraints() throws GRBException {
        // -------- Add constraints -------------

        //useOfVehiclesForEachCapacity();
        useOfVehicles();
        allowableVisits();
        capacityOfJourney();
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
            //symmetryCar();
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

    public void optimizeModel() throws GRBException {
        model.set(GRB.DoubleParam.MIPGap, Parameters.modelMipGap);
        model.set(GRB.DoubleParam.TimeLimit, Parameters.modelTimeLimit);
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
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (Master2020.PR.Journey r:  dataMIP.journeyMap.get(d).get(dataMIP.vehicles[v].vehicleType.type)) {
                    if (Math.round(gamma[d][v][r.journeyId].get(GRB.DoubleAttr.X)) == 1) {
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
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numCustomers; i++) {
                        for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                            if (dataMIP.productQuantity[i][m] == 0)
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
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is "
                        + (double)Math.round(qO[d].get(GRB.DoubleAttr.X) * 1000d) / 1000d );
            }
        }

        System.out.println("  ");
        System.out.println("  ");

    }



    public double runModel(ArrayList<Master2020.Individual.Journey>[][] journeys, OrderDistribution globalOrderDistribution) {
        try {
            double time = System.currentTimeMillis();
            this.globalOrderDistribution = globalOrderDistribution;
            this.journeys = journeys;
            this.symmetry = Parameters.symmetryOFJCM;
            initializeModel();
            initializeParameters();
            setObjective();
            activateConstraints();
            optimizeModel();
            displayResults(false);

            if (model.get(GRB.IntAttr.SolCount) == 0){
                System.out.println("No solution found");
                feasible = false;
                optimal = false;
                createEmptyIndividualAndOrderDistribution();
                System.out.println("Terminate model");
                terminateModel();
                this.MIPGap = -1;
                runTime = (System.currentTimeMillis() - time)/1000;
            }
            else{
                if (optimstatus == 2){
                    feasible = true;
                    optimal = true;
                    createIndividualAndOrderDistributionObject();
                    if (Parameters.verboseJourneyBased)
                        printSolution();
                    this.MIPGap = model.get(GRB.DoubleAttr.MIPGap);
                    runTime = (System.currentTimeMillis() - time)/1000;
                }
                else {
                    System.out.println("Create and store results");
                    feasible = true;
                    optimal = false;
                    createIndividualAndOrderDistributionObject();
                    if (Parameters.verboseJourneyBased)
                        printSolution();
                    System.out.println("Terminate model");
                    this.MIPGap = model.get(GRB.DoubleAttr.MIPGap);
                    runTime = (System.currentTimeMillis() - time)/1000;
                }
            }
            return optimstatus;

        } catch (GRBException | FileNotFoundException e) {
            System.out.println("ERROR: " + e);
            return -1;
        } catch (Error e) {
            System.out.println(e);
            return -1;
        }
    }

    public OrderDistribution getOrderDistribution(){
        return orderDistribution;
    }

    public Individual getIndividual(){
        return individual;
    }

    public ArrayList<Master2020.Individual.Journey>[][] getOptimalJourneys() throws Exception {
        if (optimstatus != 2)
            throw new Exception("Model not optimized");
        ArrayList<Master2020.Individual.Journey>[][] optimalJourneys = new ArrayList[dataMIP.numPeriods][dataMIP.numVehicleTypes];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++){
                optimalJourneys[p][vt] = new ArrayList<Master2020.Individual.Journey>();
            }
            for (int v = 0; v < dataMIP.numVehicles; v++){
                for (int j = 0; j < journeys[p][dataMIP.vehicles[v].vehicleType.type].size(); j++){
                    if (Math.round(gamma[p][v][j].get(GRB.DoubleAttr.X)) == 1){
                        optimalJourneys[p][dataMIP.vehicles[v].vehicleType.type].add(journeys[p][dataMIP.vehicles[v].vehicleType.type].get(j));
                    }
                }

            }
        }
        return optimalJourneys;
    }

    public void createIndividualAndOrderDistributionObject() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        //todo: change objective to be a cost of taking a journey
        ModelConverter.initializeIndividualFromModel(this);
        this.individual.setFitness(this.model.get(GRB.DoubleAttr.ObjVal));
    }

    public void createEmptyIndividualAndOrderDistribution() throws GRBException {
        this.individual = new Individual(dataMIP.newData);
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        this.individual.setOrderDistribution(orderDistribution);
    }


    public static void main (String[] args) throws IOException, GRBException {
        Data data = Master2020.DataFiles.DataReader.loadData();
        DataMIP dataMip = DataConverter.convert(data);
        JourneyCombinationModel jcm = new JourneyCombinationModel(dataMip);
        //jcm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual individual = jcm.getIndividual();
        Master2020.StoringResults.Result res = new Master2020.StoringResults.Result(individual, "JBM");
        res.store();
        //PlotIndividual visualizer = new PlotIndividual(data);
        //visualizer.visualize(individual);
        System.out.println(" ");
    }
}