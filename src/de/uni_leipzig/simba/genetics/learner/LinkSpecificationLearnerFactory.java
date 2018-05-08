package de.uni_leipzig.simba.genetics.learner;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.genetics.learner.LinkSpecificationLearner;
/**
 * Factory to get concrete link specification learner.
 * @author Klaus Lyko
 *
 */
public class LinkSpecificationLearnerFactory {
	
	/**
	 * Name to get the BATCH Genetic Link Specification learner.
	 */
	public static final String BATCH_LEARNER = "geneticbatchlearner";
	/**
	 * Name to get the ACTIVE Genetic Link Specification learner.
	 */
	public static final String ACTIVE_LEARNER = "geneticactivelearner";
	
	public static final String ACTIVE_COR_LEARNER = "geneticactivecorrelationlearner";
	
	public static final String ACTIVE_PROCESSING_LEARNER = "preprocessinglearner";
	
//	/**Name to get the UNSUPERVISED Genetic Linkspecification learner.*/
//	public static final String UNSUPERVISED_LEARNER = "unsupervisedlearner";
	
	/**
	 * Get link specification learner for the name. Please use the constants.
	 * @param name Name of the Learner.
	 * @return
	 */
	public static LinkSpecificationLearner getLinkSpecificationLearner(String name) {
		 Logger logger = Logger.getLogger("LIMES");
	     logger.info("Getting LinkSpec learner with name "+name);
	     if(name == null)
	    	 return new GeneticBatchLearner();
	     if(name.equalsIgnoreCase(BATCH_LEARNER))
	    	 return new GeneticBatchLearner();
	     if(name.equalsIgnoreCase(ACTIVE_LEARNER))
	    	 return new GeneticActiveLearner();
	     if (name.equalsIgnoreCase(ACTIVE_COR_LEARNER))
	    	 return new GeneticCorrelationActiveLearner();
	     if (name.equalsIgnoreCase(ACTIVE_COR_LEARNER))
	    	 return new PropertyLearner();
//	     if (name.equalsIgnoreCase(UNSUPERVISED_LEARNER))
//	    	 return new UnsupervisedLearner();
	     return new GeneticBatchLearner();	     
	}
}
