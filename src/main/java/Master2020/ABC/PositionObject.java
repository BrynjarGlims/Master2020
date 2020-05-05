package Master2020.ABC;

import java.util.ArrayList;

public class PositionObject {

    public ArrayList<Integer>[] customerVisits;
    public ArrayList<Double>[] parsedPosition;
    public ArrayList<Integer>[] parsedIndices;

    public PositionObject(ArrayList<Integer>[] customerVisits, ArrayList<Double>[] parsedPosition, ArrayList<Integer>[] parsedIndices){
        this.customerVisits = customerVisits;
        this.parsedIndices = parsedIndices;
        this.parsedPosition = parsedPosition;
    }
}
