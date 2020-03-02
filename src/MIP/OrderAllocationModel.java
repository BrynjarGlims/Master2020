package MIP;

import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Parameters;
import Individual.Individual;
import Individual.AdSplit;
import gurobi.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class OrderAllocationModel {


    public GRBEnv env;
    public GRBModel model;
    public Data data;
    public Result result;
    public int optimstatus;
    public double objval;
    public String symmetry;

    // derived variables
    public int numVehiclesUsed = 0;
    public double volumeOvertime = 0;

    //variables
    public GRBVar[][][][]y;  //this is a constant, and redundant.
    public GRBVar[][][] uND;
    public GRBVar[][][] uD;
    public GRBVar[][][][][] qND;
    public GRBVar[][][][][] qD;
    public GRBVar[] qO;


    public void initializeModel() throws GRBException, FileNotFoundException {
        env = new GRBEnv(true);
        this.env.set("logFile",  "OrderAllocationModel.log");
        this.env.start();
        this.model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "ArcFlowModel");
        this.data = DataReader.loadData();
    }


    public void initializeParameters() throws GRBException {
        createParameters();
        initializeYVariables();
        initializeUVariables();
        initializeQVariables();
        initializeQOVariables();

    }


    public void createParameters() {
        this.y = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers];
        this.uD = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.uND = new GRBVar[data.numberOfPeriods][data.numberOfCustomers][];
        this.qD = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers][];
        this.qND = new GRBVar[data.numberOfPeriods][data.numberOfVehicles][data.numberOfTrips][data.numberOfCustomers][];
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
            String constraint_name = String.format("5.4 -Overtime on day %d. OvertimeLimit %d ", d, Parameters.overtimeLimit[d]);
            // Create constraint and defind RHS
            model.addConstr(lhs, GRB.LESS_EQUAL, Parameters.overtimeLimit[d], constraint_name);
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
                            lhs.addTerm(1, qD[d][v][r][i][m]);
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




    public void activateConstraints(String symmetry) throws GRBException {
        // -------- Add constraints -------------
        // 5.2 is implemented in variable declaration
        // 5.6 is implemented in variable declaration


        //capacityConstraint
        capacityVehicle();
        capacityOvertimeAtDepot();

        //flowConstraints

        flowAllowableVisitPeriods();
        flowDeliveryQuantityRestriction();

        //quantityConstraints
        quantityNonDividableDelivery();
        quantityDividableBounds();
        quantityTotalDemandDelivered();
        quantitySingleDeliveryOfNonDivCommodity();
        quantityDeliveryRequirementNonDivCommodity();
        quantityMinFrequencyDivCommodity();
        quantityMaxFrequencyDivCommodity();

        //symmetry breaking constraints
        if (symmetry.equals("none")){
            System.out.println("----------------------------No symmetry chosen----------------------------------------");
        }
        else {
            System.out.println("-----------------------Using symmetry : " + symmetry + " (not standard) -------------------------");
            System.out.println("Only simple symmetry breaking chosen");
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



        //Create qO (overtime) variables
        System.out.println("Print of qO-variables: Overtime at the depot");
        for (int d = 0; d < data.numberOfPeriods; d++) {
            if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                System.out.println("On day " + d + " the overtime incurred at the warehouse is "
                        + (double)Math.round(qO[d].get(GRB.DoubleAttr.X) * 1000d) / 1000d );
            }
        }
    }







    public void calculateResultValues() throws GRBException {
        if (optimstatus == 2){
            for (int d = 0; d < data.numberOfPeriods; d++) {
                if (qO[d].get(GRB.DoubleAttr.X) >= 0.001) {
                    volumeOvertime += qO[d].get(GRB.DoubleAttr.X);
                }
            }
        }


    }

    public Result runModel(Individual individual) {
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
                return null;
            }
            else if (optimstatus == 2){
                if (Parameters.plotArcFlow){
                    GraphPlot plotter = new GraphPlot(data);
                    plotter.visualize(true);
                }

                System.out.println("Create and store results");
                printSolution();
                System.out.println("Terminate model");
                terminateModel();
                return null;
            }
            else{
                System.out.println("Create and store results");
                printSolution();
                System.out.println("Terminate model");
                terminateModel();
                return null;
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

    public static void main(String[] args) {
        Individual individual = Individual.makeIndividual();
        AdSplit.adSplitPlural(individual);
        individual.updateFitness();
        OrderAllocationModel orderAllocationModel = new OrderAllocationModel();
        orderAllocationModel.runModel(individual);

    }

}

