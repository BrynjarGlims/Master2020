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


    public double[] parsePosition(int size){
        double[] position = new double[size];
        for (int vt = 0 ; vt < parsedIndices.length ; vt++){
            for (int i = 0 ; i <  parsedIndices[vt].size() ; i++){
                position[parsedIndices[vt].get(i)] = parsedPosition[vt].get(i);
            }
        }
        return position;
    }
}
