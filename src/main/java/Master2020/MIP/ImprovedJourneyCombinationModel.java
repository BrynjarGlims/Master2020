package Master2020.MIP;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.PenaltyControl;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.Individual.Trip;
import Master2020.PR.*;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Testing.SolutionTest;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class ImprovedJourneyCombinationModel extends Model {
    public GRBEnv env;
    public GRBModel model;
    public DataMIP dataMIP;
    public PathGenerator pg;
    public Result result;
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
    public GRBVar[][][] q;
    public GRBVar[] qO;
    public ArrayList<Journey>[][] journeys;   //period, vehicleType


    public ImprovedJourneyCombinationModel(Data data) throws GRBException {
        super(data);
        this.dataMIP = DataConverter.convert(data);
        env = new GRBEnv(true);
        this.env.set("logFile",  "JourneyBasedModel.log");
        this.env.start();
        this.symmetry = Parameters.symmetryOFJCM;
        this.globalOrderDistribution = new OrderDistribution(data);
        this.globalOrderDistribution.makeInitialDistribution();

    }

    @Override
    public ArrayList<Master2020.Individual.Journey>[][] getJourneys() {
        //Gives all jourenys, not the optimal ones.
        return journeys;
    }

    public void initializeModel() throws GRBException, FileNotFoundException {
        if (this.model == null){
            this.model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "JourneyCombinationModel");
            //this.model.set(GRB.IntParam.LogToConsole, 0); //removes print of gurobi
        }
        else if(optimstatus == 9){
            this.model.dispose();
            this.env.dispose();
            env = new GRBEnv(true);
            this.env.set("logFile",  "JourneyBasedModel.log");
            this.env.start();
            this.model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "JourneyCombinationModel");
            //this.model.set(GRB.IntParam.LogToConsole, 0); //removes print of gurobi
        }
        else {
            this.model.dispose();
            this.model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "JourneyCombinationModel");
            //this.model.set(GRB.IntParam.LogToConsole, 0); //removes print of gurobi
        }

    }



    public void initializeParameters() throws GRBException {
        this.gamma = new GRBVar[dataMIP.numPeriods][dataMIP.numVehicleTypes][];
        this.u = new GRBVar[dataMIP.numPeriods][dataMIP.numCustomers][];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int c = 0; c < dataMIP.numCustomers; c++){
                this.u[p][c] = new GRBVar[dataMIP.numProductsPrCustomer[c]];
            }
        }
        this.q = new GRBVar[dataMIP.numPeriods][dataMIP.numCustomers][];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int c = 0; c < dataMIP.numCustomers; c++){
                this.q[p][c] = new GRBVar[dataMIP.numProductsPrCustomer[c]];
            }
        }
        this.qO = new GRBVar[dataMIP.numPeriods];


        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++) {
                gamma[d][vt] = new GRBVar[journeys[d][vt].size()];
                for (int j = 0; j < journeys[d][vt].size(); j++ ){
                    numJourneyVariables++;
                    String variable_name = String.format("gamma[%d][%d][%d]", d, vt, j);
                    gamma[d][vt][j] = model.addVar(0.0, 1.0, journeys[d][vt].get(j).getFitnessWithVehicleCost(globalOrderDistribution), GRB.BINARY, variable_name);

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
            for (int d = 0; d < dataMIP.numPeriods; d++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    String variable_name = String.format("q[%d][%d][%d]", d, i, m);
                    q[d][i][m] = model.addVar(0.0, DataMIP.upperBoundQuantity , 0, GRB.CONTINUOUS, variable_name);
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



    public void useOfVehicles() throws GRBException {
        // Constraint 5.68
        // if a journey is used, then the vehicle corresponding to that journey is considered used
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++){
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int j = 0; j < journeys[d][vt].size(); j++) {
                    lhs.addTerm(1, gamma[d][vt][j]);
                }
                String constraint_name = String.format("5.70 - Can only %d journeys for vehicle type %d period  %d", data.numberOfVehiclesInVehicleType[vt], vt, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, data.numberOfVehiclesInVehicleType[vt], constraint_name);
            }
        }
    }


    public void allowableVisits() throws GRBException {
        // Constraint 5.69
        // Allowable visits to customer on spesific day
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for(int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                    for (int j = 0; j < journeys[d][vt].size();j++ ){
                        for (Trip t : journeys[d][vt].get(j).trips){
                            if (t.customers.contains(i)){
                                lhs.addTerm(1, gamma[d][vt][j]);
                                break;
                            }
                            /*
                            for (int customer : t.customers){
                                if (customer == i){
                                    lhs.addTerm(1, gamma[d][vt][j]);
                                    break;
                                }
                            }
                             */
                        }
                    }
                }
                String constraint_name = String.format("5.71 - Legal delivery day %d for customer %d: %d (yes:1, no:0)", d, i, dataMIP.possibleDeliveryDays[d][i]);
                model.addConstr(lhs, GRB.EQUAL, dataMIP.possibleDeliveryDays[d][i], constraint_name);
            }
        }
    }


    public void capacityOfJourney() throws GRBException {  //evaluate this constraint
        // Constraint 5.70
        //Capacity constraint on each delivery
        double bigM = 0;
        for (VehicleType vt :dataMIP.vehicleTypes) {
            bigM = bigM < vt.capacity ? vt.capacity : bigM;
        }
        int multiplier = 1;
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++) {
                for (Journey j : journeys[d][vt]){
                    for (Trip t : j.trips){
                        multiplier = multiplier < t.customers.size() ? t.customers.size() : multiplier;
                    }
                }
            }
        }
        bigM *= multiplier;

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++) {
                for (int j = 0; j < journeys[d][vt].size(); j++) {
                    for (Trip t : journeys[d][vt].get(j).trips){
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        for (int i : t.customers){
                            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                                lhs.addTerm(1, q[d][i][m]);
                                //System.out.print("q["+ d + "][" + v + "]["+r+"]["+i+"]["+m+"] + " );
                            }
                        }

                        lhs.addTerm(bigM + Parameters.modelMipGap - dataMIP.vehicleTypes[vt].capacity  , gamma[d][vt][j]);


                        String constraint_name = String.format("5.72 -Connection gamma and q for day %d vehicle type %d. M = %f", d, vt, dataMIP.vehicleTypes[vt].capacity);
                        //System.out.println(" ");
                        //System.out.println(constraint_name);
                        model.addConstr(lhs, GRB.LESS_EQUAL, bigM , constraint_name);
                    }
                }
            }
        }
    }







    public void overtimeAtDepot() throws GRBException {
        // Constraint 5.74:
        // Overitme at the depot
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productQuantity[i][m] > 0)
                        lhs.addTerm(1.0, q[d][i][m]);
                }
            }
            lhs.addTerm(-1.0, qO[d]); // Add the over time variable for that day
            // Create name
            String constraint_name = String.format("5.74 -Overtime on day %d. OvertimeLimit %f ", d, dataMIP.overtimeLimit[d]);
            model.addConstr(lhs, GRB.LESS_EQUAL, dataMIP.overtimeLimit[d], constraint_name);
        }
    }

    public void fixedQOfNonDiv() throws GRBException {
        // Constraint 5.75:
        // If one choose to deliver a non-div good, than a certain Q must be delivered
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 0 && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(dataMIP.productQuantity[i][m], u[d][i][m]);
                        lhs.addTerm(-1, q[d][i][m]);
                        String constraint_name = String.format("5.75 -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, dataMIP.productQuantity[i][m], dataMIP.numProductsPrCustomer[i]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void boundsOnDivGood() throws GRBException {
        // Constraint 5.76 a):
        // Lower bound for delivery for non-div product. (same as before)
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int i = 0; i < dataMIP.numCustomers; i++) {
                for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                    if (dataMIP.productTypes[i][m] == 1 && dataMIP.productQuantity[i][m] > 0) {
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(-1, q[d][i][m]);

                        lhs.addTerm(dataMIP.minAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 a) -Min delivery of dividable product %d customer %d on day %d. Min amount: %f", m, i, d, dataMIP.minAmountDivProduct[i][m]);
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
                        lhs.addTerm(1, q[d][i][m]);
                        lhs.addTerm(-dataMIP.maxAmountDivProduct[i][m], u[d][i][m]);
                        String constraint_name = String.format("5.76 b) -Max delivery of div.product %d customer %d on day %d. Max amount %f", m, i, d, dataMIP.maxAmountDivProduct[i][m]);
                        // Activate the constraint
                        model.addConstr(lhs, GRB.LESS_EQUAL, 0, constraint_name);
                    }
                }
            }
        }
    }

    public void volumeOfTotalDelivery() throws GRBException {
        // Constraint 5.77
        // Demand of every product must be satisfied in the planning horizon (same as before)
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                if (dataMIP.productQuantity[i][m] > 0) {
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        lhs.addTerm(1, q[d][i][m]);
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
                    lhs.addTerm(-r.cost, gamma[d][v + 1][r.journeyId]);
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
                    lhs.addTerm(-r.numCustomers, gamma[d][v + 1][r.journeyId]);
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
                    lhs.addTerm(-r.numTrips, gamma[d][v + 1][r.journeyId]);
                }
                String constraint_name = String.format("5.88 Sym4 - Number of trips used for vehicle %d must be larger than vehicle %d in period %d and vehicle type %d", v, v + 1, d, dataMIP.vehicles[v + 1].vehicleType.type);
                model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
            }
        }
    }

    public void symmetryCar() throws GRBException {

        // Constrant 5.66 //// Can be used with all symetries
        System.out.println("------------------- Symmetri: trip --------------------");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++) {
                int counter = 0;
                int lastVehicle = -1;
                for (int v = 0; v < dataMIP.numVehicles; v++) {
                    if (dataMIP.vehicles[v].vehicleType.type != vt) {
                        continue;
                    }
                    if (counter >= dataMIP.vehicleTypes.length - 2) {  //todo check
                        continue;
                    }
                    if (lastVehicle == -1){
                        lastVehicle = v;
                        continue;
                    }
                    GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                    for (int j = 0; j < journeys[d][vt].size(); j++) {
                        lhs.addTerm(1, gamma[d][lastVehicle][j]);
                        lhs.addTerm(-1, gamma[d][v][j]);
                    }
                    String constraint_name = String.format("5.88 Sym1 - Cars in use for %d and %d for period %d and vehicle type " , lastVehicle, v, d, vt);
                    model.addConstr(lhs, GRB.GREATER_EQUAL, 0, constraint_name);
                    lastVehicle = v;
                }
            }
        }
    }

    private void fixationRemoveNonDeliveries() throws GRBException {
        for (int d = 0 ; d < data.numberOfPeriods; d++){
            for (int i = 0; i < data.numberOfCustomers; i++){
                if (data.customers[i].requiredVisitPeriod[d] == 0){
                    for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                        //lock quantity
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, q[d][i][m]);
                        String constraint_name = String.format("Fixation 1 - No quantity of div good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);

                        //lock delivery
                        lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, u[d][i][m]);
                        constraint_name = String.format("Fixation 1 - No delivery of div good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);

                    }

                }
            }
        }

    }


    public void activateConstraints() throws GRBException {
        // -------- Add constraints -------------

        //useOfVehiclesForEachCapacity();
        useOfVehicles();
        allowableVisits();
        capacityOfJourney();
        overtimeAtDepot();
        fixationRemoveNonDeliveries();


        // ------- Constraints form AFM ---------
        fixedQOfNonDiv();
        boundsOnDivGood();
        volumeOfTotalDelivery();
        constraint76();
        constraint77();

        constraint78();
        constraint79();




        // ----------------- Symmetry breaking constraints ------------

        // Four choices: none, car, cost, customers, trips
        if (!symmetry.equals("none")) {
            //symmetryCar();
        }



    }

    public void optimizeModel() throws GRBException {
        model.set(GRB.DoubleParam.MIPGap, Parameters.modelMipGap);
        model.set(GRB.DoubleParam.TimeLimit, Parameters.modelJCMTimeLimit);
        if (Parameters.removePresolve){
            model.set(GRB.IntParam.Presolve, 0);
        }
        model.optimize();
        model.get(GRB.DoubleAttr.Runtime);
        //System.out.println(GRB.Status.OPTIMAL);
        //System.out.println(GRB.DoubleAttr.Runtime);
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
        /*
        // Print lambda variables: choosing a path //
        System.out.println("Print of gamma-variables: If a vehicle  uses a path");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int j = 0; j < journeys[d][dataMIP.vehicles[v].vehicleType.type].size(); j++) {
                    if (Math.round(gamma[d][v][j].get(GRB.DoubleAttr.X)) == 1) {
                        System.out.println("Vehicle " + v + " on period " + d + " uses journey " + j);
                        System.out.println("And visits customers: ");
                        for (Trip t : journeys[d][dataMIP.vehicles[v].vehicleType.type].get(j).trips){
                            for (int i : t.customers){
                                System.out.print(" - " + i);
                            }
                            System.out.println(" #### ");
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
                for (int i = 0; i < dataMIP.numCustomers; i++) {
                    for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                        for (int r = 0; r < dataMIP.numTrips; r++) {
                            if (q[d][v][r][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                                System.out.println("Quantity " + q[d][v][r][i][m].get(GRB.DoubleAttr.X) +
                                        " of product " + m + " is delivered to " + "customer " +
                                        i + " with vehicle " + v + " on period " + d + " on trip " + r + " journey: see later" );
                            }
                        }
                    }
                }
                for (int j = 0; j < journeys[d][dataMIP.vehicles[v].vehicleType.type].size(); j++){
                    if (gamma[d][v][j].get(GRB.DoubleAttr.X) > 0.8){
                        System.out.print(" Day " + d + " vehicle " + v  + " uses journey " + j );
                        for (Trip t : journeys[d][dataMIP.vehicles[v].vehicleType.type].get(j).trips){
                            System.out.print(t.customers.toString());
                        }
                        System.out.println(" ###### ");

                    }
                }
            }
        }
        System.out.println("   ");
        System.out.println("   ");



        System.out.println("Print of q-variables: Quantity of m delivered to store i");
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                if (dataMIP.productTypes[i][m] == 0){
                    double load = 0;
                    for (int d = 0; d < dataMIP.numPeriods; d++) {
                        for (int v = 0; v < dataMIP.numVehicles; v++) {
                            for (int r = 0; r < dataMIP.numTrips; r++) {
                                load += q[d][v][r][i][m].get(GRB.DoubleAttr.X);
                            }
                        }
                    }
                    System.out.println("The load is " + load + "and should be " + dataMIP.productQuantity[i][m]);
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        //Create qO (overtime) variables
        System.out.println("Print of qO-variables: Overtime at the depot");
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is " +
                        qO[d].get(GRB.DoubleAttr.X) );
            }
        }

         */

        System.out.println("  ");
        System.out.println("  ");

        System.out.println("Print of orders:");
        for (int i = 0; i < dataMIP.numCustomers; i++) {
            for (int m = 0; m < dataMIP.numProductsPrCustomer[i]; m++) {
                double sum = 0;
                for (int d = 0; d < dataMIP.numPeriods; d++) {
                    sum += q[d][i][m].get(GRB.DoubleAttr.X);
                    if (q[d][i][m].get(GRB.DoubleAttr.X) > 0.00001) {
                        System.out.print("C: " + i + "OrderID : " + data.customers[i].orders[m].orderID + " m: " + m + "volume: ");
                        System.out.println(q[d][i][m].get(GRB.DoubleAttr.X));
                    }
                }
                System.out.println("C: " + i + " m" + m + " is " + sum + " should be " + dataMIP.productQuantity[i][m] + " true value " + data.customers[i].orders[m].volume + " name: " + data.customers[i].customerName);
            }
        }




    }



    public double runModel(ArrayList<Master2020.Individual.Journey>[][] journeys) {
        try {
            double time = System.currentTimeMillis();
            this.journeys = journeys;
            SolutionTest.checkJourneyLength(journeys, dataMIP);
            SolutionTest.checkJourneyForTimeWarp(journeys,dataMIP,globalOrderDistribution);
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
                    if (Parameters.verboseJourneyCombination)
                        printSolution();
                    this.MIPGap = model.get(GRB.DoubleAttr.MIPGap);
                    runTime = (System.currentTimeMillis() - time)/1000;
                }
                else if (model.get(GRB.IntAttr.SolCount) > 0) {
                    optimstatus = 2;
                    System.out.println("Create and store results");
                    feasible = true;
                    optimal = false;
                    createIndividualAndOrderDistributionObject();
                    if (Parameters.verboseJourneyCombination)
                        printSolution();
                    System.out.println("Terminate model");
                    this.MIPGap = model.get(GRB.DoubleAttr.MIPGap);
                    runTime = (System.currentTimeMillis() - time)/1000;
                }
                else {
                    optimstatus = -1; //  no solution found
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
        ArrayList<Master2020.Individual.Journey>[][] optimalJourneys = new ArrayList[dataMIP.numPeriods][dataMIP.numVehicleTypes];
        for (int p = 0; p < dataMIP.numPeriods; p++){
            for (int vt = 0; vt < dataMIP.numVehicleTypes; vt++){
                optimalJourneys[p][vt] = new ArrayList<Master2020.Individual.Journey>();
                for (int j = 0; j < journeys[p][vt].size(); j++){
                    if (Math.round(gamma[p][vt][j].get(GRB.DoubleAttr.X)) == 1){
                        optimalJourneys[p][vt].add(journeys[p][vt].get(j));
                    }
                }

            }
        }
        return optimalJourneys;
    }

    public void createIndividualAndOrderDistributionObject() throws GRBException {
        this.individual = new Individual(dataMIP.newData, new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty, Parameters.frequencyOfPenaltyUpdatesPGA));
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        //todo: change objective to be a cost of taking a journey
        ModelConverter.initializeOrderDistributionFromModel(this);
        this.individual.setFitness(this.model.get(GRB.DoubleAttr.ObjVal));
    }

    public void createEmptyIndividualAndOrderDistribution() throws GRBException {
        this.individual = new Individual(dataMIP.newData, new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty, Parameters.frequencyOfPenaltyUpdatesPGA));
        this.orderDistribution = new OrderDistribution(dataMIP.newData);
        this.individual.setOrderDistribution(orderDistribution);
    }


    public static void main (String[] args) throws IOException, GRBException {
        Data data = Master2020.DataFiles.DataReader.loadData();
        JourneyCombinationModel jcm = new JourneyCombinationModel(data);
        //jcm.runModel(Master2020.DataFiles.Parameters.symmetry);
        Individual individual = jcm.getIndividual();
    }
}
