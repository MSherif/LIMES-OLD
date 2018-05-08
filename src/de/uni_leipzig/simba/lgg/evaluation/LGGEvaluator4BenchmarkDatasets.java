/**
 * 
 */
package de.uni_leipzig.simba.lgg.evaluation;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.lgg.CompleteLGG;
import de.uni_leipzig.simba.lgg.LGG;
import de.uni_leipzig.simba.lgg.LGGFactory;
import de.uni_leipzig.simba.lgg.refinementOperator.RefinementNode;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.multilinker.MappingMath;

/**
 * Evaluate Refinement based LGG for benchmark datasets
 * DBLP-ACM, Abt-Buy,Amazon-GoogleProducts, DBLP-Scholar, 
 * Person1, Person2, Restaurants, DBLP-LinkedMDB and Dailymed-DrugBank
 * 
 * @author sherif
 * 
 */
public class LGGEvaluator4BenchmarkDatasets {
	/**
	 * 
	 */
	private static final Logger logger = Logger.getLogger(LGGEvaluator4BenchmarkDatasets.class.getName());
	protected static final double MIN_COVERAGE = 0.6;

	public static Cache source;
	public static Cache sourceTrainCache = new HybridCache();
	public static Cache sourceTestCache = new HybridCache();
	public static Cache target;
	public static Cache targetTrainCache = new HybridCache();
	public static Cache targetTestCache = new HybridCache();
	public static Mapping reference = new Mapping();
	public static String resultStr = new String();

	/**
	 * Computes a sample of the reference dataset for experiments
	 */
	public static Mapping sampleReferenceMap(Mapping reference, double fraction) {
		if(fraction == 1){
			return reference;
		}
		int mapSize = reference.map.keySet().size();
		if (fraction > 1) {
			fraction = 1 / fraction;
		}
		int size = (int) (mapSize * fraction);
		Set<Integer> index = new HashSet<>();
		//get random indexes
		for (int i = 0; i < size; i++) {
			int number;
			do {
				number = (int) (mapSize * Math.random()) - 1;
			} while (index.contains(number));
			index.add(number);
		}

		//get data
		Mapping sample = new Mapping();
		int count = 0;
		for (String key : reference.map.keySet()) {
			if (index.contains(count)) {
				sample.map.put(key, reference.map.get(key));
			}
			count++;
		}

		// compute sample size
		for (String key : sample.map.keySet()) {
			for (String value : sample.map.get(key).keySet()) {
				sample.size++;
			}
		}
		return sample;
	}

	public static Mapping sampleReference(Mapping reference, float start, float end) {
		if(start == 0 && end == reference.size()){
			return reference;
		}
		int count = 0;
		Mapping sample = new Mapping();
		for (String key : reference.map.keySet()) {
			for (String value : reference.map.get(key).keySet()) {
				if(count < start*reference.size()){
					count++;
				}else{
					sample.add(key, value, reference.map.get(key).get(value));
					sample.size++;
					count++;
				}
				if(count >= end*reference.size()){
					return sample;
				}
			}
		}
		return null;
	}

	/**
	 * Extract the source and target instances based on the input sample
	 * @param learnMap
	 * @return
	 * @author sherif
	 */
	protected static void fillTrainingCaches(Mapping learnMap) {
		if (learnMap.size() == reference.size()){
			sourceTrainCache = source;
			targetTrainCache = target;
		}else{
			sourceTrainCache = new HybridCache();
			targetTrainCache = new HybridCache();
			for (String s : learnMap.map.keySet()) {
				if(source.containsUri(s)){
					sourceTrainCache.addInstance(source.getInstance(s));
					for (String t : learnMap.map.get(s).keySet()) {
						if(target.containsUri(t)){
							targetTrainCache.addInstance(target.getInstance(t));
						}else{
							logger.warn("Instance " + t + " not exist in the target dataset");
						}
					}
				}else{
					logger.warn("Instance " + s + " not exist in the source dataset");
				}
			}
		}
	}

	/**
	 * Extract the source and target instances based on the input sample
	 * @param trainMap
	 * @return
	 * @author sherif
	 */
	protected static void fillTestingCaches(Mapping trainMap) {
		if (trainMap.size() == reference.size()){
			sourceTestCache = source;
			targetTestCache = target;
		}else{
			sourceTestCache = new HybridCache();
			targetTestCache = new HybridCache();
			for (String s : trainMap.map.keySet()) {
				if(source.containsUri(s)){
					sourceTestCache.addInstance(source.getInstance(s));
					for (String t : trainMap.map.get(s).keySet()) {
						if(target.containsUri(t)){
							targetTestCache.addInstance(target.getInstance(t));
						}else{
							logger.warn("Instance " + t + " not exist in the target dataset");
						}
					}
				}else{
					logger.warn("Instance " + s + " not exist in the source dataset");
				}
			}
		}
	}



