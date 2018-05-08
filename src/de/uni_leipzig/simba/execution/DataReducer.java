/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution;

import de.uni_leipzig.gk.cluster.BorderFlowHard;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.Instruction.Command;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
/**
 *
 * @author ngonga
 */
public class DataReducer {

    /** Computes prototypical values for a given property with respect to a given
     * similarity measure.
     * @param cache Cache containing the data to analyze
     * @param property Property to look at
     * @param measure Similarity measure to be used
     * @param threshold Similarity threshold
     * @return List of prototypical values
     */
    static Logger logger = Logger.getLogger("LIMES");
    public static Set<String> getPrototypical(Cache cache, String property, String measure, double threshold) {
        // get mapping
        String measureExpression = measure + "(x." + property + ", y." + property + ")";
        Instruction inst = new Instruction(Command.RUN, measureExpression, threshold + "", -1, -1, -1);
        ExecutionEngine ee = new ExecutionEngine(cache, cache, "?x", "?y");
        Mapping data = ee.executeRun(inst);
        logger.info("Mapping = "+data);
        //compute clusters using Borderflow
        Map<Set<String>, Set<String>> clusters = getClusters(data);
        //compute prototypical examples/cluster
        Collection<Set<String>> values = clusters.values();
        Set<String> result = new HashSet<String>();
        for(Set<String> v: values)
            result.add(getPrototypical(data, v));
        logger.info("Got "+result.size()+" prototypical values for property <"+property+"> with measure <"+measure+">"
                + "and threshold "+threshold);
        return result;
    }

    /** Computes a soft clustering of the data at hand
     * 
     * @param graph Mapping computed by LIMES
     * @return Set of URIs with prototypical values
     */
    private static Map<Set<String>, Set<String>> getClusters(Mapping graph) {
        Map<Set<String>, Set<String>> results = new HashMap<Set<String>, Set<String>>();
        try {
            File f = File.createTempFile("www", "ww");
            String name = f.getAbsolutePath();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(name)));
            for (String s : graph.map.keySet()) {
                for (String t : graph.map.get(s).keySet()) {
                    writer.println(s + "\t" + t + "\t" + graph.getSimilarity(s, t));
                }
            }
            writer.close();            
            BorderFlowHard bf = new BorderFlowHard(name); 
            bf.hardPartitioning = false;
            Map<Set<String>, Set<String>> clusters = bf.cluster(-1, true, true, true);            
            return clusters;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
    
    public static String getPrototypical(Mapping m, Set<String> clusters)
    {
        //deal with empty clusters
        if(clusters.isEmpty()) return null;
        //if cluster are of size 1 or 2, then any node is fine
        if(clusters.size()==1 || clusters.size() == 2) return clusters.iterator().next();
        
        //else get node with largest total similarity to other nodes in the cluster
        double maxSim = 0, sim;
        String best = clusters.iterator().next();
        for(String s: clusters)
        {
            sim = 0;
            for(String t:clusters)
            {
                sim = sim + m.getSimilarity(s, t);
            }
            if(sim > maxSim)
            {
                maxSim = sim;
                best = s;
            }
        }
        return best;
    }
    
    public static void main(String args[])
    {
        Cache c = new MemoryCache();
        for(int i=0; i<100; i++)
        {
            c.addTriple(i+"", "label", Math.floor(1000*Math.random())+"");
        }
        System.out.println(c);
        Set<String> results = getPrototypical(c, "label", "levenshtein", 0.33);
        System.out.println(results);
    }
}
