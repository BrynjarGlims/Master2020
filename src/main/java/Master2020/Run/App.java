package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.PeriodicIndividual;
import Master2020.MIP.DataConverter;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PR.*;
import Master2020.Population.PeriodicPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.StoringResults.Result;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import Master2020.Individual.Trip;
import gurobi.GRBException;


import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class App {


    public static Data data;
    public static Population population;
    public static PeriodicPopulation periodicPopulation;
    public static OrderDistributionPopulation odp;
    public static OrderDistributionCrossover ODC;
    public static OrderDistribution globalOrderDistribution;
    public static double bestIndividualScore;
    public static HashSet<Individual> repaired;
    public static Individual bestIndividual;
    public static int numberOfIterations;
    public static OrderAllocationModel orderAllocationModel;


    public static void initialize() throws GRBException {
        data = DataReader.loadData();
        population = new Population(data);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(globalOrderDistribution);
        bestIndividualScore = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(population);
        orderAllocationModel = new OrderAllocationModel(data);
        repaired = new HashSet<>();
        numberOfIterations = 0;
    }

    public static void initializePeriodic() throws GRBException {
        data = DataReader.loadData();
        periodicPopulation = new PeriodicPopulation(data);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(periodicPopulation);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        periodicPopulation.setOrderDistributionPopulation(odp);
        periodicPopulation.initializePopulation(globalOrderDistribution);
        bestIndividualScore = Double.MAX_VALUE;
        BiasedFitness.setBiasedFitnessScore(periodicPopulation);
        orderAllocationModel = new OrderAllocationModel(data);
        repaired = new HashSet<>();
        numberOfIterations = 0;
    }


    private static void findBestOrderDistribution(){
        //Find best OD for the distribution
        odp.calculateFillingLevelFitnessScoresPlural();
        for (Individual individual : population.getTotalPopulation()){
            individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // // TODO: 24/03/2020 Rejects converting from feasible to infeasible
        }
    }

    public static Individual PIX(){
        return PIX(population);
    }

    public static Individual PIX(Population population){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
        for (OrderDistribution od : crossoverOD) { //todo: EVALUEATE IF THIS IS DECENT
            odp.addOrderDistribution(od);
        }
        return GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0]); // TODO: 02.04.2020 add to a pool?
    }




    public static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }


    public static void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.ODMIPProbability){
            OrderDistribution optimalOD = orderAllocationModel.createOptimalOrderDistribution(individual.journeyList);
            if (optimalOD.fitness != Double.MAX_VALUE) {  // Distribution found
                if (individual.infeasibilityCost == 0) {
                    individual.setOptimalOrderDistribution(optimalOD, true);
                } else {
                    individual.setOptimalOrderDistribution(optimalOD, false);
                }
                odp.addOrderDistribution(optimalOD);
            }
        }
    }

    public static void tripOptimizer(Individual individual){
        for (int p = 0 ; p < data.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Trip trip : individual.tripList[p][vt]) {
                    if (ThreadLocalRandom.current().nextDouble() < Parameters.tripOptimizerProbability) {
                        TripOptimizer.optimizeTrip(trip, individual.orderDistribution);
                    }
                }
            }
        }
        individual.setGiantTourFromTrips();
    }

    public static void repair(){
        repair(population);
    }

    public static void repair(Population population){
        repaired.clear();
        for (Individual infeasibleIndividual : population.infeasiblePopulation){
            if (ThreadLocalRandom.current().nextDouble() < Parameters.repairProbability){
                if (Repair.repair(infeasibleIndividual, infeasibleIndividual.orderDistribution)){
                    repaired.add(infeasibleIndividual);
                }
            }
        }
        population.infeasiblePopulation.removeAll(repaired);
        population.feasiblePopulation.addAll(repaired);
    }

    public static void selection(){
        selection(population);
    }


    public static void selection(Population population){

        // Reduce population size
        population.improvedSurvivorSelection();
        odp.removeNoneUsedOrderDistributions(population);
        numberOfIterations++;
        bestIndividual = population.returnBestIndividual();
        bestIndividualScore = bestIndividual.getFitness(false);


        // Check if it has improved for early termination
        if (bestIndividualScore == bestIndividual.getFitness(false)){
            population.setIterationsWithoutImprovement(population.getIterationsWithoutImprovement()+1);
        }
        else{
            population.setIterationsWithoutImprovement(0);
        }

        Individual bestFeasibleIndividual = population.returnBestIndividual();
        Individual bestInfeasibleIndividual = population.returnBestInfeasibleIndividual();
        bestFeasibleIndividual.printDetailedFitness();
        bestInfeasibleIndividual.printDetailedFitness();
    }


    public static void runMIPAFM(int samples){
        for (int i = 0 ; i < samples ; i++){
            Parameters.randomSeedValue += 1;
            Data data = Master2020.DataFiles.DataReader.loadData();
            DataMIP dataMip = DataConverter.convert(data);
            ArcFlowModel afm = new ArcFlowModel(dataMip);
            afm.runModel(Master2020.DataFiles.Parameters.symmetry);
            Individual bestIndividual = afm.getIndividual();
            Result result = new Result(bestIndividual, "AFM", afm.feasible, afm.optimal);
            try{
                result.store(afm.runTime, afm.MIPGap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void runMIPPFM(int samples){
        for (int i = 0 ; i < samples ; i++){
            Parameters.randomSeedValue += 1;
            Data data = Master2020.DataFiles.DataReader.loadData();
            DataMIP dataMip = DataConverter.convert(data);
            PathFlowModel pfm = new PathFlowModel(dataMip);
            pfm.runModel(Master2020.DataFiles.Parameters.symmetry);
            Individual bestIndividual = pfm.getIndividual();
            Result result = new Result(bestIndividual, "PFM", pfm.feasible, pfm.optimal);
            try{
                result.store(pfm.runTime, pfm.MIPGap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void runMIPJBM(int samples){
        for (int i = 0 ; i < samples ; i++){
            Parameters.randomSeedValue += 1;
            Data data = Master2020.DataFiles.DataReader.loadData();
            DataMIP dataMip = DataConverter.convert(data);
            JourneyBasedModel jbm = new JourneyBasedModel(dataMip);
            jbm.runModel(Master2020.DataFiles.Parameters.symmetry);
            Individual bestIndividual = jbm.getIndividual();
            System.out.println("hei");
            Result result = new Result(bestIndividual, "JBM", jbm.feasible, jbm.optimal);
            try{
                result.store(jbm.runTime, jbm.MIPGap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    public static void runGA(int samples) throws IOException, GRBException {
        double time = System.currentTimeMillis();
        for (int i = 0 ; i < samples ; i++) {
            System.out.println("Initialize population..");
            initialize();

            while ((population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                    numberOfIterations < Parameters.maxNumberOfGenerations)) {
                System.out.println("Start generation: " + numberOfIterations);

                //Find best OD for the distribution
                System.out.println("Assign best OD..");
                findBestOrderDistribution();

                //Generate new population
                for (Individual individual : population.infeasiblePopulation) {
                    if (!Master2020.Testing.IndividualTest.testIndividual(individual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PRIOR");
                    }
                }

                System.out.println("Populate..");
                while (population.infeasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize &&
                        population.feasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize) {
                    Individual newIndividual = PIX();
                    if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PIX");
                    }
                    educate(newIndividual);
                    if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: EDUCATE");
                    }
                    setOptimalOrderDistribution(newIndividual);
                    if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: OD OPTIMIZER");
                    }
                    tripOptimizer(newIndividual);
                    if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: TRIP OPTIMIZER");
                    }
                    population.addChildToPopulation(newIndividual);


                }
                for (Individual individual : population.getTotalPopulation()) {
                    IndividualTest.checkIfIndividualIsComplete(individual);
                }

                System.out.println("Repair..");
                repair();

                System.out.println("Selection..");
                selection();
            }



            Individual bestIndividual = population.returnBestIndividual();
            if (!Master2020.Testing.IndividualTest.testIndividual(bestIndividual)) {
                System.out.println("BEST INDIVIDUAL IS NOT COMPLETE");
            }
            System.out.println("Individual feasible: " + bestIndividual.isFeasible());
            System.out.println("Fitness: " + bestIndividual.getFitness(false));
            if (Parameters.savePlots) {
                PlotIndividual visualizer = new PlotIndividual(data);
                visualizer.visualize(bestIndividual);
            }
            double runTime = (System.currentTimeMillis() - time)/1000;
            Result res = new Result(population, "GA");
            res.store(runTime, -1);
            orderAllocationModel.terminateEnvironment();
        }
    }



    public static void runPeriodicGA(int samples) throws IOException, GRBException {
        double time = System.currentTimeMillis();
        for (int i = 0 ; i < samples ; i++) {
            System.out.println("Initialize population..");
            initializePeriodic();
            System.out.println("Initialization completed");

            while ((periodicPopulation.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                    numberOfIterations < Parameters.maxNumberOfGenerations)) {
                System.out.println("Start generation: " + numberOfIterations);


                //Find best OD for the distribution
                System.out.println("Assign best OD..");
                //findBestOrderDistribution();  //todo: design choice, implement later

                //Generate new population

                /*  // TODO: 22/04/2020 Implement testing
                for (Individual individual : population.infeasiblePopulation) {
                    if (!Master2020.Testing.IndividualTest.testIndividual(individual)) {
                        System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PRIOR");
                    }
                }
                */


                System.out.println("Populate..");
                for (int p = 0; p < data.numberOfPeriods; p++){
                    population = periodicPopulation.populations[p];
                    System.out.println(" ");
                    System.out.println(" ####### Start Period " + p + " ########");
                    System.out.println(" ");
                    while (periodicPopulation.populations[p].infeasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize &&
                            periodicPopulation.populations[p].feasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize) {

                        Individual newIndividual = PIX(periodicPopulation.populations[p]);

                        /*
                        if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                            System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PIX");
                        }
                        educate(newIndividual);
                        if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                            System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: EDUCATE");
                        }

                        tripOptimizer(newIndividual);
                        if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                            System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: TRIP OPTIMIZER");
                        }

                         */
                        periodicPopulation.populations[p].addChildToPopulation(newIndividual);


                    }

                    /*  todo: implement this if needed
                    for (Individual individual : population.getTotalPopulation()) {
                        IndividualTest.checkIfIndividualIsComplete(individual);
                    }

                     */

                    System.out.println("Repair..");
                    repair(periodicPopulation.populations[p]);

                    System.out.println("Selection..");
                    selection(periodicPopulation.populations[p]);
                }

                for (int j = 0; j < Parameters.newIndividualCombinationsGenerated; j++){
                    PeriodicIndividual newPeriodicIndividual = generatePeriodicIndividual();
                    periodicPopulation.addPeriodicIndividual(newPeriodicIndividual);
                    newPeriodicIndividual.printDetailedInformation();
                }




                // // TODO: 22/04/2020  Implement so it works on the best individual of the population
                /*
                setOptimalOrderDistribution(newIndividual);
                if (!Master2020.Testing.IndividualTest.testIndividual(newIndividual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: OD OPTIMIZER");
                }
                
                 */


            }
            /* // TODO: 22/04/2020 FIX 
            Individual bestIndividual = population.returnBestIndividual();
            if (!Master2020.Testing.IndividualTest.testIndividual(bestIndividual)) {
                System.out.println("BEST INDIVIDUAL IS NOT COMPLETE");
            }
            System.out.println("Individual feasible: " + bestIndividual.isFeasible());
            System.out.println("Fitness: " + bestIndividual.getFitness(false));
            if (Parameters.savePlots) {
                PlotIndividual visualizer = new PlotIndividual(data);
                visualizer.visualize(bestIndividual);
            }
            double runTime = (System.currentTimeMillis() - time)/1000;
            Result res = new Result(population, "GA");
            res.store(runTime, -1);
            orderAllocationModel.terminateEnvironment();
            
             */
        }
    }

    private static PeriodicIndividual generatePeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = TournamentSelection.performSelection(periodicPopulation.populations[p]);
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness());
        return newPeriodicIndividual;
    }



    public static void main(String[] args) throws Exception {
        /*
        Parameters.numberOfCustomers = Integer.parseInt(args[1]);
        if (args[0].equals("AFM"))
            runMIPAFM(Parameters.samples);
        else if (args[0].equals("PFM"))
            runMIPPFM(Parameters.samples);
        else if (args[0].equals("JBM"))
            runMIPJBM(Parameters.samples);
        else {
            runGA(Parameters.samples);
        }

         */
        if (!Parameters.isPeriodic) {
            System.out.println("######## RUN STANDARD GA #########");
            runGA(Parameters.samples);
        } else {
            System.out.println("######## RUN PERIODIC GA #########");

            runPeriodicGA(Parameters.samples);
        }



//        double time = System.currentTimeMillis();
//        Data data = Master2020.DataFiles.DataReader.loadData();
//        DataMIP dataMip = DataConverter.convert(data);
//        PathGenerator pg = new PathGenerator(dataMip);
//        JourneyGenerator jg = new JourneyGenerator(dataMip);
//        dataMip.setPathMap(pg.generateAllPaths());
//        dataMip.setJourneyMap(jg.generateAllJourneys());
//        double timePost = System.currentTimeMillis();
//        double total = (timePost - time)/1000;
//        System.out.println("time: " + total);
//        double paths = 0;
//        for (int p = 0 ; p < dataMip.numPeriods ; p++){
//            for (int vt = 0 ; vt < dataMip.numVehicleTypes ; vt++){
//                paths += dataMip.journeyMap.get(p).get(vt).size();
//            }
//        }
//        System.out.println("paths: " + paths);

    }

}