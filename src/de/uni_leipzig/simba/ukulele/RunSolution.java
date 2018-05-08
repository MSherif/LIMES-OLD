package de.uni_leipzig.simba.ukulele;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Locale;

import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.IFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;


/**
 *  To keep track of EAGLE's solutions. Handles results of a specific dataset. 
 *  Computes F-Measures and AUC values. Logs to file.
 *
 * @author Klaus Lyko <klaus.lyko@informatik.uni-leipzig.de>
 *
 */
public class RunSolution {	
	/**Evaluation data*/
	public EvaluationData data;
	/**data entries*/
	LinkedList<EvaluationPseudoMemory> perRunAndDataSet = new LinkedList<EvaluationPseudoMemory>();
	/**used EAGLE fitness function*/
	IFitnessFunction fitness;
	
	/*to compute means over all runs*/
	Statistics fitness_Stat = new Statistics();
	Statistics F_full = new Statistics();
	Statistics F_1to1 = new Statistics();
	Statistics PFM = new Statistics();
	
	Statistics AUC_pfm = new Statistics();
	Statistics AUC_full = new Statistics();
	Statistics AUC_1to1 = new Statistics();
	
	/**log file results*/
	File logFile;
	FileWriter writer;
	public static final String SEP = ";";
	/**
	 *
	 * @param data
	 * @param fitness
	 */
	public RunSolution(EvaluationData data, IFitnessFunction fitness,
			File logFile) {
		
		perRunAndDataSet = new LinkedList<EvaluationPseudoMemory>();
		this.fitness = fitness;
		this.data = data;
		try {
			writer = new FileWriter(logFile, true);
			String out = ""+data.getName();
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Process current run, e.g. entries in the perRunAndDataSet list. Computes F-Measures, and AUC
	 * @throws IOException 
	 */
	public void processRun(int nr, boolean logAtomics) throws IOException {
		String out ="";
		
		if(logAtomics) {
			out = ""+data.getName()+" run "+nr;
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			out = "gen"+SEP+"fitness"+SEP+"pfm"+SEP+"f_full"+SEP+"f_1to1"+SEP+"dur"+SEP+"AUC_full"+SEP+"AUC_1to1"+SEP+"AUC_pfm"+SEP+"metric"+SEP+"prec"+SEP+"recall";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
		}
		double xBefore = 0;
		
		double aucFull = 0; double yFullBefore = 0;
		
		double auc1to1 = 0; double y1to1Before = 0;
		
		double aucpfm = 0; double ypfmBefore = 0;
		
		Mapping reference = data.getReferenceMapping();
		for(int i = 0; i<perRunAndDataSet.size(); i++) {
			EvaluationPseudoMemory mem = perRunAndDataSet.get(i);
			Mapping map = fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold(), true);
			// For real F-measures use best 1-to-1 mapping
			Mapping map_1to1  = Mapping.getBestOneToOneMappings(map);
			double prec, recall, fMeasure, prec_1to1, recall_1to1, fMeasure_1to1;
			PRFCalculator prf = new PRFCalculator();
			prec = prf.precision(map, reference);
			recall = prf.recall(map, reference);
			fMeasure = prf.fScore(map, reference);
			
			if(Double.isNaN(fMeasure) || Double.isInfinite(fMeasure)) {
				System.err.println("NaN computation on Fmeasure, setting it to  0");
				fMeasure = 0;
			}
			
			mem.precision=prec;
			mem.recall=recall;
			mem.fmeasue=fMeasure;
			
			prec_1to1 = prf.precision(map_1to1, reference);
			recall_1to1 = prf.recall(map_1to1, reference);
			fMeasure_1to1 = prf.fScore(map_1to1, reference);
			
			if(Double.isNaN(fMeasure_1to1) || Double.isInfinite(fMeasure_1to1)) {
				System.err.println("NaN computation on Fmeasure 1-to-1, setting it to  0");
				fMeasure_1to1 = 0;
			}
			
			mem.precision_1to1=prec_1to1;
			mem.recall_1to1=recall_1to1;
			mem.fmeasue_1to1=fMeasure_1to1;
			// compute auc values
			double xNow=mem.generation;
			aucFull += computeAUCSummand(xBefore, xNow, yFullBefore, fMeasure);
			auc1to1 += computeAUCSummand(xBefore, xNow, y1to1Before, fMeasure_1to1);
			aucpfm += computeAUCSummand(xBefore, xNow, ypfmBefore, mem.pseudoFMeasure);
			//log
			if(logAtomics) {
				logAtomic(mem, aucFull, auc1to1, aucpfm);
			}
			xBefore = xNow;
			yFullBefore = fMeasure;
			y1to1Before = fMeasure_1to1;	
			ypfmBefore = mem.pseudoFMeasure;
		}
		// log to statistics final fs,auc
		F_full.add(yFullBefore);
		F_1to1.add(y1to1Before);
		PFM.add(ypfmBefore);
		AUC_full.add(aucFull);
		AUC_1to1.add(auc1to1);
		AUC_pfm.add(aucpfm);
		
		out = data.getName()+" run:"+nr+"\n"+"gens"+SEP+"fit"+SEP+"pfm"+SEP+"f"+SEP+"f_1to1"+SEP+"dur"+SEP+"AUC"+SEP+"AUC_1to1"+SEP+"AUC_pfm";
		writer.write(out);
		writer.write(System.getProperty("line.separator"));
		logAtomic(perRunAndDataSet.getLast(),aucFull,auc1to1,aucpfm);
	}
	
	
	private void logAtomic(EvaluationPseudoMemory mem, double aucFull, double auc1to1, double auc_pfm) throws IOException {
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		String out = ""+mem.generation+SEP+
//				mem.fitness+SEP+
//				mem.pseudoFMeasure+SEP+
				df.format(mem.fitness)+SEP+
				df.format(mem.pseudoFMeasure)+SEP+
				df.format(mem.fmeasue)+SEP+
				df.format(mem.fmeasue_1to1)+SEP+
				df.format(mem.runTime)+SEP+
				df.format(aucFull)+SEP+
				df.format(auc1to1)+SEP+
				df.format(auc_pfm)+SEP+
				mem.metric+SEP+
				df.format(mem.precision)+SEP+
				df.format(mem.recall);
			
		writer.write(out);
		
		writer.write(System.getProperty("line.separator"));
		// write to file
		writer.flush();
//		System.out.println("PFM::"+mem.pseudoFMeasure+" ... "+df.format(mem.pseudoFMeasure));
		
	}
	
	/**
	 * Computes an area under the curve (AUC) entry;
	 * @param xBefore x-axis value before
	 * @param xNow	x-axis value now
	 * @param yBefore y-axis value before
	 * @param yNow y-axis value now;
	 * @return
	 */
	public static double computeAUCSummand(double xBefore, double xNow, double yBefore, double yNow) {
		double result = 0;
		double deltaX = xNow-xBefore;
		double deltaY = Math.abs(yBefore-yNow);
		double minY = Math.min(yBefore, yNow);
		double quadreVal = deltaX*minY;
		double triangleVal = deltaX*deltaY*0.5;
		result = quadreVal+triangleVal;
		if(Double.isNaN(result) || Double.isInfinite(result)) {
			System.err.println("NaN computation on AUC for (x0,x1,y0,y1):"+xBefore+", "+xNow+", "+yBefore+", "+yNow);
			result = 0;
		}
		return result;
	}
	
	
//	public static void main(String args[]) {
//		// (x,y)
//		// (0,0), (1,1) = 0,5
//		// (1,1), (2,2) = 1,5
//		// (2.2), (3,1) = 1,5
//		System.out.println("AUC((0,0),(1,1))= "+computeAUCSummand(0,1,0,1));
//		System.out.println("AUC((1,1),(2,2))= "+computeAUCSummand(1,2,1,2));
//		System.out.println("AUC((2,2),(3,1))= "+computeAUCSummand(2,3,2,1));
//	}
}
