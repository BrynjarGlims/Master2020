package Master2020.MIP;
import Master2020.DataFiles.*;

import gurobi.GRB;
import gurobi.GRBException;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkImages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GraphPlot {
    private HashMap<Integer, String> colorMap = new HashMap<>();
    private Data data;
    FileSinkImages fs = new FileSinkImages(FileSinkImages.OutputType.png, FileSinkImages.Resolutions.HD720);

    private String[] COLORS = {
            "#E5198D", "#E9AE01", "#009C94", "#16AA39", "#1678C1", "#9627BA", "#F16301", "#CF1919",
            "#B5CC18", "#975B33", "#5829BB"
    }; // pink, yellow, teal, green, blue, purple, orange, red, olive, brown, violet


    public GraphPlot(Data data) {
        this.data = data;
    }

    private Graph makeGraph(int day, boolean arcFlow){
        String graphId = arcFlow ? Parameters.instanceName + "-arcFlow-day-" + day : Parameters.instanceName + "-pathFlow-day-" + day;
        Graph graph = new MultiGraph(graphId);
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        graph.addAttribute("ui.stylesheet", "url('resources/style.css')");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        return graph;
    }

    private void setColorMap(){
        for (Vehicle v : data.vehicles){
            colorMap.putIfAbsent(v.vehicleID, COLORS[v.vehicleID]);
        }
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
        System.out.println("node: " + graph.getNode(data.numberOfCustomers));

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

    private void addEdgesToGraphArcFlow(int d, Graph graph) throws GRBException {
        int from;
        int to;
        String edgeId;
        for (int v = 0; v < data.numberOfVehicles; v++) {
            for (int r = 0; r < data.numberOfTrips; r++) {
                for (int i = 0; i < data.numberOfNodes; i++) {
                    for (int j = 0; j < data.numberOfNodes; j++) {
                        if (data.arcs[d][v][r][i][j].get(GRB.DoubleAttr.X) == 1){
                            from = Math.min(i, data.numberOfCustomers);
                            to = Math.min(j, data.numberOfCustomers);
                            edgeId = d + "-" + v + "-" + r + "-" + from + "-" + to;
                            Edge edge = graph.addEdge(edgeId, from, to, true);
                            edge.setAttribute("ui.style", String.format("fill-color: %s;", colorMap.get(data.vehicles[v].vehicleID)));
                        }
                    }
                }
            }
        }
    }

    private void addEdge(int from, int to, String edgeId, int vehicleId, Graph graph){
        Edge edge = graph.addEdge(edgeId, from, to, true);
        System.out.println("from " + from + " to " + to);
        edge.setAttribute("ui.style", String.format("fill-color: %s;", colorMap.get(vehicleId)));
    }

    private void addEdgesToGraphPathFlow(int d, Graph graph) throws GRBException {
        /*  // TODO: 21/02/2020 implement if needed 
        int from;
        int to;
        String edgeId;
        for (int v = 0; v < data.numberOfVehicles; v++) {
            for (int r = 0; r < data.numberOfTrips; r++) {
                for (Path p : data.pathMap.get(d).get(data.vehicles[v].vehicleType.type)) {
                    if (data.paths[d][v][r][p.pathId].get(GRB.DoubleAttr.X) == 1){
                        from = data.numCustomers;
                        to = p.customers[0].customerID;
                        edgeId = d + "-" + v + "-" + r + "-" + from + "-" + to;
                        addEdge(from, to, edgeId, data.vehicles[v].vehicleID, graph);
                        for (int i = 1 ; i < p.customers.length ; i++){
                            from = p.customers[i-1].customerID;
                            to = p.customers[i].customerID;
                            edgeId = d + "-" + v + "-" + r + "-" + from + "-" + to;
                            addEdge(from, to, edgeId, data.vehicles[v].vehicleID, graph);
                        }
                        from = p.customers[p.customers.length-1].customerID;
                        to = data.numCustomers;
                        edgeId = d + "-" + v + "-" + r + "-" + from + "-" + to;
                        addEdge(from, to, edgeId, data.vehicles[v].vehicleID, graph);
                    }
                }
            }
        }
        
         */
    }



    private Graph fillGraph(int day, boolean save, boolean arcFlow) throws GRBException {
        Graph graph = makeGraph(day, arcFlow);
        // add customers to graph
        for (Customer c : data.customers){
            addCustomerToGraph(c, graph);
        }
        //add depot to graph
        addDepotToGraph(graph);
        // add edges to graph
        if (arcFlow){
            addEdgesToGraphArcFlow(day, graph);
        }
        else {
            addEdgesToGraphPathFlow(day, graph);
        }
        if (save){ saveGraph(graph, "results/plots" + graph.getId() + ".png");}
        return graph;
    }

    public void visualize(boolean arcFlow) throws GRBException {
        // initialize colors
        setColorMap();
        ArrayList<Graph> graphs = new ArrayList<>();
        for (int d = 0 ; d < data.numberOfPeriods ; d++){
            Graph g = fillGraph(d, true, arcFlow);
            graphs.add(g);
        }
        graphs.get(0).display(false); //CHANGE DAY
    }

    public static void loadSavedGraph(String path) {
        Graph g = new MultiGraph(path);
        try {
            g.read(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        g.display(false);
    }

    public void saveGraph(Graph graph, String path) {
        try {
            fs.writeAll(graph, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}