package de.uni_leipzig.simba.learning.refinement.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.BatchEvaluation;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;
import de.uni_leipzig.simba.learning.refinement.supervised.SupervisedRefinementAlgorithm;
import de.uni_leipzig.simba.selfconfig.EuclidSupervisedExperiment;
import de.uni_leipzig.simba.selfconfig.EuclidSupervisedExperiment.EuclidEvaluationMemory;

/**
 * Class to compare both EAGLE, EUCLID and LION based on supervised batch mode using the same 
 * test data throughout all experiments (and all runs within all experiments).
 * @author Klaus Lyko
 *
 */
public class BatchComparison {
	protected static Logger logger = Logger.getLogger("LIMES");
	public boolean useFullCaches = false;
	File logFile = new File("resources/results/BatchComparison.csv");
	public static final String SEP =";";
	String logExt = "";
	long duration = 600;
	public BatchComparison(int runs, long duration, String logExt) {
		this.logExt = logExt;
		if(logExt.length()>0)
			logFile = new File("resources/results/BatchComparison_"+logExt+".csv");
		
		String out = "Dataset"+SEP+"Algorithm"+SEP+"mean(f_full)"+SEP+"std(f_full)";
		for(int i=1;i<=runs;i++)
			out+=SEP+"run "+i;
		log(out, false);
		this.duration=duration;
	}
	
	/**
	 * Runs LIONs experiments
	 * @param data
	 * @param trainingData
	 */
	public void runLIONClassical(EvaluationData data, Mapping[] trainingDatas) {
		int i = 1;
		Statistics statFFull = new Statistics();
		for(Mapping trainingData : trainingDatas) {
			String dataName = data.getName();
			if(logExt.length()>0)
				data.setName("batch_std("+logExt+")_"+data.getName()+"_run"+i);
			else	
				data.setName("batch_std_"+data.getName()+"_run"+i);
			System.out.println("Running classical batched LION on dataset "+data.getName());
			SupervisedRefinementAlgorithm.hardRootExpansion = true;
			SupervisedRefinementAlgorithm algo = new SupervisedRefinementAlgorithm(data, 100, true, useFullCaches, false);
			algo.init(data, 0, 0.9, 1.0, duration);
			Mapping toAsk = algo.learn(trainingData, -1);
			algo.end();
			List<EvaluationMemory> results = algo.getMemList();
			statFFull.add(results.get(results.size()-2).realfFull);
			data.setName(dataName);
			i++;
		}
		String out = data.getName()+SEP+"LION_std"+SEP+statFFull.mean+SEP+statFFull.standardDeviation+SEP;
		for(Double d : statFFull.elements)
			out+=d+SEP;
		this.log(out, true);
	}
	
	public void runLIONImproved(EvaluationData data, Mapping[] trainingDatas, float supVisedPortion) {
		int i = 1;
		Statistics statFFull = new Statistics();
		for(Mapping trainingData : trainingDatas) {
			String dataName = data.getName();
			if(logExt.length()>0)
				data.setName("batch_("+logExt+")_"+supVisedPortion+"f+pfm_"+data.getName()+"_run"+i);
			else	
				data.setName("batch_"+supVisedPortion+"f+pfm_"+data.getName()+"_run"+i);
			System.out.println("Running improved batched LION on dataset "+data.getName());
			SupervisedRefinementAlgorithm.hardRootExpansion = true;
			SupervisedRefinementAlgorithm algo = new SupervisedRefinementAlgorithm(data, 100, true, useFullCaches, true);
			algo.setTrimmedPortion(supVisedPortion);
			algo.init(data, 0, 0.9, 1.0, duration);
			Mapping toAsk = algo.learn(trainingData, -1);
			algo.end();
			List<EvaluationMemory> results = algo.getMemList();
			statFFull.add(results.get(results.size()-2).realfFull);
			data.setName(dataName);
			i++;
		}
		String out = data.getName()+SEP+"LION_impr: f*"+supVisedPortion+""+SEP+statFFull.mean+SEP+statFFull.standardDeviation+SEP;
		for(Double d : statFFull.elements)
			out+=d+SEP;
		this.log(out, true);
	}
	
	
	
