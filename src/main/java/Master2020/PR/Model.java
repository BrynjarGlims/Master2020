package Master2020.PR;

import Master2020.DataFiles.Data;
import Master2020.Individual.Individual;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBVar;

import java.util.ArrayList;

abstract public class Model {

    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[] qO;
    public DataMIP dataMIP;
    private OrderDistribution orderDistribution;
    private Individual individual;
    private ArrayList<Master2020.Individual.Journey>[][] journeys;

    public Model(DataMIP dataMIP){
        this.dataMIP = dataMIP;
    }

    public ArrayList<Master2020.Individual.Journey>[][] getJourneys(){
        return journeys;
    }

    public abstract Individual getIndividual();
    public abstract OrderDistribution getOrderDistribution();
}
