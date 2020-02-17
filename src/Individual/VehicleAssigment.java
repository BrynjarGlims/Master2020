package Individual;
import DataFiles.*;

import java.util.ArrayList;
import java.util.Spliterator;

public class VehicleAssigment {

    ArrayList<Integer> chromosome;

    VehicleAssigment(){
        chromosome = new ArrayList<Integer>();
    }

    VehicleAssigment( ArrayList<Integer> prevChromosome){
        chromosome = prevChromosome;

    }

    public void addVehicle( int vehicleID){
        chromosome.add(vehicleID);
    }

    public ArrayList<Integer> getChromosome() {
        return chromosome;
    }
}
