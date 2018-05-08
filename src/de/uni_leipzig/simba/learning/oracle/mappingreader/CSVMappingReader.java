package de.uni_leipzig.simba.learning.oracle.mappingreader;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.util.DataCleaner;

public class CSVMappingReader implements MappingReader {

    private String SEP = ",";
    private boolean cleanIRI = false;

    public Mapping getMapping(String filePath) {
        Mapping result = new Mapping();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String s = reader.readLine();
            String split[];
            //first read name of properties. URI = first column
            if( s != null) {
            	ArrayList<String> properties = new ArrayList<String>();
                //split first line
                split = s.split(SEP);
                for(int i=0; i< split.length; i++)
                    properties.add(split[i]);
               //read remaining lines
                s = reader.readLine();
                String value;
           
	            while (s != null) {
	                //split first line
	            	split = s.split(SEP);
	            	if(split.length!=properties.size())
	            		split = DataCleaner.separate(s, SEP, properties.size());
	            	if(!cleanIRI)
	            		result.add(removeBraces(split[0]), removeBraces(split[1]), 1.0);      
	            	else
	            		result.add(removeBraces(split[0].replaceAll("\\<", "").replaceAll("\\>", "")), removeBraces(split[1].replaceAll("\\<", "").replaceAll("\\>", "")), 1.0);
	                s = reader.readLine();
	            }
            } else {
            	Logger logger = Logger.getLogger("Limes");
            	logger.error("No line in reference + "+filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void setSeparator(String s) {
        SEP = s;
    }

    /**
     * To clean URIs of surronding braces "<" and ">".
     * @param clean true to clean, false to use URI as is.
     */
    public void setCleanIRI(boolean clean) {
    	this.cleanIRI = clean;
    }
    
    public static void main(String args[]) {
        CSVMappingReader reader = new CSVMappingReader();
        String test;
        test = "Examples/GeneticEval/Datasets/dbpedia-linkedmdb/reference2.csv";
        Mapping m = reader.getMapping(test);
        System.out.println(m);
      //  System.out.println("First line: "+m.map.entrySet().iterator().next().getKey());
    }

    public String getType() {
        if(SEP.equalsIgnoreCase("\t")) return "TAB";
        return "CSV";
    }
    /**
     * Method to remove quotes at the beginning and end of a String. Needed to correctly parse values of a comma-separated file.
     * @param s
     * @return
     */
    public static String removeBraces(String s) {
    	if(s.startsWith("\""))
    		s = s.substring(1);
    	if(s.endsWith("\""))
    		s = s.substring(0,s.length()-1);
    	return s;
    }
    @Test
    public void testRemoveBraces() {
    	String[] test = { "\"abc\"", "\"abc", "abc\"", "abc"    			
    	};
    	boolean correct = true;
    	for(String s : test) {
    		System.out.println(s + " => "+removeBraces(s));
    		correct=correct&&removeBraces(s).equalsIgnoreCase("abc");
    	}
    	assertTrue(correct);
    }
}
