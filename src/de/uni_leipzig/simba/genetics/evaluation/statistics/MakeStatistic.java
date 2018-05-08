package de.uni_leipzig.simba.genetics.evaluation.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Class to create statistics out of logged runs (each 5 cycles).
 * @author Klaus Lyko
 *
 */
public class MakeStatistic {
	public String SEP = ";";
	private FileWriter writer;
	private File file;
	Logger logger = Logger.getLogger("LIMES");
	public static void main(String args[]) {
		String folder = "C:/Users/Lyko/Desktop/SHK/repositories/drupal/jar/Examples/GeneticEval/testResults/";
//		folder = "C:/Users/Lyko/Desktop/Ba Arbeit/Limes/testResults/";
		folder = "C:/Users/Lyko/Desktop/Ba Arbeit/Dokumentation/Ba/Evolution Of Limes/testresults/";
		String file = "AL_pop=100_gens=50AL_Abt-Buy.csv";
		MakeStatistic stat = new MakeStatistic();
		//stat.createFile("BatchPub20");
//		String filePath = folder+file;
		stat.readCompleteAL(folder, file, "AL", "50");
	
	}
	
	/**
	 * 
	 * @param folder
	 * @param filePath
	 */
    public void meanStdDeriv(String folder, String filePath) {
    
        try {
        	 BufferedReader reader = new BufferedReader(new FileReader(filePath));
             String s = reader.readLine();
             String splittitle [] = s.split(";");
             for(int a=0; a<splittitle.length;a++)
             	System.out.println(""+a+" "+splittitle[a]);
             String split[];
             SEP = ";";
             String name = "Batch"+filePath.substring(filePath.lastIndexOf("/")+1, filePath.lastIndexOf("."));
             name += "_meanStdDeri";
             logger.info("in File: "+name);
             this.createFile(folder, name);
          
             String title = "Batch(20);Batch(100);AL(20);AL(100)";
             
             writer.write(title);
 			writer.write(System.getProperty("line.separator"));
 			// write to file		
			Statistics stat_1 = new Statistics();
			Statistics stat_2 = new Statistics();
			Statistics stat_3 = new Statistics();
			Statistics stat_4 = new Statistics();
           //first read name of properties. URI = first column
            for(int ex = 0; ex < 10; ex ++) {
            	
                
            	// 3;6;9;12
            	s=reader.readLine();
            	for(int i = 3; i<13; i+=3) {
            		
            		split = s.split(SEP);
            		
            		double stdDeri = 0d;
            		try{
            			stdDeri = Double.parseDouble(split[i]);
            		}catch(NumberFormatException e) {
            			Logger.getLogger("LIMES").info("no valid stdDeri, setting to 0");
            		}
            		
	            	switch(i) {
		            	case 3: stat_1.add(stdDeri);
		            	break;
		            	case 6: stat_2.add(stdDeri);
		            	break;
		            	case 9: stat_3.add(stdDeri);
		            	break;
		            	case 12: stat_4.add(stdDeri);
		            	break;            	
	            	}
            	}
            	
    			
          }
            String out = ""+stat_1.mean+";"+stat_2.mean+";"+stat_3.mean+";"+stat_4.mean;
        	writer.write(out);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();	
          writer.close();
          reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally{
        	
        }
    }
    
    /**
     * 
     * @param folder
     * @param filePath
     * @param prefix
     * @param pop
     */
    public void readCompleteAL(String folder, String filePath, String prefix, String pop){
    	  
        try {
            BufferedReader reader = new BufferedReader(new FileReader(folder+filePath));
            String s = reader.readLine();
            String splittitle [] = s.split(";");
            for(int a=0; a<splittitle.length;a++)
            	System.out.println(""+a+" "+splittitle[a]);
            String split[];
            String name = filePath.substring(filePath.lastIndexOf("/")+1, filePath.lastIndexOf("."));
            logger.info("Folder: "+folder+" in File: "+name);
            this.createFile(folder, name);
         
            String pre = prefix+" ("+pop+")";
            String title = "oracle;"+pre+";"+pre+" std derivation;"+ pre+" mean runtime";
            
            writer.write(title);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
           //first read name of properties. URI = first column
			Statistics meanStdDeri=new Statistics();
            for(int ex = 0; ex < 10; ex ++) {
            	Statistics stat_fscore = new Statistics();
            	long dur = 0;
            	int oracle=0;
            	for(int cy = 0; cy <5; cy++) {
            		s = reader.readLine();
            		split = s.split(SEP);
            		oracle = Integer.parseInt(split[7]);
            		oracle*=10;
            //		oracle = 0;
            		double fScore = 0d;
            		try{
            			fScore = Double.parseDouble(split[3]);
            		}
            		catch(NumberFormatException e) {
            			logger.info("error parsing fScore "+split[3]);
            		}
            		dur += Long.parseLong(split[6]);
            		stat_fscore.add(fScore);            			
            	}
            	meanStdDeri.add(stat_fscore.standardDeviation);
            	String out = oracle+";"+stat_fscore.mean+";"+stat_fscore.standardDeviation+";"+(dur/5);
            	writer.write(out);
    			writer.write(System.getProperty("line.separator"));
    			// write to file
    			writer.flush();	
    			
          }
            String out = " ;"+meanStdDeri.mean+"; ";
            writer.write(out);
            writer.flush();
          writer.close();
          reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void createFile(String folder, String name) {
    	file = new File(folder+"Means"+name+".csv");
		try {
			writer = new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
