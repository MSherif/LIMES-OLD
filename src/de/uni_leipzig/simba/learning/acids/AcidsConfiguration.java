package de.uni_leipzig.simba.learning.acids;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.learner.Configuration;
import de.uni_leipzig.simba.measures.MeasureFactory;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class AcidsConfiguration implements Configuration {
	
    static Logger logger = Logger.getLogger("LIMES");

    /**
     * Identifies linear-kernel support vector machines.
     */
    static final int LINEAR_KERNEL = 1;
    
    /**
     * Identifies degree-2 polynomial-kernel support vector machines.
     */
    static final int POLY2_KERNEL = 2;
    
    /**
     * The selected kernel type.
     */
    int kernelType;
    
    /**
	 * The configuration expression.
	 */
	String expression;
	
	
	
	public AcidsConfiguration(Mapping propertyMapping, int kernelType) {
//		String temp;
		
		this.kernelType = kernelType;
		
		switch(kernelType) {
		// Takes LINEAR_KERNEL as default.
		default:
		case LINEAR_KERNEL:
			/*
			 * TODO Create measures as implementations of Measure class.
			 * TODO Build default expression.
			 * 
			for(String p : propertyMapping.map.keySet()) {
				measureName = MeasureFactory.getMeasure("random", propertyType.get(p)).getName();
				for(String q : propertyMapping.map.get(p).keySet())
					mapping.put(measureName + "(x." + p + ",y." + q + ")", 1.0);
			}
			*/
			break;
		case POLY2_KERNEL:
			// TODO
			break;
		}
	}

	@Override
	public String getExpression() {
		return expression;
	}

}
