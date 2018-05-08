package de.uni_leipzig.simba.genetics.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
/**
 * @deprecated
 * @author Lyko
 *
 */
public class EvalFileLogger {
	private FileWriter writer;
	private File file;
	private String fileName;
	private static EvalFileLogger instance = null;
	public static final String SEP = ";";
	private EvalFileLogger(String path) {
		setFileName(path);
		file = new File(path);
	}
	
	/**
	 * Implements Singleton pattern.
	 * @return The only instance of this class.
	 */
	public static EvalFileLogger getInstance() {
		if(instance == null)
			instance = new EvalFileLogger("GeneticLog.txt");
		return instance;
	}

	/**
	 * Creates the file with folder + name
	 * @param should end with "/"
	 * @param name
	 * @param override true if we want to override
	 */
	public void createFile(String folder, String name, boolean override) {
		if(!folder.substring(folder.length()-2).equalsIgnoreCase("/"))
			folder += "/";
		file = new File(folder+name);
		try {
			writer = new FileWriter(file, !override);
			// write to stream
			String out = "Gen"+SEP+"averageFitness"+SEP+"Fitness"+SEP+"fScore"+SEP+"recall"+SEP+"precision"+SEP+"Expression";
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
			String out = gen+""+SEP+""+fitness+""+SEP+""+fScore+""+SEP+"\""+solution+"\"";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			writer.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeComplete(int nr, double avgFit, String solution, double fitness, double fScore, double recall, double prec) {

		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		try {
			writer = new FileWriter(file, true);
		// write to stream
			String out = nr+""+SEP+""+df.format(avgFit)+""+SEP+""+df.format(fitness)+""+SEP+""+df.format(fScore)+""+SEP+""+df.format(recall)+""+SEP+""+df.format(prec)+""+SEP+"\""+solution+"\"";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			writer.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeWithDuration(int nr, double avgFit, String solution, double fitness, double fScore, double recall, double prec, long dur) {
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		try {
			writer = new FileWriter(file, true);
			// write to stream
			String out = nr+""+SEP+""+df.format(avgFit)+""+SEP+""+df.format(fitness)+""+SEP+""+df.format(fScore)+""+SEP+""+df.format(recall)+""+SEP+""+df.format(prec)+""+SEP+""+dur+""+SEP+"\""+solution+"\"";
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
	public boolean fileExists(String name) {
		file = new File(name);
		return file.exists();
	}
	
	public void writeHead() {
		try {
			writer = new FileWriter(file, true);
			// write to stream
			String out = "Gen"+SEP+"averageFitness"+SEP+"Fitness"+SEP+"fScore"+SEP+"recall"+SEP+"precision"+SEP+"duration"+SEP+"Expression";
			writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
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
