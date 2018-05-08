package de.uni_leipzig.simba.grecall.optimizer;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Baseline;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.DownwardRefinementMonAs;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.DownwardRefinementNaive;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.DownwardRefinement;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.RecallOptimizer;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.specification.LinkSpec;



public class RecallOptimizerFactory {
	
	private static final Logger logger = Logger.getLogger(RecallOptimizerFactory.class.getName());
	public static final String BASELINE = "baseline";
	//public static final String NAIVE_OPTIMIZER	= "naive";
	public static final String DownwardRefinement = "downward_refinement";
	public static final String DownwardRefinementmonAs = "downward_refinement_monAS";
	
	/**
	 * @param name
	 * @return a specific recall regulator instance based on specification file description
	 * @author kleanthi
	 * @param recallThreshold 
	 * @param spec 
	 * @param targetSize 
	 * @param sourceSize 
	 */
	public static RecallOptimizer getOptimizer(String name, long sourceSize, long targetSize,
										SimpleOracle oracle, double recall) {
		
		if(name.equalsIgnoreCase(BASELINE))
			return new Baseline(sourceSize, targetSize, oracle, recall);
		//if(name.equalsIgnoreCase(NAIVE_OPTIMIZER))
		//	return new DownwardRefinementNaive(sourceSize, targetSize, oracle, recall);
		if(name.equalsIgnoreCase(DownwardRefinement))
			return new DownwardRefinement(sourceSize, targetSize, oracle, recall);
		if(name.equalsIgnoreCase(DownwardRefinementmonAs))
			return new DownwardRefinementMonAs(sourceSize, targetSize, oracle, recall);
		
		logger.error("Sorry, " + name + " is not yet implemented. Exit with error ...");
		System.exit(1);
		return null;
	}
}
