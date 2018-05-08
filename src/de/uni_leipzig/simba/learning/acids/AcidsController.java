package de.uni_leipzig.simba.learning.acids;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.learning.acids.algorithm.AcidsSetting;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class AcidsController {

    static Logger logger = Logger.getLogger("LIMES");
    
    int trainingSetSize;
    
    /**
     * @param trainingSetSize
     */
    public AcidsController(int trainingSetSize) {
		super();
		this.trainingSetSize = trainingSetSize;
	}

	/**
	 * Runs the ACIDS algorithm.
	 */
	public void run() {
		// sources and targets => endpoints
		// oracle => create it with OracleFactory
		
//		AcidsSetting setting = new AcidsSetting(sources, targets, oracle, trainingSetSize);
	}
	
}
