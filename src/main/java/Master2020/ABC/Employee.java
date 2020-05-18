package Master2020.ABC;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.PenaltyControl;
import Master2020.ProductAllocation.OrderDistribution;

import java.util.HashMap;

public class Employee extends Bee {


    public HashMap<Onlooker, Double> onlookerFitnesses;
    public int trials;

    public double[] bestPosition;
    public double bestFitness = Double.MAX_VALUE;


    public Employee(Data data, int period, PeriodSwarm colony, PenaltyControl penaltyControl) {
        super(data, period, colony, penaltyControl);
        onlookerFitnesses = new HashMap<>();
        scout();
    }



    public double[] scout(){
        if (getFitness(this.position) < bestFitness){
            this.bestPosition = this.position.clone();
            this.bestFitness = getFitness(this.bestPosition);
        }
        double[] bestScoutedPosition = super.scout();
        double bestFitness = getFitness(bestScoutedPosition);
        double[] position;
        double fitness;
        for (int i = 1 ; i < Parameters.numberOfScoutTrials ; i++){
            position = super.scout();
            fitness = getFitness(position);
            if (fitness < bestFitness){
                bestScoutedPosition = position;
                bestFitness = fitness;
            }
        }
        trials = 0;
        this.position = bestScoutedPosition;
        this.fitness = bestFitness;
        return this.position;
    }

    public void search(Bee neighbor){
        double[] currentPosition = position.clone();
        updatePosition(currentPosition, neighbor.position, true);
        enhance();
        double newFitness = getFitness(currentPosition);
        updateTrials(newFitness, currentPosition, true);

    }

    private void updateTrials(double fitness, double[] currentPosition, boolean searched){
        if (fitness < this.fitness){
            this.fitness = fitness;
            this.position = currentPosition;
            if (fitness < colony.globalBestFitness){
                colony.globalBestPosition = this.position.clone();
                colony.globalBestFitness = this.fitness;
                trials = 0;
            }
            else if (fitness < colony.globalBestFitness * Parameters.globalTrialsCutoff){
                trials = 0;
            }
            else {
                trials = searched ? trials++ : trials;
            }
        }
        else {
            trials = searched ? trials++ : trials;
        }
    }

    public void updateToBestPosition(){
        double currentBestFitness = fitness;
        Bee currentBestBee = this;
        for (Onlooker onlooker : onlookerFitnesses.keySet()){
            if (onlooker.fitness < currentBestFitness){
                currentBestFitness = onlooker.fitness;
                currentBestBee = onlooker;
            }
        }
        double[] currentBestPosition = currentBestBee.position.clone();
        updateTrials(currentBestFitness, currentBestPosition, false);
        onlookerFitnesses.clear();
    }



}
