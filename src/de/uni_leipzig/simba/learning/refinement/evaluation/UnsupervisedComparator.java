package de.uni_leipzig.simba.learning.refinement.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoEvaluation;
import de.uni_leipzig.simba.learning.refinement.RefinementBasedLearningAlgorithm;
import de.uni_leipzig.simba.selfconfig.MyExperiment;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;

/**
 * Class to evaluate EAGLE, LION and EUCLID based upon unsupervised learning, using timebased termination criteria.
 * @author Klaus Lyko
 *
 */
public class UnsupervisedComparator {
	File logFile = new File("UnSupervisedResults.csv");
	public static final String SEP = ";";
	
	
	public UnsupervisedComparator() {
		String head = "Dataset"+SEP+"Algorithm"+SEP+"full F-Score"+SEP+"PFM"+SEP+"Duration";
		log(head, false);
	}
	
//	public void createFile(String name) {
//		logFile = new File("UnSupervisedResults_"+name+".csv");
//		String head = "Dataset"+SEP+"Algorithm"+SEP+"full F-Score"+SEP+"PFM"+SEP+"Duration";
//		log(head, false);
//	}
//	
	
	long maxDur = 600;//in seconds
	 static Logger logger = Logger.getLogger("LIMES");
	public void runLION(EvaluationData data) throws IOException {
		double exp[] = {0.95};//{0.95, 0.9, 0.8}; 
		double gamma[] = {0d};//{0.75, 0.1, 0.15, 0.3};
		double reward[] = {1.0};//{1, 1.2, 1.4};
			RefinementBasedLearningAlgorithm.hardRootExpansion = true;
			RefinementBasedLearningAlgorithm algo = new RefinementBasedLearningAlgorithm(data);
			for(double e : exp) {
				for(double ga : gamma) {
					for(double re : reward) {
						algo.init(data, ga, e, re, maxDur);
						algo.start();
						List<EvaluationMemory> res = algo.getMemList();
						if(res.size()>0) {
							EvaluationMemory best = res.get(res.size()-1);
							String log = data.getName()+SEP+
									"LION exp="+e+" re="+re+SEP+
									best.realfFull+SEP+
									best.pfm+SEP+
									best.duration;
							log(log, true);
						}
					}// for reward
				}// for gamma
			}//for exp
		}
	
	public void runEUCLID(EvaluationData data) {
		String result = "\n"+data.getName()+"\n";
		result += MyExperiment.runAll(data.getSourceCache(), 
				data.getTargetCache(),
				data.getReferenceMapping(), 
				0.6, 1d, new PseudoMeasures(), maxDur);
		MyExperiment.writeToFile(result, true);
	}
	
	public void runEAGLE(EvaluationData data) throws InvalidConfigurationException {
		logger.setLevel(Level.ERROR);
		PseudoEvaluation eval = new PseudoEvaluation();
		eval.maxRuns = 5; eval.maxDuration = maxDur;
		PseudoMeasures measure = new PseudoMeasures();
		for(int run = 1; run<=5; run++) {
			eval.run(data, run, measure, "EAGLE_PFM");
		}
	}
	
	
	public static void main(String args[]) throws ParseException {
		
		CommandLineParser parser = new BasicParser();
		Options options = getCLIOptions();
		CommandLine cmd = parser.parse(options, args);
		System.out.println("Run all?"+cmd.hasOption("all")+" run EUCLID?"+cmd.hasOption("euclid")+" run EAGLE?"+cmd.hasOption("eagle"));
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
		UnsupervisedComparator comp = new UnsupervisedComparator();
		comp.maxDur = 600;
		if(cmd.hasOption("dur")) {
			comp.maxDur = Long.parseLong(cmd.getOptionValue("dur"));
			System.out.println("Parsed long: "+comp.maxDur);
		}
//		comp.setOutStreams("UnSupComparator");
		logger.setLevel(Level.ERROR);
		for(DataSets ds : allEvalData) {
			
			EvaluationData data = DataSetChooser.getData(ds);
			System.out.println("Unsupervised Comparison - Experiment "+data.getName());

			try {
				if(cmd.hasOption("lion") || cmd.hasOption("all")) {
					System.out.println("\nRunning LION on "+data.getName());
					comp.runLION(data);
				}
			} catch (IOException e) {
				System.out.println("Error running LION on "+data.getName()+" "+ e.getMessage());
				System.err.println("Error running LION on "+data.getName()+" "+ e.getMessage());
				e.printStackTrace();
			}
			if(cmd.hasOption("all")) {
				System.out.println("\nRunning EUCLID on "+data.getName());
				comp.runEUCLID(data);
				System.out.println("\nRunnung EAGLE on "+data.getName());
				try {
					comp.runEAGLE(data);
				} catch (InvalidConfigurationException e) {
					System.out.println("Error running EAGLE on "+data.getName()+" "+ e.getMessage());
					System.err.println("Error running EAGLE on "+data.getName()+" "+ e.getMessage());
					e.printStackTrace();
				}
			} else  {
				if(cmd.hasOption("eagle")) {
					System.out.println("\nRunnung EAGLE on "+data.getName());
					try {
						comp.runEAGLE(data);
					} catch (InvalidConfigurationException e) {
						System.out.println("Error running EAGLE on "+data.getName()+" "+ e.getMessage());
						System.err.println("Error running EAGLE on "+data.getName()+" "+ e.getMessage());
						e.printStackTrace();
					}
				}  
				if(cmd.hasOption("euclid")) {
					System.out.println("\nRunning EUCLID on "+data.getName());
					comp.runEUCLID(data);
				}
			}// doesn't has option all				
		}
	}
	
	
	
	public void setOutStreams(String name) {
		try {
			File stdFile = new File(name+"_stdOut.txt");
			PrintStream stdOut;
			stdOut = new PrintStream(new FileOutputStream(stdFile, false));
			File errFile = new File(name+"_errOut.txt");
			PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
			System.setErr(errOut);
			System.setOut(stdOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("all", false, "If set compare LION to both EAGLE and EUCLID");
		options.addOption("eagle", false, "If set run EAGLE");
		options.addOption("euclid", false, "If set run EUCLID");
		options.addOption("lion", false, "If set run LION");
		options.addOption("dur", true, "Set runtime in seconds.");
		return options;
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
	
}