	/**
	 * Runs EAGLE experiments: details on BatchEvaluation.java.
	 * @param data
	 * @param trainingData
	 * @throws FileNotFoundException
	 * @throws InvalidConfigurationException
	 */
	public void runEAGLE(DataSets data, Mapping[] trainingDatas) throws FileNotFoundException, InvalidConfigurationException {
		int i = 1;
		List<Statistics> fScores = new LinkedList<Statistics>();
		for(Mapping trainingData:trainingDatas) {
			BatchEvaluation evaluation = new BatchEvaluation();	
			if(logExt.length()>0)
				evaluation.logFileNameplus = "_"+logExt+"_run"+i;
			else	
				evaluation.logFileNameplus = "_run"+i;
//			evaluation.setOutStreams(data.name());
			evaluation.runTimeBased(duration, useFullCaches);// set termination criteria: time based
			// new pop && data				
			int inqueries = 10;
			int questions = 10;		
			Statistics fullF = evaluation.run(data, 20, 10, inqueries, questions, trainingData);
			fScores.add(fullF);
			i++;
		}
		writeEagleResults(DataSetChooser.getData(data), fScores);
	}
	
	public void runEUCLID(DataSets data, Mapping[] trainingDatas) {
		int i = 1;
		Map<String, Statistics> statMap = new HashMap<String, Statistics>();
		for(String type : EuclidSupervisedExperiment.types) {
			statMap.put(type, new Statistics());
		}
		for(Mapping trainingData : trainingDatas) {
	        String result = "";//"Type\tBeta\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\n";
	        EuclidSupervisedExperiment evaluator = new EuclidSupervisedExperiment(0.2f, 100);
	        evaluator.maxDuration = duration;
	       	EvaluationData ds = DataSetChooser.getData(data);
	     	String name= ds.getName()+"_run"+i;
	       	if(logExt.length()>0)
	       		name = ds.getName()+"_"+logExt+"_run"+i;
	      
//	        System.out.println("TrainingData:"+trainingData.size());
	          result = result + ds.getName()+" with "+trainingData.size()+"training examples \n";
	          result = result + evaluator.runAll(ds.getSourceCache(),
	          ds.getTargetCache(),
	          ds.getReferenceMapping(),
	          0.6, 1d, trainingData,
	          name);
	          Map<String, EuclidEvaluationMemory> runResult = evaluator.results;
	          for(String type:EuclidSupervisedExperiment.types) {
	        	  if(runResult.containsKey(type))
	        		  statMap.get(type).add(runResult.get(type).fullFScore);
	          }
//	          System.out.println(result);
			i++;
		}
		for(Entry<String, Statistics> e: statMap.entrySet()) {
			String out = data.name()+SEP+"EUCLID("+e.getKey()+")"+SEP+e.getValue().mean+SEP+e.getValue().standardDeviation;
			for(double val : e.getValue().elements)
				out+=SEP+val;
			log(out,true);
		}

 
	}
	
