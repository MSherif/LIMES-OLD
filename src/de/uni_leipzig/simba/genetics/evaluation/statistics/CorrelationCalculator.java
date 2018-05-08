package de.uni_leipzig.simba.genetics.evaluation.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import de.uni_leipzig.simba.genetics.evaluation.basics.CommandLineConfigurator;


public class CorrelationCalculator {

	public static final String folder = "C:/Users/Lyko/dependency projects/scms-drupal7/jar/resources/results/betaEval/";
	public static String SEP = ";";
	public int sourceRow = 1;
	public int targetRow = 3;
	public File file;
	
	
	public CorrelationCalculator(String pathToFile) {
		this(new File(pathToFile));
	}
	
	public CorrelationCalculator(File f) {
		this.file = f;
	}
	
	public CorrelationCalculator(String folder, String fileName) {
		this(folder+fileName);
	}
	
	public Double computeSpearman() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		line = br.readLine();
		String x,y;
		String[] sep = line.split(SEP);
		x=sep[sourceRow]; y=sep[targetRow];
		System.out.println("Computing Spearman correlation of row "+x+" and row "+y);
		List<Double> xs = new ArrayList<Double>();
		List<Double> ys = new ArrayList<Double>();
		while((line = br.readLine()) != null) {
			sep = line.split(SEP);
			xs.add(Double.parseDouble(sep[sourceRow]));
			ys.add(Double.parseDouble(sep[targetRow]));
		}
		br.close();
		// create double array
		double[] xValues,yValues;
		xValues=new double[xs.size()];
		yValues=new double[ys.size()];
		for(int i=0; i<xs.size(); i++) {
			xValues[i] = xs.get(i);
		}
		for(int i=0; i<ys.size(); i++) {
			yValues[i] = ys.get(i);
		}
		
		SpearmansCorrelation corr = new SpearmansCorrelation();
		return corr.correlation((double[])xValues, (double[])yValues);
	}
	
	
	

	
	public static void main(String args[]) throws Exception {
		// Commandliner
		CommandLineConfigurator cfg = new CommandLineConfigurator(args);
		cfg.addOption("folder", "Set if you want to compute Pearson for all files in the folder.", false, false);
		cfg.addOption("name", "Either a path to a specific file or a path to a folder", true, false);
		if(!cfg.hasOption("name"))
			cfg.printUsage();
	
		if(cfg.hasOption("folder")) {
			// we expect folder to specifiy a folder
			File folder = new File(cfg.getString("name"));
			if(folder.isDirectory()) {
				
				for(File f : folder.listFiles()) {
					try{
						if(!f.isDirectory())
						 {
							CorrelationCalculator calc = new CorrelationCalculator(f);
							System.out.println(f.getName()+" - SpearmanCorrelation = "+calc.computeSpearman());
						 }
						
					} catch(Exception e) {
//						e.printStackTrace();
					}
					
				}
			}				
		} else {
			CorrelationCalculator calc;
			calc = new CorrelationCalculator(CorrelationCalculator.folder+cfg.getString("name"));
			System.out.println("Spearman="+calc.computeSpearman());
		}
		
		
		
		
	}
}
