package de.uni_leipzig.simba.genetics.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @deprecated
 * @author Lyko
 *
 */
public class EvalLogger {
	private static String folder = "testResults";
	private FileWriter writer;
	private File file;
	private String fileName;
	private static EvalLogger instance = null;
	
	private EvalLogger(String path) {
		setFileName(path);
		file = new File(folder+"/"+path);
	}
	
	/**
	 * Implements Singleton pattern.
	 * @return The only instance of this class.
	 */
	public static EvalLogger getInstance() {
		if(instance == null)
			instance = new EvalLogger("GeneticLog.txt");
		return instance;
	}

	public void createFile(String name) {
		file = new File(folder+"/"+name);
		try {
			writer = new FileWriter(file);
			// write to stream
			String out = "Gen\tFitness\tfScore\trecall\tprecision\tExpression";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void write(String solution, double fitness, double fScore, int gen) {
		try {
			writer = new FileWriter(file, true);
			// write to stream
			String out = gen+"\"\t"+fitness+"\t"+fScore+"\t\""+solution;
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			writer.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void writeComplete(int nr, String solution, double fitness, double fScore, double recall, double prec) {
		try {
			writer = new FileWriter(file, true);
			// write to stream
			String out = nr+"\"\t"+fitness+"\t"+fScore+"\t"+recall+"\t"+prec+"\t\""+solution;
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			writer.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void writeString(String str) {
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
