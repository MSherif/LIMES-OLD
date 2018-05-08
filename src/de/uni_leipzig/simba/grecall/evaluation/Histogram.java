package de.uni_leipzig.simba.grecall.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.grecall.oracle.SimpleOracle;
import de.uni_leipzig.simba.grecall.util.DatasetConfiguration;
import de.uni_leipzig.simba.grecall.util.StatisticsBase;
import de.uni_leipzig.simba.specification.LinkSpec;

public class Histogram {
	private Integer[] maxOptTime = {100,200,400,800,1600}; //maximum optimization time
	private Double[] recall = {0.1,0.2,0.5}; //recall percentage
	private int iterations = 3;
	private Object baseDirectory;
	private Cache source;
	private Cache target;
	protected static final String TAB_DELIMITER = "\t";
	protected static final String NEW_LINE_SEPARATOR = "\n";
	private String[] FileNames = {
			"ExecutionTime.csv",
			"OverallTime.csv",
			"OptimizationTime.csv",
			"EstimatedExecutionTime.csv",
			"MappingSize.csv",
			"EstimatedSelectivity.csv"
			};	
	
	static Logger logger = Logger.getLogger("LIMES");
	public Histogram(String folderName, Cache source, Cache target) {
		this.baseDirectory = folderName;
		this.source = source;
        this.target = target;
	}

	
	
