package de.uni_leipzig.simba.grecall.optimizer.recalloptimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.specification.LinkSpec;

public class DownwardRefinement extends RecallOptimizer {
    private ArrayList<LinkSpec> waiting = new ArrayList<LinkSpec>(); // LS that are waiting to be expanded
	         
    private HashSet<String> overall = new HashSet<String>(); // all the LS that have been expanded,are waiting or have been rejected

    public DownwardRefinement(long sourceSize, long targetSize, SimpleOracle oracle, double recall) {
	super(sourceSize, targetSize, oracle, recall);

    }

    public void optimize() {
	int counter = 0;
	this.waiting.add(this.bestEntry.getX());
	this.overall.add(this.bestEntry.getX().toString());
	Iterator<LinkSpec> waitingIT = this.waiting.iterator();

	long start = System.currentTimeMillis();
	long end = start + this.timeCounter;
	
	while (this.waiting.size() != 0 && System.currentTimeMillis() < end) {
	    counter++;
	    
	    
	    logger.info("--------------Attempt: " + counter + "-------------------");
	    
	    logger.info("Best selectivity now: " + bestEntry.getY().selectivity + " || " + this.DesiredSelectivity);
	    logger.info("Best runtime: " + bestEntry.getY().runtimeCost);
	    logger.info("Best Spec: "+this.bestEntry.getX());
	    logger.info("Just started: Stilling waiting for: " + waiting.size());

	    //set iterator at top of list
	    waitingIT = this.waiting.iterator();
	    // get the top of the list == next element
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


	    logger.info("Refine current LS"+currentSpec);
	    logger.info(this.getPlanFromSpecification(currentSpec).runtimeCost);
	    LinkedHashMap<LinkSpec, NestedPlan> newPlans = new LinkedHashMap<LinkSpec, NestedPlan>();
	    newPlans = expandPlan(currentSpec);
	    logger.info("Added some stuff: Stilling waiting for: " + waiting.size());

	    // add node to history
	    this.overall.add(currentSpec.toString());
	    // remove it from the waiting list
	    waitingIT.remove();
	    
	    if (newPlans.size() != 0) {
		logger.info("You can expand by " + newPlans.size() + " nodes.");
		addToWaiting(newPlans);

	    } else {
		logger.info("End of traing destination.\n" + "You are stuck with this: " + this.bestEntry.getX());
	    }

	    
	    logger.info("Removed previous: Stilling waiting for: " + waiting.size());
	    logger.info("Overall: " + overall.size());

	}

    }

    private void addToWaiting(LinkedHashMap<LinkSpec, NestedPlan> newPlans) {

	LinkedHashMap<LinkSpec, NestedPlan> waitingPlans = new LinkedHashMap<LinkSpec, NestedPlan>();
	// compute plan for the LS in the waiting list
	for (LinkSpec sp : waiting) {
	    NestedPlan w = this.getPlanFromSpecification(sp);
	    waitingPlans.put(sp, w);
	}
	// add the new plans to the list
	waitingPlans.putAll(newPlans);
	// order them.
	LinkedHashMap<LinkSpec, Double> newOverallPlans = new LinkedHashMap<LinkSpec, Double>();
	newOverallPlans = this.scoreStrategy.LinkToScore(waitingPlans);
	LinkedHashMap<LinkSpec, Double> newOverallPlans2 = this.scoreStrategy.sortByValues(newOverallPlans, true);
	// set the ordered list of plans as waiting list
	waiting = new ArrayList<LinkSpec>(newOverallPlans2.keySet());
    }

    protected LinkedHashMap<LinkSpec, NestedPlan> expandPlan(LinkSpec currentSpec) {
	LinkedHashMap<LinkSpec, NestedPlan> newPlans = new LinkedHashMap<LinkSpec, NestedPlan>();

	for (LinkSpec child : currentSpec.getAllLeaves()) {
	    currentSpec.pathOfAtomic();
	    
	    logger.info("====>> Working on child: " + child);
	    logger.info(child.treePath);
	    if(child.treePath.contains("MINUS->right")){
		logger.info("Child belongs a MINUS -> right path");
		continue;
	    }
	    
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

	    LinkSpec tempSpec = currentSpec.clone();
	    tempSpec.pathOfAtomic();
	    
	    logger.info("New spec: " + tempSpec);
	    
	    if (!this.overall.contains(tempSpec.toString())) {
		logger.info("New node");
		NestedPlan plan = getPlanFromSpecification(tempSpec);
		logger.info("Selectivity now: " + plan.selectivity + " || " + this.DesiredSelectivity+ " || " + this.root.getY().selectivity);
		if (!checkSelectivity(plan.selectivity).equals("lower")) { 
		    newPlans.put(tempSpec, plan);
		    logger.info("Alles gut.Runtime result: " + plan.runtimeCost);  
		}else{
		    logger.info("too low selectivity");  
		}
		overall.add(tempSpec.toString());
	    }
	    child.threshold = oldThreshold;
	}
	
	logger.info("Old spec: " + currentSpec);
	return newPlans;
    }

}
