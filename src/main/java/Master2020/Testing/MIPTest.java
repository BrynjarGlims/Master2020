package Master2020.Testing;

import Master2020.DataFiles.Parameters;
import Master2020.PR.ArcFlowModel;
import Master2020.PR.DataMIP;
import gurobi.GRB;
import gurobi.GRBException;

public class MIPTest {
    public static void printZSolutions(ArcFlowModel afm) throws GRBException {
        DataMIP dataMIP = afm.dataMIP;
        for (int d = 0; d < 6; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    if (Math.round(afm.z[d][v][r].get(GRB.DoubleAttr.X)) == 1){
                        System.out.println(afm.z[d][v][r].get(GRB.StringAttr.VarName));
                        for (int i = 0; i < dataMIP.numCustomers; i++){
                            if (Math.round(afm.x[d][v][r][dataMIP.numCustomers][i].get(GRB.DoubleAttr.X)) == 1){
                                System.out.println(afm.x[d][v][r][dataMIP.numCustomers][i].get(GRB.StringAttr.VarName));
                            }
                        }
                        System.out.println("------------------------------");

                    }

                }
            }
        }
    }

    public static void getDetailedResult(ArcFlowModel afm) throws GRBException {
        DataMIP dataMIP = afm.dataMIP;
        double travelValue = 0;
        double vehicleUseValue = 0;
        double overtimeAtDepot = 0;
        double objectiveValue = 0;
        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                for (int r = 0; r < dataMIP.numTrips; r++) {
                    for (int i = 0; i < dataMIP.numNodes; i++) {
                        for (int j = 0; j < dataMIP.numNodes; j++) {
                            travelValue += Math.round(afm.x[d][v][r][i][j].get(GRB.DoubleAttr.X))*dataMIP.travelTime[i][j]*dataMIP.vehicles[v].vehicleType.drivingCost;
                        }
                    }
                }
            }
        }

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            for (int v = 0; v < dataMIP.numVehicles; v++) {
                vehicleUseValue += Math.round(afm.z[d][v][0].get(GRB.DoubleAttr.X)) * dataMIP.vehicles[v].vehicleType.unitCost;
            }
        }

        for (int d = 0; d < dataMIP.numPeriods; d++) {
            vehicleUseValue += afm.qO[d].get(GRB.DoubleAttr.X) * Parameters.overtimeCost[d];
        }

        objectiveValue = travelValue + vehicleUseValue + overtimeAtDepot;
        System.out.println("Objective value: " + objectiveValue);
        System.out.println("Travel Cost: " + travelValue);
        System.out.println("Vehicle Cost: " + vehicleUseValue );
        System.out.println("Overtime at depot cost: " + overtimeAtDepot);


    }

    public static  void checkSpesificCase(ArcFlowModel afm) throws GRBException {
        DataMIP dataMIP = afm.dataMIP;
        for (int i = 0; i < dataMIP.numCustomers; i++){
            System.out.println(afm.x[4][2][0][7][i].get(GRB.StringAttr.VarName) + " = " + afm.x[4][2][0][7][i].get(GRB.DoubleAttr.X));
        }
    }
}