	public Mapping getTrainingData(Mapping fullKnown, int size) {
		return ExampleOracleTrimmer.getRandomTrainingData(fullKnown, size);
	}
	
	
	public static void main(String args[]) throws ParseException, IOException, ClassNotFoundException {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(getCLIOptions(), args);
		
		Logger logger = Logger.getLogger("LIMES");
		logger.setLevel(Level.ERROR);
		DataSets allEvalData[] = {
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPACM,				
				DataSets.DBPLINKEDMDB,
				DataSets.DBLPSCHOLAR,
		}; 
		int runs = 3;
		long duration = 600;
		float perc = 0.3f;
		String logExt = "";
		float supervisedPortion = 0.8f;
		if(cmd.hasOption("supervisedPortion"))
			supervisedPortion = Float.parseFloat(cmd.getOptionValue("supervisedPortion"));
		if(cmd.hasOption("dur"))
			duration = Long.parseLong(cmd.getOptionValue("dur"));
		if(cmd.hasOption("perc"))
			perc = Float.parseFloat(cmd.getOptionValue("perc"));
		if(cmd.hasOption("logExt"))
			logExt = cmd.getOptionValue("logExt");
		BatchComparison comp = new BatchComparison(runs, duration, logExt);
		if(cmd.hasOption("trimmed")) {
			comp.useFullCaches = false;
		}
		
		try {comp.setOutStreams("BatchComparator");} catch (FileNotFoundException e1) {e1.printStackTrace();}
		for(DataSets ds : allEvalData) {
			EvaluationData data = DataSetChooser.getData(ds);
			data.setName(data.getName());
			// create multiple training data sets
			Mapping[] learnDatas = new Mapping[runs];
			for(int i = 0; i<runs; i++) {
//				learnDatas[i] = comp.getTrainingData(data.getReferenceMapping(), (int)(data.getReferenceMapping().map.keySet().size()*perc)
//						Math.min(100, (int)(data.getReferenceMapping().size()*0.2))
//						);
				learnDatas[i]=TrainingDataHandler.getTrainingData(data, perc, i, false);
			}
			
			
			logger.setLevel(Level.ERROR);
			System.out.println("Batch Comparison - Experiment "+data.getName());
			if(cmd.hasOption("all")) {
				System.out.println("\nRunning EUCLID on "+data.getName());
				comp.runEUCLID(ds, learnDatas);
				try {
					System.out.println("\nRunning EAGLE on "+data.getName());
					comp.runEAGLE(ds, learnDatas);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					System.err.println("\nError (file not found) while running EAGLE on "+data.getName());
					e.printStackTrace();
					
				} catch (InvalidConfigurationException e) {
					// TODO Auto-generated catch block
					System.err.println("\nError (invalid configuration) while running EAGLE on "+data.getName());
					e.printStackTrace();
				}
				System.out.println("\nRunning LION on "+data.getName());
				if(cmd.hasOption("improve") || cmd.hasOption("bothlion"))
					comp.runLIONImproved(data, learnDatas, supervisedPortion);
				if(!cmd.hasOption("improve") || cmd.hasOption("bothlion"))
					comp.runLIONClassical(data, learnDatas);
			} else
			if(cmd.hasOption("bothlion")) {
				comp.runLIONImproved(data, learnDatas, supervisedPortion);

			} else
			if(cmd.hasOption("lion")) {
				comp.runLIONClassical(data, learnDatas);
			}
		}
	}
	
	
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("perc", true, "portion of training data");
		options.addOption("dur", true, "Specify duration");
		options.addOption("logExt", true, "Specify file Extansion");
		options.addOption("supervisedPortion", true, "Set portion of supervised part of quality evaluation for LION improved.");
		options.addOption("all", false, "If set compare LION to both EAGLE and EUCLID");
		options.addOption("improve", false, "If we set we use improved LION");
		options.addOption("bothlion", false, "If set we only compare LION and LION improved");
		options.addOption("lion", false, "If set only run LION");
		options.addOption("trimmed", false, "If set LION as well as EAGLE only operate over trimmed Caches");
		return options;
	}
	
	
	public void setOutStreams(String name) throws FileNotFoundException {
		File stdFile = new File(name+"_stdOut.txt");
		PrintStream stdOut = new PrintStream(new FileOutputStream(stdFile, false));
		File errFile = new File(name+"_errOut.txt");
		PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
		System.setErr(errOut);
		System.setOut(stdOut);
	}
	
	public void log(String s, boolean append) {
		try {
			FileWriter writer = new FileWriter(logFile, append);
			writer.write(s);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void writeEagleResults(EvaluationData data, List<Statistics> fScores) {
		Statistics stat = new Statistics();
		for(Statistics si:fScores) {
			stat.add(si.mean);
		}
		String out = data.getName()+SEP+"EAGLE"+SEP+stat.mean+SEP+stat.standardDeviation;	
		for(double val : stat.elements)
			out+=SEP+val;
		log(out,true);
	}

}
