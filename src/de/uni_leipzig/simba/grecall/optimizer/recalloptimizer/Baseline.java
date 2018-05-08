package de.uni_leipzig.simba.grecall.optimizer.recalloptimizer;

import java.util.LinkedHashMap;
import java.util.List;

import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.specification.LinkSpec;

public class Baseline extends RecallOptimizer {

    public Baseline(SimpleOracle or) {
	this.oracle = or;
    }

    public Baseline(long sourceSize, long targetSize, SimpleOracle oracle, double recall) {
	super(sourceSize, targetSize, oracle, recall);

    }

    @Override
    public LinkSpec getNewSpec() {
	return this.root.getX();
    }

    @Override
    public void optimize() {
    }

    @Override
    protected LinkedHashMap<LinkSpec, NestedPlan> expandPlan(LinkSpec currentSpecs) {
	return null;
    }

}
