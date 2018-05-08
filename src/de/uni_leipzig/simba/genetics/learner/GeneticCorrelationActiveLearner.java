package de.uni_leipzig.simba.genetics.learner;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.IGPProgram;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.util.PropMapper;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;

/**
 * Genetic Programming Learner using Correlation badsed approaches: clsutering and a spreading activation algorithm 
 * based on weight decay to compute the most informative link candidates.
 * @author Victor Christen
 * @author Klaus Lyko
 *
 */
public class GeneticCorrelationActiveLearner extends GeneticActiveLearner {

    static boolean testClustering = true;
    /**
     * Constant to set clustering based appraoch.
     */
    public static final boolean CLUSTERING = true;
    /**
     * Constant to use spreadding activation based on weight decay.
     */
    public static final boolean WEIGHT_DECAY = false;

    
    public GeneticCorrelationActiveLearner() {
        super();
    }
    /**
     * Constructor to set method. Use the constants 
     * @param method True=Clustering based approach. False = Spreading activation based on weight decay.
     */
    public GeneticCorrelationActiveLearner(boolean method) {
        super();
        testClustering = method;
    }

    // first get all Mappings for the current population
//	GPPopulation pop = this.gp.getGPPopulation();
    @Override
    protected Mapping getMappingForOutput(Mapping trainingData, ExpressionFitnessFunction fitness) {
        pop.sortByFitness();
        HashSet<Metric> metrics = new HashSet<Metric>();
        List<Mapping> candidateMaps = new LinkedList<Mapping>();
        // and add the best
        if (metric != null && metric.isValid()) {
            metrics.add(metric);
        }
        for (IGPProgram p : pop.getGPPrograms()) {
            Metric m = getMetric(p);
            if (m != null && m.isValid() && !metrics.contains(m)) {
                //logger.info("Adding metric "+m);
                metrics.add(m);
            }
        }
        // fallback solution if we have too less candidates
        if (metrics.size() <= 1) {
            return super.getMappingForOutput(trainingData, fitness);
        }

        // get mappings for all distinct metrics
        logger.info("Getting " + metrics.size() + " full mappings to determine controversy matches...");
        for (Metric m : metrics) {
            if (m.isValid() && m.getExpression().indexOf("falseProp") == -1) {
                candidateMaps.add(fitness.getMapping(m.getExpression(), m.getThreshold(), true));
            }
        }

        // get most controversy matches
        logger.info("Getting " + this.trainingDataSize + " controversy match candidates from " + candidateMaps.size() + " maps...");;
        //List<Triple> controversyMatches = alDecider.getControversyCandidates(candidateMaps, trainingDataSize);
        String metric = fitness.getMetric(gp.getFittestProgram()).getExpression();
        List<Triple> controversyMatches;
        if (testClustering) {
            logger.info("Starting clustering based approach");
            MappingCorrelation cor = new MappingCorrelation(this.source, this.target, metric);
            controversyMatches = cor.getDisimilarMappings(alDecider.getControversyCandidates(candidateMaps), trainingDataSize, 2);
        } else {
            logger.info("Starting weight decay based approach");
            WeightDecayCorrelation wdc = new WeightDecayCorrelation(this.source, this.target, metric);
            controversyMatches = wdc.getDisimilarMappings(alDecider.getControversyCandidates(candidateMaps), trainingDataSize, 2);
        }

        //List<Triple>
        alDecider.retrieved.addAll(controversyMatches);
        logger.info("Calculation of new data to be evaluated is ready...");
        logger.info(controversyMatches.toString());
        // construct answer
        Mapping answer = new Mapping();
        for (Triple t : controversyMatches) {
            answer.add(t.getSourceUri(), t.getTargetUri(), t.getSimilarity());
        }
        return answer;
    }

    /**
     * Test main shows usage.
     *
     * @param args
     */
    public static void main(String args[]) {
//        String configFile = "Examples/GeneticEval/PublicationData.xml";
//        Oracle o = OracleFactory.getOracle("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv", "csv", "simple");

        String configFile = "Examples/GeneticEval/DBLP-Scholar.xml";
        Oracle o = OracleFactory.getOracle("Examples/GeneticEval/Datasets/DBLP-Scholar/DBLP-Scholar_perfectMapping.csv", "csv", "simple");

        ConfigReader cR = new ConfigReader();
        cR.validateAndRead(configFile);
        int size = 10;

        // gets default property Mapping
        PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);

        LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_COR_LEARNER);
        // params for the learner
//        HashMap<String, Object> param = new HashMap<String, Object>();
//        param.put("populationSize", 20);
//        param.put("generations", 100);
//        param.put("mutationRate", 0.5f);
//        param.put("preserveFittest", false);
//        param.put("propertyMapping", propMap);
//        param.put("trainingDataSize", size);
//        param.put("granularity", 2);
//        param.put("config", cR);
        SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
        params.setPopulationSize(20);
        params.setGenerations(100);
        params.setMutationRate(0.5f);
        params.setPreserveFittestIndividual(false);
        params.setTrainingDataSize(size);
        params.setGranularity(2);
        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        // initial data

        answer = learner.learn(new Mapping());
        Mapping oracleAnswer = new Mapping();
        // looking for answers from oracle aka user
        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                if (o.ask(e1.getKey(), e2.getKey())) {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                } else {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                }
            }
        }
        try {
            FileWriter fw = new FileWriter("DBLP-ACM-ClusterEval.txt");
            fw.append("cycle fMeasure" + System.getProperty("line.separator"));
            for (int cycle = 0; cycle < 5; cycle++) {
                System.out.println("Performing learning cycle " + cycle);
                // learn
                answer = learner.learn(oracleAnswer);
                logger.info(answer.toString());
                // get best solution so far:
                answerMetric = learner.terminate();
                if (answerMetric.isValid()) {
                    PRFCalculator prfC = new PRFCalculator();
                    double fS = prfC.precision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
                    System.out.println("Cycle " + cycle + " Best = " + answerMetric + "\n\tF-Score = " + fS);
                    fw.append(cycle + " " + fS + System.getProperty("line.separator"));
                } else {
                    Logger.getLogger("Limes").warn("Method returned no valid metric!");
                }

                System.out.println("Gathering more data from user ... ");
                oracleAnswer = new Mapping();
                for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
                    for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                        if (o.ask(e1.getKey(), e2.getKey())) {
                            oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                        } else {
                            oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                        }
                    }
                }
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
