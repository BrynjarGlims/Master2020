package Master2020.MIP;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Individual.GiantTour;
import Master2020.Individual.Individual;
import Master2020.Individual.Journey;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.*;
import Master2020.Individual.Trip;
import org.nustaq.kson.KsonStringOutput;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class OrderAllocationModel {


    private OrderDistribution orderDistribution;
    private ArrayList<Journey>[][] journeys;

    private GRBEnv env;
    private GRBModel model;
    private Data data;
    private int optimstatus;
    private double objval;
    private String symmetry;

    // derived variables
    private int numVehiclesUsed = 0;
    private double volumeOvertime = 0;

    //variables
    public GRBVar[][][] uND;
    public GRBVar[][][] uD;
    public GRBVar[][][] qND;
    public GRBVar[][][] qD;
    public GRBVar[] qO;

    public OrderAllocationModel(Data data) throws GRBException {
        env = new GRBEnv(true);
        this.env.start();
        this.data = data;
    }

    private void initializeModel() throws GRBException, FileNotFoundException {
        if (this.model == null){
            this.model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "OrderAllocationModel");
            this.model.set(GRB.IntParam.LogToConsole, 0); //removes print of gurobi
        }
        else{
            this.model.dispose();
            this.model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "OrderAllocationModel");
            this.model.set(GRB.IntParam.LogToConsole, 0); //removes print of gurobi
        }
    }


    private void initializeParameters() throws GRBException {
        createParameters();
        initializeUVariables();
        initializeQVariables();
        initializeQOVariables();
    }


    private void createParameters() {
        //this.y = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers];
        this.uD = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.uND = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.qD = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.qND = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];// TODO: 02.03.2020 Remember to set all other quantities to zero
        this.qO = new GRBVar[data.numberOfPeriods];

        for (int period = 0; period < data.numberOfPeriods; period++) {
            for (int customerID = 0; customerID < data.numberOfCustomers; customerID++) {
                this.uND[period][customerID] = new GRBVar[data.customers[customerID].numberOfNonDividableOrders];
                this.uD[period][customerID] = new GRBVar[data.customers[customerID].numberOfDividableOrders];
            }
        }

        for (int period = 0; period < data.numberOfPeriods; period++) {
            for (int customerID = 0; customerID < data.numberOfCustomers; customerID++) {
                this.qD[period][customerID] = new GRBVar[data.customers[customerID].numberOfDividableOrders];
                this.qND[period][customerID] = new GRBVar[data.customers[customerID].numberOfNonDividableOrders];

            }
        }
    }

    private void initializeUVariables() throws GRBException {

        // Create uND variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    String variable_name = String.format("uND[%d][%d][%d]", d, i, m);
                    uND[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }

        // Create uD variables:
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    String variable_name = String.format("uD[%d][%d][%d]", d, i, m);
                    uD[d][i][m] = model.addVar(0.0, 1.0, 0, GRB.BINARY, variable_name);
                }
            }
        }
    }

    private void initializeQVariables() throws GRBException {

        //Create qND variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    String variable_name = String.format("qND[%d][%d][%d]", d, i, m);
                    qND[d][i][m] = model.addVar(0.0, Parameters.upperBoundQuantity, 0, GRB.CONTINUOUS, variable_name);
                    //todo: set upper bound quantity correctly
                }
            }
        }


        //Create qD variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    String variable_name = String.format("qD[%d][%d][%d]", d,  i, m);
                    qD[d][i][m] = model.addVar(0.0, Parameters.upperBoundQuantity, 0, GRB.CONTINUOUS, variable_name);
                    //todo: set upper bound quantity correctly
                }
            }
        }
    }


    private void initializeQOVariables() throws GRBException {

        //Create qO (overtime) variables
        for (int d = 0; d < data.numberOfPeriods; d++) {
            String variable_name = String.format("qO[%d]", d);
            qO[d] = model.addVar(0.0, Parameters.upperBoundOvertime, Parameters.overtimeCost[d], GRB.CONTINUOUS, variable_name);
        }


    }

    private void setObjective() throws GRBException {
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // TODO: 20.11.2019 Change objective
    }


    private void terminateModel() throws GRBException {

    }




    private void capacityOfVehicleAtTrip() throws GRBException {
        // Constraint 5.4: Overtime constraint at the warehouse if the goods delivered is higher
        // than the overtime limit
        GRBLinExpr lhsQ = new GRBLinExpr();

        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                if (journeys[d][vt].isEmpty()) {
                    continue;
                }
                for (Journey journey : journeys[d][vt]){
                    for (Trip trip : journey.trips) {
                        lhsQ = new GRBLinExpr();
                        for (int i : trip.customers) {
                            for (int m = 0; m < data.customers[i].dividableOrders.length; m++) {
                                lhsQ.addTerm(1.0, qD[d][i][m]);
                            }
                            for (int m = 0; m < data.customers[i].nonDividableOrders.length; m++) {
                                lhsQ.addTerm(1.0, qND[d][i][m]);
                            }
                        }
                        String constraint_name = String.format("1. - Capacity of a trip with vehicle type %d at period %d. Capacity %f ", vt, d, data.vehicleTypes[vt].capacity);
                        model.addConstr(lhsQ, GRB.LESS_EQUAL, data.vehicleTypes[vt].capacity, constraint_name);
                    }
                }
            }
        }
    }


    private void capacityOvertimeAtDepot() throws GRBException {
        // Constraint 5.4: Overtime constraint at the warehouse if the goods delivered is higher
        // than the overtime limit

        for (int d = 0; d < data.numberOfPeriods; d++) {
            GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt ++) {  // TODO: 03.03.2020 Can be changed
                for (Journey journey : journeys[d][vt]){
                    for (Trip trip : journey.trips){
                        for (int customerID : trip.customers) {
                            for (int m = 0; m < data.customers[customerID].numberOfDividableOrders; m++) {
                                lhs.addTerm(1.0, qD[d][customerID][m]);
                            }
                            for (int m = 0; m < data.customers[customerID].numberOfNonDividableOrders; m++) {
                                lhs.addTerm(1.0, qND[d][customerID][m]);
                            }
                        }
                    }

                }
            }
            lhs.addTerm(-1.0, qO[d]); // Add the over time variable for that day
            String constraint_name = String.format("2 -Overtime on day %d. OvertimeLimit %f ", d, Data.overtimeLimit[d]);
            model.addConstr(lhs, GRB.LESS_EQUAL, Data.overtimeLimit[d], constraint_name);
        }
    }



    private void quantityNonDividableDelivery() throws GRBException {
        // Constraint 5.5: If one choose to deliver a non-div good, than a certain Q must be delivered
        // than the overtime limit
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt ++) {
                for (Journey journey : journeys[d][vt]){
                    for (Trip trip : journey.trips){
                        for (int i : trip.customers) {
                            for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                                lhs.addTerm(data.customers[i].nonDividableOrders[m].volume, uND[d][i][m]);
                                lhs.addTerm(-1, qND[d][i][m]);
                                String constraint_name = String.format("3. -Fixed quantity for store %d of product %d on day %d. Fixed quantitiy %f. Number of products: %d", i, m, d, data.customers[i].nonDividableOrders[m].volume, data.customers[i].numberOfNonDividableOrders);
                                // Activate the constraint
                                model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                            }
                        }
                    }
                }
            }
        }
    }


    private void quantityDividableBounds() throws GRBException {
        // Constraint 5.7 a): Lower bound for delivery for div product.
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++) {
                for (Journey journey : journeys[d][vt]){
                    for (Trip trip : journey.trips){

                        for (int i : trip.customers) {
                            for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                                GRBLinExpr lhsU = new GRBLinExpr();
                                GRBLinExpr lhsL = new GRBLinExpr();
                                lhsL.addTerm(-1, qD[d][i][m]);
                                lhsL.addTerm(data.customers[i].dividableOrders[m].minVolume, uD[d][i][m]);
                                String constraint_name = String.format("4a -Min delivery of dividable product %d customer %d on " +
                                        "day %d. Min amount: %f", m, i, d, data.customers[i].dividableOrders[m].minVolume);
                                // Activate the constraint
                                model.addConstr(lhsL, GRB.LESS_EQUAL, 0, constraint_name);
                                lhsU.addTerm(1, qD[d][i][m]);
                                lhsU.addTerm(-data.customers[i].dividableOrders[m].maxVolume, uD[d][i][m]);
                                constraint_name = String.format("4b -Max delivery of div.product %d customer %d on day %d. " +
                                        "Max amount %f", m, i, d, data.customers[i].dividableOrders[m].maxVolume);
                                // Activate the constraint
                                model.addConstr(lhsU, GRB.LESS_EQUAL, 0, constraint_name);
                            }
                        }
                    }
                }
            }
        }
    }


    private void quantityTotalDemandDelivered() throws GRBException {
        // Constraint 5.8: Demand of every product must be satisfied in the planning horizon
        for (int i = 0; i < data.customers.length; i++) {
            for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int d = 0; d < data.numberOfPeriods; d++) {
                    lhs.addTerm(1, qD[d][i][m]);
                }
                String constraint_name = String.format("5 - Total delivery of div product %d to customer %d. Quantity %f", m, i, data.customers[i].dividableOrders[m].volume);
                model.addConstr(lhs, GRB.EQUAL, data.customers[i].dividableOrders[m].volume, constraint_name);
            }
        }
    }

    private void quantitySingleDeliveryOfNonDivCommodity() throws GRBException {
        // Constraint 5.21: Only one non-div product is delivered to the store. // TODO: 23.11.2019 Change numbering to correct
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    lhs.addTerm(1, uND[d][i][m]);
                }
                // Activate the constraint
                String constraint_name = String.format("6 -Only one nondiv product for customer %d on day %d", i, d);
                model.addConstr(lhs, GRB.LESS_EQUAL, 1, constraint_name);
            }
        }
    }

    private void quantityDeliveryRequirementNonDivCommodity() throws GRBException {
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

    private void quantityMinFrequencyDivCommodity() throws GRBException {
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

    private void quantityMaxFrequencyDivCommodity() throws GRBException {
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

    private void fixationRemoveNonDeliveries() throws GRBException {
        for (int d = 0 ; d < data.numberOfPeriods; d++){
            for (int i = 0; i < data.numberOfCustomers; i++){
                if (data.customers[i].requiredVisitPeriod[d] == 0){
                    for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                        //lock quantity
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, qD[d][i][m]);
                        String constraint_name = String.format("Fixation 1 - No quantity of div good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);

                        //lock delivery
                        lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, uD[d][i][m]);
                        constraint_name = String.format("Fixation 1 - No delivery of div good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);

                    }
                    for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                        //lock quantity
                        GRBLinExpr lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, qND[d][i][m]);
                        String constraint_name = String.format("Fixation 1 - No quantity of nondiv good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);

                        //lock delivery
                        lhs = new GRBLinExpr();  //Create the left hand side of the equation
                        lhs.addTerm(1, uND[d][i][m]);
                        constraint_name = String.format("Fixation 1 - No delivery of nondiv good delivered, period %d customer %d product %d", d,i,m);
                        model.addConstr(lhs, GRB.EQUAL, 0, constraint_name);
                    }
                }
            }
        }

    }




    private void activateConstraints() throws GRBException {
        // -------- Add constraints -------------
        // 5.2 is implemented in variable declaration
        // 5.6 is implemented in variable declaration


        //capacityConstraint
        capacityOfVehicleAtTrip();
        capacityOvertimeAtDepot();

        //quantityConstraints
        quantityNonDividableDelivery();
        quantityDividableBounds();
        quantityTotalDemandDelivered();
        quantitySingleDeliveryOfNonDivCommodity();
        quantityDeliveryRequirementNonDivCommodity();


        // TODO: 02.03.2020 Frequency Constraints to be added. 
        //quantityMinFrequencyDivCommodity();
        //quantityMaxFrequencyDivCommodity();

        //fixation
        fixationRemoveNonDeliveries();



    }

    private void optimizeModel() throws GRBException {
        //if gap is needed to be changed:
        model.set(GRB.DoubleParam.MIPGap, Parameters.modelMipGap);
        model.set(GRB.DoubleParam.TimeLimit, Parameters.modelTimeLimit);
        model.optimize();
        model.get(GRB.DoubleAttr.Runtime);
        this.optimstatus = model.get(GRB.IntAttr.Status);
    }

    private void displayResults( boolean ISS ) throws GRBException {
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

    private void printSolution() throws GRBException {
        // Create uND variables: if a product is delivered to customer m
        System.out.println("Print of uND-variables: If a product m is delivered to customer i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    if (uND[d][i][m].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Non-diviable Product " + data.customers[i].nonDividableOrders[m].orderID + " in customer " + i + " is delivered on day " + d);
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("   ");

        int numberOfNonDivProd = 0;
        // Create uD variables: if a product is delivered to customer m
        System.out.println("Print of uD-variables: If a product m is delivered to customer i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    if (uD[d][i][m].get(GRB.DoubleAttr.X) == 1) {
                        System.out.println("Diviable Product " + data.customers[i].dividableOrders[m].orderID + " in customer " + i + " is delivered on day " + d);
                        numberOfNonDivProd++;
                    }
                }
            }
        }

        System.out.println("   ");
        System.out.println("Number of dividable products: " +  numberOfNonDivProd);
        System.out.println("   ");

        //Create q variables: Quantity of m delivered to store i
        System.out.println("Print of q-variables: Quantity of m delivered to store i");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            for (int i = 0; i < data.numberOfCustomers; i++) {
                double quantitiyCust = 0 ;
                for (int m = 0; m < data.customers[i].numberOfDividableOrders; m++) {
                    if (qD[d][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                        System.out.println("Dividable Quantity " + (double) Math.round(qD[d][i][m].
                                get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " +data.customers[i].dividableOrders[m].orderID +
                                " is delivered to customer " + i + " on day " + d);
                        quantitiyCust += qD[d][i][m].get(GRB.DoubleAttr.X);
                    }
                }
                for (int m = 0; m < data.customers[i].numberOfNonDividableOrders; m++) {
                    if (qND[d][i][m].get(GRB.DoubleAttr.X) >= 0.001) {
                        System.out.println("Nondividable Quantity " + (double) Math.round(qND[d][i][m].
                                get(GRB.DoubleAttr.X) * 1000d) / 1000d + " of product " + data.customers[i].nonDividableOrders[m].orderID +
                                " is delivered to customer " + i + " on day " + d);
                        quantitiyCust += qND[d][i][m].get(GRB.DoubleAttr.X);
                    }
                }
                if (quantitiyCust >= 0.0001 ){
                    //System.out.println("Quantity for customer "+ i + " is :" + quantitiyCust);
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

    private void initializeODObject() throws GRBException {
        this.orderDistribution.makeDistributionFromOrderAllocationModel(this);
    }

    private OrderDistribution createODFromMIP(ArrayList<Journey>[][] journeys) {
        try {
            this.journeys = journeys;
            this.orderDistribution = new OrderDistribution(data);
            initializeModel();
            initializeParameters();
            setObjective();
            activateConstraints();
            optimizeModel();
            if (optimstatus == 3) {
                if (Parameters.verbose){
                    System.out.println("No optimal od found");
                }
            }
            else if (optimstatus == 2){
                initializeODObject();
            }
            else{
                System.out.println("Unkonwn optimization status");
            }
            return this.orderDistribution;

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

    public void terminateEnvironment(){
        try {
            this.model.dispose();
            this.env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }



    //MASTER FUNCTION
    public OrderDistribution createOptimalOrderDistribution(ArrayList<Journey>[][] journeys){
        return this.createODFromMIP(journeys);
    }

    public static void main(String[] args) throws GRBException {
        Data data = DataReader.loadData();
        OrderDistribution orderDistribution = new OrderDistribution(data);
        orderDistribution.makeInitialDistribution();
        Individual individual = new Individual(data);
        individual.setOrderDistribution(orderDistribution);
        individual.giantTour.initializeGiantTour();
        OrderAllocationModel oam = new OrderAllocationModel(data);
        OrderDistribution od = oam.createOptimalOrderDistribution(individual.journeyList);
        System.out.println(od);



    }


}

