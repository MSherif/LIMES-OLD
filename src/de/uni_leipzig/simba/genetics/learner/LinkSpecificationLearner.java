package de.uni_leipzig.simba.genetics.learner;

import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.learner.SupervisedLearnerParameters;
import de.uni_leipzig.simba.io.KBInfo;

/**
 * Interface for Link Specification Learner.
 *
 * @author Klaus Lyko
 *
 */
public interface LinkSpecificationLearner {

    /**
     * Method to initiate link specification learning process.
     *
     * @param source Defines the source knowledge base.
     * @param target Defines the source knowledge base.
     * @param parameters Map of parameters needed to perform this learning
     * method.
     * @throws InvalidConfigurationException
     */
    public void init(KBInfo source, KBInfo target, SupervisedLearnerParameters parameters) throws InvalidConfigurationException;

   
    /**
     * Method to initiate link specification learning process with custom Caches. E.g. those only holding
     * known data.
     * @param source Defines the source knowledge base.
     * @param target Defines the source knowledge base.
     * @param parameters Map of parameters needed to perform this learning 
     * @param sourceCache source Cache
     * @param targetCache target Cache
     * @throws InvalidConfigurationException
     */
    public void init(KBInfo source, KBInfo target, SupervisedLearnerParameters parameters, Cache sourceCache, Cache targetCache) throws InvalidConfigurationException;

    /*
     * Method to determine which parameters are needed to perform this learning
     * approach. @return HashMap mapping the name of the parameter to a expected
     * class.
     *
     * public HashMap<String, Class> getParameters();
     */
    /**
     * Perform (a single iteration) to learn a LinkSpecification.
     *
     * @param trainingData Mapping of instances evaluated by an oracle. May hold
     * all available training data in case of a batch learning approach, or just
     * the answers to the last most informative matches for an active learning
     * approach.
     * @return A mapping of most informative candidates to be evaluated by an
     * oracle to perform the next iteration. Null (or an empty Mapping?) in case
     * the the learner has terminated.
     */
    public Mapping learn(Mapping trainingData);

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
    public ExpressionFitnessFunction getFitnessFunction();
    
    /**
     * Method to manually adjust Caches used to learn, those Caches should hold
     * the whole (known) data to learn and map upon.
     * 
     * @param sC
     * @param tC
     */
    public void setCaches(Cache sC, Cache tC);
}
