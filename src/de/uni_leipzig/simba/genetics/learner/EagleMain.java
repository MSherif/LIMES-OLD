package de.uni_leipzig.simba.genetics.learner;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
/**
 * Class to exemplify usage of EAGLE to both learn Link Specification supervised and unsupervised.
 * Relies on local examples, e.g. Persons1&2, DBLP-ACM
 * @author Klaus Lyko
 *
 */
public class EagleMain {
	
	/**
	 * Runs a supervised active learning example on EAGLE. Using the DBLP-ACM mapping task.
	 * Data is in 
	 */
	public static void supervisedExample() {
		 Map<String, Double> results= new TreeMap<String, Double>();

	        /**
	         * Parse ConfigFile: ConfigFileReader, PropertyMapping, Dataset: Cache
	         */
	    	EvaluationData eData = DataSetChooser.getData(DataSets.DBLPACM);
	        ConfigReader cR = eData.getConfigReader();
	        Oracle o = new SimpleOracle();
	        o.loadData(eData.getReferenceMapping());
	        
	        // gets default property Mapping
	        PropertyMapping propMap = eData.getPropertyMapping();

	        LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_LEARNER);
	        // params for the learner
			SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
		      params.setPopulationSize(10);
		      params.setGenerations(10);
		      params.setMutationRate(0.5f);
		      params.setPreserveFittestIndividual(true);
		      params.setTrainingDataSize(10);
		      params.setGranularity(2); //optinal used only for numeric properties (
	        Mapping answer;
	        Metric answerMetric;
	        try {
	            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
	        } catch (InvalidConfigurationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        // initial data for learning 
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
	        /* Learning cycles */
	        for (int cycle = 0; cycle < 5; cycle++) {
	            System.out.println("Performing learning cycle " + cycle);
	            // learn, get most informative link candidates
	            answer = learner.learn(oracleAnswer);
	            // get best solution so far:
	            answerMetric = learner.terminate();
	            if (answerMetric.isValid()) { // compute quality and store reults
	                PRFCalculator prfC = new PRFCalculator();
	                double fS = prfC.precision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
	                System.out.println("Cycle " + cycle + " Best = " + answerMetric + "\n\tF-Score = " + fS);
	                results.put("Cycle("+cycle+") Best : " + answerMetric.toString()+"\n\tF-Score ",fS);
	            } else {
	                Logger.getLogger("Limes").warn("Method returned no valid metric!");
	            }
	            // ask oracle to validate most informative link candidates
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
	        // Output results
	        for( Entry<String, Double> entry : results.entrySet()){
				System.out.println(entry.getKey()+": "+entry.getValue());
			}
	}
	
	/**
	 * Example how to use an unsupervised learning algorithm with EAGLE
	 */
	public static void unsupervisedExample() {
		EvaluationData data = DataSetChooser.getData(DataSets.PERSON1_CSV);
		/*
		 * Also possible DataSets.PERSON2_CSV, RESTAURANTS_CSV, DBLPACM, ...
		 */
		UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(data.getConfigReader(), data.getPropertyMapping());
//		params.setPseudoFMeasure(new PseudoMeasures ()); //define PFM
//		params.setPFMBetaValue(1d); //set beta value for PFM
		params.setGenerations(10);
		params.setPopulationSize(10);
		
		UnsupervisedLinkSpecificationLearner learner = new UnsupervisedLearner();
		try {
			learner.init(params.getConfigReader().sourceInfo, params.getConfigReader().targetInfo, params);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		Mapping m = learner.learn();
		Metric result = learner.terminate();
		System.out.println("Finished learning. Best Mapping has size :"+m.size());
		System.out.println("Best Metric: "+result);
		PRFCalculator prf = new PRFCalculator();
		double fScore = prf.fScore(m, data.getReferenceMapping());
		System.out.println("FScore = "+fScore);
	}
	
	
	public static void main(String args[]) {
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("-s")) {
				supervisedExample();
			} else
				if(args[0].equalsIgnoreCase("-u")) {
					unsupervisedExample();
				} else {
					printUsage();
				}
				
		} else {
			printUsage();
			supervisedExample();
		}
	}
	
	private static void printUsage() {
		System.out.println("Invalid program call");
		System.out.println("Usage: java <{-s,-u}>");
		System.out.println("\t -s for supervised example");
		System.out.println("\t -u for unsupervised example");
	}
}
