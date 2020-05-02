package Master2020.Population;

import Master2020.DataFiles.Data;
import Master2020.ProductAllocation.OrderDistribution;
import scala.xml.Null;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class PeriodicOrderDistributionPopulation {




    public ArrayList<OrderDistribution> distributions;
    public Data data;

    public PeriodicOrderDistributionPopulation(Data data){
        this.data = data;
    }

    public void initialize(int numOrderDistributions){
        distributions = new ArrayList<>();
        OrderDistribution od;
        for (int i = 0 ; i < numOrderDistributions ; i++){
            od = new OrderDistribution(data);
            od.makeInitialDistribution();
            distributions.add(od);
        }
    }


    public OrderDistribution diversify(int numSamples){
        OrderDistribution mostDiverse = null;
        double diversityScore = 0;
        ArrayList<OrderDistribution> samples = new ArrayList<>();
        OrderDistribution od;
        for (int i = 0 ; i < numSamples ; i++){
            od = new OrderDistribution(data);
            od.makeInitialDistribution();
            samples.add(od);
        }
        double tempScore;
        for (OrderDistribution o : samples){
            tempScore = getDiversityScore(o);
            if (tempScore > diversityScore){
                mostDiverse = o;
                diversityScore = tempScore;
            }
        }
        return mostDiverse;
    }

    public double getDiversityScore(OrderDistribution od){
        double score = 0;
        for (OrderDistribution o : distributions){
            score += od.diversityScore(o);
        }
        return score;
    }

}
