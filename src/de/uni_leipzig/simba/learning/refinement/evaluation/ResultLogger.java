package de.uni_leipzig.simba.learning.refinement.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;


import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;
import de.uni_leipzig.simba.specification.LinkSpec;
/**
 * Just a quick hack to store some data.
 * @author Klaus Lyko
 *
 */
public class ResultLogger {
	String folder = "resources/results/";
	String fileName = "refine.csv";
	String logName = "refine2Log.txt";
	String treeName = "refine2Tree.txt";
	
	String dataset = "";
	String SEP = ";";
	File file, logFile, treeFile;
	private FileWriter writer;
	DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
	
	
	public ResultLogger(EvaluationData data) {
		df.applyPattern( "#,###,######0.00000" );
		dataset = data.getName()+"_";
	}
	
	/**
	 * Creates log file / overrides it
	 * @return
	 */
	public boolean createFile() {
		file = new File(folder+dataset+fileName);
		logFile = new File(folder+dataset+logName);
		this.treeFile = new File(folder+dataset+treeName);
		try {
			writer = new FileWriter(file, false);
			String out = "RUN"+SEP+"Score"+SEP+"Expansion"+SEP+"PenaltyScore"+SEP+"RefineMents"+SEP+"Best"+SEP+"Refines";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
			writer.close();
			
			writer = new FileWriter(logFile, false);
			out = "Log for dataset "+dataset;
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
			writer.close();
			
			writer = new FileWriter(treeFile, false);
			out = "SearchTree for Dataset "+dataset;
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void writeString(String str) {
		for(File f : new File[]{file, logFile, treeFile})
			try {
				writer = new FileWriter(f, true);
				writer.write(str);
				writer.write(System.getProperty("line.separator"));
				writer.flush();
				writer.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * Log a single entry.
	 * @param run nr of run
	 * @param refines SearchNode wich is refined
	 * @param refinements Spec it's refined into
	 * @param score current bestScore
	 */
	public void logEntry(int run, SearchTreeNode refines, Set<LinkSpec> refinements, double score) {
		String str = "run"+""+refines.outputSearchTree();
//		String str=""+run+SEP+df.format(refines.getScore())+SEP+
//				refines.getExpansion()+SEP+
//				df.format(refines.getPenaltyScore())+SEP+
//				refinements.size()+SEP+
//				df.format(score)+SEP+refines;
		try {
			writer = new FileWriter(logFile, true);
			writer.write(str);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void writeRealTitle() {
		String str = "Loop"+SEP+"PFM"+SEP+"STN Score"+SEP+"STN PenScore"+SEP+"Duration in s"+SEP+
				"FScore 1:1"+SEP+"Prec 1:1"+SEP+"Recall 1:1"+SEP+
				"full FScore"+SEP+" full Prec"+SEP+"full Recall"+SEP+"Node";
		writeString(str);
	}
	
	public void logEntry(EvaluationMemory mem) {
		String str=""+mem.loop+SEP+
				df.format(mem.pfm)+SEP+
				df.format(mem.node.getScore())+SEP+
				df.format(mem.node.getPenaltyScore())+SEP+
				mem.duration/1000+SEP+
				df.format(mem.realf)+SEP+
				df.format(mem.realPrec)+SEP+
				df.format(mem.realRecall)+SEP+
				df.format(mem.realfFull)+SEP+
				df.format(mem.realPrecFull)+SEP+
				df.format(mem.realRecallFull)+SEP+
				mem.node.toString().replaceAll("\n", "").replaceAll(";", "");
		try {
			writer = new FileWriter(file, true);
			writer.write(str);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logEntries(List<EvaluationMemory> mems) throws IOException {
		writer = new FileWriter(file, true);
		for(EvaluationMemory mem : mems) {
			String str=""+mem.loop+SEP+
					df.format(mem.pfm)+SEP+
					df.format(mem.node.getScore())+SEP+
					df.format(mem.node.getPenaltyScore())+SEP+
					mem.duration/1000+SEP+
					df.format(mem.realf)+SEP+
					df.format(mem.realPrec)+SEP+
					df.format(mem.realRecall)+SEP+
					df.format(mem.realfFull)+SEP+
					df.format(mem.realPrecFull)+SEP+
					df.format(mem.realRecallFull)+SEP+
					mem.node.toString().replaceAll("\n", "").replaceAll(";", "");
			writer.write(str);
			writer.write(System.getProperty("line.separator"));
		
		}
		writer.flush();
		writer.close();
	}

	public void writeLogString(String str) {
		try {
			writer = new FileWriter(logFile, true);
			writer.write(str);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void writeTreeString(String treeString) {
		try {
			writer = new FileWriter(treeFile, true);
			writer.write(treeString);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
