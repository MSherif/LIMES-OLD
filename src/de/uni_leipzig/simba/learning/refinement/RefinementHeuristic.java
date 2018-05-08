package de.uni_leipzig.simba.learning.refinement;

import java.util.Comparator;

import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;

/**
 * Search algorithm heuristic for the refinement based algorithm.
 * 
 * @author Jens Lehmann
 *
 */
public interface RefinementHeuristic extends Comparator<SearchTreeNode> {

	void setEvaluationData(EvaluationData evalData);
	
}
