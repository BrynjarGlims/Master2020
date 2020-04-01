package Genetic;

import DataFiles.Data;
import DataFiles.DataReader;
import DataFiles.Order;
import ProductAllocation.OrderDelivery;
import ProductAllocation.OrderDistribution;

import java.util.*;

public class OrderDistributionCrossover {

    public Data data;


    public OrderDistributionCrossover(Data data){
        this.data = data;
    }

    public OrderDistribution[] crossover(OrderDistribution parent1, OrderDistribution parent2){
        OrderDistribution[] children = {new OrderDistribution(data), new OrderDistribution(data)};

        List<Integer> notDelivered = new ArrayList<>();
        for (int i = 0 ; i < data.numberOfDeliveries ; i++){
            notDelivered.add(i);
        }
        Collections.shuffle(notDelivered);
        List<Integer> parent1Orders = new ArrayList<>(notDelivered.subList(0,data.numberOfDeliveries / 2));
        List<Integer> parent2Orders = new ArrayList<>(notDelivered.subList(data.numberOfDeliveries / 2, notDelivered.size()));

        inheritParent1(children, parent1, parent2, parent1Orders);

        HashSet<Integer> child1NotAdded = new HashSet<>();
        HashSet<Integer> child2NotAdded = new HashSet<>();

        inheritParent2(children, child1NotAdded, child2NotAdded, parent1, parent2, parent2Orders);

        addRemainingCustomers(child1NotAdded, children[0]);
        addRemainingCustomers(child2NotAdded, children[1]);

        return children;
    }

    private void inheritParent1(OrderDistribution[] children, OrderDistribution parent1, OrderDistribution parent2, List<Integer> orders){
        for (int i : orders){
            children[0].addOrderDelivery(parent1.orderDeliveries[i]);
            children[1].addOrderDelivery(parent2.orderDeliveries[i]);
        }
    }

    private void inheritParent2(OrderDistribution[] children, HashSet<Integer> child1NotAdded, HashSet<Integer> child2NotAdded, OrderDistribution parent1, OrderDistribution parent2, List<Integer> orders){
        for (int i : orders){
            if (data.orders[i].isDividable){
                children[0].addOrderDelivery(parent2.orderDeliveries[i]);
                children[1].addOrderDelivery(parent1.orderDeliveries[i]);
            }
            else {
                if (checkValidity(children[0], parent2.orderDeliveries[i])){
                    children[0].addOrderDelivery(parent2.orderDeliveries[i]);
                }
                else{
                    child1NotAdded.add(i);
                }
                if (checkValidity(children[1], parent1.orderDeliveries[i])){
                    children[1].addOrderDelivery(parent1.orderDeliveries[i]);
                }
                else{
                    child2NotAdded.add(i);
                }
            }
        }
    }

    private void addRemainingCustomers(HashSet<Integer> notAdded, OrderDistribution child){
        for (int i : notAdded){
            child.addOrderDelivery(new OrderDelivery(data.numberOfPeriods, data.orders[i], child.getBestPeriod(i), data.orders[i].volume));
        }
    }

    private boolean checkValidity(OrderDistribution child, OrderDelivery orderDelivery){
        return child.containsNonDividable(orderDelivery.order.customerID, orderDelivery.getPeriod());
    }

    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistributionCrossover ODC = new OrderDistributionCrossover(data);
        OrderDistribution p1 = new OrderDistribution(data);
        OrderDistribution p2 = new OrderDistribution(data);

        p1.makeInitialDistribution();
        p2.makeInitialDistribution();


        OrderDistribution[] children = ODC.crossover(p1, p2);
        OrderDistribution child1 = children[0];
        OrderDistribution child2 = children[1];

        //System.out.println(Arrays.toString(child1.volumePerPeriod));
    }

}
