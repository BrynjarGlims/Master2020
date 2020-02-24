package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Spliterator;

public class VehicleAssigment {

    HashMap[] chromosome;
    Data data;

    VehicleAssigment(Data data){
        this.data = data;
        chromosome = new HashMap[data.numberOfPeriods];
        for (int p = 0; p < data.numberOfPeriods; p++){
            chromosome[p] = new HashMap<Integer, Integer>();
        }
    }

    public void setChromosome( HashMap<Integer, Integer> chromosome , int periodID ) {
        this.chromosome[periodID] = chromosome;
    }

}
