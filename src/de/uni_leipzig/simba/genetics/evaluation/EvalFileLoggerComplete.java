package de.uni_leipzig.simba.genetics.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.apache.log4j.Logger;
/**
 * @deprecated atleast i think so
 * @author Lyko
 *
 */
public class EvalFileLoggerComplete {
	static Logger logger = Logger.getLogger("Limes");
	private static String folder = "testResults";
	private FileWriter writer;
	private File file;
	private String fileName;
	private static EvalFileLoggerComplete instance = null;
	public static final String SEP = ";";
	private EvalFileLoggerComplete(String path) {
		setFileName(path);
		file = new File(folder+"/"+path);
	}
	
	/**
	 * Implements Singleton pattern.
	 * @return The only instance of this class.
	 */
	public static EvalFileLoggerComplete getInstance() {
		if(instance == null)
			instance = new EvalFileLoggerComplete("GeneticLog.txt");
		return instance;
	}

	public void createFile(String name) {
		String subFolder = name.substring(0, name.lastIndexOf("/"));
		// name = Examples/GeneticEval/testResults
		logger.info("name="+name);
		logger.info("subFolder="+subFolder);
		String fileName=subFolder+"/"+folder+name.substring(name.lastIndexOf("/"), name.indexOf("_"))+name.substring(name.lastIndexOf("/"));
		logger.info("FilePath="+fileName);
		file = new File(fileName);
		try {
		//	writer = new FileWriter(file, true);
			// write to stream
			if(!fileExists(fileName)) {
					writer = new FileWriter(file, true);
				String out = "Cycle"+SEP+"Gen"+SEP+"averageFitness"+SEP+"Fitness"+SEP+"fScore"+SEP+"recall"+SEP+"precision"+SEP+"oracleSize"+SEP+"duration"+SEP+"Expression";
				writer.write(out);
				writer.write(System.getProperty("line.separator"));
				// write to file
				writer.flush();				
			} else {
				writer = new FileWriter(file, true);
			}
			writer.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void writeComplete(int cycle, long duration, int nr, double avgFit, String solution, double fitness, double fScore, double recall, double prec, int oracleSize) {

		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,######0.00000" );
		try {
			writer = new FileWriter(file, true);
			// write to stream
			String out = cycle+""+SEP+""+nr+""+SEP+""+df.format(avgFit)+""+SEP+""+df.format(fitness)+""+SEP+""+df.format(fScore)+""+SEP+""+df.format(recall)+""+SEP+""+df.format(prec)+""+SEP+""+oracleSize+""+SEP+""+duration+""+SEP+"\""+solution+"\"";
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
