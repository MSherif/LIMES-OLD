package de.uni_leipzig.simba.execution.planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.mapper.AtomicMapper.Language;
import de.uni_leipzig.simba.mapper.atomic.EDJoin;
import de.uni_leipzig.simba.mapper.atomic.JaroMapper;
import de.uni_leipzig.simba.mapper.atomic.PPJoinPlusPlus;
import de.uni_leipzig.simba.mapper.atomic.TotalOrderBlockingMapper;
import de.uni_leipzig.simba.mapper.atomic.fastngram.FastNGram;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.measures.MeasureFactory;
import de.uni_leipzig.simba.measures.MeasureProcessor;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;

public class DynamicPlanner implements ExecutionPlanner {

    static Logger logger = Logger.getLogger("LIMES");
    public Cache source;
    public Cache target;
    public Language lang;
    LinkedHashMap<String, NestedPlan> plans = new LinkedHashMap<String, NestedPlan>();

    /**
     * Constructor. Caches are needed for stats.
     *
     * @param s
     *            Source cache
     * @param t
     *            Target get
     */
    public DynamicPlanner(Cache s, Cache t) {
	source = s;
	target = t;
	lang = Language.NULL;
    }

    /**
     * Computes atomic costs for a measure
     *
     * @param measure
     * @param threshold
     * @return
     */
    public double getAtomicRuntimeCosts(String measure, double threshold) {
	Measure m = MeasureFactory.getMeasure(measure);
	double runtime;
	if (m.getName().equalsIgnoreCase("levenshtein")) {
	    runtime = (new EDJoin()).getRuntimeApproximation(source.size(), target.size(), threshold, lang);
	} else if (m.getName().equalsIgnoreCase("euclidean")) {
	    runtime = (new TotalOrderBlockingMapper()).getRuntimeApproximation(source.size(), target.size(), threshold,
		    lang);
	} else if (m.getName().equalsIgnoreCase("qgrams")) {
	    runtime = (new FastNGram()).getRuntimeApproximation(source.size(), target.size(), threshold, lang);
	} else {
	    runtime = (new PPJoinPlusPlus()).getRuntimeApproximation(source.size(), target.size(), threshold, lang);
	}
	logger.info("Runtime approximation for " + measure + " is " + runtime);
	return runtime;
    }

    /**
     * Computes atomic mapping sizes for a measure
     *
     * @param measure
     * @param threshold
     * @return
     */
    public double getAtomicMappingSizes(String measure, double threshold) {
	Measure m = MeasureFactory.getMeasure(measure);
	double size;
	if (m.getName().equalsIgnoreCase("levenshtein")) {
	    size = (new EDJoin()).getMappingSizeApproximation(source.size(), target.size(), threshold, lang);
	}
	if (m.getName().equalsIgnoreCase("euclidean")) {
	    size = (new TotalOrderBlockingMapper()).getMappingSizeApproximation(source.size(), target.size(), threshold,
		    lang);
	}
	if (m.getName().equalsIgnoreCase("qgrams")) {
	    size = (new FastNGram()).getMappingSizeApproximation(source.size(), target.size(), threshold, lang);
	} else {
	    size = (new PPJoinPlusPlus()).getMappingSizeApproximation(source.size(), target.size(), threshold, lang);
	}
	return size;
    }

    /**
     * Check if all the atomic steps of a plan are executed
     *
     */
    public boolean isExecuted() {
	for (Map.Entry<String, NestedPlan> plan : this.plans.entrySet()) {
	    String key = plan.getKey();
	    NestedPlan value = plan.getValue();
	    if (value.dBit == false)
		return false;
	}
	return true;

    }

    /**
     * Computes costs for a filtering
     *
     * @param filterExpression
     *            Expression used to filter
     * @param mappingSize
     *            Size of mapping
     * @return Costs for filtering
     */
    public double getFilterCosts(List<String> measures, int mappingSize) {
	// need costs for single operations on measures
	// = MeasureProcessor.getMeasures(filterExpression);
	double cost = 0;
	for (String measure : measures) {
	    cost = cost + MeasureFactory.getMeasure(measure).getRuntimeApproximation(mappingSize);
	}
	logger.info("Runtime approximation for filter expression " + measures + " is " + cost);
	return cost;
    }

    public void updatePlan(NestedPlan plan, double rt, double selectivity, double msize) {
	for (Map.Entry entry : this.plans.entrySet()) {
	    NestedPlan planValue = (NestedPlan) entry.getValue();
	    if (plan == planValue) {
		planValue.dBit = true;
		planValue.runtimeCost = rt;
		planValue.selectivity = selectivity;
		planValue.mappingSize = msize;
	    }
	}

    }

