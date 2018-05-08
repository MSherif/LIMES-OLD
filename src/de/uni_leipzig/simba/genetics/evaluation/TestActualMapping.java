package de.uni_leipzig.simba.genetics.evaluation;

import java.util.HashMap;
import java.util.Map.Entry;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

public class TestActualMapping {

//	private static double accThreshold;
	
	Cache fullSource;
	Cache fullTarget;
	Mapping fullReference;
	
	public TestActualMapping(Cache source, Cache target, Mapping reference) {
		fullSource = source;
		fullTarget = target;
		fullReference = reference;
	}
	
	public TestActualMapping(ConfigReader cR, String pathToReference) {
		this(HybridCache.getData(cR.sourceInfo), HybridCache.getData(cR.targetInfo), OracleFactory.getOracle(pathToReference, "csv", "simple").getMapping());
	}


	/**
	 * To test certain expressions on data provided.
	 * @param cR instance of configReader.
	 * @param cache Cached data of source.
	 * @param cache2 Cached data of target.
	 * @param optimal optimalMapping to test against.
	 * @param expression LIMES expression to be tested.
	 * @param accThreshold global threshold to test with.
	 * @return
	 */
	public static HashMap<String, Double> testData(ConfigReader cR, Cache cache, Cache cache2, Mapping optimal, String expression, double accThreshold) {
		HashMap<String, Double> res = new HashMap<String, Double>();
		Filter f = new LinearFilter();
		// get Mapper
		SetConstraintsMapper sCM = SetConstraintsMapperFactory.getMapper( "simple", cR.sourceInfo, cR.targetInfo, 
				cache, cache2, f, cR.granularity);
		// getMapping
		try{
		Mapping m = sCM.getLinks(expression, accThreshold);
		PRFCalculator prfC = new PRFCalculator();
		res.put("precision", prfC.precision(m, optimal));
		res.put("recall", prfC.recall(m, optimal));
		res.put("f-score", prfC.fScore(m, optimal));
		}catch(java.lang.OutOfMemoryError e) {
			res.put("precision", Double.NaN);
			res.put("recall", Double.NaN);
			res.put("f-score", Double.NaN);
		}
		return res;
	}
	

	/**
	 * To test certain expressions on data provided in the configFile.
	 * @param cR instance of configReader.
	 * @param optimal optimalMapping to test against.
	 * @param expression LIMES expression to be tested.
	 * @param accThreshold global threshold to test with.
	 * @return
	 */
	public HashMap<String, Double> testData(ConfigReader cR, String expression, double accThreshold) {
		return testData(cR, fullSource, fullTarget, fullReference, expression, accThreshold);
	}
	
	public static void main(String args[]) {
		EvaluationData data = DataSetChooser.getData(DataSets.PERSON1);
		TestActualMapping tester = new TestActualMapping(data.getSourceCache(), data.getTargetCache(), data.getReferenceMapping());
		HashMap<String, Double> answer = tester.testData(data.getConfigReader(), 
				"levensthein(x.http://www.okkam.org/ontology_person1.owl#soc_sec_id,y.http://www.okkam.org/ontology_person2.owl#soc_sec_id)",
				0.34d);
		for(Entry<String, Double> e : answer.entrySet()) {
			System.out.println(e.getKey()+"="+e.getValue());
		}
	}
}
