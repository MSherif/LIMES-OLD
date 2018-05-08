package de.uni_leipzig.simba.learning.refinement.operator;

import java.util.Set;

import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.specification.LinkSpec;


/**
 * A refinement operator for which the syntactic length of the generated
 * refinements can be limited.
 * 
 * @author Jens Lehmann
 * @author Klaus Lyko
 */
public interface LengthLimitedRefinementOperator {

	/**
	 * Optional refinement operation, where the learning algorithm can
	 * specify an additional bound on the length of specs. 
	 * 
	 * @param spec The spec, which will be refined.
	 * @param maxLength The maximum length of returned specs, where length is defined by {@link LinkSpec#size()}.
	 * @return A set of refinements obeying the above restrictions.
	 */
	public Set<LinkSpec> refine(LinkSpec spec, int maxLength);

	/**
	 * Needed to specify ource, target vars, and to get the appropriate PropertyMapping
	 * @param evalData
	 */
	public void setEvalData(EvaluationData evalData);	
	
}
