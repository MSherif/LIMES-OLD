package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;
import de.uni_leipzig.simba.util.Clock;
/**
 * Used log EvaluationPseudoMemories to files. Used for EAGLE and its derivates evaluation.
 * Supports logging of all atomic results (e.g. per run and per generation) and means of them over all runs.
 * @author Klaus Lyko
 *
 */
public class PseudoEvalFileLogger {
	public String fileName;
	public String folder;
	public static final String SEP = ";";
	File f_all, f_mean;
	public String nameExtansion = "";
	
	public PseudoEvalFileLogger(String folder, String file) {
		this.fileName=file;
		this.folder = folder;
	}
	
	/**
	 * now returns the Statistics for the full Score over all runs
	 * @param list
	 * @param numberOfRuns
	 * @param numberOfGenerations
	 * @param params
	 * @return
	 */
	public Statistics log(List<EvaluationPseudoMemory> list, int numberOfRuns, int numberOfGenerations, EvaluationData params){
//		if(!createFiles((String)params.get(MapKey.EVALUATION_RESULTS_FOLDER), (String)params.get(MapKey.EVALUATION_FILENAME)))
		if(!createFiles(params.getEvauationResultFolder(), params.getEvaluationResultFileName()))
			return new Statistics();
		else {
			Statistics fScore = new Statistics();
			Statistics fScore_1to1 = new Statistics();
			Statistics pfm = new Statistics();
			Statistics dur = new Statistics();
			int gen=1;
			int runs = 0;
			for(int iter = 0; iter<list.size(); iter++) {
				EvaluationPseudoMemory mem = list.get(iter);
				Long durVal = new Long(mem.runTime);
				if(mem.generation==gen) {
					fScore.add(mem.fmeasue);
					fScore_1to1.add(mem.fmeasue_1to1);
					pfm.add(mem.pseudoFMeasure);
					dur.add(durVal.doubleValue());
					runs++;
				} else {						
					// log old values
					if(fScore.count>0) {
						writeMeanEntry(gen, fScore, fScore_1to1, pfm, dur, runs);
//						writeMeanEntry(gen, pfm, dur, runs);
					}
					// reset
						fScore = new Statistics();
						fScore.add(mem.fmeasue);
						fScore_1to1 = new Statistics();
						fScore_1to1.add(mem.fmeasue_1to1);
						pfm = new Statistics();
						pfm.add(mem.pseudoFMeasure);
						dur = new Statistics();
						dur.add(durVal.doubleValue());
						runs=1;
					}
					gen = mem.generation;
				}
				writeMeanEntry(gen, fScore, fScore_1to1, pfm, dur, runs);
//				writeMeanEntry(gen, pfm, dur, runs);
			logAll(list, params);
			// log the means to
			HashMap<String, Object> loggingParams = new HashMap<String, Object>();
//			for(MapKey key:DataSetChooser.getLoggingKeys())
//				loggingParams.put(key.toString(), params.get(key));
			loggingParams.put("name", params.getName());
			loggingParams.put("runs", numberOfRuns);
			loggingParams.put("gens", numberOfGenerations);
			writeParams(loggingParams);
			return fScore;
		}
	}
	
	/**
	 * Log all individuals.
	 */
	public void logAll(List<EvaluationPseudoMemory> list, EvaluationData params) {
		try {
				FileWriter writer = new FileWriter(f_all, true);
				for(EvaluationPseudoMemory mem : list) {
					writer.write(mem.toString(SEP));
					writer.write(System.getProperty("line.separator"));
					writer.flush();
				}
				writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private boolean createFiles(String folder, String name) {
		f_all = new File(folder+this.nameExtansion+name);
		f_mean = new File(folder+this.nameExtansion+"MEAN_"+name);
		return writeCaptions();
	}


	private void writeMeanEntry(int gen, Statistics f, Statistics f_1to1, Statistics pfm, Statistics dur, int runs) {
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		try {
			FileWriter writer = new FileWriter(f_mean, true);
			String out = ""+gen+
					SEP+df.format(f.mean)+SEP+df.format(f.standardDeviation)+
					SEP+df.format(f_1to1.mean)+SEP+df.format(f_1to1.standardDeviation)+
					SEP+df.format(pfm.mean)+SEP+df.format(pfm.standardDeviation)+
					SEP+dur.mean/1000+SEP+runs;
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch (IOException e) {
		}
	}
	
	private boolean writeCaptions() {
		String head = "Cycle"+SEP+"Generation"+SEP+
				"Fitness"+SEP+"PFM"+SEP+
				"F-Score"+SEP+"Recall"+SEP+"Precision"+SEP+
				"F-Score_1to1"+SEP+"Recall_1to1"+SEP+"Precision_1to1"+SEP+
				"Duration"+SEP+"reference data"+SEP+"Metric";
		String head_mean = "Generation"+SEP+"f()"+SEP+"std(f)"+SEP+
				"f_1to1()"+SEP+"std(f_1to1)"+SEP+
				"pfm()"+SEP+"std(pfm)"+SEP+"d()"+SEP+"cylces";
		try {
			FileWriter writer = new FileWriter(f_all, false);
			writer.write(head);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
			
			writer = new FileWriter(f_mean, false);
			writer.write(head_mean);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	private boolean writeParams(HashMap<String, Object> param) {
		try {
			FileWriter writer = new FileWriter(f_all, true);
			writer.write(System.getProperty("line.separator"));
			for(Entry<String, Object> e : param.entrySet()) {
				writer.write(e.getKey()+SEP+e.getValue().toString()+System.getProperty("line.separator"));
			}
			writer.flush();
			writer.close();
			
			writer = new FileWriter(f_mean, true);
			for(Entry<String, Object> e : param.entrySet()) {
				writer.write(e.getKey()+SEP+e.getValue().toString()+System.getProperty("line.separator"));
			}
			writer.flush();
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void main(String args[]) {
//		PseudoEvalFileLogger logger =  new PseudoEvalFileLogger("Examples", "durations test");
		Clock c1 = new Clock();
		long l1 = 100l;
		long l2 = 1444552l;
		Long durVal1 = new Long(l1);
		Long durVal2 = new Long(l2);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double d1 = durVal1.doubleValue();
		double d2 = durVal2.doubleValue();
		
		System.out.println(durVal1 = c1.totalDuration()*-1);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(durVal2 = c1.totalDuration()*-1);
		d1 = durVal1.doubleValue();
		d2 = durVal2.doubleValue();
		System.out.println(durVal1);
		System.out.println(durVal2);
		System.out.println(d1);
		System.out.println(d2);
		System.out.println(new Long(durVal1).doubleValue());
		System.out.println(new Long(durVal2).doubleValue());
		
		
	}
}
