package de.uni_leipzig.simba.execution.evaluation.dynamicevaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.DynamicPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.execution.rewriter.Rewriter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.specification.LinkSpec;

public class DynamicPlannerEvaluation {

    public List<String> specifications;
    protected Cache source;
    protected Cache target;
    private String baseDirectory;
    protected ExecutionEngine ee;
    static Logger logger = Logger.getLogger("LIMES");

    public DynamicPlannerEvaluation(String baseDirectory, Cache source, Cache target, List<String> specs) {

	logger.info("Source size = " + source.getAllUris().size());
	logger.info("Target size = " + target.getAllUris().size());
	specifications = specs;
	this.baseDirectory = baseDirectory;
	ee = new ExecutionEngine(source, target, "?x", "?y");
	try {
	    PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
	    FileAppender fileAppender = new FileAppender(layout,
		    (this.baseDirectory + "/test.txt").replaceAll(".xml", "") + ".log", false);
	    fileAppender.setLayout(layout);
	    logger.removeAllAppenders();
	    logger.addAppender(fileAppender);
	} catch (Exception e) {
	    logger.warn("Exception creating file appender.");
	}
	logger.setLevel(Level.DEBUG);
	logger.info("Running on " + specifications.size() + " specs");
    }

    public void runAllConfigs(String outputFile, int iterations) {

	try {
	    List<Long> cp, cpr, hp, hpr, dp, dpr;
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
	    writer.println("CP\tRW+CP\tHP\tRW+HP\tCP\tRW+CP\tSize");
	    LinkSpec specification = new LinkSpec();
	    for (String spec : specifications) {
		logger.info("Running spec " + spec);
		specification.readSpec(spec.split(Pattern.quote(">="))[0],
			Double.parseDouble(spec.split(Pattern.quote(">="))[1]));
		try {
		    cp = runExperiment("c", false, spec, iterations);
		    cpr = runExperiment("c", true, spec, iterations);
		    hp = runExperiment("h", false, spec, iterations);
		    hpr = runExperiment("h", true, spec, iterations);
		    dp = runExperiment("d", false, spec, iterations);
		    dpr = runExperiment("d", true, spec, iterations);
		    writer.println(cp.get(0) + "\t" + cpr.get(0) + "\t" + hp.get(0) + "\t" + hpr.get(0) + "\t"
			    + dp.get(0) + "\t" + dpr.get(0) + "\t" + specification.size());
		    writer.flush();
		} catch (Exception e) {
		    logger.info("Error running " + spec);
		    e.printStackTrace();
		}
	    }
	    writer.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public List<Long> runExperiment(String planner, boolean rewrite, String specification, int iterations) {
	ExecutionPlanner p;
	Mapping cMapping;
	Mapping hMapping;
	Mapping dMapping;

	Mapping m = new Mapping();
	LinkSpec spec = new LinkSpec();
	spec.readSpec(specification.split(Pattern.quote(">="))[0],
		Double.parseDouble(specification.split(Pattern.quote(">="))[1]));
	long begin, end, duration = Long.MAX_VALUE;
	for (int i = 0; i < iterations; i++) {

	    // create planner
	    if (planner.startsWith("c")) {
		p = new CanonicalPlanner();
	    } else if (planner.startsWith("h")) {
		p = new HeliosPlanner(source, target);
	    } else
		p = new DynamicPlanner(source, target);
	    Rewriter rewriter = new AlgebraicRewriter();

	    begin = System.currentTimeMillis();
	    // rewrite spec if required
	    if (rewrite) {
		spec = rewriter.rewrite(spec);
	    }

	    // generate plan and run
	    NestedPlan np = p.plan(spec);
	    if (p instanceof DynamicPlanner) {
		while(((DynamicPlanner) p).isExecuted() == false){
		    //get first unexecuted plan 
		     //if plan is not atomic:
		    	//do not execute, apply command-filter function
		    //else
		    	//execute it and keep track of estimations
		    //go to dynamic planner plans list and update the estimations with real values
		    //change dBit of plan to true
		    
		    //call plan function again.
		    
		}
		
	    }
	    
	    m = ee.runNestedPlan(np);
	    if (p instanceof CanonicalPlanner) {
		cMapping = m;
	    } else if (p instanceof HeliosPlanner) {
		hMapping = m;
	    } else
		dMapping = m;

	    end = System.currentTimeMillis();
	    duration = Math.min(duration, (end - begin));
	}
	// return results
	ArrayList<Long> result = new ArrayList<Long>();
	result.add(duration);
	result.add((long) m.getNumberofMappings());
	return result;
    }

    public static void main(String args[]) {
	DataConfiguration dataCr = null;
	if (args.length != 0) {
	    dataCr = new DataConfiguration(args[0]);
	} else
	    System.exit(1);
	String DatasetName = args[0];
	logger.info("Current dataset: " + DatasetName);
	EvaluationData data = dataCr.getDataset();

	// read specs
	String SpecificationsFileName = "datasets/" + DatasetName + "/specifications/specifications.txt";
	List<String> specifications = dataCr.getSpecifications(SpecificationsFileName);

	// create results folder
	String BaseDirectory = "datasets/" + DatasetName + "/planner_results/";
	File dirName = new File(BaseDirectory);
	if (!dirName.isDirectory()) {
	    try {
		dirName.mkdir();
	    } catch (SecurityException se) {
	    }
	}

	DynamicPlannerEvaluation exp = new DynamicPlannerEvaluation(BaseDirectory, dataCr.getSource(),
		dataCr.getTarget(), specifications);
	exp.runAllConfigs(BaseDirectory + "/results.txt", 1);
    }

}
