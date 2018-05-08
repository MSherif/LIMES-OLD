/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.evaluation;

import com.mxgraph.util.mxCellRenderer;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.execution.rewriter.Rewriter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.specification.LinkSpec;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author ngonga
 */
public class AutomaticExperiments {

    public List<String> specifications;
    protected Cache source;
    protected Cache target;
    protected ExecutionEngine ee;
    static Logger logger = Logger.getLogger("LIMES");

    public AutomaticExperiments(String folderName, String specFile) {
	DataSets d = null;

	if (folderName.equalsIgnoreCase("DBLP-ACM")) {
	    d = DataSets.DBLPACM;
	} else if (folderName.equalsIgnoreCase("DBLP-Scholar")) {
	    d = DataSets.DBLPSCHOLAR;
	} else if (folderName.equalsIgnoreCase("Person1")) {
	    d = DataSets.PERSON1;
	} else if (folderName.equalsIgnoreCase("Person2")) {
	    d = DataSets.PERSON2;
	} else {
	    System.out.println("Experiment " + folderName + " Not implemented yet");
	}
	EvaluationData data = DataSetChooser.getData(d);
	source = data.getSourceCache();
	target = data.getTargetCache();
	logger.info("Source size = " + source.getAllUris().size());
	logger.info("Target size = " + target.getAllUris().size());
	specifications = readSpecFile(specFile);
	ee = new ExecutionEngine(source, target, "?x", "?y");
	try {
	    PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
	    FileAppender fileAppender = new FileAppender(layout, specFile.replaceAll(".xml", "") + ".log", false);
	    fileAppender.setLayout(layout);
	    logger.removeAllAppenders();
	    logger.addAppender(fileAppender);
	} catch (Exception e) {
	    logger.warn("Exception creating file appender.");
	}
	logger.setLevel(Level.DEBUG);
	logger.info("Running on " + specifications.size() + " specs");

    }

    public AutomaticExperiments(String sourceFile, String targetFile, String specFile) {
	source = de.uni_leipzig.simba.selfconfig.Experiment.readFile(sourceFile);
	target = de.uni_leipzig.simba.selfconfig.Experiment.readFile(targetFile);
	logger.info("Source size = " + source.getAllUris().size());
	logger.info("Target size = " + target.getAllUris().size());
	specifications = readSpecFile(specFile);
	ee = new ExecutionEngine(source, target, "?x", "?y");
	try {
	    PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
	    FileAppender fileAppender = new FileAppender(layout, specFile.replaceAll(".xml", "") + ".log", false);
	    fileAppender.setLayout(layout);
	    logger.removeAllAppenders();
	    logger.addAppender(fileAppender);
	} catch (Exception e) {
	    logger.warn("Exception creating file appender.");
	}
	logger.setLevel(Level.DEBUG);
	logger.info("Running on " + specifications.size() + " specs");
    }

    public List<String> readSpecFile(String specFile) {
	List<String> result = new ArrayList<String>();
	try {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(specFile), "UTF8"));
	    String s = reader.readLine();
	    while (s != null) {
		s = s.replace("\"", "");
		if (s.contains(">=")) {
		    LinkSpec spec = new LinkSpec();
		    spec.readSpec(s.split(">=")[0], 5);
		    // System.out.println(s + "\t" + spec.size());
		    result.add(s);
		}
		s = reader.readLine();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return result;
    }

    public void runAllConfigs(String outputFile, int iterations) {

	try {
	    List<Long> cp, cpr, hp, hpr;
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
	    writer.println("CP\tRW+CP\tHP\tRW+HP\tSize");
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
		    writer.println(cp.get(0) + "\t" + cpr.get(0) + "\t" + hp.get(0) + "\t" + hpr.get(0) + "\t"
			    + specification.size());
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

	Mapping m = new Mapping();
	LinkSpec spec = new LinkSpec();
	spec.readSpec(specification.split(Pattern.quote(">="))[0],
		Double.parseDouble(specification.split(Pattern.quote(">="))[1]));
	long begin, end, duration = Long.MAX_VALUE;
	for (int i = 0; i < iterations; i++) {

	    // create planner
	    if (planner.startsWith("c")) {
		p = new CanonicalPlanner();
	    } else {
		p = new HeliosPlanner(source, target);
	    }
	    Rewriter rewriter = new AlgebraicRewriter();

	    begin = System.currentTimeMillis();
	    // rewrite spec if required
	    if (rewrite) {
		spec = rewriter.rewrite(spec);
	    }

	    // generate plan and run
	    NestedPlan np = p.plan(spec);
	    m = ee.runNestedPlan(np);
	    if (p instanceof CanonicalPlanner) {
		cMapping = m;
	    } else {
		hMapping = m;
	    }
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
	// AutomaticExperiments exp = new
	// AutomaticExperiments("datasets/DBLP.csv", "datasets/Scholar.csv",
	// "datasets/acm-dblp.txt");
	// exp.runAllConfigs("datasets/dblp-scholar_results.txt", 10);
	// AutomaticExperiments exp = new
	// AutomaticExperiments("datasets/DBLP.csv", "datasets/ACM.csv",
	// "datasets/acm-dblp.txt");
	// exp.runAllConfigs("datasets/dblp-acm-sizes", 0);
	// AutomaticExperiments exp = new
	// AutomaticExperiments("datasets/persons11.csv",
	// "datasets/persons12.csv", "datasets/person1.txt");
	// exp.runAllConfigs("datasets/persons1_results.txt", 10);
	// AutomaticExperiments exp = new
	// AutomaticExperiments("datasets/restaurant1.csv",
	// "datasets/restaurant2.csv", "datasets/restaurant.txt");
	// exp.runAllConfigs("datasets/restaurant_results.txt", 10);
	AutomaticExperiments exp = new AutomaticExperiments("datasets/DBLP-ACM/ACM.csv", "datasets/DBLP-ACM/DBLP2.csv",
		"datasets/test/test_spec.txt");
	exp.runAllConfigs("datasets/test/lgd_results.txt", 1);
    }
}
