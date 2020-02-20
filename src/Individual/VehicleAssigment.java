package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Spliterator;

public class VehicleAssigment {

    HashMap[][] chromosome;
    Data data;

    VehicleAssigment(Data data){
        this.data = data;
        chromosome = new HashMap[data.numberOfPeriods][data.numberOfVehicleTypes];
    }

    public void setChromosome( HashMap<Integer, Integer> chromosome , int periodID, int vehicleTypeID) {
        this.chromosome[periodID][vehicleTypeID] = chromosome;
    }

}
