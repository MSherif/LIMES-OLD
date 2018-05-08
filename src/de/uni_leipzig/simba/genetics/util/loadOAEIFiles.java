package de.uni_leipzig.simba.genetics.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.learner.LinkSpecificationLearner;
import de.uni_leipzig.simba.genetics.learner.LinkSpecificationLearnerFactory;
import de.uni_leipzig.simba.genetics.learner.SupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.learner.UnSupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.learner.UnsupervisedLearner;
import de.uni_leipzig.simba.genetics.learner.UnsupervisedLinkSpecificationLearner;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.query.FileQueryModule;
import de.uni_leipzig.simba.selfconfig.Experiment;
import de.uni_leipzig.simba.specification.LinkSpec;

public class loadOAEIFiles {
	
	
	
	public static Mapping readOAEI2014IdentityMapping(String filePath) {
		return Experiment.readOAEIMapping(filePath);
	}
	
	public static Cache readOAEI2014File(String filePath) {
		
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(filePath);
		
		Cache cache = new MemoryCache();
		
		FileQueryModule module = new FileQueryModule(cR.sourceInfo);
		
		module.fillCache(cache);
		
		return cache;
	}
	
	
	public static void testOAEI2014() {
		EvaluationData data = DataSetChooser.getData(DataSets.OAEI2014BOOKS);

		
		Mapping ref = data.getReferenceMapping();
		System.out.println(ref);
		System.out.println(ref.size());
//		InputStreamReader r = new InputStreamReader(System.in);
//
//		try {
//			r.read();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(data.getConfigReader(), data.getPropertyMapping());
		params.setGenerations(50);
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
		double precision = prf.precision(m,  data.getReferenceMapping());
		double recall = prf.recall(m,  data.getReferenceMapping());
		System.out.println("FScore = "+fScore);
		System.out.println("Precision = "+precision);
		System.out.println("Recall = "+recall);
	}
	
	public static void testOAEI2014Supervised() {
		Map<String, Double> results= new TreeMap<String, Double>();
		EvaluationData data = DataSetChooser.getData(DataSets.OAEI2014BOOKS);
		Oracle o = new SimpleOracle();
		o.loadData(data.getReferenceMapping());
		LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_LEARNER);
        // params for the learner
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(data.getConfigReader(), data.getPropertyMapping());
	      params.setPopulationSize(20);
	      params.setGenerations(100);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(true);
	      params.setTrainingDataSize(10);
	      params.setGranularity(2); //optinal used only for numeric properties (
        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(data.getConfigReader().getSourceInfo(), data.getConfigReader().getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // initial data for learning 
        answer = learner.learn(data.getReferenceMapping());
        Mapping oracleAnswer = new Mapping();
        
        
        answerMetric = learner.terminate();
	      if (answerMetric.isValid()) { // compute quality and store reults
	          PRFCalculator prfC = new PRFCalculator();
	          double fS = prfC.precision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
	          System.out.println("Cycle " + 0 + " Best = " + answerMetric + "\n\tF-Score = " + fS);
	          results.put("Cycle("+0+") Best : " + answerMetric.toString()+"\n\tF-Score ",fS);
	      } else {
	          Logger.getLogger("Limes").warn("Method returned no valid metric!");
	      }
        // looking for answers from oracle aka user
//        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
//            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
//                if (o.ask(e1.getKey(), e2.getKey())) {
//                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
//                } else {
//                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
//                }
//            }
//        }
//        /* Learning cycles */
//        for (int cycle = 0; cycle < 5; cycle++) {
//            System.out.println("Performing learning cycle " + cycle);
//            // learn, get most informative link candidates
//            answer = learner.learn(oracleAnswer);
//            // get best solution so far:
//            answerMetric = learner.terminate();
//            if (answerMetric.isValid()) { // compute quality and store reults
//                PRFComputer prfC = new PRFComputer();
//                double fS = prfC.computePrecision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
//                System.out.println("Cycle " + cycle + " Best = " + answerMetric + "\n\tF-Score = " + fS);
//                results.put("Cycle("+cycle+") Best : " + answerMetric.toString()+"\n\tF-Score ",fS);
//            } else {
//                Logger.getLogger("Limes").warn("Method returned no valid metric!");
//            }
//            // ask oracle to validate most informative link candidates
//            System.out.println("Gathering more data from user ... ");
//            oracleAnswer = new Mapping();
//            for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
//                for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
//                    if (o.ask(e1.getKey(), e2.getKey())) {
//                        oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
//                    } else {
//                        oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
//                    }
//                }
//            }
//        }
        // Output results
        for( Entry<String, Double> entry : results.entrySet()){
			System.out.println(entry.getKey()+": "+entry.getValue());
		}
		
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Cache sC = readOAEI2014File("resources/OAEI2014/oaei2014_identity.xml");
//		
//		for(Instance inst : sC.getAllInstances()) {
//			System.out.println(inst);
//		}
		
		testOAEI2014Supervised();
//		testMulipleValueLinking();
	}
	
	
	public static void testMulipleValueLinking() {
		Instance i1 = new Instance("i1");
		i1.addProperty("p1", "aaaa");
		i1.addProperty("p1", "bbbb");
		i1.addProperty("p1", "cccc");
		i1.addProperty("p1", "dddd");
		
		Instance i2 = new Instance("i2");
		i2.addProperty("p1", "aaaa");
		
		
		Instance i3 = new Instance("i3");
		i3.addProperty("p1", "cccc");
		i3.addProperty("p1", "dddd");
		
		Cache sC = new MemoryCache(); 
		sC.addInstance(i1);
		Cache tC = new MemoryCache();
		tC.addInstance(i2);
		tC.addInstance(i3);
		
		LinkSpec spec = new LinkSpec();
		spec.setAtomicFilterExpression("trigrams", "x.p1", "y.p1");
		spec.threshold = 0.1d;
		
		ExecutionPlanner planner = new HeliosPlanner(sC, tC);
		ExecutionEngine engine;
		
		engine = new ExecutionEngine(sC, tC, "?x", "?y");
		
		Mapping mapping = new Mapping();
		try {
			 mapping = engine.runNestedPlan(planner.plan(spec));
		}catch(Exception e) {
			System.err.print("Error executing spec "+spec);
			e.printStackTrace();
			
		}

		System.out.println(mapping.size());
		System.out.println(mapping);
		
		
	}

}
