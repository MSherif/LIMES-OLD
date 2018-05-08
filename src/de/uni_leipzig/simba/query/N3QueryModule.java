/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.query;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.preprocessing.Preprocessor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * DEPRECATED and BUGGY
 *
 * @author ngonga
 */
public class N3QueryModule implements QueryModule {

    final static Logger logger = Logger.getLogger("LIMES");
    KBInfo kb;

    public void fillCache(Cache c) {
        //contains all prefixes found in the N3 file
        HashMap<String, String> prefixes = new HashMap<String, String>();

        Logger logger = Logger.getLogger("LIMES");
        try {
            // in case a N3 is used, endpoint is the file to read
            BufferedReader reader = new BufferedReader(new FileReader(kb.endpoint));
            String s = reader.readLine();
            String split[];
            //first read name of properties. URI = first column
            if (s != null) {
                //split lines and check for prefixes
                s = reader.readLine();
                String subject, predicate, object;
                while (s != null) {
                    s = s.trim();
                    if (s.length() > 0) {
                        //process prefixes
                        if (s.startsWith("@prefix")) {
                            split = s.split(" ");
                            //write the prefixes in the knowledge base
                            //assume consistency
                            logger.info("adding prefix " + split[1] + "-->" + split[2]);
                            kb.prefixes.put(split[1], split[2]);
                        } //we assume this is a triple
                        else {
                            split = s.split(" ");
                            subject = split[0].replace("<", "").replace(">", "");
                            predicate = getPrefixedData(split[1].replace("<", "").replace(">", ""));
                            logger.info("get preprocessing for " + predicate);
                            for (String predicateLabel : kb.functions.get(predicate).keySet()) {
                            	logger.info(split[2]);
                                object = Preprocessor.process(split[2].replace("<", "").replace(">", ""),
                                        kb.functions.get(predicate).get(predicateLabel));
                                if (object.indexOf("\"") != -1) {
//                            	logger.info("avoiding \"");
                                    object = object.replaceAll("\"", "");
                                }
                                c.addTriple(subject, predicateLabel, object);
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
     * Get Prefix for data if it exists.
     *
     * @param s
     */
    public String getPrefixedData(String s) {
        if (!s.startsWith("http://")) {
            return s;
        } else {
            int splitIndex = Math.max(s.lastIndexOf("/"), s.lastIndexOf("#"));
            if (splitIndex > 0 && splitIndex < s.length() - 1) {
                String baseUri, suffix;
                baseUri = s.substring(0, splitIndex + 1);
                suffix = s.substring(splitIndex + 1);
                String prefix = "";
                if (kb.prefixes.containsValue(baseUri)) {
                    prefix = kb.getPrefix(baseUri);
                }
                if (prefix.length() > 0 && prefix != null) {
                    return prefix + ":" + suffix;
                } else {
                    logger.warn("No prefix found for URI: " + s);
                    return s;
                }
            }
        }
        return s;
    }

    public N3QueryModule(KBInfo kbinfo) {
        kb = kbinfo;
    }

    public static void main(String args[]) {
        KBInfo k = new KBInfo();
        k.endpoint = "C:/Users/Lyko/Desktop/drugbank_dump.nt";
        k.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        k.prefixes.put("owl", "http://www.w3.org/2002/07/owl#");

        k.properties.add("rdf:type");
        k.properties.add("owl:equivalentProperty");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("rdf:type", "lowercase");

        k.functions.put("rdf:type", map);
        map.clear();
        map.put("owl:equivalentProperty", "uri");
        k.functions.put("owl:equivalentProperty", map);
        HybridCache c = new HybridCache();
        N3QueryModule qm = new N3QueryModule(k);
        qm.fillCache(c);
//        c.size()
        System.out.println(c);
//        Instance i = c.getAllInstances().get(0);
//        System.out.println(i);
//        System.out.println(i.getAllProperties());
//
//        KBInfo k2 = new KBInfo();
//        k2.endpoint = "C:/Users/Klaus/workspace/LIMES/resources/Persons1/person12.nt";
//        HybridCache c2 = new HybridCache();
//        N3QueryModule qm2 = new N3QueryModule(k2);
//        qm2.fillCache(c2);
//
//        Instance i2 = c2.getAllInstances().get(0);
//
//
//        for (String prop : i.getAllProperties()) {
//            System.out.println(prop + " --> " + i.getProperty(prop));
//        }
//        System.out.println(" ---- ");
//        for (String prop : i2.getAllProperties()) {
//            System.out.println(prop + " --> " + i2.getProperty(prop));
//
//        }
//

    }
}
