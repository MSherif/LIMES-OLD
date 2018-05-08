package de.uni_leipzig.simba.learning.refinement.operator;

import java.util.HashSet;
import java.util.Set;

import de.uni_leipzig.simba.specification.LinkSpec;

public class ThresholdDecrementSimple implements ThresholdDecreaser {

	double decreaseFactorHigh = 0.850d;
	double decreaseLow = 0.75d;
	
	@Override
	public Set<Double> decrease(LinkSpec spec) {
		Set<Double> set = new HashSet<Double>();
		set.add(spec.threshold*decreaseFactorHigh);
//		set.add(spec.threshold*decreaseLow);
		return set;
	}
	
}
