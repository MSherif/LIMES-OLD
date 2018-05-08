package de.uni_leipzig.simba.genetics.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

/**
 * Implementation of our custom FitnessFunction.
 * As we're using the <code>DeltaFitnessEvaluator</code> higher values mean the
 * individual is lesser fit. This is the fitness function for the Active Learning
 * approach. Most of the functionality is equal to this for the batch learner
 * @version 0.3 inherit basic functionality from the ExpressionFitnessFunction.
 * @author Klaus Lyko
 *
 */
public class ALFitnessFunction extends ExpressionFitnessFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7148797234498410940L;
	protected static ALFitnessFunction instance = null;
	/**
	 * Constructor to set the measure the fitness calculation should be based on.
	 * @param a_config A ExpresionConfiguration needed to fill caches.
	 * @param verificationFile Path to the file holding the perfect mapping.
	 * @param measure either "recall", "precision", or "f-score".
	 * @param sampleSize Size of samples used to verify solutions. 0 means all.
	 */
	public ALFitnessFunction(LinkSpecGeneticLearnerConfig a_config, Oracle o, 
			String measure, int sampleSize) {
	
		super(a_config);
		m_config = a_config;		
		this.numberOfExamples = sampleSize;
		//store data
		optimalMapping = o.getMapping();
		reference = o.getMapping();
		if(a_config.sC != null)
			sC = a_config.sC;
		else
			sC = HybridCache.getData(a_config.source);
		if(a_config.tC != null)
			tC = a_config.tC;
		else
			tC = HybridCache.getData(a_config.target);
		
		if(sampleSize > 0) {
			trimmedSourceCache = new MemoryCache();
			trimmedTargetCache = new MemoryCache();	
			crossProduct = 0;
		}
		else {
			trimmedSourceCache = sC;
			trimmedTargetCache = tC;
			crossProduct = sC.size() * tC.size();
		}
		// get initial Mapper
		sCM = SetConstraintsMapperFactory.getMapper( "simple", a_config.source, a_config.target, 
				trimmedSourceCache, trimmedTargetCache, f, 2);
		sCMFull = SetConstraintsMapperFactory.getMapper( "simple",  a_config.source, a_config.target, 
				sC, tC, f, 2);
		this.measure=measure;
		System.gc();
		
	}

	/**
	 * SingleTon Pattern
	 * @param a_config LinkSpecConfig
	 * @param o Oracle
	 * @param measure f-score recall or precision
	 * @param sampleSize
	 * @return
	 */
	public static ALFitnessFunction getInstance(LinkSpecGeneticLearnerConfig a_config, Oracle o, String measure, int sampleSize) {
		if(instance == null) {
			instance = new ALFitnessFunction(a_config, o, measure, sampleSize);
		}
		return instance;
	}
	/**
	 * Implementing default Singleton pattern. Uses f-score to calculate fitness
	 * @param a_config A ExpresionConfiguration needed to fill caches.
	 * @param verificationFile Path to the file holding the perfect mapping.
	 * @param sampleSize Size of samples used to verify solutions. 0 means all.
	 * @return Instance of ExpressionFitnessFunction.
	 */
	public static ALFitnessFunction getInstance(LinkSpecGeneticLearnerConfig a_config, Oracle o, int sampleSize) {
		return getInstance(a_config, o, "f-score", sampleSize);
	}	

	/**
	 * Add asked Instance to the caches used for evaluation.
	 * @param controversyMatches List of Triples the Oracle was asked about.
	 */
	public void fillCachesIncrementally(List<Triple> controversyMatches) {
		for(Triple t : controversyMatches) {
			if(!trimmedSourceCache.containsUri(t.getSourceUri())) {
				logger.info("Adding instance "+t.getSourceUri()+" to sC");
				Instance i = sC.getInstance(t.getSourceUri());
				if(i != null)
					trimmedSourceCache.addInstance(i);
				else {
					logger.error("could Not locate instance"+t.getSourceUri()+" in source Cache");
				}
			}
			if(!trimmedTargetCache.containsUri(t.getTargetUri())) {
				logger.info("Adding instance "+t.getTargetUri()+" to tC");
				Instance i = tC.getInstance(t.getTargetUri());
				if(i != null)
					trimmedTargetCache.addInstance(i);
				else {
					logger.error("could Not locate instance"+t.getTargetUri()+" in target Cache");
				}
			}
		}
		sCM = SetConstraintsMapperFactory.getMapper( "simple", m_config.source, m_config.target, 
				trimmedSourceCache, trimmedTargetCache, f, 2);
		crossProduct = trimmedSourceCache.size() * trimmedTargetCache.size();
	}
	/**
	 * Fill caches with all instances contained in Mapping.
	 * @param trainingData
	 */
	public void fillCachesIncrementally(Mapping trainingData) {
		logger.info("Fill Caches Incrementally with "+trainingData.size()+" instances");
		for(Entry<String, HashMap<String, Double>> e1 : trainingData.map.entrySet()) {
			for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
//				logger.info("Looking for instance "+e1.getKey()+" and "+e2.getKey());
				if(trimmedSourceCache == null)
					trimmedSourceCache = new HybridCache();
				if(trimmedTargetCache == null)
					trimmedTargetCache = new HybridCache();
				if(!trimmedSourceCache.containsUri(e1.getKey())) {
					Instance i = sC.getInstance(e1.getKey());
					if(i != null)
						trimmedSourceCache.addInstance(i);
					else {
//						logger.error("Could not retrieve instance "+e1.getKey()+" in source Cache");
					}
				}
				if(!trimmedTargetCache.containsUri(e2.getKey())) {
					Instance i = tC.getInstance(e2.getKey());
					if(i != null)
						trimmedTargetCache.addInstance(i);
					else {
//						logger.error("Could not retrieve instance "+e2.getKey()+" in target Cache");
					}
				}
			}
		}
		sCM = SetConstraintsMapperFactory.getMapper( "simple", m_config.source, m_config.target, 
				trimmedSourceCache, trimmedTargetCache, f, 2);
		crossProduct = trimmedSourceCache.size() * trimmedTargetCache.size();
	}
	/**
	 * Method to add instances to reference?.
	 * @param m Mapping of matches, designated as such by an oracle.
	 */
	public void addToReference(Mapping m) {
		logger.info("Filling reference of size "+reference.size()+" with "+m.size()+" additional matches.");
		for(Entry<String, HashMap<String, Double>> e1 : m.map.entrySet()) {
			for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
				reference.add(e1.getKey(), e2.getKey(), 1d);
			}
		}
		logger.info("Reference has now "+reference.size()+" Matches.");
	}
	
	public void destroy() {
		instance = null;
	}
	
	public void setCaches(Cache sC, Cache tC) {
		logger.info("Manually setting Caches");
		this.sC = sC;
		this.tC = tC;
	}
}
