/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.learner;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.measures.MeasureFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class LinearConfiguration implements Configuration {

    // Maps metric(p[], q[]) to associated weight
    HashMap<String, Double> mapping;
    public double INIT_THRESHOLD=0.8;
    //threshold for mapping
    public double threshold;
    static Logger logger = Logger.getLogger("LIMES");

    /** Assumes a linear combination
     *
     * @param expression Linear combination to use
     */
    public LinearConfiguration(HashMap<String, TreeSet<String>> propertyMapping,
            HashMap<String, String> propertyType) {
        mapping = new HashMap<String, Double>();
        String measure;
        for (String p : propertyMapping.keySet()) {
            measure = MeasureFactory.getMeasure("default", propertyType.get(p)).getName();
            for (String q : propertyMapping.get(p)) {
                mapping.put(measure + "(" + p + "," + q + ")", 0.5);
            }
        }
        //default
        threshold = 0.9;
    }

    public LinearConfiguration(Mapping propertyMapping,
            HashMap<String, String> propertyType) {
        mapping = new HashMap<String, Double>();
        //double t = 0;
        String measure;
        for (String p : propertyMapping.map.keySet()) {
            measure = MeasureFactory.getMeasure("random", propertyType.get(p)).getName();
            for (String q : propertyMapping.map.get(p).keySet()) {
                //should not be necessary
                if(propertyMapping.getSimilarity(p, q) > 0)
                {
                    mapping.put(measure + "(x." + p + ",y." + q + ")", 1.0);
                }
//                        propertyMapping.getSimilarity(p, q));
                //t = t + propertyMapping.getSimilarity(p, q);
            }
        }
        //default
        logger.info("Size of property map is " + propertyMapping.size());
        setInitialThreshold(propertyMapping, INIT_THRESHOLD, false);
        logger.info("Setting threshold to " + threshold);
        //System.out.println(mapping);
    }

    /** Returns multiplier times number of non-zero mapping properties in m
     *
     * @param m Property mapping
     * @param multiplier Multiplier for initial config
     * @param weighted If true, then similarity of properties is considered
     */
    public void setInitialThreshold(Mapping m, double multiplier, boolean weighted) {
        threshold = mapping.size()*multiplier;
        logger.info("Initialized threshold to "+threshold);
    }

    //returns the String correponding to the metric being worked with here
    public String getExpression() {
        String expression = "";
        ArrayList<String> nodes;
        if (mapping.size() == 0) {
            return expression;
        } else if (mapping.size() == 1) {
            for (String s : mapping.keySet()) {
                expression = s;
            }
            return expression;
        } else {
            nodes = new ArrayList<String>();
            for (String s : mapping.keySet()) {
                nodes.add(s);
            }
            expression = mapping.get(nodes.get(0)) + "*" + nodes.get(0);
            //System.out.println("--> " + expression);
            for (int i = 1; i < nodes.size(); i++) {
                if (mapping.get(nodes.get(i)) > 0) {
                    expression = "ADD(" + expression + ", "
                            //        + mapping.get(nodes.get(i)) + "*" + nodes.get(i) + ")";
                            + mapping.get(nodes.get(i)) + "*" + nodes.get(i) + ")";
                    //System.out.println("--> " + expression);
                } else {
                    mapping.remove(nodes.get(i));
                }
            }
        }
        //logger.info("Generated expression " + expression + " >= "+threshold);
        return expression;
    }

    public static HashMap<String, String> generateStringTyping(Mapping m) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String s : m.map.keySet()) {
            map.put(s, "string");
            for (String p : m.map.get(s).keySet()) {
                map.put(p, "string");
            }
        }
        return map;
    }

    public static void main(String args[]) {
        Mapping m = new Mapping();
        m.add("http://dbpedia.org/property/bioavailability",
                "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/proteinBinding",
                9.0);
        m.add("http://www.w3.org/2000/01/rdf-schema#label",
                "http://www.w3.org/2000/01/rdf-schema#label",
                1.0);
        m.add("http://www.w3.org/2000/01/rdf#label",
                "http://www.w3.org/2000/01/rdfa#label",
                1.0);

        LinearConfiguration lc = new LinearConfiguration(m, generateStringTyping(m));
        System.out.println("\n" + lc.getExpression());
    }
}

