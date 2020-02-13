package Individual;

import java.util.ArrayList;

public class Label {

    public int[] vehicleTravelTime;
    public double loadInfeasibility;
    public Label parentNode;
    public double cost;

    public Label(int[] vehicleTravelTime, double loadInfeasibility, Label parentNode){

        this.vehicleTravelTime = vehicleTravelTime;
        this.loadInfeasibility = loadInfeasibility;
        this.parentNode = parentNode;
        this.deriveCost();

    }

    public void deriveCost(){
        this.cost = 0; //todo: implement

    }
}