    public NestedPlan plan(LinkSpec spec) {
	return plan(spec, source, target, new Mapping(), new Mapping());

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
     * @param sourceMapping
     *            Size of source mapping
     * @param targetMapping
     *            Size of target mapping
     * @return Nested instructionList for the given spec
     */
    public NestedPlan plan(LinkSpec spec, Cache source, Cache target, Mapping sourceMapping, Mapping targetMapping) {
	NestedPlan plan = new NestedPlan();
	// atomic specs are simply ran
	if (spec.isAtomic()) {
	    // here we should actually choose between different implementations
	    // of the operators based on their runtimeCost
	    Parser p = new Parser(spec.getFilterExpression(), spec.threshold);
	    plan.instructionList = new ArrayList<Instruction>();
	    plan.addInstruction(new Instruction(Instruction.Command.RUN, spec.getFilterExpression(),
		    spec.threshold + "", -1, -1, 0));

	    if (this.plans.containsKey(spec.toString())) {
		NestedPlan pl = this.plans.get(spec.toString());
		if (pl.dBit == false) {
		    plan.runtimeCost = getAtomicRuntimeCosts(p.getOperation(), spec.threshold);
		    plan.mappingSize = getAtomicMappingSizes(p.getOperation(), spec.threshold);
		    // there is a function in EDJoin that does that
		    plan.selectivity = plan.mappingSize / (double) (source.size() * target.size());
		} else {
		    plan.runtimeCost = pl.runtimeCost;
		    plan.selectivity = pl.selectivity;
		    plan.mappingSize = pl.mappingSize;
		}
	    } else {
		plan.runtimeCost = getAtomicRuntimeCosts(p.getOperation(), spec.threshold);
		plan.mappingSize = getAtomicMappingSizes(p.getOperation(), spec.threshold);
		// there is a function in EDJoin that does that
		plan.selectivity = plan.mappingSize / (double) (source.size() * target.size());
		this.plans.put(spec.toString(), plan);
	    }

	    // System.out.println("Plan for " + spec.filterExpression + ":\n" +
	    // plan);

	} else {
	    // no optimization for non AND operators really
	    if (!spec.operator.equals(Operator.AND)) {
		List<NestedPlan> children = new ArrayList<NestedPlan>();
		// set children and update costs
		double runtimeCost = 0;
		for (LinkSpec child : spec.children) {
		    NestedPlan childPlan = plan(child, source, target, sourceMapping, targetMapping);
		    children.add(childPlan);
		    runtimeCost = runtimeCost + childPlan.runtimeCost;
		}
		// add costs of union, which are 1
		runtimeCost = runtimeCost + (spec.children.size() - 1);
		plan.subPlans = children;
		// set operator
		double selectivity = 0d;
		if (spec.operator.equals(Operator.OR)) {

		    plan.operator = Instruction.Command.UNION;
		    selectivity = 1 - children.get(0).selectivity;
		    plan.runtimeCost = children.get(0).runtimeCost;
		    for (int i = 1; i < children.size(); i++) {
			selectivity = selectivity * (1 - children.get(i).selectivity);
			// add filtering costs based on approximation of mapping
			// size
			if (plan.filteringInstruction != null) {

			    plan.runtimeCost = plan.runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    source.size() * target.size() * (1 - selectivity));
			}
		    }
		    selectivity = 1 - selectivity;
		} else if (spec.operator.equals(Operator.MINUS)) {
		    plan.operator = Instruction.Command.DIFF;
		    // p(A \ B \ C \ ... ) = p(A) \ p(B U C U ...)
		    selectivity = children.get(0).selectivity;
		    for (int i = 1; i < children.size(); i++) {
			selectivity = selectivity * (1 - children.get(i).selectivity);
			// add filtering costs based on approximation of mapping
			// size
			if (plan.filteringInstruction != null) {
			    runtimeCost = runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    source.size() * target.size() * (1 - selectivity));
			}
		    }
		    // selectivity = selectivity;
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
			    runtimeCost = runtimeCost
				    + MeasureProcessor.getCosts(plan.filteringInstruction.getMeasureExpression(),
					    source.size() * target.size() * selectivity);
			}
		    }
		    // plan.selectivity = selectivity;
		}

