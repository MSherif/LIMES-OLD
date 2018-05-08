package de.uni_leipzig.simba.genetics.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.learner.UnSupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.learner.UnsupervisedLearner;
import de.uni_leipzig.simba.genetics.learner.UnsupervisedLinkSpecificationLearner;
import de.uni_leipzig.simba.io.ConfigReader;

public class Tester {
	/**
	 * Returns a similarity value of a date distance measure in days. This is computed
	 * as e^(-alpha * dist)
	 * @param alpha
	 * @param daydist
	 * @return
	 */
	public static double getDateSimilarity(float alpha, long daydist) {
		return Math.pow(Math.E, (-0.005 * daydist));  
	}
	
	
	public static void testAbt() {
		EvaluationData data = DataSetChooser.getData(DataSets.ABTBUY);
		Cache sC=data.getSourceCache();
		List<Instance> ins=sC.getAllInstances();
		for(Instance inst : ins) {
			System.out.println(inst);
		}
		
	}
	
	public static void readDBPediaDump(String file) {
		try {
			FileReader in = new FileReader(new File(file));
			BufferedReader br = new BufferedReader(in);
			String line = br.readLine();
			while(line != null) {
				System.out.println(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void testPointsetMeasures() {
		
		EvaluationData cities = DataSetChooser.getData(DataSets.TOWNS);
		

		
		UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(cities.getConfigReader(), cities.getPropertyMapping());
		params.setGenerations(10);
		params.setPopulationSize(10);
		params.setPreserveFittestIndividual(false);
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
	}
	
	
	public static void main(String args[]) {
		
//		testAbt();
		
		
		testPointsetMeasures();
		
		
		
//		System.out.println("alpha == 0.01");
//		for(long i = 0; i<=10; i++)
//			System.out.println("<tr><td>"+ i +"</td><td>"+ getDateSimilarity(0.01f, i)+"</td></tr>");
//		for(long i = 1; i<=5; i++)
//			System.out.println("tr><td>"+ (365*i) +"</td><td>"+ getDateSimilarity(0.01f, (365*i)) + "</td></tr>");
		
//		String dbpeadiaPerson = "C:/Users/Lyko/workspace/LIMES/resources/persondata_en.nt";
//		readDBPediaDump(dbpeadiaPerson);
	}
}
