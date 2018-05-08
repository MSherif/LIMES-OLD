package de.uni_leipzig.simba.ukulele;

/**
 * @author Klaus Lyko <klaus.lyko@informatik.uni-leipzig.de>
 *
 */
public class EvaluationParameter {
	//ROCKER
	public double rockerCoverage = 0.8d;
	//EAGLE
	/**Number of generations for EAGLE to run*/
	public int generations = 100;
	/**EAGLE population size*/
	public int population = 20;
	/**EAGLE mutation rate*/
	public float mutationProbability = 0.6f;
	/**EAGLE reproduction rate*/
	public float reproductionProbability = 0.5f;
	/**Number of runs for EAGLE*/
	public int runs = 5;
	/**EAGLE supervised: run on full caches*/
	public boolean useFullCaches = true;
	
	
	// for EUCLID
	/**Coverage of properties for EUCLID to consider*/
	public double euclidCoverage = 0.8d;
	/**Number of iterations for EUCLID*/
	public int euclidIterations = 20;
	
	
}
