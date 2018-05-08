package de.uni_leipzig.simba.genetics.core;

import java.util.Set;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.util.KBInfoRebuilder;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.specification.LinkSpec;
/**
 * Fitness Function to be used in case a learning of preprocessing is activated.
 * @author Klaus Lyko
 *
 */
public class PreprocessingFitnessFunction extends ALFitnessFunction{
	private static final long serialVersionUID = 4216946532578426671L;

	public PreprocessingFitnessFunction(LinkSpecGeneticLearnerConfig a_config,
			Oracle o, String measure, int sampleSize) {
		super(a_config, o, measure, sampleSize);
	}
	

	public static ALFitnessFunction getInstance(LinkSpecGeneticLearnerConfig a_config, Oracle o, String measure, int sampleSize) {
		if(instance == null) {
			instance = new PreprocessingFitnessFunction(a_config, o, measure, sampleSize);
		}
		return instance;
	}

	@Override
	public Mapping getMapping(String expression, double accThreshold, boolean full) {
		// we expect preprocessing functions to be part of a Link Specification 
		// and to be framed by square brackets right behind properties
		if(expression.contains("[") && expression.contains("]"))
			rebuildCaches(expression);
		try {
			if(full) {
				logger.info("get full Mapping for "+expression+">="+accThreshold);
				return sCMFull.getLinks(expression, accThreshold);
			} else {
				return sCM.getLinks(expression, accThreshold);
			}
		}catch(java.lang.OutOfMemoryError e) {
			logger.warn("Out of memory trying to get Map for expression\""+expression+">="+accThreshold+"\".");
			System.gc();
			return new Mapping();
		}
	}
	
	/**
	 * Method to rebuild Cache based an a Link Specification. Extracts mentioned properties for both source and target and adds them 
	 * to these Caches if neccessary.
	 */
	private void rebuildCaches(String expression) {
		LinkSpec par = new LinkSpec();
		par.readSpec(expression, 0.5d);
		// for all leaves e.g. trigrams()
		for(LinkSpec spec : par.getAllLeaves()) {
			// should be something like trigrams(x.a[lowercase],y.a[lowercase])
			// we extract the property name and its preprocessingChain
			// Note, we assume that the first property is for the source and the second one for the target
			Parser p = new Parser(spec.getFilterExpression(), spec.threshold);
//			logger.info("Rebuilding Caches");
			String sourceProp = p.term1.substring(p.term1.indexOf(".")+1);
			trimmedSourceCache = addPropertyToCache(trimmedSourceCache, sourceProp);
//			logger.info("Rebuilding full source Cache");
			sC = addPropertyToCache(sC, sourceProp);
//			logger.info("Rebuilding target Caches");
			String targetProp = p.term2.substring(p.term2.indexOf(".")+1);
			trimmedTargetCache = addPropertyToCache(trimmedTargetCache, targetProp);
//			logger.info("Rebuilding full target Cache");
			tC = addPropertyToCache(tC, targetProp);
		}
		// finally reset Mappers
		sCM = SetConstraintsMapperFactory.getMapper( "simple", m_config.source, m_config.target, 
				trimmedSourceCache, trimmedTargetCache, f, 2);
		sCMFull = SetConstraintsMapperFactory.getMapper( "simple",  m_config.source, m_config.target, 
				sC, tC, f, 2);
	}
	
	/**
	 *  Adds the property to the Cache.  Consider the following example the method is called with these Parameters: 
	 *  <ul><li>Cache c holding instances with a property <i>'a'</i>
	 *  	<li>PopertyName <i>'a[uppercase]'</i>
	 *  </ul>
	 *  If it isn't already part of c we try to add it as a new property with the same name <i>'a[uppercase'</i>
	 *  based on the property <i>'a'</i> processed with the preprocessing function chain<i>'uppercase'</i>
	 * @param c A Cache.
	 * @param propName A name of a property and a preprocessing function chain in square brackets e.g. <i>propertyName[functionChain]</i>
	 * @return The extended Cache c.
	 */
	private Cache addPropertyToCache(Cache c, String propName) {
//		logger.info("processing cache of size "+c.size()+" for prop "+propName+" getting all props...");
		Set<String> availableProps = c.getAllProperties();
//		logger.info("processing cache of size "+c.size()+" for prop "+propName);
		if(availableProps.contains(propName))
			return c;
		else {
			if(propName.contains("[") && propName.contains("]")) {
				String sProp = propName.substring(0, propName.indexOf("["));
				String processingChain = propName.substring(propName.indexOf("[")+1, propName.indexOf("]"));
				if(availableProps.contains(sProp)) {
//					logger.info("Have to add property '"+propName + "' to Cache" + availableProps);
					String sol[] = KBInfoRebuilder.getBestProcessingSolution(c, sProp, processingChain);
					if(sol[1].length()>0) {
						logger.info("Found more efficient way to add property "+sol[2]+": simply rely on "+sol[0]+" and process it with "+sol[1]);
						c = c.addProperty(sol[0], sol[2], sol[1]);
					}
					else
					c = c.addProperty(sProp, propName, processingChain);
				}
				else {
					logger.error("Have to add property '"+propName + "' to Cache. But we could not find property "+sProp+" in availableProps="+availableProps);
				}
			}
//			logger.info("finished Cache for propName "+propName);
			return c;
		}
	}
}
