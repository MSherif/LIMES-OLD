package de.uni_leipzig.simba.genetics.learner;

import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
/**
 * Wraps around learn paarmeters for supervides learning methods usiong genetic programming.
 * @author Klaus Lyko
 *
 */
public class SupervisedLearnerParameters extends LearnerParameters {
	
	public SupervisedLearnerParameters(ConfigReader cR,
			PropertyMapping propMap) {
		super(cR, propMap);
	}

	private int trainingDataSize = 10;

	/**
	 * @return the trainingDataSize
	 */
	public int getTrainingDataSize() {
		return trainingDataSize;
	}

	/**
	 * @param trainingDataSize the trainingDataSize to set
	 */
	public void setTrainingDataSize(int trainingDataSize) {
		this.trainingDataSize = trainingDataSize;
	}
}