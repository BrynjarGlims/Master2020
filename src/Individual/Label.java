package Individual;

import java.util.ArrayList;

public class Label {

    public ArrayList<Integer> vehicleTravelTime;
    public int loadInfeasability;
    public Label parentNode;
    public double cost;

    public Label(ArrayList<Integer> vehicleTravelTime, int loadInfeasability, Label parentNode, double cost){

        this.vehicleTravelTime = vehicleTravelTime;
        this.loadInfeasability = loadInfeasability;
        this.parentNode = parentNode;
        this.cost = cost;
    }
}
