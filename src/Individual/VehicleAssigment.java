package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Spliterator;

public class VehicleAssigment {

    ArrayList<Integer>[][] chromosome;
    Data data;

    VehicleAssigment(Data data){
        this.data = data;
        chromosome = new ArrayList[data.numberOfPeriods][data.numberOfVehicleTypes];
        initialize();
    }

    private void initialize(){
        for (int p = 0; p < data.numberOfPeriods; p++){
            for (int vt = 0; vt < data.numberOfVehicleTypes; vt++){
                chromosome[p][vt] = new ArrayList<Integer>();
            }
        }
    }

    public void VehicleAssigment( ArrayList<Integer> prevChromosome, int p, int vt){

        chromosome[p][vt] = prevChromosome;

    }

    public void addVehicle( int vehicleID, int p, int vt){
        chromosome[p][vt].add(vehicleID);
    }

    public void setChromosome(ArrayList<Integer> chromosome, int periodID, int vehicleTypeID) {
        this.chromosome[periodID][vehicleTypeID] = chromosome;
    }

    public ArrayList<Integer>[][] getChromosome() {
        return chromosome;
    }


}