	/**
	 * Remove mapping entries with missing source or target instances
	 * @param map
	 * @return
	 * @author sherif
	 */
	protected static Mapping removeLinksWithNoInstances(Mapping map) {
		Mapping result = new Mapping();
		for (String s : map.map.keySet()) {
			for (String t : map.map.get(s).keySet()) {
				if(source.containsUri(s) && target.containsUri(t)){
					result.add(s,t, map.map.get(s).get(t));
				}
			}
		}
		return result;
	}


	private static String evaluatePrundGain(DataSets d) {
		resultStr += "CompleteLGG\t" + d +"\n" +
				"Sample\tpruneNodesCount\tPruningTime\n";
		EvaluationData data = DataSetChooser.getData(d);
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);

		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));

		for(int s = 1 ; s <= 10 ; s +=1){
			logger.info("Running Complete LGG for the " + d + " dataset with positive example size = " +  s*10 + "%");
			Mapping referenceSample = sampleReferenceMap(reference, s/10f);
			fillTrainingCaches(referenceSample);
			referenceSample.initReversedMap();

			// 1. Learning phase
			CompleteLGG lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
			lgg.getBestSolution();

			resultStr +=  (int) s*10 + "%"							+ "\t" + 
					lgg.pruneNodeCount + "\t" + lgg.pruningTime + "\n";

			System.out.println(d + " Results so far:\n" + resultStr);
		}
		System.out.println(d + " Final rasults:\n" + resultStr);
		return resultStr;
	}

	private static String evaluateLGG(DataSets d, String lggType) {
		return evaluateLGG(d, lggType, -1);
	}


	private static String evaluateLGG(DataSets d, String lggType, int posExFrac) {
		if(lggType.equalsIgnoreCase("complete")){
			return evaluateCompleteLGG(d, posExFrac);
		}
		resultStr += lggType + "LGG\t" + d +"\n" +
				"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";
		EvaluationData data = DataSetChooser.getData(d);
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);

		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		//		System.out.println(reference.size());System.exit(1);
		if(posExFrac <= 0){ // learn using 10%, 20%, ... , 100%
			for(int s = 1 ; s <= 10 ; s +=1){
				logger.info("Running " + lggType + " LGG for the " + d + " dataset with positive example size = " +  s*10 + "%");
				Mapping referenceSample = sampleReferenceMap(reference, s/10f);
				fillTrainingCaches(referenceSample);
				referenceSample.initReversedMap();

				// 1. Learning phase
				long start = System.currentTimeMillis();
				LGG lgg = LGGFactory.createOperator( lggType, sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
				Mapping mapSample = lgg.getMapping();

				String metricExpr = lgg.getMetricExpression();
				resultStr +=  (int) s*10 + "%"							+ "\t" + 
						PRFCalculator.precision(mapSample, referenceSample)+ "\t" + 
						PRFCalculator.recall(mapSample, referenceSample) 	+ "\t" + 
						PRFCalculator.fScore(mapSample, referenceSample) 	+ "\t" +
						(System.currentTimeMillis() - start) 			+ "\t" +
						metricExpr 					+ "\t" ;

				// 2. Apply for the whole KB
				SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
						new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
				start = System.currentTimeMillis();
				String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
				Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
				Mapping kbMap = mapper.getLinks(expression, threshold);

				resultStr += PRFCalculator.precision(kbMap, reference)	+ "\t" + 
						PRFCalculator.recall(kbMap, reference) 	 	+ "\t" + 
						PRFCalculator.fScore(kbMap, reference) 	 	+ "\t" +
						(System.currentTimeMillis() - start) 		+ "\n" ;
				System.out.println(d + " Results so far:\n" + resultStr);
			}
		}else{ // learn using provided leaningRat
			logger.info("Running " + lggType + " LGG for the " + d + " dataset with positive example size = " +  posExFrac + "%");
			Mapping referenceSample = sampleReferenceMap(reference, posExFrac/100f);
			fillTrainingCaches(referenceSample);
			logger.info("Learning using " + referenceSample.size() + " examples.");

			referenceSample.initReversedMap();

			// 1. Learning phase
			long start = System.currentTimeMillis();
			LGG lgg = LGGFactory.createOperator( lggType, sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
			Mapping mapSample = lgg.getMapping();

			PRFCalculator prf = new PRFCalculator();
			String metricExpr = lgg.getMetricExpression();
			resultStr +=  posExFrac + "%"							+ "\t" + 
					prf.precision(mapSample, referenceSample)+ "\t" + 
					prf.recall(mapSample, referenceSample) 	+ "\t" + 
					prf.fScore(mapSample, referenceSample) 	+ "\t" +
					(System.currentTimeMillis() - start) 			+ "\t" +
					metricExpr 					+ "\t" ;

			// 2. Apply for the whole KB
			SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
					new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
			start = System.currentTimeMillis();
			String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
			Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
			Mapping kbMap = mapper.getLinks(expression, threshold);

			resultStr += prf.precision(kbMap, reference)	+ "\t" + 
					prf.recall(kbMap, reference) 	 	+ "\t" + 
					prf.fScore(kbMap, reference) 	 	+ "\t" +
					(System.currentTimeMillis() - start) 		+ "\n" ;
			System.out.println(d + " Results so far:\n" + resultStr);

		}
		System.out.println(d + " Final rasults:\n" + resultStr);
		return resultStr;
	}	



	/**
	 * learning where the training data fraction is posExFrac
	 * and testing on only rest the unseen data 
	 * @param d
	 * @param lggType
	 * @param posExFrac
	 * @return
	 * @author sherif
	 */
	private static double evaluateLGGWithSeperation(DataSets d, String lggType, double posExFrac) {
		EvaluationData data = DataSetChooser.getData(d);

		//Fill caches
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);
		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));

		if(lggType.equalsIgnoreCase("complete")){
			double f1 = EvaluateCompleteLGGWithSeperation(d, lggType, posExFrac);
			//			resultStr = resultStr += //"Complete LGG\t" + d +"\n" +
			//					"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\tpruneNodeCount\tpruningTime\n" +
			//					resultStr;
			return f1;
		}
		double f1 = EvaluateSimpleLGGWithSeperation(d, lggType, posExFrac);
		//		resultStr = lggType + "LGG\t" + d +"\n" +
		//				"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n" +
		//				resultStr;
		return f1;
	}

	private static double EvaluateSimpleLGGWithSeperation(DataSets d, String lggType, double posExFrac) {
		//		resultStr += lggType + "LGG\t" + d +"\n" +
		//				"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";

		if(posExFrac < 0){
			for(int trainingFactor = 1 ; trainingFactor <= 10 ; trainingFactor +=1){
				singleEvaluateSimpleLGGWithSeperation(d, trainingFactor);
			}
		}else{
			double f1 = singleEvaluateSimpleLGGWithSeperation(d, posExFrac);
			//			System.out.println(d + " Final rasults:\n" + resultStr);
			return f1;
		}
		return -1;
	}

	/**
	 * evaluation of LGG where the training data fraction is posExFrac
	 * and testing on the rest unseen data 
	 * @param d
	 * @param lggType
	 * @param trainingFactor
	 * @author sherif
	 */
	private static double singleEvaluateSimpleLGGWithSeperation(DataSets d, double trainingFactor) {
		double fScore = -1;
		logger.info("Running Simple LGG for the " + d + " dataset with positive example size = " +  trainingFactor*10 + "%");
		Mapping trainMap = sampleReferenceMap(reference, trainingFactor/10f);
		Mapping testMap  = MappingMath.removeSubMap(reference, trainMap);
		fillTrainingCaches(trainMap);
		fillTestingCaches(testMap);
		trainMap.initReversedMap();

		// 1. Learning phase
		long start = System.currentTimeMillis();
		LGG lgg = LGGFactory.createOperator( "Simple", sourceTrainCache, targetTrainCache, trainMap, MIN_COVERAGE);
		Mapping learnedMap = lgg.getMapping();
		learnedMap.initReversedMap();

		String metricExpr = lgg.getMetricExpression();
		resultStr +=  (int) trainingFactor*10 + "%"							+ "\t" + 
				PRFCalculator.precision(learnedMap, trainMap)+ "\t" + 
				PRFCalculator.recall(learnedMap, trainMap) 	+ "\t" + 
				PRFCalculator.fScore(learnedMap, trainMap) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 					+ "\t" ;

		// 2. Apply for the rest of the KB
		SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
				new KBInfo("?x"), new KBInfo("?y"), sourceTestCache, targetTestCache, new LinearFilter(), 2);
		start = System.currentTimeMillis();
		String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
		Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
		Mapping learnedTestMap = mapper.getLinks(expression, threshold);

		fScore = PRFCalculator.fScore(learnedTestMap, testMap);
		resultStr += PRFCalculator.precision(learnedTestMap, testMap)	+ "\t" + 
				PRFCalculator.recall(learnedTestMap, testMap) 	 	+ "\t" + 
				fScore 	 	+ "\t" +
				(System.currentTimeMillis() - start) 		+ "\n" ;
		System.out.println(d + " Results so far:\n" + resultStr);
		return fScore;
	}	


	/**
	 * @param d
	 * @return
	 * @author sherif
	 */
	private static String evaluateCompleteLGG(DataSets d) {
		resultStr += "Complete LGG\t" + d +"\n" +
				"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\tpruneNodeCount\tpruningTime\n";
		EvaluationData data = DataSetChooser.getData(d);
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);

		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		for(int s = 10 ; s <= 10 ; s +=1){
			logger.info("Running Complete WOMBAT for the " + d + " dataset with positive example size = " +  s*10 + "%");
			Mapping referenceSample = sampleReferenceMap(reference, s/10f);
			fillTrainingCaches(referenceSample);
			referenceSample.initReversedMap();

			// 1. Learning phase
			long start = System.currentTimeMillis();
			//			LGG lgg = LGGFactory.createOperator( "complete", sourceSample, targetSample, referenceSample, MIN_COVERAGE);
			CompleteLGG lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
			Mapping mapSample = lgg.getMapping();

			String metricExpr = lgg.getMetricExpression();
			resultStr +=  (int) s*10 + "%"							+ "\t" + 
					PRFCalculator.precision(mapSample, referenceSample)+ "\t" + 
					PRFCalculator.recall(mapSample, referenceSample) 	+ "\t" + 
					PRFCalculator.fScore(mapSample, referenceSample) 	+ "\t" +
					(System.currentTimeMillis() - start) 			+ "\t" +
					metricExpr 										+ "\t" ;

			// 2. Apply for the whole KB
			SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
					new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
			start = System.currentTimeMillis();
			String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
			Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
			Mapping kbMap = mapper.getLinks(expression, threshold);

			resultStr += PRFCalculator.precision(kbMap, reference)	+ "\t" + 
					PRFCalculator.recall(kbMap, reference) 	 	+ "\t" + 
					PRFCalculator.fScore(kbMap, reference) 	 	+ "\t" +
					(System.currentTimeMillis() - start) 		+ "\t" +
					lgg.pruneNodeCount 							+ "\t" + 	
					lgg.pruningTime 							+ "\n";

			System.out.println(d + " Results so far:\n" + resultStr);
		}
		System.out.println(d + " Final rasults:\n" + resultStr);
		return resultStr;
	}	

	private static String evaluateCompleteLGG(DataSets d, int posExFrac) {
		EvaluationData data = DataSetChooser.getData(d);
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);

		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		logger.info("Running Complete WOMBAT for the " + d + " dataset with positive example size = " +  posExFrac + "%");
		Mapping referenceSample = sampleReferenceMap(reference, (double) posExFrac/100);
		System.out.println("referenceSample.size:" + referenceSample.size);
		fillTrainingCaches(referenceSample);
		referenceSample.initReversedMap();

		// 1. Learning phase
		long start = System.currentTimeMillis();
		//			LGG lgg = LGGFactory.createOperator( "complete", sourceSample, targetSample, referenceSample, MIN_COVERAGE);
		CompleteLGG lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
		Mapping mapSample = lgg.getMapping();

		String metricExpr = lgg.getMetricExpression();
		resultStr +=  (int) posExFrac + "%"							+ "\t" + 
				PRFCalculator.precision(mapSample, referenceSample)+ "\t" + 
				PRFCalculator.recall(mapSample, referenceSample) 	+ "\t" + 
				PRFCalculator.fScore(mapSample, referenceSample) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 										+ "\t" ;

		// 2. Apply for the whole KB
		SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
				new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
		start = System.currentTimeMillis();
		String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
		Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
		Mapping kbMap = mapper.getLinks(expression, threshold);

		resultStr += PRFCalculator.precision(kbMap, reference)	+ "\t" + 
				PRFCalculator.recall(kbMap, reference) 	 	+ "\t" + 
				PRFCalculator.fScore(kbMap, reference) 	 	+ "\t" +
				(System.currentTimeMillis() - start) 		+ "\t" +
				lgg.pruneNodeCount 							+ "\t" + 	
				lgg.pruningTime 							+ "\n";

		System.out.println(d + " Results so far:\n" + resultStr);
		System.out.println(d + " Final rasults:\n" + resultStr);
		return resultStr;
	}	

	private static double EvaluateCompleteLGGWithSeperation(DataSets d, String lggType, double posExFrac) {
		//		resultStr += "Complete LGG\t" + d +"\n" +
		//				"Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\tpruneNodeCount\tpruningTime\n";

		if(posExFrac < 0){
			for(int trainingFactor = 1 ; trainingFactor <= 10 ; trainingFactor +=1){
				singleEvaluateCompleteLGGWithSeperation(d, trainingFactor);
			}
		}else{
			double f1 = singleEvaluateCompleteLGGWithSeperation(d, posExFrac);
			System.out.println(d + " Final rasults:\n" + resultStr);
			return f1;
		}
		return -1;
	}

	private static double singleEvaluateCompleteLGGWithSeperation(DataSets d, double trainingFactor) {
		double FScore = -1;
		logger.info("Running Complete WOMBAT for the " + d + " dataset with positive example size = " +  trainingFactor*10 + "%");
		Mapping trainMap = sampleReferenceMap(reference, trainingFactor/10);
		Mapping testMap  = MappingMath.removeSubMap(reference, trainMap);
		fillTrainingCaches(trainMap);
		fillTestingCaches(testMap);
		trainMap.initReversedMap();

		// 1. Learning phase
		long start = System.currentTimeMillis();
		//		LGG lgg = LGGFactory.createOperator( "complete", sourceTrainCache, targetTrainCache, trainMap, MIN_COVERAGE);
		CompleteLGG lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, trainMap, MIN_COVERAGE);
		Mapping learnedMap = lgg.getMapping();
		learnedMap.initReversedMap();

		PRFCalculator prf = new PRFCalculator();
		String metricExpr = lgg.getMetricExpression();
		resultStr +=  (int) trainingFactor*10 + "%"							+ "\t" + 
				prf.precision(learnedMap, trainMap)+ "\t" + 
				prf.recall(learnedMap, trainMap) 	+ "\t" + 
				prf.fScore(learnedMap, trainMap) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 										+ "\t" ;

		// 2. Apply for the whole KB
		SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
				new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
		start = System.currentTimeMillis();
		String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
		Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
		Mapping kbMap = mapper.getLinks(expression, threshold);

		FScore = prf.fScore(kbMap, reference);
		resultStr += prf.precision(kbMap, reference)	+ "\t" + 
				prf.recall(kbMap, reference) 	 	+ "\t" + 
				FScore 	 	+ "\t" +
				(System.currentTimeMillis() - start) 		+ "\t" +
				lgg.pruneNodeCount 							+ "\t" + 	
				lgg.pruningTime 							+ "\n";

		System.out.println(d + " Results so far:\n" + resultStr);
		System.out.println(d + " Final results:\n" + resultStr);
		return FScore;
	}	


	private static void Fmeasure2Time(String[] args) {
		logger.info("Running experiment: " + args[0]);
		if(args[0].equalsIgnoreCase("all")){
			for(DataSets d : DataSets.values()){
				logger.info("Evaluate dataset: " + d);
				Fmeasure2Time(d);	
			}
		}else if(args[0].equalsIgnoreCase("DBLP-ACM")){
			Fmeasure2Time(DataSets.DBLPACM);
		}else if(args[0].equalsIgnoreCase("Abt-Buy")){
			Fmeasure2Time(DataSets.ABTBUY);
		}else if(args[0].equalsIgnoreCase("Amazon-GoogleProducts")){
			Fmeasure2Time(DataSets.AMAZONGOOGLE);
		}else if(args[0].equalsIgnoreCase("DBLP-Scholar")){
			Fmeasure2Time(DataSets.DBLPSCHOLAR);
		}else if(args[0].equalsIgnoreCase("Person1")){
			Fmeasure2Time(DataSets.PERSON1);
		}else if(args[0].equalsIgnoreCase("Person2")){
			Fmeasure2Time(DataSets.PERSON2);
		}else if(args[0].equalsIgnoreCase("Restaurants")){
			Fmeasure2Time(DataSets.RESTAURANTS);
		}else if(args[0].equalsIgnoreCase("Restaurants_CSV")){
			Fmeasure2Time(DataSets.RESTAURANTS_CSV);
		}else if(args[0].equalsIgnoreCase("DBpedia-LinkedMDB")){
			Fmeasure2Time(DataSets.DBPLINKEDMDB);
		}else if(args[0].equalsIgnoreCase("Dailymed-DrugBank")){
			Fmeasure2Time(DataSets.DRUGS);
		}else{
			System.out.println("Experiment " + args[0] + " Not implemented yet");
		}
	}

	private static String Fmeasure2Time(DataSets d) {
		resultStr += " CompleteLGG\t" + d +"\n" +
				"Sample\tFP\tTP\tF\tT\n";
		EvaluationData data = DataSetChooser.getData(d);
		source = data.getSourceCache();
		target = data.getTargetCache();
		reference = data.getReferenceMapping();

		// remove error mappings (if any)
		int refMapSize = reference.size();
		reference = removeLinksWithNoInstances(reference);

		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		for(int s = 1 ; s <= 10 ; s +=1){
			logger.info("Running complete LGG for the " + d + " dataset with positive example size = " +  s*10 + "%");
			Mapping referenceSample = sampleReferenceMap(reference, s/10f);
			fillTrainingCaches(referenceSample);
			referenceSample.initReversedMap();

			// WOMBAT with prune
			// 1. Learning phase
			long start = System.currentTimeMillis();
			CompleteLGG lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
			Mapping mapSample = lgg.getMapping();

			PRFCalculator prf = new PRFCalculator();
			String metricExpr = lgg.getMetricExpression();

			// 2. Apply for the whole KB
			SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
					new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
			start = System.currentTimeMillis();
			String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
			Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
			Mapping kbMap = mapper.getLinks(expression, threshold);
			System.out.println("Search tree size with prune:" + lgg.root.size());
			resultStr += (int) s*10 + "%"					+ "\t" +  
					prf.fScore(kbMap, reference) 	+ "\t" +
					(System.currentTimeMillis() - start) 	+ "\t" ;

			// WOMBAT without prune
			// 1. Learning phase
			start = System.currentTimeMillis();
			lgg = new CompleteLGG(sourceTrainCache, targetTrainCache, referenceSample, MIN_COVERAGE);
			lgg.usePruning = false;
			mapSample = lgg.getMapping();
			metricExpr = lgg.getMetricExpression();

			// 2. Apply for the whole KB
			mapper = SetConstraintsMapperFactory.getMapper("simple",
					new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
			start = System.currentTimeMillis();
			expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
			threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
			kbMap = mapper.getLinks(expression, threshold);
			System.out.println("Search tree size without prune:" + lgg.root.size());
			resultStr += prf.fScore(kbMap, reference) 	+ "\t" +
					(System.currentTimeMillis() - start) 		+ "\n" ;
			System.out.println(d + " Results so far:\n" + resultStr);
		}
		System.out.println(d + " Final rasults:\n" + resultStr);
		return resultStr;
	}

	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		RefinementNode.saveMapping = true;
//		evaluateUnsupervisedSimpleLGGWithSeperation(DataSets.DBLPSCHOLAR);
//		System.exit(0);
//		evaluateUnsupervisedCompleteLGGWithSeperation(DataSets.DBLPSCHOLAR);
//		System.exit(0);

			

		String overAllResults = new String();
		for(int repeat = 0 ; repeat < 25 ; repeat++ ){
			double f1 = -1, bestF1 = -1;
			String bestResult = new String();
			long startTime = System.currentTimeMillis();
			//			while (bestF1 < 1 && (System.currentTimeMillis() < (startTime + 120000))){
			resultStr = new String();
			try{
				f1 = evaluateLGGWithSeperation(toDataset(args[0]), args[1], Double.parseDouble(args[2]));
				System.out.println("************************---------------"+resultStr);
			}catch(Exception e){
				System.err.println(e);
				repeat--;
			}
			//				if(f1 > bestF1){
			//					bestF1 = f1;
			bestResult = resultStr;
			//				}
			//			}
			System.out.println("----- BEST RESULT SO FAR-----");
			System.out.println(bestResult);
			overAllResults += bestResult;
			//			if(bestF1 == 1){
			//				break;
			//			}
			break;
		}
		System.out.println("----- OVERALL RESULT -----");
		System.out.println(overAllResults);


		/*
		printGoldStandardSizes(); System.exit(0);	
		//		evaluatePrundGain(args);
		//		Fmeasure2Time(args);
		//		evaluateLGG(args);
		String bestResultSoFar = new String();
		double f = 0d;
		for(int i = 0 ; i < 100 ; i++){
			String result = evaluateLGG(args, 2);
			//			double runF = Double.parseDouble(result.split("\t")[20]);
			//			System.out.println(runF);
			//			if(f < runF){
			//				f = runF;
			//				bestResultSoFar = result;
			//			}
		}
		System.out.println("Best result so fart: " + bestResultSoFar);		
		 */
	}

	private static void evaluateLGG(String[] args) {
		if(args.length < 2){
			System.out.println("Please enter LGG type and dataset name(s)");
			System.exit(1);
		}
		String lggType = new String();
		if(LGGFactory.getNames().contains(args[0].toLowerCase())){
			lggType = args[0];
		}else{
			System.out.println(args[0] + " is not yet implemented");
			System.exit(1);
		}
		logger.info("Running experiment: " + args[1]);
		if(args[1].equalsIgnoreCase("all")){
			for(DataSets d : DataSets.values()){
				logger.info("Evaluate dataset: " + d);
				evaluateLGG(d, lggType);	
			}
		}else{
			for(int i = 0; i< args.length ; i++){
				if(args[i].equalsIgnoreCase("DBLP-ACM")){
					evaluateLGG(DataSets.DBLPACM, lggType);
				}else if(args[i].equalsIgnoreCase("Abt-Buy")){
					evaluateLGG(DataSets.ABTBUY, lggType);
				}else if(args[i].equalsIgnoreCase("Amazon-GoogleProducts")){
					evaluateLGG(DataSets.AMAZONGOOGLE, lggType);
				}else if(args[i].equalsIgnoreCase("DBLP-Scholar")){
					evaluateLGG(DataSets.DBLPSCHOLAR, lggType);
				}else if(args[i].equalsIgnoreCase("Person1")){
					evaluateLGG(DataSets.PERSON1, lggType);
				}else if(args[i].equalsIgnoreCase("Person2")){
					evaluateLGG(DataSets.PERSON2, lggType);
				}else if(args[i].equalsIgnoreCase("Restaurants")){
					evaluateLGG(DataSets.RESTAURANTS, lggType);
				}else if(args[i].equalsIgnoreCase("Restaurants_CSV")){
					evaluateLGG(DataSets.RESTAURANTS_CSV, lggType);
				}else if(args[i].equalsIgnoreCase("DBpedia-LinkedMDB")){
					evaluateLGG(DataSets.DBPLINKEDMDB, lggType);
				}else if(args[i].equalsIgnoreCase("Dailymed-DrugBank")){
					evaluateLGG(DataSets.DRUGS, lggType);
				}else{
					System.out.println("Experiment " + args[i] + " Not implemented yet");
				}
			}
		}
	}

	private static String evaluateLGG(String[] args, int posExFrac) {
		if(args.length < 2){
			System.out.println("Please enter LGG type and dataset name(s)");
			System.exit(1);
		}
		String lggType = new String();
		if(LGGFactory.getNames().contains(args[0].toLowerCase())){
			lggType = args[0];
		}else{
			System.out.println(args[0] + " is not yet implemented");
			System.exit(1);
		}
		logger.info("Running experiment: " + args[1]);
		if(args[1].equalsIgnoreCase("all")){
			for(DataSets d : DataSets.values()){
				logger.info("Evaluate dataset: " + d);
				return evaluateLGG(d, lggType, posExFrac);

			}
		}else{
			for(int i = 1; i< args.length ; i++){
				if(args[i].equalsIgnoreCase("DBLP-ACM")){
					return evaluateLGG(DataSets.DBLPACM, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Abt-Buy")){
					return evaluateLGG(DataSets.ABTBUY, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Amazon-GoogleProducts")){
					return evaluateLGG(DataSets.AMAZONGOOGLE, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("DBLP-Scholar")){
					return evaluateLGG(DataSets.DBLPSCHOLAR, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Person1")){
					return evaluateLGG(DataSets.PERSON1, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Person2")){
					return evaluateLGG(DataSets.PERSON2, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Restaurants")){
					return evaluateLGG(DataSets.RESTAURANTS, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Restaurants_CSV")){
					return evaluateLGG(DataSets.RESTAURANTS_CSV, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("DBpedia-LinkedMDB")){
					return evaluateLGG(DataSets.DBPLINKEDMDB, lggType, posExFrac);
				}else if(args[i].equalsIgnoreCase("Dailymed-DrugBank")){
					return evaluateLGG(DataSets.DRUGS, lggType, posExFrac);
				}else{ 
					System.out.println("Experiment " + args[i] + " Not implemented yet");
				}
			}
		}
		return null;
	}


	private static void evaluatePrundGain(String[] args) {
		logger.info("Running experiment: " + args[0]);
		if(args[0].equalsIgnoreCase("all")){
			for(DataSets d : DataSets.values()){
				logger.info("Evaluate dataset: " + d);
				evaluatePrundGain(d);	
			}
		}else if(args[0].equalsIgnoreCase("DBLP-ACM")){
			evaluatePrundGain(DataSets.DBLPACM);
		}else if(args[0].equalsIgnoreCase("Abt-Buy")){
			evaluatePrundGain(DataSets.ABTBUY);
		}else if(args[0].equalsIgnoreCase("Amazon-GoogleProducts")){
			evaluatePrundGain(DataSets.AMAZONGOOGLE);
		}else if(args[0].equalsIgnoreCase("DBLP-Scholar")){
			evaluatePrundGain(DataSets.DBLPSCHOLAR);
		}else if(args[0].equalsIgnoreCase("Person1")){
			evaluatePrundGain(DataSets.PERSON1);
		}else if(args[0].equalsIgnoreCase("Person2")){
			evaluatePrundGain(DataSets.PERSON2);
		}else if(args[0].equalsIgnoreCase("Restaurants")){
			evaluatePrundGain(DataSets.RESTAURANTS);
		}else if(args[0].equalsIgnoreCase("Restaurants_CSV")){
			evaluatePrundGain(DataSets.RESTAURANTS_CSV);
		}else if(args[0].equalsIgnoreCase("DBpedia-LinkedMDB")){
			evaluatePrundGain(DataSets.DBPLINKEDMDB);
		}else if(args[0].equalsIgnoreCase("Dailymed-DrugBank")){
			evaluatePrundGain(DataSets.DRUGS);
		}else{
			System.out.println("Experiment " + args[0] + " Not implemented yet");
		}
	}


	public static DataSets toDataset(String d) {
		if(d.equalsIgnoreCase("DBLP-ACM")){
			return (DataSets.DBLPACM);
		}else if(d.equalsIgnoreCase("Abt-Buy")){
			return(DataSets.ABTBUY);
		}else if(d.equalsIgnoreCase("Amazon-GoogleProducts")){
			return(DataSets.AMAZONGOOGLE);
		}else if(d.equalsIgnoreCase("DBLP-Scholar")){
			return(DataSets.DBLPSCHOLAR);
		}else if(d.equalsIgnoreCase("Person1")){
			return(DataSets.PERSON1);
		}else if(d.equalsIgnoreCase("Person2")){
			return(DataSets.PERSON2);
		}else if(d.equalsIgnoreCase("Restaurants")){
			return(DataSets.RESTAURANTS);
		}else if(d.equalsIgnoreCase("Restaurants_CSV")){
			return(DataSets.RESTAURANTS_CSV);
		}else if(d.equalsIgnoreCase("DBpedia-LinkedMDB")){
			return(DataSets.DBPLINKEDMDB);
		}else if(d.equalsIgnoreCase("Dailymed-DrugBank")){
			return(DataSets.DRUGS);
		}else{
			System.out.println("Experiment " + d + " Not implemented yet");
			System.exit(1);
		}
		return null;
	}

	public static void printGoldStandardSizes(){
		for(DataSets d : DataSets.values()){
			EvaluationData data = DataSetChooser.getData(d);
			System.out.println("---------> " + d.name() + "\t" + data.getReferenceMapping().size);
		}
	}


	//	--------------------------------
	/**
	 * Evaluate the unsupervised version of WOMBAT Simple
	 * @param d
	 */
	private static void evaluateUnsupervisedSimpleLGGWithSeperation(DataSets d) {
		logger.info("Running unsupervised simple WOMBAT for the " + d + " dataset");
		EvaluationData data = DataSetChooser.getData(d);
		//Fill caches
		source = data.getSourceCache();
		target = data.getTargetCache();

		// 1. Learning phase
		long start = System.currentTimeMillis();
		//		LGG lgg = LGGFactory.createOperator( "unsupervised", sourceTrainCache, targetTrainCache, trainMap, MIN_COVERAGE);
		LGG lgg = LGGFactory.createOperator( "unsupervised simple", source, target, null, MIN_COVERAGE);
		Mapping learnedMap = lgg.getMapping();
		learnedMap = Mapping.getBestOneToOneMappings(learnedMap);
		learnedMap.initReversedMap();

		//2. Test results
		reference = data.getReferenceMapping();
		int refMapSize = reference.size();
		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		reference = removeLinksWithNoInstances(reference);
		String metricExpr = lgg.getMetricExpression();
		resultStr +=  "UnsupervisedLGG\t" + d +"\n" +
				"P\tR\tF\tT\tMetricExpr\n" + 
				PRFCalculator.precision(learnedMap, reference)+ "\t" + 
				PRFCalculator.recall(learnedMap, reference) 	+ "\t" + 
				PRFCalculator.fScore(learnedMap, reference) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 					+ "\t" ;
		System.out.println(resultStr);
	}

	/**
	 * Evaluate the unsupervised version of WOMBAT Complete
	 * @param d
	 */
	private static void evaluateUnsupervisedCompleteLGGWithSeperation(DataSets d) {
		logger.info("Running unsupervised simple WOMBAT for the " + d + " dataset");
		EvaluationData data = DataSetChooser.getData(d);
		//Fill caches
		source = data.getSourceCache();
		target = data.getTargetCache();

		// 1. Learning phase
		long start = System.currentTimeMillis();
		//		LGG lgg = LGGFactory.createOperator( "unsupervised", sourceTrainCache, targetTrainCache, trainMap, MIN_COVERAGE);
		LGG lgg = LGGFactory.createOperator( "unsupervised complete", source, target, null, MIN_COVERAGE);
		Mapping learnedMap = lgg.getMapping();
		learnedMap = Mapping.getBestOneToOneMappings(learnedMap);
		learnedMap.initReversedMap();

		//2. Test results
		reference = data.getReferenceMapping();
		int refMapSize = reference.size();
		logger.info("Number of removed error mappings = " + (refMapSize - reference.size()));
		reference = removeLinksWithNoInstances(reference);
		String metricExpr = lgg.getMetricExpression();
		resultStr +=  "UnsupervisedLGG\t" + d +"\n" +
				"P\tR\tF\tT\tMetricExpr\n" + 
				PRFCalculator.precision(learnedMap, reference)+ "\t" + 
				PRFCalculator.recall(learnedMap, reference) 	+ "\t" + 
				PRFCalculator.fScore(learnedMap, reference) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 					+ "\t" ;
		System.out.println(resultStr);
	}


}
