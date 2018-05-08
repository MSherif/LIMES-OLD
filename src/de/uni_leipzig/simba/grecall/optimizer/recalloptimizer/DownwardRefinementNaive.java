package de.uni_leipzig.simba.grecall.optimizer.recalloptimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.measures.MeasureProcessor;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;

public class DownwardRefinementNaive extends RecallOptimizer {
    private ArrayList<LinkSpec> waiting = new ArrayList<LinkSpec>(); // LS that
								     // are
								     // waiting
								     // to be
								     // expanded
    private TreeSet<String> overall = new TreeSet<String>(); // all the LS that
							     // have been
							     // expanded,are
							     // waiting or have
							     // been rejected

    public DownwardRefinementNaive(long sourceSize, long targetSize, SimpleOracle oracle, double recall) {
	super(sourceSize, targetSize, oracle, recall);

    }

    public void optimize() {
	int counter = 0;
	this.waiting.add(this.bestEntry.getX());
	this.overall.add(this.bestEntry.getX().toString());
	Iterator<LinkSpec> waitingIT = this.waiting.iterator();
	long start = System.currentTimeMillis();
	long end = start + this.timeCounter;

	while (waitingIT.hasNext() && System.currentTimeMillis() < end) {// check
									 // specs
									 // that
									 // need
									 // to
									 // be
									 // explored
	    counter++;

	    logger.info("--------------Attempt: " + counter + "-------------------");
	    double bestruntime = bestEntry.getY().runtimeCost;
	    double bestSelectivity = bestEntry.getY().selectivity;
	    logger.info("Best selectivity now: " + bestSelectivity + " || " + this.DesiredSelectivity);
	    logger.info("Best runtime: " + bestruntime);
	    logger.info("Stilling waiting for: " + waiting.size());

	    LinkSpec currentSpec = new LinkSpec();
	    currentSpec = waitingIT.next();

	    // compare best plan so far with the new node you are about to
	    // expand
	    NestedPlan currentPlan = this.getPlanFromSpecification(currentSpec);
	    if (this.scoreStrategy.compare(currentSpec, this.bestEntry.getX(), currentPlan,
		    this.bestEntry.getY()) == true) {
		this.bestEntry.setX(currentSpec);
		this.bestEntry.setY(currentPlan);
		logger.info("Best spec: " + this.bestEntry.getX());
		logger.info("Best runtime: " + this.bestEntry.getY().runtimeCost);
		logger.info("Best selectivity: " + this.bestEntry.getY().selectivity + "::" + this.DesiredSelectivity);
		if (checkSelectivity(this.bestEntry.getY().selectivity).equals("equal"))
		    break;
	    }
	    // remove it from the waiting list
	    waitingIT.remove();

	    logger.info("Stilling waiting for: " + waiting.size());
	    logger.info("Refine current LS");
	    logger.info(currentSpec);
	    logger.info(this.getPlanFromSpecification(currentSpec).runtimeCost);

	    LinkedHashMap<LinkSpec, NestedPlan> newPlans = new LinkedHashMap<LinkSpec, NestedPlan>();
	    newPlans = expandPlan(currentSpec);

	    if (newPlans.size() != 0) {
		logger.info("You can expand by " + newPlans.size() + " nodes.");
		addToWaiting(newPlans);

	    } else {
		logger.info("End of traing destination.\n" + "You are stuck with this: " + this.bestEntry.getX());
	    }

	    logger.info("Stilling waiting for: " + waiting.size());
	    logger.info("Overall: " + overall.size());
	    if (waiting.size() == 0)
		break;
	    if (checkSelectivity(this.bestEntry.getY().selectivity).equals("higher")) {
		waitingIT = this.waiting.iterator();
	    } else
		break;
	}
    }

    private void addToWaiting(LinkedHashMap<LinkSpec, NestedPlan> newPlans) {
	// this is strategy
	LinkedHashMap<LinkSpec, Double> newOverallPlans = new LinkedHashMap<LinkSpec, Double>();
	// compute plan of new LS
	newOverallPlans = this.scoreStrategy.LinkToScore(newPlans);
	// order LS based on their plan's runtime
	LinkedHashMap<LinkSpec, Double> newOverallPlans2 = this.scoreStrategy.sortByValues(newOverallPlans, true);
	ArrayList<LinkSpec> list = new ArrayList<LinkSpec>(newOverallPlans2.keySet());
	// empty waiting list
	waiting.clear();
	// add only the LS with the lowest runtime score
	waiting.add(list.get(0));
	for (LinkSpec sp : waiting) {
	    logger.info(sp);
	}

    }

    @Override
    protected LinkedHashMap<LinkSpec, NestedPlan> expandPlan(LinkSpec currentSpec) {
	LinkedHashMap<LinkSpec, NestedPlan> newPlans = new LinkedHashMap<LinkSpec, NestedPlan>();
	for (LinkSpec child : currentSpec.getAllLeaves()) {
	    logger.info("====>> Working on child: " + child);
	    double oldThreshold = child.threshold;
	    if (oldThreshold == 1.0) {// don't add it as a plan
		logger.info("Cannot optimize anymore");
		continue;
	    }
	    double newThreshold = this.oracle.returnNextThreshold(child.threshold);
	    if (newThreshold == -0.1d) {// don't add it as a plan
		logger.info("something went horribly wrong here. Can't find next threshold");
		continue;
	    }

	    child.threshold = newThreshold;
	    logger.info("New spec: " + currentSpec);
	    LinkSpec tempSpec = currentSpec.clone();

	    if (this.overall.contains(tempSpec.toString())) {
		logger.info("Already waiting for you to expand" + this.overall.size());
		child.threshold = oldThreshold;
		continue;
	    }
	    overall.add(tempSpec.toString());// add it even though the
					     // selectivity might be low
	    NestedPlan plan = getPlanFromSpecification(tempSpec);
	    if (checkSelectivity(plan.selectivity).equals("lower")) { // don't
								      // add it
								      // as a
								      // plan
		logger.info("Selectivity now: " + plan.selectivity + " || " + this.DesiredSelectivity);
		logger.info("went too far, go back, sel too low");
		child.threshold = oldThreshold;
		continue;
	    }
	    newPlans.put(tempSpec, plan);
	    logger.info("Selectivity result: " + plan.selectivity + " || " + this.DesiredSelectivity);
	    logger.info("Runtime result: " + plan.runtimeCost);
	    child.threshold = oldThreshold;
	}
	logger.info("Old spec: " + currentSpec);
	return newPlans;
    }

}