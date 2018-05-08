/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.dofin.algorithm;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.io.serializer.TabSeparatedSerializer;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.query.CsvQueryModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author ngonga
 */
public class BenchmarkController {

    public static void main(String args[]) {
long begin = System.currentTimeMillis();        
        String file;
        if(args.length == 0) file = "E:/Work/Data/AAAI-Benchmark/QueryFeatures.txt/QueryFeatures.txt";
        else file = args[0];
        KBInfo kbinfo = new KBInfo();
        kbinfo.endpoint = file;
        CsvQueryModule csv = new CsvQueryModule(kbinfo);
        csv.setSeparation("\t");
        MemoryCache fullCache = new MemoryCache();
        csv.fillAllInCache(fullCache);
        Cache c = fullCache;//.getSample(1000);
        //System.out.println(c);
        List<String> properties = kbinfo.properties;
        HashMap<String, Mapping> mappings = new HashMap<String, Mapping>();
        List<String> pps = new ArrayList<String>();
        double w1 = getAverageSize(c, "Features");
        double w2 = getAverageSize(c, "Query");
        System.out.println(w1+" "+w2);
        w1 = 1/(1+Math.ceil(w1/40.0));
        w2 = 1/(1+Math.ceil(w2/50.0));
        
        System.out.println("Translates to "+w1+" "+w2);
//        for (String p : properties) {
//            if (!p.equals("ID")) {
//                Mapping m = new Mapping();
//                if (!p.startsWith("Var")) {
//                    m = execute(p, c, "levenshtein", 1.0);
//                    mappings.put(p, m);
////                } else {
////                    m = execute(p, c, "euclidean", 1.0);
////                    mappings.put(p, m);
////                }
//                    if (m.size() > 0 && m.size() / c.size() < 10) {
//                        pps.add(p);
//
//                    }
//                }
//                System.out.println(m.size() + " for property " + p);
//            }
//        }
//
//        System.out.println(pps);
        //System.out.println(Dofin.runScalable(mappings, c.getAllUris()));
        Mapping m1 = execute("Query", c, "levenshtein", w2);
        Mapping m = execute("Features", c, "levenshtein", w1);
        m = SetOperations.intersection(m1, m);
        TabSeparatedSerializer tss  = new TabSeparatedSerializer();
        if(args.length < 1) tss.open("E:/tmp/results-"+w1+"-"+w2+".txt");
        else tss.open(args[1]+w1+"-"+w2+".all.txt");
        for(String s: m.map.keySet())
        {
            for(String t: m.map.get(s).keySet())
            {
                //if(!s.equals(t))
                tss.printStatement(s, "similar", t, m.getSimilarity(s, t));
            }
        }
        tss.close();
        long end = System.currentTimeMillis();
        System.out.println("Computation took "+((end-begin)/1000)+" s");
    }

    public static double getAverageSize(Cache c, String property) {
        double count = 0;
        double total = 0;
        for (String s : c.getAllUris()) {
            TreeSet<String> values = c.getInstance(s).getProperty(property);
            if (!values.equals(null) && !values.isEmpty()) {
                for (String v : values) {
                    count++;
                    total = total + (double)v.length();
                }
            }
        }
        return total/count;
    }

    /**
     * Runs a similarity computation
     *
     * @param property Property to use for comparison
     * @param c Cache
     * @param measure Similarity measure
     * @param threshold Threshold
     * @return
     */
    public static Mapping execute(String property, Cache c, String measure, double threshold) {
        String measureExpression = measure + "(x." + property + ", y." + property + ")";
        Instruction inst = new Instruction(Instruction.Command.RUN, measureExpression, threshold + "", -1, -1, -1);
        ExecutionEngine ee = new ExecutionEngine(c, c, "?x", "?y");
        return ee.executeRun(inst);
    }

    public static Mapping execute(String p1, String p2, double w1, double w2, Cache c, String measure, double threshold) {
        String measureExpression1 = measure +" (x." + p1 + ", y." + p1 + ")";
        String measureExpression2 = measure +" (x." + p2 + ", y." + p2 + ")";        
        Instruction inst = new Instruction(Instruction.Command.RUN, measureExpression1, w1 + "", -1, -1, -1);
        ExecutionEngine ee = new ExecutionEngine(c, c, "?x", "?y");
        Mapping m1 = ee.executeRun(inst);
        
        inst = new Instruction(Instruction.Command.RUN, measureExpression2, w2 + "", -1, -1, -1);
        ee = new ExecutionEngine(c, c, "?x", "?y");
        Mapping m2 = ee.executeRun(inst);
        return SetOperations.intersection(m1, m2);
    }
}
