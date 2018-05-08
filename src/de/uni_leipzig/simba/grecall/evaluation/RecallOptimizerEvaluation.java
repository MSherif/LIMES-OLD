package de.uni_leipzig.simba.grecall.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.evaluation.AutomaticExperiments;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.execution.rewriter.Rewriter;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.grecall.optimizer.RecallOptimizerFactory;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Baseline;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Optimizer;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.RecallOptimizer;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.grecall.util.DatasetConfiguration;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.grecall.util.StatisticsBase;
import de.uni_leipzig.simba.specification.LinkSpec;

public class RecallOptimizerEvaluation {

    private static final String FILE_HEADER = "Original LS\tC-RO\tRO-MA";
    private static final String SELECTIVITY_HEADER = "Original LS\tDesired\tC-RO\tRO-MA";
    private static final String OPP_FILE_HEADER = "C-RO\tRO-MA";
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String TAB_DELIMITER = "\t";

    private String baseDirectory;
    private List<String> specifications;
    private SimpleOracle oracle;
    private ExecutionEngine ee;
    private Integer[] maxOptTime = {10000000}; //maximum optimization time
    //private Integer[] maxOptTime = { 100, 200, 400, 800, 1600 }; // maximum
								 // optimization
								 // time
    //private Double[] recall = { 0.1, 0.2, 0.5 }; // recall percentage
    private Double[] recall = {0.1}; //recall percentage
    //private int iterations = 3; // number of iterations
    private int iterations = 1; // number of iterations
    private ArrayList<String> OptimizerType = new ArrayList<String>() {
	{
	    add(0, "downward_refinement");
	    add(1, "downward_refinement_monAS");
	}
    };

    private Cache source;
    private Cache target;
    private String[] FileNames = { "ExecutionTime.csv", "OverallTime.csv", "OptimizationTime.csv",
	    "EstimatedExecutionTime.csv", "MappingSize.csv", "EstimatedSelectivity.csv", "Selectivity.csv",
	    "OptimizedSpecifications.csv" };


    static Logger logger = Logger.getLogger("LIMES");

    public RecallOptimizerEvaluation(String folderName, List<String> specifications, SimpleOracle oracle, Cache source,
	    Cache target) {
	this.specifications = specifications;
	// "datasets/"+DatasetName+"/results/";
	this.baseDirectory = folderName;
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
	this.source = source;
	this.target = target;
	this.oracle = oracle;
    }
    
    public LinkedHashMap<String, Experiment> runBaseline() {
	LinkedHashMap<String, Experiment> baselines = new LinkedHashMap<String, Experiment>();
	
	for (String specification : specifications) {
	    logger.info("+++++++++++++++++++++++++++++++" + "Baseline"
	    + "+++++++++++++++++++++++++++++++");
	    Experiment experiment = new Experiment(specification);

	    RecallOptimizer rr = new Baseline(this.oracle);
	    experiment.optimize(rr, (long) 0.0);
	    experiment.run(ee);
	    
	    baselines.put(specification, experiment);

	}
	return baselines;
    }

