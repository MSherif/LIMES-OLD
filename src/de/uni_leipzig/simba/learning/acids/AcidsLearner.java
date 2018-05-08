package de.uni_leipzig.simba.learning.acids;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.learner.Configuration;
import de.uni_leipzig.simba.learning.learner.Learner;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class AcidsLearner implements Learner {

    static Logger logger = Logger.getLogger("LIMES");
    
    /**
     * The oracle for learning
     */
    Oracle oracle;

    /**
     * Cache containing all relevant source data
     */
    Cache source;

    /**
     * Info to the source, e.g. variable
     */
    KBInfo sourceInfo;

    /**
     * Cache containing all relevant target data
     */
    Cache target;

    /**
     * Info to the source, e.g. variable
     */
    KBInfo targetInfo;

    /**
     * Contains current learned configuration
     */
    AcidsConfiguration config;

    /**
     * Contains all known positive examples and their similarity
     */
    Mapping positives;

    /**
     * Contains all known negative examples and their similarity
     */
    Mapping negatives;

    /**
     * Contains current Mapping returned by configuration
     */
    Mapping results, oldResults;

    /**
     * Mapper used within the learning
     */
    SetConstraintsMapper mapper;
    
	public AcidsLearner(KBInfo sourceInfo, KBInfo targetInfo, Cache source,
			Cache target, Oracle oracle, Mapping propertyMapping, int kernelType) {
		super();
		this.sourceInfo = sourceInfo;
		this.targetInfo = targetInfo;
		this.source = source;
		this.target = target;
		this.oracle = oracle;
		
		this.positives = new Mapping();
		this.negatives = new Mapping();
		this.config = new AcidsConfiguration(propertyMapping, kernelType);
		
        mapper = SetConstraintsMapperFactory.getMapper("simple", sourceInfo,
                targetInfo, source, target, new LinearFilter(), 2);

	}

    // ---------------------------------------------------- //
    
	@Override
	public boolean computeNextConfig(int n) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Configuration getCurrentConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getPrecision() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRecall() {
		// TODO Auto-generated method stub
		return 0;
	}

}
