package de.uni_leipzig.simba.genetics.learner;


import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.core.PreprocessingFitnessFunction;
import de.uni_leipzig.simba.genetics.util.PropMapper;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;

/**
 * Learner to also learn preprocessing steps
 * @author Klaus Lyko
 *
 */
public class PropertyLearner extends GeneticActiveLearner implements LinkSpecificationLearner {

    @Override
    protected void setUpFitness() throws InvalidConfigurationException {
        Configuration.reset();
        fitness = PreprocessingFitnessFunction.getInstance(config, o,
                "f-score", trainingDataSize);
        //	fitness.useFullCaches();
        fitness.useFScore();
        config.setFitnessFunction(fitness);
    }
    
    @Override
	protected void setUp() throws InvalidConfigurationException {
		// control property mapping
		if(propMap == null)
			propMap = new PropertyMapping();
		if(!propMap.wasSet()) {
			logger.warn("No Property Mapping set we use a fallback solution.");
			if(cR == null)
				propMap.setDefault(source, target);
			else {
				// or we find a way to use the class mapper.
				propMap.setDefault(source, target);				
			}
		}
		o = new SimpleOracle();
		o.loadData(new Mapping());
		// configure evolution
		config = new LinkSpecGeneticLearnerConfig(source, target, propMap);
		setUpFitness();
		config.setPopulationSize(popSize);
		config.setCrossoverProb(crossoverRate);
		config.setMutationProb(mut);
		config.setPreservFittestIndividual(preserveFittest);
		
		gpP = new ExpressionProblem(config, true);
		gp = gpP.create();
	}

    
    public static void main(String args[]) {
        
        Map<String, Double> results= new TreeMap<String, Double>();
    	
        String configFile = "Examples/GeneticEval/PublicationData.xml";
        ConfigReader cR = new ConfigReader();
        cR.validateAndRead(configFile);
        Oracle o = OracleFactory.getOracle("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv", "csv", "simple");
        int size = 10;

        // gets default property Mapping
        PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);

        LinkSpecificationLearner learner = new PropertyLearner();
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
	      params.setTrainingDataSize(50);
	      params.setGranularity(2);
	      params.setTrainingDataSize(size);
        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
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

        for (int cycle = 0; cycle < 5; cycle++) {
            System.out.println("Performing learning cycle " + cycle);
            // learn
            answer = learner.learn(oracleAnswer);
            // get best solution so far:
            answerMetric = learner.terminate();
            if (answerMetric.isValid()) {
                PRFCalculator prfC = new PRFCalculator();
                double fS = prfC.precision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
                System.out.println("Cycle " + cycle + " Best = " + answerMetric + "\n\tF-Score = " + fS);
                results.put("Cycle("+cycle+") Best : " + answerMetric.toString()+"\n\tF-Score ",fS);
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
        for( Entry<String, Double> entry : results.entrySet()){
			System.out.println(entry.getKey()+": "+entry.getValue());
		}
    }
}