    public void runAllConfigs() {
	for (int iteration = 0; iteration < iterations; iteration++) {
	    
	    logger.info("Running baseline for iteration: " + iteration);
	    LinkedHashMap<String, Experiment> baselines = runBaseline();
	 
	    for (double rec : recall) {
		for (int maxTime : maxOptTime) {
		    int specCounter = 0;
		    
		    for (String specification : specifications) {
			logger.info("Iteration: " + iteration);
			logger.info("Running for k: " + rec + " and maxTime: " + maxTime);

			LinkedHashMap<String, Experiment> experiments = new LinkedHashMap<String, Experiment>();
			specCounter++;
			//experiments for each specification
			experiments = new LinkedHashMap<String, Experiment>();
			//just add baseline experiment first
			Experiment baselineExperiment = baselines.get(specification);
			experiments.put("baseline", baselineExperiment);
			//for each different optimizer typer, 
			//do the following
			for (int i = 0; i < OptimizerType.size(); i++) {
			    logger.info("+++++++++++++++++++++++++++++++" + OptimizerType.get(i)
				    + "+++++++++++++++++++++++++++++++");
			    Experiment experiment = new Experiment(specification);
			    //create optimizer
			    RecallOptimizer rr = RecallOptimizerFactory.getOptimizer(OptimizerType.get(i), source.size(), target.size(), oracle,
					rec);
			    //optimize
			    experiment.optimize(rr, maxTime);
			    //run the experiment
			    experiment.run(ee);
			    
			    experiments.put(OptimizerType.get(i), experiment);
			}
			
			// iteration folder
			String str1 = this.baseDirectory + "/" + iteration;
			File dirName = new File(str1);
			if (!dirName.isDirectory()) {
			    try {
				dirName.mkdir();
			    } catch (SecurityException se) {
			    }
			}
			// max optimization time folder
			String str2 = str1 + "/" + maxTime;
			dirName = new File(str2);
			if (!dirName.isDirectory()) {
			    try {
				dirName.mkdir();
			    } catch (SecurityException se) {
			    }
			}
			// k folder
			String DirectoryName = str2 + "/" + String.valueOf(rec * 100) + "%";
			dirName = new File(DirectoryName);
			if (!dirName.isDirectory()) {
			    try {
				dirName.mkdir();
			    } catch (SecurityException se) {
			    }
			}
			//report links for each experiment 
			for (Map.Entry<String, Experiment> entry : experiments.entrySet()) {
			    String OptimizerType = entry.getKey();
			    Experiment experiment = entry.getValue();
			    
			    // save mapping
			    // create optimization folder
			    if (iteration == 0) {
				// if (iteration == 0 && specCounter % 20 == 0)
				// {

				String OptimizerTypeFolder = DirectoryName + "/" + OptimizerType;
				dirName = new File(OptimizerTypeFolder);
				if (!dirName.isDirectory()) {
				    try {
					dirName.mkdir();
				    } catch (SecurityException se) {
				    }
				}
				String specFilename = OptimizerTypeFolder + "/" + specCounter + ".txt";
				File file = new File(specFilename);
				FileWriter writer = null;
				if (!file.exists()) {
				    try {
					writer = new FileWriter(specFilename);
					Mapping m = experiment.getMapping();
					String mappingPairs = m.pairsOutput();
					writer.append(mappingPairs);
					writer.close();
				    } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}
			    }

			}

			int filenameCounter = 0;
			// report results for this iteration, k, max and specification
			// optimization time
			for (String filename : FileNames) {
			    try {
				String FileName = DirectoryName + "/" + filename;
				File f = new File(FileName);
				FileWriter writer = null;
				// create file if it doesn't exist
				if (f.exists()) {
				    writer = new FileWriter(f, true);
				} else {
				    writer = new FileWriter(f);
				    if (filenameCounter != 2) { // optimization time report - original's is 0 - no need to report
					if (filenameCounter == 5) { // approximated selectivity file
					    writer.append(SELECTIVITY_HEADER.toString());
					    writer.append(NEW_LINE_SEPARATOR);
					} else {
					    writer.append(FILE_HEADER.toString());
					    writer.append(NEW_LINE_SEPARATOR);
					}
				    } else {// optimization time stats
					writer.append(OPP_FILE_HEADER.toString());
					writer.append(NEW_LINE_SEPARATOR);

				    }
				}
				DecimalFormat df = new DecimalFormat("#");
				df = new DecimalFormat("#");
				df.setMaximumFractionDigits(50);
				// report statistics for each experiment

				for (Map.Entry<String, Experiment> entry : experiments.entrySet()) {
				    String OptimizerType = entry.getKey();
				    Experiment experiment = entry.getValue();

				    float[] statistics = experiment.getStatistics();
				    LinkSpec spec = experiment.getSpec();

				    if (filenameCounter == 7) { // last file is
								// specification
								// file
					String LS = spec.toStringOneLine();
					writer.append((String) LS);
					writer.append(TAB_DELIMITER);
				    } else { // third file is the optimization
					     // file
					if (filenameCounter == 2 && OptimizerType.equals("baseline"))
					    continue;
					//estimate true selectivity
					if(filenameCounter == 6){
					    float t = statistics[4]/(float)(source.size()*target.size());
					    writer.append(df.format(t));
					    writer.append(TAB_DELIMITER);
					}else{
					    
					    if (filenameCounter == 5 && OptimizerType.equals("downward_refinement")) {
						// add desired selectivity
						float t2 = statistics[6];
						writer.append(df.format(t2));
						writer.append(TAB_DELIMITER);
					    }
					    float t = statistics[filenameCounter];
					    writer.append(df.format(t));
					    writer.append(TAB_DELIMITER);
					}
					

				    }

				}
				writer.append(NEW_LINE_SEPARATOR);
				writer.flush();
				writer.close();
				filenameCounter++;
			    } catch (Exception e) {
				e.printStackTrace();
			    }

			}
		    }
		}
	    }
	}
    }

    public static void main(String args[]) {

	DatasetConfiguration dataCr = null;
	if (args.length != 0) {
	    dataCr = new DatasetConfiguration(args);
	} else
	    dataCr = new DatasetConfiguration();

	// for each dataset
	for (String DatasetName : dataCr.getDatasets()) {

	    logger.info("Current dataset: " + DatasetName);
	    dataCr.setCurrentData(DatasetName);
	    EvaluationData data = dataCr.getCurrentDataset();
	    // TODO: CHANGE THIS
	    String SpecificationsFileName = "datasets/" + DatasetName + "/specifications/specifications_smaller.txt";
	    List<String> specifications = dataCr.getSpecifications(SpecificationsFileName);

	    SimpleOracle or = new SimpleOracle(DatasetName);
	    or.loadOracle("max");
	    
	    String BaseDirectory = "datasets/" + DatasetName + "/results_smaller/";
	    File dirName = new File(BaseDirectory);
	    if (!dirName.isDirectory()) {
		try {
		    dirName.mkdir();
		} catch (SecurityException se) {
		}
	    }
	    logger.info(SpecificationsFileName);
	    RecallOptimizerEvaluation exp = new RecallOptimizerEvaluation(BaseDirectory, specifications, or,
		    dataCr.getSource(), dataCr.getTarget());
	    exp.runAllConfigs();
	    exp = null;

	}

    }

}