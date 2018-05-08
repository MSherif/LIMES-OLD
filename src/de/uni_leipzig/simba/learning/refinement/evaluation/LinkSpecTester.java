package de.uni_leipzig.simba.learning.refinement.evaluation;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.specification.LinkSpec;

public class LinkSpecTester {
	
	
	public static void test(EvaluationData data, LinkSpec spec) {
		ExecutionEngine engine = new ExecutionEngine(data.getSourceCache(), data.getTargetCache(),
				data.getConfigReader().sourceInfo.var, data.getConfigReader().targetInfo.var);
		ExecutionPlanner planer = new CanonicalPlanner();
		Mapping m = engine.runNestedPlan(planer.plan(spec));
		PRFCalculator prf = new PRFCalculator();
		double f = prf.fScore(m, data.getReferenceMapping());
		System.out.println("Spec "+spec+" on "+data.getName()+"\n\t f="+f);
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
