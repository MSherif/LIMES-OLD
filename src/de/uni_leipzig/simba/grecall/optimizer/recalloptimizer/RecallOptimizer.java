package de.uni_leipzig.simba.grecall.optimizer.recalloptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.optimizer.RecallOptimizerFactory;
import de.uni_leipzig.simba.grecall.optimizer.strategy.RuntimeScoreStrategy;
import de.uni_leipzig.simba.grecall.optimizer.strategy.ScoreStrategy;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.measures.MeasureProcessor;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;

public abstract class RecallOptimizer implements Optimizer {
    protected static final Logger logger = Logger.getLogger(RecallOptimizerFactory.class.getName());

    protected float DesiredSelectivity = 0.0f;
    protected DiffPair<LinkSpec, NestedPlan> root;
    protected SimpleOracle oracle = null;
    protected double percentageRecall = 0.0d;
    protected double sourceSize;
    protected double targetSize;
    protected DiffPair<LinkSpec, NestedPlan> bestEntry;
    protected ScoreStrategy scoreStrategy;
    protected long timeCounter = 0;
    protected List<Double> thresholds = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);

    public RecallOptimizer(long sourceSize, long targetSize, SimpleOracle oracle, double recall) {

	this.sourceSize = sourceSize;
	this.targetSize = targetSize;
	this.oracle = oracle;
	this.percentageRecall = recall;
	this.setScoreStrategy();

    }

    public RecallOptimizer() {

    }

    protected abstract LinkedHashMap<LinkSpec, NestedPlan> expandPlan(LinkSpec currentSpec);

    public void setScoreStrategy() {
	this.scoreStrategy = new RuntimeScoreStrategy();
    }

    public Float getExecutionTimeEstimation() {
	return (float) this.bestEntry.getY().runtimeCost;
    }

    public double getSelectivityEstimation() {
	return this.bestEntry.getY().selectivity;
    }

    public double getDesiredSelectivity() {
	return this.DesiredSelectivity;
    }

    public LinkSpec getNewSpec() {
	return this.bestEntry.getX();
    }

    public void setSpec(LinkSpec spec) {
	NestedPlan plan = null;
	this.bestEntry = new DiffPair<LinkSpec, NestedPlan>(spec.clone(), plan);
	this.root = new DiffPair<LinkSpec, NestedPlan>(spec.clone(), plan);

	this.bestEntry.getX().pathOfAtomic();
	// logger.info("Original spec: " + spec);
	for (LinkSpec child : this.bestEntry.getX().getAllLeaves()) {

	    if (thresholds.contains(child.threshold))
		continue;
	    if (child.treePath.contains("MINUS->right"))
		continue;
	    double newThreshold = 0.0d;
	    
	    newThreshold = this.oracle.returnNextThreshold(child.threshold);

	    if (newThreshold == -0.1d)
		newThreshold = child.threshold;

	    child.threshold = newThreshold;

	}
	plan = getPlanFromSpecification(this.bestEntry.getX());

	this.bestEntry.setY(plan);
	this.root.setY(plan);
	this.DesiredSelectivity = (float) (this.bestEntry.getY().selectivity * this.percentageRecall);

	logger.info("Approximated spec: " + this.bestEntry.getX());
	logger.info("Selectivity original: " + this.bestEntry.getY().selectivity);
	logger.info("Selectivity desired: " + this.DesiredSelectivity);

    }

    protected String checkSelectivity(double selectivity) {
	// selectivity is higher that the desired - keep improving!
	if (selectivity > this.DesiredSelectivity)
	    return "higher";
	else if (selectivity == this.DesiredSelectivity)
	    // selectivity is equal to the desired - STOP
	    return "equal";
	else // selectivity is lower to the desired - STOP
	    return "lower";
    }

    /**
     * Generates a instructionList based on the optimality assumption used in
     * databases
     *
     * @param spec
     *            Specification for which a instructionList is needed
     * @param source
     *            Source cache
     * @param target
     *            Target cache
     * @return Nested instructionList for the given spec
     */
    protected NestedPlan getPlanFromSpecification(LinkSpec spec) {
	// logger.info(spec);
	return getPlanFromSpecification(spec, 1);
    }

    /**
     * Generates a instructionList based on the optimality assumption used in
     * databases
     *
     * @param spec
     *            Specification for which a instructionList is needed
     * @param sourceSize
     *            Source cache size
     * @param targetSize
     *            Target cache size
     * @return Nested instructionList for the given spec
     */
    protected NestedPlan getPlanFromSpecification(LinkSpec spec, int flag) {
	NestedPlan plan = new NestedPlan();
	// atomic specs are simply ran
	if (spec.isAtomic()) {
	    plan.instructionList = new ArrayList<Instruction>();
	    plan.addInstruction(new Instruction(Instruction.Command.RUN, spec.getFilterExpression(),
		    spec.threshold + "", -1, -1, 0));
	    if (spec.treePath.contains("MINUS->right")) {
		double oldThreshold = spec.threshold;
		double newThreshold = this.oracle.returnNextThreshold(spec.threshold);
		spec.threshold = newThreshold;
		plan.runtimeCost = this.oracle.askOracleForRuntime(spec);
		plan.selectivity = this.oracle.askOracleForSelectivity(spec);
		spec.threshold = oldThreshold;
	    } else {
		plan.runtimeCost = this.oracle.askOracleForRuntime(spec);
		plan.selectivity = this.oracle.askOracleForSelectivity(spec);
	    }

	    //logger.info(spec);
	    //logger.info("Selectivity of atomic: " + plan.selectivity + " and runtime " + plan.runtimeCost);

	} else {
	    // no optimization for non AND operators really
	    if (!spec.operator.equals(Operator.AND)) {
		List<NestedPlan> children = new ArrayList<NestedPlan>();
		// set children and update costs
		plan.runtimeCost = 0;
		for (LinkSpec child : spec.children) {
		    NestedPlan childPlan = getPlanFromSpecification(child, flag);
		    children.add(childPlan);
		    plan.runtimeCost = plan.runtimeCost + childPlan.runtimeCost;
		}
		// add costs of union, which are 1
		plan.runtimeCost = plan.runtimeCost + (spec.children.size() - 1);
		plan.subPlans = children;

		// set operator
		double selectivity;
		if (spec.operator.equals(Operator.OR)) {
		    plan.operator = Instruction.Command.UNION;
		    selectivity = 1 - children.get(0).selectivity;
		    for (int i = 1; i < children.size(); i++) {
			selectivity = selectivity * (1 - children.get(i).selectivity);
			// add filtering costs based on approximation of mapping
			// size
			if (plan.filteringInstruction != null) {
			    plan.runtimeCost = plan.runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    sourceSize * targetSize * (1 - selectivity));
			}
		    }
		    plan.selectivity = 0.5 * (1 - selectivity);
		    //logger.info("OR");
		    //logger.info("Selectivity : " + plan.selectivity + " and runtime " + plan.runtimeCost);
		} else if (spec.operator.equals(Operator.MINUS)) {
		    plan.operator = Instruction.Command.DIFF;
		    // p(A \ B \ C \ ... ) = p(A) \ p(B U C U ...)
		    selectivity = children.get(0).selectivity;
		    for (int i = 1; i < children.size(); i++) {
			selectivity = selectivity * (1 - children.get(i).selectivity);
			// add filtering costs based on approximation of mapping
			// size
			if (plan.filteringInstruction != null) {
			    plan.runtimeCost = plan.runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    sourceSize * targetSize * (1 - selectivity));
			}
		    }
		    plan.selectivity = 0.5 * selectivity;
		    //logger.info("MINUS");
		    //logger.info("Selectivity : " + plan.selectivity + " and runtime " + plan.runtimeCost);
		} else if (spec.operator.equals(Operator.XOR)) {
		    plan.operator = Instruction.Command.XOR;
		    // A XOR B = (A U B) \ (A & B)
		    selectivity = children.get(0).selectivity;
		    for (int i = 1; i < children.size(); i++) {

			selectivity = (1 - (1 - selectivity) * (1 - children.get(i).selectivity))
				* (1 - selectivity * children.get(i).selectivity);
			// add filtering costs based on approximation of mapping
			// size
			if (plan.filteringInstruction != null) {
			    plan.runtimeCost = plan.runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    sourceSize * targetSize * selectivity);
			}
		    }
		    plan.selectivity = 0.5 * selectivity;
		    // logger.info("XOR");
		    // logger.info("Selectivity : "+plan.selectivity+" and
		    // runtime "+plan.runtimeCost);
		}
		plan.filteringInstruction = new Instruction(Instruction.Command.FILTER, spec.getFilterExpression(),
			spec.threshold + "", -1, -1, 0);

	    } // here we can optimize. - but we don't
	    else if (spec.operator.equals(Operator.AND)) {
		List<NestedPlan> children = new ArrayList<NestedPlan>();
		plan.runtimeCost = 0;
		plan.filteringInstruction = null;
		double selectivity = 1d;
		for (LinkSpec child : spec.children) {
		    NestedPlan childPlan = getPlanFromSpecification(child);
		    children.add(childPlan);
		    plan.runtimeCost = plan.runtimeCost + childPlan.runtimeCost;
		    selectivity = selectivity * childPlan.selectivity;
		    if (plan.filteringInstruction != null) {
			plan.runtimeCost = plan.runtimeCost
				+ MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					sourceSize * targetSize * selectivity);
		    }
		}
		// my addition
		plan.subPlans = children;
		plan.operator = Instruction.Command.INTERSECTION;
		plan.selectivity = 0.5 * selectivity;
		//logger.info("AND");
		//logger.info("Selectivity : " + plan.selectivity + " and runtime " + plan.runtimeCost);
	    }
	}
	return plan;
    }

    public void setTimeCounter(long timeLimit) {
	this.timeCounter = timeLimit;

    }

}
