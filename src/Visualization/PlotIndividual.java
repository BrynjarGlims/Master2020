package visualization;

import DataFiles.*;
import Individual.*;
import ProductAllocation.OrderDistribution;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkImages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PlotIndividual {

    Data data;
    int counter = 0;
    private HashMap<Integer, HashMap<Integer, String>> colorMap = new HashMap<>();

    FileSinkImages fs = new FileSinkImages(FileSinkImages.OutputType.png, FileSinkImages.Resolutions.HD720);
    private String[] COLORS_RED = {
        "#F1948A", "#F1948A", "#E74C3C", "#CB4335", "#B03A2E", "#943126", "#78281F"
    }; //RED COLORS
    private String[] COLORS_PURPLE = {
            "#BB8FCE", "#A569BD", "#8E44AD", "#7D3C98", "#6C3483", "#5B2C6F", "#4A235A"
    }; //PURPLE COLORS

    private String[] COLORS_GREEN = {
            "#ABEBC6", "#82E0AA", "#58D68D", "#2ECC71", "#28B463", "#239B56", "#1D8348", "#186A3B"
    }; //GREEN COLORS

    private String[] COLORS_YELLOW = {
            "#F9E79F", "#F9E79F", "#F7DC6F", "#F4D03F", "#F1C40F", "#D4AC0D", "#B7950B", "#9A7D0A", "#7D6608"
    }; //YELLOW COLORS

    private String[] COLORS_BLUE = {
            "#AED6F1", "#85C1E9", "#5DADE2", "#3498DB", "#2E86C1", "#2874A6", "#21618C", "#1B4F72"
    }; //BLUE COLORS

    //colors for each vehicle type
    private String[][] COLORS = new String[][]{COLORS_RED, COLORS_PURPLE, COLORS_GREEN, COLORS_YELLOW, COLORS_BLUE};

    public PlotIndividual(Data data) {
        this.data = data;
    }


    private void setColorMap(){
        for (int i = 0 ; i < data.numberOfVehicleTypes ; i++){
            colorMap.putIfAbsent(i, new HashMap<Integer, String>());
        }
        for (Vehicle v : data.vehicles){
            colorMap.get(v.vehicleType.vehicleTypeID).putIfAbsent(v.vehicleID, COLORS[v.vehicleType.vehicleTypeID][v.vehicleID % COLORS[v.vehicleType.vehicleTypeID].length]);
        }


    }

    private Graph makeGraph(int period){
        String graphId = "individual-" + period;
        Graph graph = new MultiGraph(graphId);
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        graph.addAttribute("ui.stylesheet", "url('resources/style.css')");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        return graph;
    }

    private void addDepotToGraph(Graph graph){
        int id = data.numberOfCustomers;
        double x = data.depot.xCoordinate;
        double y = data.depot.yCoordinate;
        Node node;
        String nodeId = "d" + id;
        node = graph.addNode(nodeId);
        node.setAttribute("xy", x, y);
        node.addAttribute("ui.class", "depot");
        node.addAttribute("ui.label", "depot");
    }

    private void addCustomerToGraph(Customer customer, Graph graph){
        int id = customer.customerID;
        double x = customer.xCoordinate;
        double y = customer.yCoordinate;
        Node node;
        String nodeId;
        nodeId = "c" + id;
        node = graph.addNode(nodeId);
        node.setAttribute("xy", x, y);
        node.addAttribute("ui.class", "customer");
        node.addAttribute("ui.label", id);
    }

    private void addEdgesToGraph(int period, Individual individual, Graph graph){
        for (int vt = 0 ; vt < individual.giantTour.chromosome[period].length ; vt++){
            if (individual.giantTour.chromosome[period][vt].isEmpty()){
                continue;
            }
            addEdge(data.numberOfCustomers, individual.giantTour.chromosome[period][vt].get(0),
                    period + "-" + data.numberOfCustomers + "-" + individual.giantTour.chromosome[period][vt].get(0),
                    data.vehicles[individual.vehicleAssigment.chromosome[period].get(individual.giantTour.chromosome[period][vt].get(0))], graph);
            int tempDividerIndex = 0;
            int tripDivider = individual.giantTourSplit.chromosome[period][vt].get(tempDividerIndex);
            System.out.println(tripDivider);
            for (int customer = 1 ; customer < individual.giantTour.chromosome[period][vt].size() ; customer++){
                if (customer == tripDivider){
                    addEdge(individual.giantTour.chromosome[period][vt].get(customer - 1), data.numberOfCustomers,
                            period + "-" + individual.giantTour.chromosome[period][vt].get(customer - 1) + "-" + data.numberOfCustomers,
                            data.vehicles[individual.vehicleAssigment.chromosome[period] .get(individual.giantTour.chromosome[period][vt].get(customer - 1))], graph);

                    addEdge(data.numberOfCustomers, individual.giantTour.chromosome[period][vt].get(customer),
                            period + "-" + data.numberOfCustomers + "-" + individual.giantTour.chromosome[period][vt].get(customer),
                            data.vehicles[individual.vehicleAssigment.chromosome[period].get(individual.giantTour.chromosome[period][vt].get(customer))], graph);
                    tempDividerIndex++;
                    tripDivider = individual.giantTourSplit.chromosome[period][vt].get(tempDividerIndex);
                }
                else {
                    addEdge(individual.giantTour.chromosome[period][vt].get(customer - 1), individual.giantTour.chromosome[period][vt].get(customer),
                            period + "-" + individual.giantTour.chromosome[period][vt].get(customer - 1) + "-" + individual.giantTour.chromosome[period][vt].get(customer),
                            data.vehicles[individual.vehicleAssigment.chromosome[period].get(individual.giantTour.chromosome[period][vt].get(customer))], graph);
                }
            }
            System.out.println(individual.giantTour.chromosome[period][vt].get(individual.giantTour.chromosome[period][vt].size() - 1));
            addEdge(individual.giantTour.chromosome[period][vt].get(individual.giantTour.chromosome[period][vt].size() - 1), data.numberOfCustomers,
                    period + "-" + individual.giantTour.chromosome[period][vt].get(individual.giantTour.chromosome[period][vt].size() - 1) + "-" + data.numberOfCustomers,
                    data.vehicles[individual.vehicleAssigment.chromosome[period].get(individual.giantTour.chromosome[period][vt].get(individual.giantTour.chromosome[period][vt].size() - 1))], graph);
        }
    }


    private void addEdge(int from, int to, String edgeId, Vehicle vehicle, Graph graph){
        Edge edge = graph.addEdge(edgeId + counter, from, to, true);
        System.out.println("from " + from + " to " + to);
        edge.setAttribute("ui.style", String.format("fill-color: %s;", colorMap.get(vehicle.vehicleType.vehicleTypeID).get(vehicle.vehicleID)));
        counter++;
    }


    private Graph fillGraph(int period, Individual individual, boolean save){
        Graph graph = makeGraph(period);
        for (Customer c : data.customers){
            addCustomerToGraph(c, graph);
        }
        addDepotToGraph(graph);
        addEdgesToGraph(period, individual, graph);
        if (save){
            saveGraph(graph, "results/plots" + data.numberOfCustomers + "-" + data.numberOfVehicles + "-" + period + ".png");
        }
        return graph;
    }

    public void visualize(Individual individual){
        setColorMap();
        ArrayList<Graph> graphs = new ArrayList<>();
        for (int period = 0 ; period < data.numberOfPeriods ; period++){
            graphs.add(fillGraph(period, individual, Parameters.savePlots));
        }
        graphs.get(0).display(false);
    }

    private void saveGraph(Graph graph, String path) {
        try {
            fs.writeAll(graph, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args){
        Data data = DataReader.loadData();
        OrderDistribution od = new OrderDistribution(data);


        //todo: implement


    }




}
