package de.uni_leipzig.simba.genetics.learner;

import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.core.PseudoFMeasureFitnessFunction;
import de.uni_leipzig.simba.io.KBInfo;

/**
 * Interface for Link Specification Learner.
 *
 * @author Klaus Lyko
 *
 */
public interface UnsupervisedLinkSpecificationLearner {

    /**
     * Method to initiate link specification learning process.
     *
     * @param source Defines the source knowledge base.
     * @param target Defines the source knowledge base.
     * @param parameters Map of parameters needed to perform this learning
     * method.
     * @throws InvalidConfigurationException
     */
    public void init(KBInfo source, KBInfo target, UnSupervisedLearnerParameters parameters) throws InvalidConfigurationException;
    
    /**
	 * Starts the learning process and returns the Mapping learned by the best individual
	 * @return
	 */
    public Mapping learn();

    /**
     * Returns the best Metric learned until now.
     *
     * @return The Metric wrapping around the Link Specification, that is a
     * metric expression and a global acceptance threshold.
     */
    public Metric terminate();

    /**
     * FIXME Just for test purposes
     *
     * @return
     */
    public PseudoFMeasureFitnessFunction getFitnessFunction();
}

