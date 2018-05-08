package de.uni_leipzig.simba.util;

import java.util.HashMap;

import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.learner.Configuration;
import de.uni_leipzig.simba.learning.learner.Learner;
import de.uni_leipzig.simba.learning.learner.LinearCombinationLearner;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;

public class RavenExampleRunner {
	
	public static final String pathToConfig = "Examples/GeneticEval/";
	public static final String pathToData = pathToConfig+"Datasets/";
	
	public static final String ReferenceFile = "DBLP-ACM/DBLP-ACM_perfectMapping.csv";
	public static final String LimesConfigFile = "PublicationData.xml"; 
	
	int rounds = 1;
	
	public static Mapping propertyMapping = new Mapping();
	public static HashMap<String,String> propertyTypes = new HashMap<String,String>();
	
	public void run() {	
		// get Data
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(pathToConfig+LimesConfigFile);
		HybridCache sC = HybridCache.getData(cR.sourceInfo);
		HybridCache tC = HybridCache.getData(cR.targetInfo);
		Oracle o = OracleFactory.getOracle(pathToData+ReferenceFile, "CSV", "simple");
		/* Generate a basic property Mapping: We assume both have the same number of properties
		*  we map according to their index
		*  For types:
		*  if a property is a number it is marked as "PROPNAME AS number" otherwise a String  
		**/
		for(int i = 0; i <  1; i++) {//Math.max(cR.sourceInfo.properties.size(),cR.targetInfo.properties.size()); i++) {
			/* add the property match*/
			propertyMapping.add(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i), 1.0);
			/* function chains for the props, e.g. "lowercase->replace(something,else)"*/
			String sourcePropFunctionChain = "";//cR.sourceInfo.functions.get(cR.sourceInfo.properties.get(i));
			String targetPropFunctionChain = "";//cR.targetInfo.functions.get(cR.targetInfo.properties.get(i));
			if(sourcePropFunctionChain.indexOf("number") == -1) {// no number - we assume string
				propertyTypes.put(cR.sourceInfo.properties.get(i), "string");
			} else { // a number
				propertyTypes.put(cR.sourceInfo.properties.get(i), "spatial");
			}
			if(targetPropFunctionChain.indexOf("number") == -1) {// no number - we assume string
				propertyTypes.put(cR.targetInfo.properties.get(i), "string");
			} else { // a number
				propertyTypes.put(cR.targetInfo.properties.get(i), "spatial");
			}
		}
		
		
//		Learner learner = new BooleanClassifierLearner(cR.sourceInfo, cR.targetInfo, sC, tC, o, propertyMapping, propertyTypes, 0.5d, 0.5d);
		Learner learner = new LinearCombinationLearner(cR.sourceInfo, cR.targetInfo, sC, tC, o, propertyMapping, propertyTypes, 0.5d, 0.5d);
//		Learner learner = new PerceptronLearner(cR.sourceInfo, cR.targetInfo, sC, tC, o, propertyMapping, propertyTypes, 0.5d, 0.5d);
		

		learner.computeNextConfig(rounds);
		Configuration config = learner.getCurrentConfig();
		double prec = learner.getPrecision();
		double rec = learner.getRecall();
		String output = "====>"+rounds+": Config:\n"+config.getExpression()+"\n"+"Precision:"+prec+" Recall:"+rec;
		System.out.println(output);
	}
			
	
	public static void main(String args[]) {
		for(int i = 0; i<3; i++){
			RavenExampleRunner runner = new RavenExampleRunner();
			runner.run();
		}
	}
}