	public void getMin(){
		
		for(double rec: recall){
			for(int maxTime:maxOptTime){
				for(String file:FileNames){
					//specification number, <set of float numbers>
					LinkedHashMap<Integer,LinkedList<Float>> statistics = new LinkedHashMap<Integer,LinkedList<Float>>();
					String header = null;
					
					for(int iteration = 0 ; iteration<iterations ; iteration++){	
						BufferedReader br = null;
						try {
			    			String csvFile = this.baseDirectory+"/results/"+iteration+"/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+file;
			    			br = new BufferedReader(new FileReader(csvFile));
			    			int specCounter = 0;
			    			String line = "";
			    			header = br.readLine();
			    			if(header.contains("Original LS"))
			    				header = header.replace("Original LS", "Baseline");
			    			if(header.contains("Simple"))
			    				header = header.replace("Simple", "DFS");
			    			if(header.contains("DownWardOp - monotony assumption"))
			    				header = header.replace("DownWardOp - monotony assumption", "RO-MA");
			    			if(header.contains("DownWardOp"))
			    				header = header.replace("DownWardOp", "C-RO");
			    			while ((line = br.readLine()) != null) {
			    				
			    				String[] fullEntry = line.split(TAB_DELIMITER);
			    				LinkedList<Float> temp = new LinkedList<Float>();
			    				for(int pos = 0 ; pos < fullEntry.length ; pos++){
			    					temp.add(Float.parseFloat(fullEntry[pos]));
			    					
			    				}
			    				
			    				if(statistics.get(specCounter)==null)
			    					statistics.put(specCounter, temp);
			    				else{//get old statistics
			    					LinkedList<Float> oldStats = statistics.get(specCounter);
			    					for(int pos = 0 ; pos < oldStats.size() ; pos++){
			    						if(oldStats.get(pos) > temp.get(pos)){
			    							oldStats.set(pos, temp.get(pos));
			    						}
			    					}
			    					statistics.put(specCounter, oldStats);
			    					//for(float st:statistics.get(specCounter)){
			    					//	logger.info(st);
			    					//}
			    				}
			    				
			    				
			    				specCounter++;
			    			}
			
			    		} catch (FileNotFoundException e) {
			    			e.printStackTrace();
			    		} catch (IOException e) {
			    			e.printStackTrace();
			    		} finally {
			    			if (br != null) {
			    				try {
			    					br.close();
			    				} catch (IOException e) {
			    					e.printStackTrace();
			    				}
			    			}
			    		}
						
						
					}
					String  OptimizerTypeFolder = this.baseDirectory+"/results/min";
					File dirName = new File(OptimizerTypeFolder);
					if(!dirName.isDirectory()){
			    	 	try{
			    	 		dirName.mkdir();
			    	 		} catch(SecurityException se){
			    	 	} 
					}
					
					String  OptimizerTypeFolder2 = OptimizerTypeFolder+"/"+maxTime;
					File dirName2 = new File(OptimizerTypeFolder2);
					if(!dirName2.isDirectory()){
			    	 	try{
			    	 		dirName2.mkdir();
			    	 		} catch(SecurityException se){
			    	 	} 
					}
					String  OptimizerTypeFolder3 = OptimizerTypeFolder2+"/"+String.valueOf(rec*100)+"%";
					File dirName3 = new File(OptimizerTypeFolder3);
					if(!dirName3.isDirectory()){
			    	 	try{
			    	 		dirName3.mkdir();
			    	 		} catch(SecurityException se){
			    	 	} 
					}
					//file = file.replace(".csv", ".tsv");
					String specFilename = OptimizerTypeFolder3+"/"+file;
		    		File file1 = new File(specFilename);
		    		FileWriter writer = null;
		    		DecimalFormat df = new DecimalFormat("#");
		    		df = new DecimalFormat("#");
			    	df.setMaximumFractionDigits(50);
					if(!file1.exists()){
		    			try {
							writer = new FileWriter(specFilename);
							writer.append(header);writer.append(NEW_LINE_SEPARATOR);
							for (Entry<Integer, LinkedList<Float>> entry : statistics.entrySet()) {
							    Integer key = entry.getKey();
							    LinkedList<Float> value = entry.getValue();
							    int k = 0;
							    for(float no:value){
							    	writer.append(df.format(no));
							    	if(k != value.size()-1)
							    		writer.append(TAB_DELIMITER);
							    	k++;
							    }
							    writer.append(NEW_LINE_SEPARATOR);
							}
				    		
				    		writer.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    		}
					
					
				}
			}
		}
	}
	@SuppressWarnings("resource")
	public void selectivity(){
		DecimalFormat df = new DecimalFormat("#");
		df = new DecimalFormat("#");
    	df.setMaximumFractionDigits(50);
		
		float datasetSize = (float)this.source.size() * (float)this.target.size();
		
		String fileName = "MappingSize.csv";
		for(double rec: recall){
			for(int maxTime:maxOptTime){
				String output = this.baseDirectory+"/results/"+"min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"Selectivity.csv";
				String csvFile = this.baseDirectory+"/results/"+"min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+fileName;
				BufferedReader br = null;
				File f = new File(output);
	    		BufferedWriter writer = null;
				String all = "";
				String header = null;
				try {
	    			
	    			br = new BufferedReader(new FileReader(csvFile));
	    			String line = "";
	    			header = br.readLine();
	    			if(header.contains("Original LS"))
	    				header = header.replace("Original LS", "Baseline");
	    			if(header.contains("Simple"))
	    				header = header.replace("Simple", "DFS");
	    			if(header.contains("DownWardOp - monotony assumption"))
	    				header = header.replace("DownWardOp - monotony assumption", "RO-MA");
	    			if(header.contains("DownWardOp"))
	    				header = header.replace("DownWardOp", "C-RO");
	    			while ((line = br.readLine()) != null) {
	    				String[] fullEntry = line.split(TAB_DELIMITER);
	    				
	    				for(int pos = 0 ; pos < fullEntry.length ; pos++){
	    					Float t = Float.parseFloat(fullEntry[pos])/datasetSize;
	    					
				    		if(f.exists()){
				    			writer = new BufferedWriter(new FileWriter(output, true));
				    			all += (df.format(t) + TAB_DELIMITER);
				    			//writer.append(df.format(t));writer.append(TAB_DELIMITER);
				    		}else{
				    			writer = new BufferedWriter(new FileWriter(output));
				    			//writer.append(header.toString());writer.append(NEW_LINE_SEPARATOR);
				    			all += (header.toString() + NEW_LINE_SEPARATOR);
				    			//writer.append(df.format(t));writer.append(TAB_DELIMITER);
				    			all += (df.format(t) + TAB_DELIMITER);
				    		}
				    		
	    				}
	    				//writer.append(NEW_LINE_SEPARATOR);
	    				all += NEW_LINE_SEPARATOR;
	    				writer.append(all);
			    		
	    			}
	    			
	    			writer.flush();
	    			writer.close();
	    		} catch (FileNotFoundException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (br != null) {
	    				try {
	    					br.close();
	    					
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
				
			}
		}
	}
	
	public void combineSelectivityFiles() {
		DecimalFormat df = new DecimalFormat("#");
		df = new DecimalFormat("#");
    	df.setMaximumFractionDigits(50);
    	for(double rec: recall){
			for(int maxTime:maxOptTime){
				String realSelectivity = this.baseDirectory+"/results"+"/min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"Selectivity.csv";
				String estimatedSelectivity = this.baseDirectory+"/results/"+"min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"EstimatedSelectivity.csv";
				String output = this.baseDirectory+"/results/"+"min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"SelectivityCombination.csv";
				
				LinkedList<LinkedList<Float>> reals = new LinkedList<LinkedList<Float>>();
				LinkedList<LinkedList<Float>> estimations = new LinkedList<LinkedList<Float>>();
				BufferedReader brEstimated = null;
				BufferedReader brReal = null;
				
				File f = new File(output);
	    		BufferedWriter writer = null;
				
				try {
	    			
					brEstimated = new BufferedReader(new FileReader(estimatedSelectivity));
	    			String line = "";
	    			brEstimated.readLine();
	    			while ((line = brEstimated.readLine()) != null) {
	    				String[] fullEntry = line.split(TAB_DELIMITER);
	    				LinkedList<Float> temp = new LinkedList<Float>();
	    				for(int pos = 0 ; pos < fullEntry.length ; pos++){
	    					Float t = Float.parseFloat(fullEntry[pos]);
	    					if(pos == 1)
	    						temp.add(0,t);
	    					else
	    						temp.add(t);
				    	}
	    				estimations.add(temp);
			    		
	    			}
	    			
	    		} catch (FileNotFoundException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (brEstimated != null) {
	    				try {
	    					brEstimated.close();
	    					
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
				///////////////////////////////////////////////////
				try {
	    			
					brReal = new BufferedReader(new FileReader(realSelectivity));
	    			String line = "";
	    			brReal.readLine();
	    			while ((line = brReal.readLine()) != null) {
	    				String[] fullEntry = line.split(TAB_DELIMITER);
	    				LinkedList<Float> temp = new LinkedList<Float>();
	    				for(int pos = 0 ; pos < fullEntry.length ; pos++){
	    					Float t = Float.parseFloat(fullEntry[pos]);
	    					temp.add(t);
				    	}
	    				reals.add(temp);
			    		
	    			}
	    			
	    		} catch (FileNotFoundException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (brReal != null) {
	    				try {
	    					brReal.close();
	    					
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
				String header = "Desired\t"
						+ "Estimated Sel Baseline\t"
						+ "Real Sel Baseline\t"
						+ "Estimated Sel DFS\t"
						+ "Real Sel DFS\t"
						+ "Estimated Sel C-RO\t"
						+ "Real Sel C-RO\t"
						+ "Estimated Sel RO-MA\t"
						+ "Real Sel RO-MA";
				String all = "";
				for(int k = 0 ; k < estimations.size() ; k++){
					LinkedList<Float> entry = estimations.get(k);
					all += df.format(entry.get(0)) + TAB_DELIMITER;
				    for(int i = 1 ; i < entry.size() ; i++){
				    	all += df.format(entry.get(i)) + TAB_DELIMITER;
				    	all += df.format(reals.get(k).get(i-1)) + TAB_DELIMITER;
				    }
				    all += NEW_LINE_SEPARATOR;
				}
				if(f.exists()){
	    			try {
						writer = new BufferedWriter(new FileWriter(output, true));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			
	    		}else{
	    			try {
						writer = new BufferedWriter(new FileWriter(output));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			all = header + NEW_LINE_SEPARATOR + all;
	    		}
				
				try {
					writer.append(all);
					writer.flush();
	    			writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
			
			
    	}
		
	}
	
	public void AverageRuntime(){
		DecimalFormat df = new DecimalFormat("#");
		df = new DecimalFormat("#");
    	df.setMaximumFractionDigits(3);
    	LinkedHashMap<Integer, Integer> lenghts = new LinkedHashMap<Integer, Integer>();
    	LinkedHashMap<Integer, Integer> lenghtsSize = new LinkedHashMap<Integer, Integer>();
    	String specificationFile = this.baseDirectory+"/specifications/specifications.txt";
    	String header = "Specification Size\t"
    			+ "Baseline\t"
				+ "DFS\t"
				+ "C-RO\t"
				+ "RO-MA";
		
		BufferedReader br = null;
		try {
			
			br = new BufferedReader(new FileReader(specificationFile));
			String line = "";
			int lineCounter = 1;
			while ((line = br.readLine()) != null) {
				LinkSpec spec = new LinkSpec();
				spec.readSpec(line.split(Pattern.quote(">="))[0], Double.parseDouble(line.split(Pattern.quote(">="))[1]));
				int size = spec.size();
				if(size > 7)
					size = 7;
				
	    		if(lenghts.containsKey(lineCounter) == false){
	    			lenghts.put(lineCounter,size);
	    		}
	    		if(lenghtsSize.containsKey(size) == false)
	    			lenghtsSize.put(size, 0);
	    		lenghtsSize.put(size, lenghtsSize.get(size)+1);
	    		lineCounter++;
	    		if(size == 11)
	    			logger.info(lineCounter+": "+spec.toString());
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for(double rec: recall){
			for(int maxTime:maxOptTime){
				
				String runtimeFile = this.baseDirectory+"/results"+"/min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"OverallTime.csv";
				TreeMap<Integer,LinkedList<Float>> average = new TreeMap<Integer,LinkedList<Float>>();

				BufferedReader brRuntime = null;
				try {
	    			
					brRuntime = new BufferedReader(new FileReader(runtimeFile));
	    			String line = "";
	    			
	    			int lineCounter = 1;
	    			brRuntime.readLine();
	    			while ((line = brRuntime.readLine()) != null) {
	    				String[] fullEntry = line.split(TAB_DELIMITER);
	    				
	    				int specSize = lenghts.get(lineCounter);
	    				
	    				if(average.containsKey(specSize) == false){
	    					average.put(specSize, new LinkedList<Float>());
	    					for(int pos = 0 ; pos < 4 ; pos++){
	    						average.get(specSize).add(pos, 0.0f);
	    					}
	    				}
	    				
	    				for(int pos = 0 ; pos < 4 ; pos++){
	    					Float t = Float.parseFloat(fullEntry[pos])/(float)lenghtsSize.get(specSize);
	    					Float t2 = average.get(specSize).get(pos);
	    					average.get(specSize).set(pos, t+t2);
				    	}
	    				
			    		lineCounter++;
	    			}
	    			
	    		} catch (FileNotFoundException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (brRuntime != null) {
	    				try {
	    					brRuntime.close();
	    					
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
				String output = this.baseDirectory+"/results/"+"min/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+"AverageRuntimes.csv";
				
	    		BufferedWriter writer = null;
				String all = "";
				for (Entry<Integer, LinkedList<Float>> entry : average.entrySet()) {
					Integer key = entry.getKey();
					all += df.format(key) + TAB_DELIMITER;
					LinkedList<Float> value = entry.getValue();
					int k = 0;
					for(Float no:value){
						all += df.format(no);
						if(k!=value.size()-1)
							all += TAB_DELIMITER;
						k++;
					}
					all += NEW_LINE_SEPARATOR;
					
				}
				
				try {
						writer = new BufferedWriter(new FileWriter(output));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			all = header + NEW_LINE_SEPARATOR + all;
	    		
				
				try {
					writer.append(all);
					writer.flush();
	    			writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			
			
		}
		
		
    	
	}
	public void baseline(){
		String file = "OverallTime.csv";
		String header = null;
		LinkedHashMap<Integer,LinkedList<Float>> statistics = new LinkedHashMap<Integer,LinkedList<Float>>();

		for(double rec: recall){
			for(int maxTime:maxOptTime){
				//specification number, <set of float numbers>
				for(int iteration = 0 ; iteration<iterations ; iteration++){	
					BufferedReader br = null;
					try {
		    			String csvFile = this.baseDirectory+"/results/"+iteration+"/"+maxTime+"/"+String.valueOf(rec*100)+"%"+"/"+file;
		    			br = new BufferedReader(new FileReader(csvFile));
		    			int specCounter = 0;
		    			String line = "";
		    			header = br.readLine();
		    			header = "Baseline";
		    			
		    			while ((line = br.readLine()) != null) {
		    				
		    				String[] fullEntry = line.split(TAB_DELIMITER);
		    				LinkedList<Float> temp = new LinkedList<Float>();
		    				for(int pos = 0 ; pos < 1 ; pos++){
		    					temp.add(Float.parseFloat(fullEntry[pos]));
		    					
		    				}
		    				
		    				if(statistics.get(specCounter)==null)
		    					statistics.put(specCounter, temp);
		    				else{//get old statistics
		    					LinkedList<Float> oldStats = statistics.get(specCounter);
		    					for(int pos = 0 ; pos < 1 ; pos++){
		    						if(oldStats.get(pos) > temp.get(pos)){
		    							oldStats.set(pos, temp.get(pos));
		    						}
		    					}
		    					
		    					statistics.put(specCounter, oldStats);
		    					//for(float st:statistics.get(specCounter)){
		    					//	logger.info(st);
		    					//}
		    				}
		    				
		    				
		    				specCounter++;
		    			}
		
		    		} catch (FileNotFoundException e) {
		    			e.printStackTrace();
		    		} catch (IOException e) {
		    			e.printStackTrace();
		    		} finally {
		    			if (br != null) {
		    				try {
		    					br.close();
		    				} catch (IOException e) {
		    					e.printStackTrace();
		    				}
		    			}
		    		}
					
					
				}
				
				
				
			
			}
		}
		String  OptimizerTypeFolder = this.baseDirectory+"/results/min";
		File dirName = new File(OptimizerTypeFolder);
		if(!dirName.isDirectory()){
    	 	try{
    	 		dirName.mkdir();
    	 		} catch(SecurityException se){
    	 	} 
		}
		//file = file.replace(".csv", ".tsv");
		String specFilename = OptimizerTypeFolder+"/Baseline.tsv";
		File file1 = new File(specFilename);
		FileWriter writer = null;
		DecimalFormat df = new DecimalFormat("#");
		df = new DecimalFormat("#");
    	df.setMaximumFractionDigits(50);
		if(!file1.exists()){
			try {
				writer = new FileWriter(specFilename);
				writer.append(header);writer.append(NEW_LINE_SEPARATOR);
				for (Entry<Integer, LinkedList<Float>> entry : statistics.entrySet()) {
				    Integer key = entry.getKey();
				    LinkedList<Float> value = entry.getValue();
				    int k = 0;
				    for(float no:value){
				    	writer.append(df.format(no));
				    	if(k != value.size()-1)
				    		writer.append(TAB_DELIMITER);
				    	k++;
				    }
				    writer.append(NEW_LINE_SEPARATOR);
				}
	    		
	    		writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
public static void main(String args[]){
		
		
		
		DatasetConfiguration dataCr = null;
		if(args.length != 0){
			dataCr  = new DatasetConfiguration(args);
		}
		else
			dataCr  = new DatasetConfiguration();
		
		
		//for each dataset
		for(String DatasetName: dataCr.getDatasets()){
 	    
 	    	logger.info("Current dataset: "+DatasetName);
			dataCr.setCurrentData(DatasetName);
			EvaluationData data = dataCr.getCurrentDataset();
			
		
			String BaseDirectory = "datasets/"+DatasetName;
			File dirName = new File(BaseDirectory);
			if(!dirName.isDirectory()){
	    	 	try{
	    	 		dirName.mkdir();
	    	 		} catch(SecurityException se){
	    	 	} 
			}
			
			Histogram exp = new Histogram(BaseDirectory, dataCr.getSource(),dataCr.getTarget());		
			exp.getMin();
			exp.selectivity();
			exp.combineSelectivityFiles();
			exp.AverageRuntime();
			//exp.baseline();
    	}
        	
    }
}
