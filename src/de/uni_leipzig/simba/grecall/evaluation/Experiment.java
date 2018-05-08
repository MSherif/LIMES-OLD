package de.uni_leipzig.simba.grecall.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.grecall.optimizer.RecallOptimizerFactory;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Baseline;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Optimizer;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.RecallOptimizer;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.specification.LinkSpec;

public class Experiment {
    static Logger logger = Logger.getLogger("LIMES");

    private LinkSpec spec = null;
    private float[] statistics = new float[7];
    private Mapping mapping = new Mapping();

    // 0: private float ExecutionTime = Long.MAX_VALUE;
    // 1: private float OverallTime = Long.MAX_VALUE;
    // 2: private float OptimizationTime = 0.0f;
    // 3: private float EstExecutionTime = Long.MAX_VALUE;
    // 4: private float MappingSize = Long.MAX_VALUE;
    // 5: private float ApprSelectivity = Long.MAX_VALUE;
    // 6: private float DesiredSelectivity = Long.MAX_VALUE;
    Experiment(String spec) {
	this.spec = new LinkSpec(spec.split(Pattern.quote(">="))[0], Double.parseDouble(spec.split(Pattern.quote(">="))[1]),true);
    }

    public Mapping getMapping() {
	return this.mapping;
    }

    public void optimize(RecallOptimizer rr, long timeLimit) {

	long begin, end = Long.MAX_VALUE;
	logger.info(this.spec);
	begin = System.currentTimeMillis();
	rr.setTimeCounter(timeLimit);
	rr.setSpec(this.spec);
	rr.optimize();
	end = System.currentTimeMillis();
	this.spec = rr.getNewSpec();

	if(rr instanceof Baseline)
	    this.statistics[2] = 0;
	else
	    this.statistics[2] = end - begin;
	this.statistics[3] = rr.getExecutionTimeEstimation();
	this.statistics[5] = (float) rr.getSelectivityEstimation();
	this.statistics[6] = (float) rr.getDesiredSelectivity();

    }

    public void run(ExecutionEngine ee) {
	ExecutionPlanner p;
	Mapping cMapping = null;
	long begin, end = Long.MAX_VALUE;
	p = new CanonicalPlanner();
	
	logger.info("Start computing mapping");
	begin = System.currentTimeMillis();
	NestedPlan np = p.plan(this.spec);
	cMapping = ee.runNestedPlan(np);
	end = System.currentTimeMillis();
	this.mapping = cMapping;
	logger.info("Finished");
	
	logger.info(this.spec);
	this.statistics[0] = end - begin;
	this.statistics[4] = cMapping.getNumberofMappings();
	this.statistics[1] = this.statistics[2] + this.statistics[0];

	logger.info("Execution time of LS : " + this.statistics[0]);
	logger.info("Optimization time " + this.statistics[2]);
	logger.info("Overall time of LS : " + this.statistics[1]);
	logger.info("Estimated selectivity " + this.statistics[5]);
	logger.info("Estimated execution time " + this.statistics[3]);
	logger.info("Mapping size of LS " + this.statistics[4]);

    }

    public float[] getStatistics() {
	return statistics;
    }

    public LinkSpec getSpec() {
	return this.spec;
    }
}
