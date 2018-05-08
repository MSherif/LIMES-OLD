/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.dofin.algorithm;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class Controller {

    static Logger logger = Logger.getLogger("LIMES");
    Cache cache;
    Map<String, Set<Set<String>>> discriminativeProperties;
    HashMap<String, HashMap<String, Double>> coverageMap;
    public void writeToFile(HashMap<String, HashMap<String, Double>> result, String outputFile) {
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            for (String c : result.keySet()) {
                HashMap<String, Double> properties = result.get(c);
                for (String p : properties.keySet()) {
                    writer.println(c + "\t" + p + "\t" + properties.get(p));
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, HashMap<String, Double>> getDiscriminativeProperties
            (String endpoint, String graph, int sampleSize) {
        //data structure for output. Maps a class to a set of properties of which
        // each is mapped to a coverage score
        discriminativeProperties = new HashMap<String, Set<Set<String>>>();
        HashMap<String, HashMap<String, Double>> output = new HashMap<String, HashMap<String, Double>>();
        try {
            DataSamplingFetcher df = new DataSamplingFetcher();
            df.sampleSize = sampleSize;
            //first get classes from endpoint
            Set<String> classes = df.getClasses(endpoint, graph);
            logger.info(classes.size() + " class for endpoint " + endpoint + " and graph " + graph);
            for (String c : classes) {
                // maps that contains property to coverage mapping
                HashMap<String, Double> pMap = new HashMap<String, Double>();
                Set<String> properties = df.getProperties(endpoint, graph, c);
                //create new mapping (in the LIMES sense, i.e. correspondences)
                HashMap<String, Mapping> mappings = new HashMap<String, Mapping>();
                logger.info(properties.size() + " properties for class " + c);
                // first create cache    
                cache = new MemoryCache();
                for (String p : properties) {
                    // get values for class c and property p
                    logger.info("Getting property " + p + " for class " + c + "...");
                    // then fill it with the data from the endpoint. This method can use
                    // sampling to accelerate the data filling. In general, this sould not
                    // be necessary unless we have plenty of data in one class, for example
                    // a lot of animal names
                    cache = df.fillCache(endpoint, graph, c, p, cache);
                    //System.out.println(cache);
                    //now use a measure and a distance or similarity threshold to generate the 
                    // mapping for this given property
                    logger.info("Computing the corresponding LIMES mapping ...");
                    Mapping m = df.getMapping(cache, "levenshtein", p, 2);
                    //System.out.println(m);
                    if (m.size > 0 && m != null) {
                        mappings.put(p, m);
                    }
                }
                //System.exit(1);
                ArrayList<String> uris = cache.getAllUris();
                //properties = checkCompleteness(cache, properties);
                //run the dofin algorithm on the mappings
                logger.info("Running the Dofin algorithm");
                Set<Set<String>> result = Dofin.runScalable(mappings, uris);
                discriminativeProperties.put(c, result);
                logger.info("Dofin returned the following set of properties: " + result);
                for (Set<String> p : result) {
                    pMap.put(p + "", df.getCoverage(p, cache));
                }
                output.put(c, pMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        coverageMap = output;
        return output;
    }

    /** Computes all discriminative properties for all classes in an endpoint
     * 
     * @param endpoint SPARQL endpoint
     * @param graph Graph to be used (set to null if not needed)
     */
    public HashMap<String, HashMap<String, Double>> getDiscriminativeProperties(String endpoint, String graph) {
        //data structure for output. Maps a class to a set of properties of which
        // each is mapped to a coverage score
        discriminativeProperties = new HashMap<String, Set<Set<String>>>();
        HashMap<String, HashMap<String, Double>> output = new HashMap<String, HashMap<String, Double>>();
        try {
            DataFetcher df = new DataFetcher();
            //first get classes from endpoint
            Set<String> classes = df.getClasses(endpoint, graph);
            logger.info(classes.size() + " class for endpoint " + endpoint + " and graph " + graph);
            for (String c : classes) {
                // maps that contains property to coverage mapping
                HashMap<String, Double> pMap = new HashMap<String, Double>();
                Set<String> properties = df.getProperties(endpoint, graph, c);
                //create new mapping (in the LIMES sense, i.e. correspondences)
                HashMap<String, Mapping> mappings = new HashMap<String, Mapping>();
                logger.info(properties.size() + " properties for class " + c);
                // first create cache    
                cache = new MemoryCache();
                for (String p : properties) {
                    // get values for class c and property p
                    logger.info("Getting property " + p + " for class " + c + "...");
                    // then fill it with the data from the endpoint. This method can use
                    // sampling to accelerate the data filling. In general, this sould not
                    // be necessary unless we have plenty of data in one class, for example
                    // a lot of animal names
                    cache = df.fillCache(endpoint, graph, c, p, cache);
                    System.out.println(cache);
                    //now use a measure and a distance or similarity threshold to generate the 
                    // mapping for this given property
                    logger.info("Computing the corresponding LIMES mapping ...");
                    Mapping m = df.getMapping(cache, "levenshtein", p, 2);
                    //System.out.println(m);
                    if (m.size > 0 && m != null) {
                        mappings.put(p, m);
                    }
                }
                //System.exit(1);
                ArrayList<String> uris = cache.getAllUris();
                //properties = checkCompleteness(cache, properties);
                //run the dofin algorithm on the mappings
                logger.info("Running the Dofin algorithm");
                Set<Set<String>> result = Dofin.runScalable(mappings, uris);
                logger.info("Dofin returned the following set of properties: " + result);
                discriminativeProperties.put(c, result);
                for (Set<String> p : result) {
                    pMap.put(p + "", df.getCoverage(p, cache));
                }
                output.put(c, pMap);
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        coverageMap = output;
        return output;
    }

    public static void main(String args[]) {
        System.out.println("=========\n"
                + (new Controller()).getDiscriminativeProperties("http://live.dbpedia.org/sparql",
                null, 20));
    }
}
