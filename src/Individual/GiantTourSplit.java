package Individual;
import DataFiles.*;

import java.util.ArrayList;

public class GiantTourSplit {

    public ArrayList<Integer>[][] chromosome;
    Data data;

    GiantTourSplit(Data data){
        this.data = data;
        this.chromosome = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
    }


    public void initialize(){
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                chromosome[p][vt] = new ArrayList<Integer>();
            }
        }
    }

    public void setChromosome(ArrayList<Integer> tripSplit, int p, int vt) {
        this.chromosome[p][vt] = tripSplit;
    }
}
