/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.learner;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.measures.MeasureFactory;
import de.uni_leipzig.simba.measures.MeasureProcessor;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class LinearCombinationLearner implements Learner {

    static Logger logger = Logger.getLogger("LIMES");
    //count the number of questions posed to the oracle
    int inquiries;
    // the oracle for learning
    Oracle oracle;
    // Cache containing all relevant source data
    Cache source;
    // Info to the source, e.g. variable
    KBInfo sourceInfo;
    // Cache containing all relevant target data
    Cache target;
    // Info to the source, e.g. variable
    KBInfo targetInfo;
    //contains current learned config
    LinearConfiguration config;
    //contains all known positive examples and their similarity
    Mapping positives;
    //contains all known negative examples and their similarity
    Mapping negatives;
    //contains current Mapping returned by config
    Mapping results, oldResults;
    //mapper used within the learning
    SetConstraintsMapper mapper;
    //constants for learning
    double alpha, beta;


    /**Inits the learner
     *
     * @param sInfo KBInfo for source
     * @param tInfo KBInfo for target
     * @param s Cache data for source
     * @param t Cache data for target
     * @param o Oracle for learning
     * @param propertyMapping Mapping of properties to create init config
     * @param propertyType Type of each property. Important for metric factory
     * @param a Value of weight of positives
     * @param b Value of weight for negatives
     */
    public LinearCombinationLearner(KBInfo sInfo, KBInfo tInfo, Cache s, Cache t, Oracle o,
            Mapping propertyMapping, HashMap<String, String> propertyType,
            double a, double b) {
        //set variables
        oracle = o;
        source = s;
        target = t;
        sourceInfo = sInfo;
        targetInfo = tInfo;
        inquiries = 0;
        positives = new Mapping();
        negatives = new Mapping();
        //create initial config
        config = new LinearConfiguration(propertyMapping, propertyType);
        //getLinks for given config;
        mapper = SetConstraintsMapperFactory.getMapper("simple", sourceInfo,
                targetInfo, source, target, new LinearFilter(), 2);
        //cr.source, cr.target, source, target, new PPJoinMapper(), new LinearFilter());
        logger.info("Getting initial links ...");
        results = mapper.getLinks(config.getExpression(), config.threshold);
        logger.info("Got " + results.size() + " initial links ...");
        oldResults = new Mapping();
        alpha = a;
        beta = b;

    }

    /** Compute the next config by picking random elements from the current mapping for which
     * we do not yet know whether they are positive or negative, finding that out
     * and adapting weights and threshold such that the precision reaches a certain
     * minimal accuracy. This is basically the key function for every learner.
     * @param n Number of exemples to take at once
     */
    public boolean computeNextConfig(int n) {
        //1. Compute difference between new results and oldresults
        Mapping difference = diff(results, oldResults, n);
        if(difference.size() == 0) return false; //no need to compute further
        //2. Now classify the unknown links
        Mapping newPositives = new Mapping();
        Mapping newNegatives = new Mapping();
        logger.info("\n\n\nDifference contains " + difference.size() + " entities");
        for (String s : difference.map.keySet()) {
            for (String p : difference.map.get(s).keySet()) {
                boolean positive = oracle.ask(s, p);
                inquiries++;

                //logger.info("Source instance "+source.getInstance(s));
                //logger.info("Target instance "+target.getInstance(p));
                //logger.info("Expression = "+config.getExpression());

                double sim = MeasureProcessor.getSimilarity(source.getInstance(s),
                        target.getInstance(p), config.getExpression(), sourceInfo.var,
                        targetInfo.var);
                if (positive) {
                    newPositives.add(s, p, sim);
                } else {
                    newNegatives.add(s, p, sim);
                }
            }
        }
        logger.info("New positives = " + newPositives);
        logger.info("New negatives = " + newNegatives);
        //3. Now update the weights in configuration
        Parser parser;
        double sum = 0;
        String term1, term2;

        logger.info("Configuration before learning = " + config.mapping);
        for (String expression : config.mapping.keySet()) {
            parser = new Parser(expression, 1.0);
            Measure m = MeasureFactory.getMeasure(parser.op);
            //get similarity
            for (String uri1 : newPositives.map.keySet()) {
                for (String uri2 : newPositives.map.get(uri1).keySet()) {
                    term1 = parser.term1.substring(parser.term1.indexOf(".") + 1);
                    term2 = parser.term2.substring(parser.term2.indexOf(".") + 1);
                    sum = sum + alpha * m.getSimilarity(source.getInstance(uri1),
                            target.getInstance(uri2), term1, term2);
                    logger.info("Sum = " + sum);
                }
            }
            //compute new weights
            for (String uri1 : newNegatives.map.keySet()) {
                for (String uri2 : newNegatives.map.get(uri1).keySet()) {
                    term1 = parser.term1.substring(parser.term1.indexOf(".") + 1);
                    term2 = parser.term2.substring(parser.term2.indexOf(".") + 1);
                    sum = sum - beta * m.getSimilarity(source.getInstance(uri1),
                            target.getInstance(uri2), term1, term2);
                    logger.info("Sum = " + sum);
                }
            }
            double weight = config.mapping.get(expression);
            logger.info("Weight for <" + parser.term1 + "> and <" + parser.term2 + "> goes from "
                    + weight + " to " + (weight + sum));

            config.mapping.put(expression, weight + sum);
        }
        logger.info("Configuration after learning = " + config.mapping);
        //3. add new positives to positives and new negatives to negatives
        // important. Need to reevaluate everything as the config changed.

        //3.1 first for positives
        Mapping help = new Mapping();
        double sim = 0, min = Integer.MAX_VALUE;
        double totalSum = 0;
        double counter = 0;
        for (String key : positives.map.keySet()) {
            for (String value : positives.map.get(key).keySet()) {
                sim = MeasureProcessor.getSimilarity(source.getInstance(key),
                        target.getInstance(value), config.getExpression(), sourceInfo.var,
                        targetInfo.var);
                totalSum = totalSum + sim;
                        counter++;
                if (sim > min) {
                    min = sim;
                }
                help.add(key, value, sim);
            }
        }

        for (String key : newPositives.map.keySet()) {
            for (String value : newPositives.map.get(key).keySet()) {
                sim = MeasureProcessor.getSimilarity(source.getInstance(key),
                        target.getInstance(value), config.getExpression(), sourceInfo.var,
                        targetInfo.var);
                totalSum = totalSum + sim;
                counter++;
                if (sim > min) {
                    min = sim;
                }
                help.add(key, value, sim);
            }
        }
        positives = help;

        //3.2 then for negatives
        help = new Mapping();
        sim = 0;
        double max = 0;

        for (String key : negatives.map.keySet()) {
            for (String value : negatives.map.get(key).keySet()) {
                sim = MeasureProcessor.getSimilarity(source.getInstance(key),
                        target.getInstance(value), config.getExpression(), sourceInfo.var,
                        targetInfo.var);
                totalSum = totalSum + sim;
                counter++;
                if (sim > max) {
                    max = sim;
                }
                help.add(key, value, sim);
            }
        }

        for (String key : newNegatives.map.keySet()) {
            for (String value : newNegatives.map.get(key).keySet()) {
                sim = MeasureProcessor.getSimilarity(source.getInstance(key),
                        target.getInstance(value), config.getExpression(), sourceInfo.var,
                        targetInfo.var);
                totalSum = totalSum + sim;
                counter++;
                if (sim > max) {
                    max = sim;
                }
                help.add(key, value, sim);
            }
        }
        negatives = help;

        //4. Now set the threshold to the smallest threshold that
        // allows to retrieve all the positive (max recall). We can obviously change this to
        // ensure maximal P by kicking all the negatives
        //if max recall
        config.threshold = totalSum/counter;
        logger.info("New threshold = "+config.threshold);
        //if max precision
        // config.threshold = max;
        // can also take maximal quadratic difference

        //5. Compute the new mapping according to the config
        oldResults = results;
        logger.info("Current Precision = "+getPrecision());
        logger.info("Current Recall = "+getRecall());
        results = mapper.getLinks(config.getExpression(), config.threshold);
        return true;
    }

    /** Computes the <b>unclassified</b> elements that are in mapping a and not in mapping
     *
     * @param a Mapping containing new links
     * @param b Reference mapping
     * @param maxSize Number of links to return. Simply set to infinity
     * all the links are needed
     * @return The unclassified elements from a that are not in b
     */
    public Mapping diff(Mapping a, Mapping b, int maxSize) {
        Mapping difference = new Mapping();
        int counter = 0;
        for (String uri1 : a.map.keySet()) {
            for (String uri2 : a.map.get(uri1).keySet()) {
                //get those in a but not in b
                if (!b.contains(uri1, uri2)) {
                    //important. Ensures that we only get entities in the diff
                    //that have not been classified yet
                    if (!positives.contains(uri1, uri2) && !negatives.contains(uri1, uri2)) {
                        difference.add(uri2, uri1, a.getSimilarity(uri1, uri2));
                        counter++;
                        //return difference as soon as it has reached maxSize;
                        if (counter == maxSize) {
                            //logger.info("********** Returning "+difference.size()+ " entities");
                            return difference;
                        }
                    }
                }
            }
        }
        //returns whole difference if size is below maxSize
        //logger.info("********** Returning "+difference.size()+ " entities");
        return difference;
    }

    public Configuration getCurrentConfig() {
        return config;
    }

    /** Returns the percentage of the known positives that are
     * classified as positive by this learner
     * @return
     */
    public double getPrecision() {
        double tp = 0;
        if(positives.size() == 0) return Double.NaN;
        for (String s : positives.map.keySet()) {
            for (String t : positives.map.get(s).keySet()) {
                if (results.getSimilarity(s, t) > 0) {
                    tp++;
                }
            }
        }
        return tp / positives.size();
    }

    public double getRecall() {
        return results.size();
    }
}
