package de.uni_leipzig.simba.genetics.selfconfig;

import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.learner.UnSupervisedLearnerParameters;

/**
 * Interface for genetic self configurators. The use a PseudoMeasure to validate
 * example solutions. So they implement an unsupervised learning approach and consider
 * the best solution to be a 1 to 1 Mapping.
 * @author Lyko
 * @version 0.1
 */
public interface GeneticSelfConfigurator {

	/**
	 * Initializes and runs the learning/self configuration process.
	 * @param parameters
	 * @return
	 * @throws InvalidConfigurationException
	 */
	public Metric learn(UnSupervisedLearnerParameters parameters)  throws InvalidConfigurationException;
	/**
	 * Method to get the Mapping of the learned individual.
	 * @return
	 */
	public Mapping getMapping();

//	public Metric getBestMetricString();
	
}
