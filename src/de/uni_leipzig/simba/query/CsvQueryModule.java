/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.query;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.preprocessing.Preprocessor;
import de.uni_leipzig.simba.util.DataCleaner;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.log4j.*;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 *
 * @author ngonga
 */
public class CsvQueryModule implements QueryModule {

    private String SEP = ",";
    KBInfo kb;

    public CsvQueryModule(KBInfo kbinfo) {
        kb = kbinfo;
    }

    public void setSeparation(String s) {
        SEP = s;
    }

    /**
     * Read a CSV file and write the content in a cache. The first line is the
     * name of the properties.
     *
     * @param c Cache in which the content is to be written
     */
    public void fillCache(Cache c) {
        Logger logger = Logger.getLogger("LIMES");
        try {
            // in case a CSV is use, endpoint is the file to read
            BufferedReader reader = new BufferedReader(new FileReader(kb.endpoint));
            String s = reader.readLine();
            String split[];
            //first read name of properties. URI = first column
            if (s != null) {
                ArrayList<String> properties = new ArrayList<String>();
                //split first line
                split = s.split(SEP);
                properties.addAll(Arrays.asList(split));

                s = reader.readLine();
                String rawValue;
                String id, value;
                while (s != null) {
                    //split = s.split(SEP);

                    split = DataCleaner.separate(s, SEP, properties.size());
                  
                    id = split[0];
                    for (String propertyLabel : kb.properties) {
//                    	System.out.println("Trying to access property "+propertyLabel+" at position "+properties.indexOf(propertyLabel));
                        rawValue = split[properties.indexOf(propertyLabel)];
                        for (String propertyDub : kb.functions.get(propertyLabel).keySet()) {
                            //functions.get(propertyLabel).get(propertyDub) gets the preprocessing chain that leads from 
                            //the propertyLabel to the propertyDub
                            value = Preprocessor.process(rawValue, kb.functions.get(propertyLabel).get(propertyDub));
                            if (properties.indexOf(propertyLabel) >= 0) {
                                c.addTriple(id, propertyDub, value);
                            }
                        }
                    }
                    s = reader.readLine();
                }
            } else {
                logger.warn("Input file " + kb.endpoint + " was empty or faulty");
            }
            reader.close();
            logger.info("Retrieved " + c.size() + " statements");
        } catch (Exception e) {
            logger.fatal("Exception:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read a CSV file and write the content in a cache. The first line is the
     * name of the properties.
     *
     * @param c Cache in which the content is to be written
     */
    public void fillAllInCache(Cache c) {
        Logger logger = Logger.getLogger("LIMES");
        String s = "";
        try {
            // in case a CSV is use, endpoint is the file to read
            BufferedReader reader = new BufferedReader(new FileReader(kb.endpoint));
            s = reader.readLine();
            String split[];
            //first read name of properties. URI = first column
            if (s != null) {
                ArrayList<String> properties = new ArrayList<String>();
                //split first line
                split = s.split(SEP);
                properties.addAll(Arrays.asList(split));
                logger.info("Properties = " + properties);
                logger.info("KB Properties = " + kb.properties);
                //read remaining lines

                kb.properties = properties;
                s = reader.readLine();
                String rawValue;
                String id, value;
                while (s != null) {
                    split = s.split(SEP);
                    split = DataCleaner.separate(s, SEP, properties.size());
                    id = split[0].substring(1, split[0].length()-1);
                    //logger.info(id);
                    for (String propertyLabel : kb.properties) {
                        rawValue = split[properties.indexOf(propertyLabel)];
                        if (kb.functions.containsKey(propertyLabel)) {
                            for (String propertyDub : kb.functions.get(propertyLabel).keySet()) {
                                //functions.get(propertyLabel).get(propertyDub) gets the preprocessing chain that leads from 
                                //the propertyLabel to the propertyDub
                                value = Preprocessor.process(rawValue, kb.functions.get(propertyLabel).get(propertyDub));
                                if (properties.indexOf(propertyLabel) >= 0) {
                                    c.addTriple(id, propertyDub, value);
                                }
                            }
                        } else {
                            c.addTriple(id, propertyLabel, rawValue.replaceAll(Pattern.quote("@en"), ""));
                        }
                    }
                    s = reader.readLine();
                }
            } else {
                logger.warn("Input file " + kb.endpoint + " was empty or faulty");
            }
            reader.close();
            logger.info("Retrieved " + c.size() + " statements");
        } catch (Exception e) {
            logger.fatal("Exception:" + e.getMessage());
            logger.warn(s);
            e.printStackTrace();
        }
    }
    
}