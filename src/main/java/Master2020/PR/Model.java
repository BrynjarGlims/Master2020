package Master2020.PR;

import Master2020.DataFiles.Data;
import Master2020.Individual.Individual;
import Master2020.MIP.DataConverter;
import Master2020.ProductAllocation.OrderDistribution;
import gurobi.GRBVar;

import java.util.ArrayList;

abstract public class Model {

    public GRBVar[][][] u;
    public GRBVar[][][][][] q;
    public GRBVar[] qO;
    public DataMIP dataMIP;
    public Data data;
    private OrderDistribution orderDistribution;
    private Individual individual;
    private ArrayList<Master2020.Individual.Journey>[][] journeys;

    public Model(Data data){
        this.data = data;
        this.dataMIP = DataConverter.convert(data);
    }

    public abstract ArrayList<Master2020.Individual.Journey>[][] getJourneys();
    public abstract Individual getIndividual();
    public abstract OrderDistribution getOrderDistribution();
}
