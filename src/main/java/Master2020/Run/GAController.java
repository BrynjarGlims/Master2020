package Master2020.Run;

import Master2020.DataFiles.Data;
import Master2020.DataFiles.DataReader;
import Master2020.DataFiles.Parameters;
import Master2020.Genetic.*;
import Master2020.Individual.Individual;
import Master2020.Individual.Trip;
import Master2020.Interfaces.PeriodicSolution;
import Master2020.MIP.DataConverter;
import Master2020.MIP.JCMSolution;
import Master2020.MIP.JourneyCombinationModel;
import Master2020.MIP.OrderAllocationModel;
import Master2020.PGA.PeriodicIndividual;
import Master2020.PGA.PeriodicPopulation;
import Master2020.PR.ArcFlowModel;
import Master2020.PR.DataMIP;
import Master2020.PR.JourneyBasedModel;
import Master2020.PR.PathFlowModel;
import Master2020.Population.OrderDistributionPopulation;
import Master2020.Population.Population;
import Master2020.ProductAllocation.OrderDistribution;
import Master2020.StoringResults.Result;
import Master2020.StoringResults.SolutionStorer;
import Master2020.Testing.IndividualTest;
import Master2020.Visualization.PlotIndividual;
import gurobi.GRBException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class GAController {


    public Data data;
    public Population population;
    public PeriodicPopulation periodicPopulation;
    public OrderDistributionPopulation odp;
    public OrderDistributionCrossover ODC;
    public OrderDistribution globalOrderDistribution;
    public double bestIndividualScore;
    public HashSet<Individual> repaired;
    public Individual bestIndividual;
    public int numberOfIterations;
    public OrderAllocationModel orderAllocationModel;
    public double scalingFactorOrderDistribution;
    public PeriodicIndividual bestPeriodicIndividual;
    public PenaltyControl penaltyControl;

    public String fileName;
    public String modelName = "GA";
    public double time;

    public GAController() throws GRBException {
        initialize();
    }


    public void initialize() throws GRBException {
        data = DataReader.loadData();
        population = new Population(data);
        penaltyControl = new PenaltyControl(Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty);
        odp = new OrderDistributionPopulation(data);
        ODC = new OrderDistributionCrossover(data);
        odp.initializeOrderDistributionPopulation(population);
        globalOrderDistribution = odp.getRandomOrderDistribution();
        population.setOrderDistributionPopulation(odp);
        population.initializePopulation(globalOrderDistribution, penaltyControl);
        bestIndividualScore = Double.MAX_VALUE;
        fileName = SolutionStorer.getFolderName(this.modelName);
        BiasedFitness.setBiasedFitnessScore(population);
        orderAllocationModel = new OrderAllocationModel(data);
        repaired = new HashSet<>();
        numberOfIterations = 0;
        time = System.currentTimeMillis();
    }


    private void findBestOrderDistribution(){
        //Find best OD for the distribution
        odp.calculateFillingLevelFitnessScoresPlural();
        for (Individual individual : population.getTotalPopulation()){
            individual.testNewOrderDistribution(odp.getBestOrderDistribution(individual)); // // TODO: 24/03/2020 Rejects converting from feasible to infeasible
        }
    }


    public Individual PIX(){
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
        return GiantTourCrossover.crossOver(parent1, parent2, crossoverOD[0], penaltyControl); // TODO: 02.04.2020 add to a pool?
    }


    public void educate(Individual individual, PenaltyControl penaltyControl){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.educationProbability){
            Education.improveRoutes(individual, individual.orderDistribution, penaltyControl.timeWarpPenalty, penaltyControl.overLoadPenalty);
        }
    }


    public void setOptimalOrderDistribution(Individual individual){
        if (ThreadLocalRandom.current().nextDouble() < Parameters.ODMIPProbability){
            OrderDistribution optimalOD;
            if (orderAllocationModel.createOptimalOrderDistribution(individual.journeyList) == 2){
                optimalOD = orderAllocationModel.getOrderDistribution();
                if (optimalOD.fitness != Double.MAX_VALUE) {  // Distribution found
                    individual.setOptimalOrderDistribution(optimalOD);
                    odp.addOrderDistribution(optimalOD);
                }
            }

        }
    }

    public void tripOptimizer(Individual individual){
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

    public void repair(){
        repair(population);
    }

    public void repair(Population population){
        repaired.clear();
        for (Individual infeasibleIndividual : population.infeasiblePopulation){
            if (ThreadLocalRandom.current().nextDouble() < Parameters.repairProbability){
                if (Repair.repair(infeasibleIndividual, infeasibleIndividual.orderDistribution, penaltyControl)){
                    repaired.add(infeasibleIndividual);
                }
            }
        }
        population.infeasiblePopulation.removeAll(repaired);
        population.feasiblePopulation.addAll(repaired);
    }

    public void selection(){
        selection(population);
    }


    public void selection(Population population){

        // Reduce population size

        population.improvedSurvivorSelection();
        if (!Parameters.isPeriodic){
            odp.removeNoneUsedOrderDistributions(population);
        }
        bestIndividual = population.returnBestIndividual();

        //store temporary solution
        JCMSolution solution = new JCMSolution(bestIndividual.orderDistribution, bestIndividual.journeyList);
        SolutionStorer.store(solution, time, fileName);

        // Check if it has improved for early termination
        if (bestIndividualScore == bestIndividual.getFitness(false)){
            population.setIterationsWithoutImprovement(population.getIterationsWithoutImprovement()+1);
        }
        else if(population.returnBestIndividual().getFitness(false) > bestIndividualScore){
            System.out.println(population.returnBestIndividual().getFitness(false));
            System.out.println(population.returnBestIndividual().hashCode());
            System.out.println("population has become worse");
        }
        else{
            population.setIterationsWithoutImprovement(0);
            bestIndividualScore = bestIndividual.getFitness(false);

        }

        System.out.println(Arrays.toString(FitnessCalculation.getIndividualFitness(bestIndividual, 1, Parameters.initialTimeWarpPenalty, Parameters.initialOverLoadPenalty)));




    }



    public void runGA() throws IOException, GRBException {
        double time = System.currentTimeMillis();
        System.out.println("Initialize population..");
        initialize();
        long startTime = System.currentTimeMillis();
        while ((population.getIterationsWithoutImprovement() < Parameters.maxNumberIterationsWithoutImprovement &&
                System.currentTimeMillis() - startTime < Parameters.totalRuntime)) {
            System.out.println("Start generation: " + numberOfIterations);
            System.out.println(population.getIterationsWithoutImprovement());
            //Find best OD for the distribution
            findBestOrderDistribution();

            //Generate new population
            for (Individual individual : population.infeasiblePopulation) {
                if (!Master2020.Testing.IndividualTest.testIndividual(individual)) {
                    System.out.println("BEST INDIVIDUAL IS NOT COMPLETE: PRIOR");
                }
            }


            for (int j = 0; j < Parameters.numberOfOffspring; j++) {
                Individual newIndividual = PIX();
                penaltyControl.adjust(newIndividual.hasTimeWarp(), newIndividual.hasOverLoad());
                //System.out.println("Time warp: " + newIndividual.timeWarpCost + " overLoad: " + newIndividual.overLoadCost);

                educate(newIndividual, penaltyControl);

                setOptimalOrderDistribution(newIndividual);

                tripOptimizer(newIndividual);
                newIndividual.updateFitness();
                population.addChildToPopulation(newIndividual);

            }
            for (Individual individual : population.getTotalPopulation()) {
                IndividualTest.checkIfIndividualIsComplete(individual);
            }


            repair();


            /*
            System.out.println("Feas pop: " + population.feasiblePopulation.size());
            System.out.println("Infeas pop: " + population.infeasiblePopulation.size());
            */
            selection();
            /*
            System.out.println("Feas pop after: " + population.feasiblePopulation.size());
            System.out.println("Infeas pop after: " + population.infeasiblePopulation.size());

             */
            System.out.println("OBject: " + population.returnBestIndividual().hashCode());
            System.out.println("Fitness: " + population.returnBestIndividual().getFitness(false) + " feasible: " + population.returnBestIndividual().isFeasible());


            bestIndividualScore = population.returnBestIndividual().getFitness(false);
            numberOfIterations++;
        }

        Individual bestIndividual = population.returnBestIndividual();
        System.out.println("Individual feasible: " + bestIndividual.isFeasible());
        System.out.println("Fitness: " + bestIndividual.getFitness(false));

        // debug may be removed
        /*
        DataMIP dataMIP = DataConverter.convert(data);
        System.out.println("Arc Flow with extra constraints");
        JourneyBasedModel jbm = new JourneyBasedModel(dataMIP);
        jbm.testModel(bestIndividual.journeyList, bestIndividual.orderDistribution);
        PathFlowModel pfm = new PathFlowModel(dataMIP);
        pfm.testTrips(bestIndividual.journeyList, bestIndividual.orderDistribution);
        ArcFlowModel afm = new ArcFlowModel(dataMIP);
        afm.testSolution(bestIndividual.journeyList);
        System.out.println("JCM with extra constraints");
        JourneyCombinationModel jcm = new JourneyCombinationModel(data);
        jcm.runModel(bestIndividual.journeyList);
        System.out.println("Arc standalone");
        PathFlowModel pfm = new PathFlowModel(dataMIP);
        pfm.runModel(Parameters.symmetry);
        afm = new ArcFlowModel(dataMIP);
        afm.runModel(Parameters.symmetry);
         */

        if (Parameters.savePlots) {
            PlotIndividual visualizer = new PlotIndividual(data);
            visualizer.visualize(bestIndividual);
        }
        Result res = new Result(population, modelName, fileName, population.returnBestIndividual().isFeasible(), false);
        res.store(time, -1);
        if (Parameters.ODMIPProbability > 0){
            orderAllocationModel.terminateEnvironment();
        }
    }

    public static void main(String[] args) throws GRBException, IOException {
        GAController ga = new GAController();
        ga.runGA();
    }
}
