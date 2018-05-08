package de.uni_leipzig.simba.learning.refinement.operator;

import java.util.Set;

import de.uni_leipzig.simba.specification.LinkSpec;
/**
 * Interface for decreasing the threshold of atomic measures.
 * @author Klaus Lyko
 *
 */
public interface ThresholdDecreaser {
	/**
	 * Applies threshold decreasing function at the LinkSpecification.
	 * @param spec
	 * @return decreased threshold[0,1]
	 */
	public Set<Double> decrease(LinkSpec spec);
}