		plan.filteringInstruction = new Instruction(Instruction.Command.FILTER, spec.getFilterExpression(),
			spec.threshold + "", -1, -1, 0);
		// update estimations
		if (this.plans.containsKey(spec.toString())) {
		    NestedPlan pl = this.plans.get(spec.toString());
		    if (pl.dBit == false) {
			plan.runtimeCost = runtimeCost;
			plan.selectivity = selectivity;
		    } else {
			plan.runtimeCost = pl.runtimeCost;
			plan.selectivity = pl.selectivity;
			plan.mappingSize = pl.mappingSize;
		    }
		} else {
		    plan.runtimeCost = runtimeCost;
		    plan.selectivity = selectivity;
		    this.plans.put(spec.toString(), plan);
		}
	    } // here we can optimize.
	    else if (spec.operator.equals(Operator.AND)) {
		List<NestedPlan> children = new ArrayList<NestedPlan>();
		double runtimeCost = 0;
		double selectivity = 1d;
		for (LinkSpec child : spec.children) {
		    NestedPlan childPlan = plan(child);
		    children.add(childPlan);
		    runtimeCost = runtimeCost + childPlan.runtimeCost;
		    selectivity = selectivity * childPlan.selectivity;
		}
		plan = getBestConjunctivePlan(children, selectivity);
		// update estimations
		if (this.plans.containsKey(spec.toString())) {
		    NestedPlan pl = this.plans.get(spec.toString());
		    if (pl.dBit == false) {
			plan.runtimeCost = runtimeCost;
			plan.selectivity = selectivity;
		    } else {
			plan.runtimeCost = pl.runtimeCost;
			plan.selectivity = pl.selectivity;
		    }
		} else {
		    plan.runtimeCost = runtimeCost;
		    plan.selectivity = selectivity;
		    this.plans.put(spec.toString(), plan);
		}
	    }

	}

	return plan;
    }

    /**
     * Compute the left-order best instructionList for a list of plans. Only
     * needed when more AND has more than 2 children. Simply splits the task in
     * computing the best instructionList for (leftmost, all others)
     *
     * @param plans
     *            List of plans
     * @param selectivity
     *            Selectivity of the instructionList (known beforehand)
     * @return NestedPlan
     */
    public NestedPlan getBestConjunctivePlan(List<NestedPlan> plans, double selectivity) {
	if (plans == null) {
	    return null;
	}
	if (plans.isEmpty()) {
	    return new NestedPlan();
	}
	if (plans.size() == 1) {
	    return plans.get(0);
	}
	if (plans.size() == 2) {
	    return getBestConjunctivePlan(plans.get(0), plans.get(1), selectivity);
	} else {
	    NestedPlan left = plans.get(0);
	    plans.remove(plans.get(0));
	    return getBestConjunctivePlan(left, plans, selectivity);
	}
    }

    /**
     * Computes the best conjunctive instructionList for a instructionList
     * against a list of plans by calling back the method
     *
     * @param left
     *            Left instructionList
     * @param plans
     *            List of other plans
     * @param selectivity
     *            Overall selectivity
     * @return NestedPlan
     */
    public NestedPlan getBestConjunctivePlan(NestedPlan left, List<NestedPlan> plans, double selectivity) {
	if (plans == null) {
	    return left;
	}
	if (plans.isEmpty()) {
	    return left;
	}
	if (plans.size() == 1) {
	    return getBestConjunctivePlan(left, plans.get(0), selectivity);
	} else {
	    NestedPlan right = getBestConjunctivePlan(plans, selectivity);
	    return getBestConjunctivePlan(left, right, selectivity);
	}
    }

    /**
     * Computes the best conjunctive instructionList for one pair of nested
     * plans
     *
     * @param left
     *            Left instructionList
     * @param right
     *            Right instructionList
     * @param selectivity
     * @return
     */
    public NestedPlan getBestConjunctivePlan(NestedPlan left, NestedPlan right, double selectivity) {
	double runtime1 = 0, runtime2, runtime3;
	NestedPlan result = new NestedPlan();
	double mappingSize = source.size() * target.size() * right.selectivity;

	// first instructionList: run both children and then merge
	runtime1 = left.runtimeCost + right.runtimeCost;

	// second instructionList: run left child and use right child as filter
	runtime2 = left.runtimeCost;
	runtime2 = runtime2 + getFilterCosts(right.getAllMeasures(), (int) Math.ceil(mappingSize));

	// third instructionList: run right child and use left child as filter
	runtime3 = right.runtimeCost;
	mappingSize = source.size() * target.size() * left.selectivity;
	runtime3 = runtime3 + getFilterCosts(left.getAllMeasures(), (int) Math.ceil(mappingSize));

	double min = Math.min(Math.min(runtime3, runtime2), runtime1);

	if (min == runtime1) {
	    result.operator = Instruction.Command.INTERSECTION;
	    result.filteringInstruction = null;
	    List<NestedPlan> plans = new ArrayList<NestedPlan>();
	    plans.add(left);
	    plans.add(right);
	    result.subPlans = plans;

	} else if (min == runtime2) {

	    if (right.isAtomic()) {
		result.filteringInstruction = new Instruction(Instruction.Command.FILTER, right.getEquivalentMeasure(),
			right.getInstructionList().get(0).getThreshold() + "", -1, -1, 0);
	    } else {
		result.filteringInstruction = new Instruction(Instruction.Command.FILTER, right.getEquivalentMeasure(),
			right.filteringInstruction.getThreshold() + "", -1, -1, 0);
	    }

	    result.operator = null;
	    List<NestedPlan> plans = new ArrayList<NestedPlan>();
	    plans.add(left);
	    result.subPlans = plans;

	} else {

	    if (left.isAtomic()) {
		result.filteringInstruction = new Instruction(Instruction.Command.FILTER, left.getEquivalentMeasure(),
			left.getInstructionList().get(0).getThreshold() + "", -1, -1, 0);
	    } else {
		result.filteringInstruction = new Instruction(Instruction.Command.FILTER, left.getEquivalentMeasure(),
			left.filteringInstruction.getThreshold() + "", -1, -1, 0);
	    }

	    result.operator = null;
	    List<NestedPlan> plans = new ArrayList<NestedPlan>();
	    plans.add(right);
	    result.subPlans = plans;

	}
	result.runtimeCost = min;
	result.selectivity = selectivity;
	result.mappingSize = source.size() * target.size() * selectivity;

	return result;
    }

}
