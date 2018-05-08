/**
 * 
 */
package de.uni_leipzig.simba.lgg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;


/**
 * @author sherif
 *
 */
public class LGGFactory {
	private static final Logger logger = Logger.getLogger(LGGFactory.class.getName());

	protected static final String CONJUNCTIVE_LGG 			= "conjunctive";
	protected static final String SIMPLE_LGG 				= "simple";
	protected static final String COMPLETE_LGG 				= "complete";
	protected static final String WEAK_LGG 					= "weak";
	protected static final String UNSUPERVISED_SIMPLE_LGG 	= "unsupervised simple";
	protected static final String UNSUPERVISED_COMPLETE_LGG = "unsupervised complete";


	/**
	 * @param name
	 * @return a specific module instance given its module's name
	 * @author sherif
	 */
	public static LGG createOperator(String name, Cache source, Cache target, Mapping examples, double minCoverage) {
		logger.info("Getting operator with name "+name);

		if(name.equalsIgnoreCase(CONJUNCTIVE_LGG))
			return new ConjunctiveLGG(source, target, examples, minCoverage);
		if(name.equalsIgnoreCase(COMPLETE_LGG ))
			return new CompleteLGG(source, target, examples, minCoverage);
		if(name.equalsIgnoreCase(SIMPLE_LGG ))
			return new SimpleLGG(source, target, examples, minCoverage);
		if(name.equalsIgnoreCase(WEAK_LGG ))
			return new WeakLGG(source, target, examples, minCoverage);
		if(name.equalsIgnoreCase(UNSUPERVISED_SIMPLE_LGG ))
			return new UnsupervisedSimpleLGG(source, target, examples, minCoverage);
		if(name.equalsIgnoreCase(UNSUPERVISED_COMPLETE_LGG ))
			return new UnsupervisedCompleteLGG(source, target, examples, minCoverage);
		logger.error("Sorry, " + name + " is not yet implemented. Exit with error ...");
		System.exit(1);
		return null;
	}


	/**
	 * @return list of names of all implemented operators
	 * @author sherif
	 */
	public static List<String> getNames(){
		return new ArrayList<String>(Arrays.asList(CONJUNCTIVE_LGG, SIMPLE_LGG, COMPLETE_LGG , WEAK_LGG));
	}

}