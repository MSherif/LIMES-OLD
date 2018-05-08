package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;

public class EvaluationPseudoCombinedMemory {
	public double beta;
	public Statistics pseudoFMeasure;
	public Statistics FScore;
	public Statistics precision;
	public Statistics recall;
	public Statistics FScore_1to1;
	public Statistics precision_1to1;
	public Statistics recall_1to1;
	public Statistics duration;
	public int count;
	public List<EvaluationPseudoMemory> instances;
	
	/**
	 * Used to compute means: pfm, f,p and r over both the full maps and their best 1-to1 submap for evaluating EAGLE unsupervised.
	 * @param beta
	 */
	public EvaluationPseudoCombinedMemory(double beta) {
		this.beta = beta;
		pseudoFMeasure = new Statistics();
		FScore = new Statistics();
		precision = new Statistics();
		recall = new Statistics();
		FScore_1to1 = new Statistics();
		precision_1to1 = new Statistics();
		recall_1to1 = new Statistics();
		duration = new Statistics();
		count = 0;
		instances = new LinkedList<EvaluationPseudoMemory>();
	}
	
	public void add(EvaluationPseudoMemory mem) throws Exception {
		if(this.beta != mem.betaValue) {
			throw new Exception("Trying to combine memory for beta = "+mem.betaValue+ " with results for beta ="+beta);
		}
		else {
			if(mem.isValid()) {
				pseudoFMeasure.add(mem.pseudoFMeasure);
				FScore.add(mem.fmeasue);
				precision.add(mem.precision);
				recall.add(mem.recall);			
				FScore_1to1.add(mem.fmeasue_1to1);
				precision_1to1.add(mem.precision_1to1);
				recall_1to1.add(mem.recall_1to1);		
				Long durVal = (mem.runTime<0)?new Long(-1*mem.runTime):new Long(mem.runTime);
				duration.add(durVal.doubleValue());
				count++;
				instances.add(mem);
			} else {
				System.err.println("Individual had wrong values: \n"+mem+"\nThereby it's been dismissed.");
				System.out.println("Individual had wrong values: \n"+mem+"\nThereby it's been dismissed.");
			}
			
		}		
	}
	
	/**
	 * Returns the according row for a log file: <i>beta | pseudo | f-score | precision | recall | duration</i>.
	 * All measures are there mean value and standard derivation.| is the SEP.
	 * @param SEP to separate columns.
	 * @return A single line: <i>beta | pseudo | f-score | precision | recall | duration</i>.
	 */
	public String toString(String SEP) {
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		String out = "" + beta+SEP+df.format(pseudoFMeasure.mean)+SEP+df.format(pseudoFMeasure.standardDeviation)+SEP+
				df.format(FScore.mean)+SEP+df.format(FScore.standardDeviation)+SEP+
				df.format(precision.mean)+SEP+df.format(precision.standardDeviation)+SEP+
				df.format(recall.mean)+SEP+df.format(recall.standardDeviation)+SEP+
				df.format(FScore_1to1.mean)+SEP+df.format(FScore_1to1.standardDeviation)+SEP+
				df.format(precision_1to1.mean)+SEP+df.format(precision_1to1.standardDeviation)+SEP+
				df.format(recall_1to1.mean)+SEP+df.format(recall_1to1.standardDeviation)+SEP+
				df.format(duration.mean)+SEP+df.format(duration.standardDeviation)+SEP+count;
		if(count == 1) {
			out+=SEP+instances.get(0).toString();
		} else {
			out+=SEP+"";
		}
		return out;
	}
	
	public static String getColumnHeader(String SEP) {
		return "beta"+SEP+"pfm"+SEP+"std(pfm)"+SEP+
				"f"+SEP+"std(f)"+SEP+
				"pp"+SEP+"std(pp)"+SEP+
				"pr"+SEP+"std(pr)"+SEP+
				"f_1to1"+SEP+"std(f_1to1)"+SEP+
				"pp_1to1"+SEP+"std(pp_1to1)"+SEP+
				"pr_1to1"+SEP+"std(pr_1to1)"+SEP+
				"d ms"+SEP+"std(d)"+SEP+"count"+SEP+"single indv.";
	}
	
//	public static void main(String args[]) {
//		File file = new File("resources/results/bsp.txt");
//		
//		// write to stream
//		String out = getColumnHeader("\t");
//		try {
//			FileWriter	writer = new FileWriter(file, false);
//			writer.write(out);
//			writer.write(System.getProperty("line.separator"));		
//			// write to file
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		System.out.println();
//	}
}
