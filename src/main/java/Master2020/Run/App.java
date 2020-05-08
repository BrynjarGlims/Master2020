package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.PGA.PeriodicIndividual;
import Master2020.MIP.DataConverter;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PR.*;
import Master2020.PGA.PeriodicPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import Master2020.StoringResults.Result;
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
    public static double scalingFactorOrderDistribution;
    public static PeriodicIndividual bestPeriodicIndividual;


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
        odp = new OrderDistributionPopulation(data);
        periodicPopulation = new PeriodicPopulation(data);
        periodicPopulation.initialize(odp);
        ODC = new OrderDistributionCrossover(data);
        scalingFactorOrderDistribution = Parameters.initialOrderDistributionScale;
        odp.initializeOrderDistributionPopulation(periodicPopulation);
        odp.setOrderDistributionScalingFactor(scalingFactorOrderDistribution);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        globalOrderDistribution.setOrderScalingFactor(scalingFactorOrderDistribution);
        periodicPopulation.setOrderDistributionPopulation(odp);
        periodicPopulation.initialize(globalOrderDistribution);
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

    public static Individual PIX(Population population){
        // Select parents
        Individual parent1 = TournamentSelection.performSelection(population);
        Individual parent2 = TournamentSelection.performSelection(population);
        while (parent1.equals(parent2)){
            parent2 = TournamentSelection.performSelection(population);
        }
        /*OrderDistribution[] crossoverOD = ODC.crossover(parent1.orderDistribution, parent2.orderDistribution); //these will be the same
        for (OrderDistribution od : crossoverOD) { //todo: EVALUEATE IF THIS IS DECENT
            odp.addOrderDistribution(od);
        }

         */
        return GiantTourCrossover.crossOver(parent1, parent2, globalOrderDistribution); // TODO: 02.04.2020 add to a pool?
    }




    public static void educate(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution);
        }
    }


    public static void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.ODMIPProbability){
            OrderDistribution optimalOD;
            if (orderAllocationModel.createOptimalOrderDistribution(individual.journeyList) == 2){
                optimalOD = orderAllocationModel.getOrderDistribution();
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
    }

    public static void tripOptimizer(Individual individual){
        for (int p = 0 ; p < individual.numberOfPeriods ; p++){
            for (int vt = 0 ; vt < data.numberOfVehicleTypes ; vt++){
                for (Trip trip : individual.tripList[Individual.getDefaultPeriod(p)][vt]) {
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
        if (!Parameters.isPeriodic){
            odp.removeNoneUsedOrderDistributions(population);
        }
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
        if (!Parameters.isPeriodic){
            bestFeasibleIndividual.printDetailedFitness();
            bestInfeasibleIndividual.printDetailedFitness();
        }

    }


    public static void runMIPAFM(int samples){
        for (int i = 0 ; i < samples ; i++){
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
            Parameters.randomSeedValue += 1;
        }
    }

    public static void runMIPPFM(int samples){
        for (int i = 0 ; i < samples ; i++){
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
            Parameters.randomSeedValue += 1;
        }
    }

    public static void runMIPJBM(int samples){
        for (int i = 0 ; i < samples ; i++){
            System.out.println("RUNNING FOR SAMPLE: " + Parameters.randomSeedValue);
            Data data = Master2020.DataFiles.DataReader.loadData();
            DataMIP dataMip = DataConverter.convert(data);
            JourneyBasedModel jbm = new JourneyBasedModel(dataMip);
            jbm.runModel(Master2020.DataFiles.Parameters.symmetry);
            Individual bestIndividual = jbm.getIndividual();
            Result result = new Result(bestIndividual, "JBM", jbm.feasible, jbm.optimal);
            try{
                result.store(jbm.runTime, jbm.MIPGap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Parameters.randomSeedValue += 1;
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

                numberOfIterations++;
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
        Parameters.isPeriodic = true;
        double time = System.currentTimeMillis();
        for (int i = 0; i < samples; i++) {
            System.out.println("Initialize population..");
            initializePeriodic();
            System.out.println("Initialization completed");

            while (periodicPopulation.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                    numberOfIterations < Parameters.maxNumberOfGenerations) {


                System.out.println("Start generation: " + numberOfIterations);


                //System.out.println("Populate..");
                for (int p = 0; p < data.numberOfPeriods; p++) {
                    population = periodicPopulation.populations[p];
                    //System.out.println(" ####### Start Period " + p + " ########");
                    while (periodicPopulation.populations[p].infeasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize &&
                            periodicPopulation.populations[p].feasiblePopulation.size() < Parameters.maximumSubIndividualPopulationSize) {

                        Individual newIndividual = PIX(periodicPopulation.populations[p]);

                        educate(newIndividual);

                        tripOptimizer(newIndividual);

                        periodicPopulation.populations[p].addChildToPopulation(newIndividual);

                    }

                    //System.out.println("Repair..");
                    repair(periodicPopulation.populations[p]);

                    //System.out.println("Selection..");
                    selection(periodicPopulation.populations[p]);
                }
                //System.out.println("Iteration ended");

                PeriodicIndividual newPeriodicIndividual = generateGreedyPeriodicIndividual();
                periodicPopulation.addPeriodicIndividual(newPeriodicIndividual);
                newPeriodicIndividual.printDetailedInformation();
                PeriodicIndividual bestPeriodicIndividual = periodicPopulation.returnBestIndividual();



                if (numberOfIterations % Parameters.generationsOfOrderDistributions == 0 ||
                        numberOfIterations == Parameters.maxNumberIterationsWithoutImprovement-1) {
                    createNewOptimalOrderDistribution(bestPeriodicIndividual);
                }
                updateOrderDistributionScalingParameter();

                System.out.println("Number of ORDERDISTRIBUTIONS: " + odp.setOfOrderDistributions.size());
                bestPeriodicIndividual = periodicPopulation.returnBestIndividual();
                //bestPeriodicIndividual.printDetailedInformation();
                numberOfIterations += 1;
            }

            bestPeriodicIndividual = periodicPopulation.returnBestIndividual();
            Individual bestIndividual = bestPeriodicIndividual.createStandardIndividualObject();
            bestIndividual.updateFitness();
            bestIndividual.printDetailedFitness();
            Result res = new Result(bestIndividual, "PGA" , bestPeriodicIndividual.isFeasible(), false);
            double runTime = (System.currentTimeMillis() - time)/1000;
            res.store(runTime, -1);
            orderAllocationModel.terminateEnvironment();
            Parameters.randomSeedValue += 1;
        }
    }

    private static void createNewOptimalOrderDistribution(PeriodicIndividual bestPeriodicIndividual){
        System.out.println("Perform update of volumes");
        if (orderAllocationModel.createOptimalOrderDistribution(bestPeriodicIndividual.getJourneys(), scalingFactorOrderDistribution) == 2){
            globalOrderDistribution = orderAllocationModel.getOrderDistribution();
            odp.setOfOrderDistributions.add(globalOrderDistribution);
            odp.addOrderDistribution(orderAllocationModel.getOrderDistribution());
            periodicPopulation.setOrderDistribution(globalOrderDistribution);
            bestPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
            periodicPopulation.allocateIndividual(bestPeriodicIndividual);
            periodicPopulation.reassignPeriodicIndividuals();
            System.out.println("################################## Number of fesasible individuals:" + periodicPopulation.periodicFeasibleIndividualPopulation.size());
            System.out.println("-----------------Optimal OD found!");
        } else{
            System.out.println("----------------No optimal OD found...");
        }
    }


    private static PeriodicIndividual generateGreedyPeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
        newPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = periodicPopulation.populations[p].returnBestIndividual();
            if (!individual.orderDistribution.equals(globalOrderDistribution)){
                System.out.println("------ Trying to combine individuals with diffferent ODS ------- " + individual.hashCode());
            }
            individual.updateFitness();
            if (!individual.isFeasible()){
                System.out.println("------- The added individual is not feasible -------- " + individual.hashCode());
            }
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness());
        return newPeriodicIndividual;
    }




    private static void updateOrderDistributionScalingParameter() {
        if (numberOfIterations % Parameters.numberOfGenerationsBetweenODScaling == 0 && numberOfIterations > Parameters.numberOfGenerationBeforeODScalingStarts) {
            scalingFactorOrderDistribution = (scalingFactorOrderDistribution < 1) ? Parameters.incrementPerOrderDistributionScaling + scalingFactorOrderDistribution : 1;
            globalOrderDistribution.setOrderScalingFactor(scalingFactorOrderDistribution);
            odp.setOrderDistributionScalingFactor(scalingFactorOrderDistribution);
            periodicPopulation.reassignPeriodicIndividuals();
            System.out.println("############# CURRENT ORDER DISTRIBUTION SCALING IS " + scalingFactorOrderDistribution + "################");
        }
    }

    private static PeriodicIndividual generatePeriodicIndividual(){
        PeriodicIndividual newPeriodicIndividual = new PeriodicIndividual(data);
        newPeriodicIndividual.setOrderDistribution(globalOrderDistribution);
        for (int p = 0; p < data.numberOfPeriods; p++){
            Individual individual = TournamentSelection.performSelection(periodicPopulation.populations[p]);
            newPeriodicIndividual.setPeriodicIndividual(individual, p);
        }
        System.out.println("Fitness: " + newPeriodicIndividual.getFitness());
        return newPeriodicIndividual;
    }



    public static void main(String[] args) throws Exception {


        //Parameters.numberOfCustomers = Integer.parseInt(args[1]);
        /*
        if (args[0].equals("AFM"))
            runMIPAFM(Parameters.samples);
        else if (args[0].equals("PFM"))
            runMIPPFM(Parameters.samples);
        else if (args[0].equals("JBM"))
            runMIPJBM(Parameters.samples);
        else if (args[0].equals("GA"))
            runGA(Parameters.samples);
        else if (args[0].equals("PGA")) {
            Parameters.isPeriodic = true;
            runPeriodicGA(Parameters.samples);
        }

         */

        System.out.println("SEED VALUE: " + Parameters.randomSeedValue );
        //Parameters.isPeriodic = false;
        //runMIPAFM(Parameters.samples);

        //Parameters.randomSeedValue = 31 + i;
        //runGA(Parameters.samples);

        Parameters.isPeriodic = true;
        runPeriodicGA(Parameters.samples);



    }
}