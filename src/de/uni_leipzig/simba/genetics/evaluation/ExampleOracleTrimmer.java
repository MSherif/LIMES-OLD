package de.uni_leipzig.simba.genetics.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
/**
 * @author Klaus Lyko
 *
 */
public class ExampleOracleTrimmer {

	Mapping reference = new Mapping();
	/**
	 * Method to scale down a reference mapping given by an Oracle.
	 * Only the first <i>max</i> <code>Entries</code> are used. 
	 * @param pM Oracle holding all data.
	 * @param max
	 * @return <code>Mapping</code> scaled down to max entries.
	 */
	public static Mapping trimExamples(Oracle pM, int max) {
		Mapping m = pM.getMapping();
		Mapping output = new Mapping();
		
		HashMap<String, HashMap<String, Double>> map = m.map;
		int count = 0;
	
		for(Entry<String, HashMap<String, Double>> e : map.entrySet()) {
			if(count >= max)
				break;
			String key = e.getKey();
			HashMap<String, Double> value=e.getValue();
//			Set<String> vSet=value.keySet();
			output.add(key, value);
//			for(String match: vSet) {
//				System.out.println(key+" => "+match+" : "+value.get(match));
//			}
			
			count ++;
		}
		return output;
	}
	/**
	 * Standard implementation to get random training examples. Basic approach is to
	 * get a random set of source uris of the reference mapping and for each one target
	 * uri it is mapped to!
	 * @param m
	 * @param max
	 * @return
	 */
	public static Mapping trimExamplesRandomly(Mapping m, int max) {
		Mapping output = new Mapping();

		while(output.size()<Math.min(max, m.size())) {			
			Random rand = new Random(System.currentTimeMillis());
			if(m.map.keySet().size()<=0) {// avoid empty keysets 
				continue;
			}
			String key = m.map.keySet().toArray(new String[0])[rand.nextInt(m.map.keySet().size())];
			Iterator<String> it = m.map.get(key).keySet().iterator();
			while(it.hasNext()){
				String target = it.next();
				if(!output.contains(key, target)) {
					output.add(key, target, m.getSimilarity(key, target));
					break;
				} else {
					// nothing to do here
				}
			}
		}
		return output;
	}
	/**
	 * Another implementation to get a random training data of size max out of the reference
	 * mapping m. The approach here is to randomly select source URIs of m and for each add 
	 * ALL target URIs it is mapped to.
	 * @param m
	 * @param max
	 * @return
	 */
	public static Mapping getRandomTrainingData(Mapping m, int max) {
		Mapping output = new Mapping();
		int breakPoint = Math.min(max, m.map.keySet().size());
		while(output.map.keySet().size()<breakPoint) {
			Random rand = new Random(System.currentTimeMillis());
			String key = m.map.keySet().toArray(new String[0])[rand.nextInt(m.map.keySet().size())];
			Iterator<String> it = m.map.get(key).keySet().iterator();
			while(it.hasNext()){
				String target = it.next();
				if(!output.contains(key, target)) {
					output.add(key, target, m.getSimilarity(key, target));
				} else {
					continue;
				}
			}
		}
		return output;
	}
	
	
	/**
	 * Method to scale down the Caches used to perform entity matching upon.
	 * Scaling down is done according to the given reference mapping. Returns Caches
	 * only holding instances of the  reference Mapping.
	 * @param sC Cache for source data.
	 * @param tC Cache for target data.
	 * @param m Reference Mapping (e.g. part of the optimal mapping)
	 * @return Array holding both resulting Caches, where the Cache for the source is at index 0. Cache for the target knowledge base at index 1.
	 */
	public static HybridCache[] processData(Cache sC, Cache tC, Mapping m) {
		Logger logger = Logger.getLogger("LIMES");
		if(m.size<100)
			logger.info("Scaling Caches down to "+m);
		HybridCache[] ret = new HybridCache[2];
		HybridCache h1 = new HybridCache();
		HybridCache h2 = new HybridCache();
		HashMap<String, HashMap<String, Double>> map = m.map;
		
		for(Entry<String, HashMap<String, Double>> e : map.entrySet()) {
			String key = e.getKey();
			Instance i = sC.getInstance(key);
			if(i == null){
				logger.info("unable to find instance with key "+key);
				continue;
			}				
			h1.addInstance(i);
			
			HashMap<String, Double> value = e.getValue();
			for(Entry<String, Double> e2 : value.entrySet()) {
				Instance j = tC.getInstance(e2.getKey());
				//System.out.println(e2.getKey());
				if(j != null)
					h2.addInstance(j); 
				else 
					logger.info("unable to find instance with key "+e2.getKey());
			}
		}
		ret[0] = h1;
		ret[1] = h2;
		return ret;
	}
	
	public HybridCache[] processDataEqually(HybridCache hc1, HybridCache hc2, Oracle o, int numberOfQuestions) {
		reference.map.clear();
		HybridCache[] ret = new HybridCache[2];
		HybridCache h1 = new HybridCache();
		HybridCache h2 = new HybridCache();
		int countQuestions = 0;
		Mapping alreadyAsked = new Mapping();
		
		ArrayList<String> uris1=hc1.getAllUris();
		ArrayList<String> uris2=hc2.getAllUris();
		
		while(countQuestions < numberOfQuestions) {
			Random random = new Random(System.currentTimeMillis());
			String uri1=uris1.get(random.nextInt(uris1.size()));
			String uri2=uris2.get(random.nextInt(uris2.size()));
			if(alreadyAsked.contains(uri1, uri2)) {
				continue;
			}
			h1.addInstance(hc1.getInstance(uri1));
			h2.addInstance(hc2.getInstance(uri2));
			countQuestions++;
			if(o.ask(uri1, uri2)) {
				reference.add(uri1, uri2, 1.0d);
			}				
		}
		ret[0] = h1;
		ret[1] = h2;
		Logger logger = Logger.getLogger("LIMES");
		logger.info("asking random "+numberOfQuestions+" questions got me "+reference.size()+" valid links");
		if(reference.size < numberOfQuestions/2) {
				Mapping ref2 = trimExamplesRandomly(o.getMapping(), numberOfQuestions/2);
				HybridCache[] adding = processData(hc1, hc2, ref2);
				
				HashMap<String, HashMap<String, Double>> map = ref2.map;
				
				for(Entry<String, HashMap<String, Double>> e : map.entrySet()) {
					reference.add(e.getKey(), e.getValue());
				}
				
				for(Instance i : adding[0].getAllInstances()) {
					ret[0].addInstance(i);
				}
				for(Instance i : adding[1].getAllInstances()) {
					ret[1].addInstance(i);
				}
				logger.info("adding "+ref2.size()+" valid links and instances.");
		}			
		return ret;
	}
	
	
	public static void main(String[] args) {
		String configFile = "Examples/GeneticEval/PublicationData.xml";
		String file = "Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv";
	
		configFile = "Examples/GeneticEval/DBLP-Scholar.xml";
		file = "Examples/GeneticEval/Datasets/DBLP-Scholar/DBLP-Scholar_perfectMapping.csv";
		
		Oracle o = OracleFactory.getOracle(file, "csv", "simple");
		o.getMapping();
//		Mapping optimalMapping = o.getMapping();
		//o.loadData(optimalMapping);
		
		ExampleOracleTrimmer trimmer = new ExampleOracleTrimmer();
		
		
		
//		Mapping res = trimExamples(o, 10);
//		System.out.println(res);	
		
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile);
		HybridCache sC = HybridCache.getData(cR.getSourceInfo());		
		HybridCache rC = HybridCache.getData(cR.getTargetInfo());
		HybridCache[] caches = trimmer.processDataEqually(sC, rC, o, 50);
		System.out.println(trimmer.getReferenceMapping().size());
		System.out.println("Cache 1: ");
		System.out.println(caches[0].size());
		System.out.println("Cache 2: ");
		System.out.println(caches[1].size());
	
	}
	
	public Mapping getReferenceMapping() {
		return reference;
	}
}
