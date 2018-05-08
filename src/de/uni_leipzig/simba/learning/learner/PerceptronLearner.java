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
import de.uni_leipzig.simba.util.Utils;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class PerceptronLearner implements Learner {

    public double REGRESSION = 0.8;
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
    double learningRate;
    //constant for knowning when to terminate. It is the threshold for the
    //minimal weight change in configs
    double EPSILON = 0.1;

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
    public PerceptronLearner(KBInfo sInfo, KBInfo tInfo, Cache s, Cache t, Oracle o,
            Mapping propertyMapping, HashMap<String, String> propertyType,
            double a, double b) {
        //set variables
        oracle = o;
        source = s;
        target = t;
        if (source.size() < 10 || target.size() < 10) {
            logger.fatal("No enough instances in caches");
            System.exit(1);
        }
        sourceInfo = sInfo;
        targetInfo = tInfo;
        inquiries = 0;
        positives = new Mapping();
        negatives = new Mapping();
        //create initial config
        config = new LinearConfiguration(propertyMapping, propertyType);
        //getLinks for given config;
        mapper = SetConstraintsMapperFactory.getMapper("simple", targetInfo,
                sourceInfo, target, source, new LinearFilter(), 2);
        //cr.source, cr.target, source, target, new PPJoinMapper(), new LinearFilter());

        double threshold = config.threshold * REGRESSION;
        logger.info("Getting initial links with threshold " + threshold);
        results = new Mapping();
        while (results.size < 10) {

            results = mapper.getLinks(config.getExpression(), threshold);
            logger.info("Got " + results.size() + " initial links ...");
            if (results.size < 10) {
                logger.warn("Not enough instances to learn from");
                if (threshold < 0.1) {
                    logger.fatal("Threshold < 0.1. Exiting");
                    System.exit(1);
                } else {
                    threshold = threshold * REGRESSION;
                    logger.info("\n\nNew threshold = "+threshold);
                }
            }
        }

        logger.info(getPRF());
        oldResults = new Mapping();
        learningRate = (a + b) / 2;
    }

    /** Algorithm works as follows: Starts with wi = 1 and threshold alpha*n. Then
     * compute all pairs for theta >= alpha(n-1). Subsequently, sample 2n elements that
     * are closest to the fringe (n for each side). Apply perceptron learning and compute
     * new weights. Don't forget that we use perceptron with bias. Approach implemented tries
     * to find pairs which convey maximal information for classification.
     * @param n Numbers of possibly positives and negatives to consider
     * @return A new configuration.
     */
    public boolean computeNextConfig(int n) {
        //1. first get n most informative positives
        Mapping mostInfPos = getMostInformativePositives(results, config.threshold, positives, n / 2);
        Mapping mostInfNeg = getMostInformativeNegatives(results, config.threshold, negatives, n / 2);
        logger.info("\n\nMOSTINFPOS = " + mostInfPos);
        logger.info("\n\nMOSTINFNEG = " + mostInfNeg);

        Measure m;
        boolean answer;
        Parser parser;
        String term1, term2;
        //learned vector
        HashMap<String, Double> vector = new HashMap<String, Double>();

        //initialize learned vector
        for (String expression : config.mapping.keySet()) {
            vector.put(expression, 0.0);
        }
        vector.put("bias", 0.0);

        // For each most informative positive, add components to the learned
        // vector in case the oracle preaches it as to be a negative
        for (String expression : config.mapping.keySet()) {
            parser = new Parser(expression, 1.0);
            m = MeasureFactory.getMeasure(parser.op);
            term1 = parser.term1.substring(parser.term1.indexOf(".") + 1);
            term2 = parser.term2.substring(parser.term2.indexOf(".") + 1);

            System.out.println("***" + source.getAllUris().get(0));
            System.out.println(target.getAllUris().get(0)+"\n");
            
            for (String key : mostInfPos.map.keySet()) {
                for (String value : mostInfPos.map.get(key).keySet()) {
                    answer = oracle.ask(key, value);
                    if (!answer) {
                        System.out.println(key);
                        System.out.println(value);
                        vector.put(expression, vector.get(expression)
                                + m.getSimilarity(source.getInstance(key),
                                target.getInstance(value), term1, term2));                        
                    }
                }
            }
        }

        double examples = 0;
        //learn the bias and update the true negatives
        for (String key : mostInfPos.map.keySet()) {
            for (String value : mostInfPos.map.get(key).keySet()) {
                answer = oracle.ask(key, value);
                if (!answer) {
                    logger.info("(" + key + "," + value + ") should be negative");
                    vector.put("bias", vector.get("bias") + mostInfPos.getSimilarity(key, value));
                    negatives.add(key, value, 1.0);
                    examples++;
                }
            }
        }

        // For each most informative negative, add components to the learned
        // vector in case the oracle preaches it as to be a positive
        for (String expression : config.mapping.keySet()) {
            parser = new Parser(expression, 1.0);
            m = MeasureFactory.getMeasure(parser.op);
            term1 = parser.term1.substring(parser.term1.indexOf(".") + 1);
            term2 = parser.term2.substring(parser.term2.indexOf(".") + 1);

            for (String key : mostInfNeg.map.keySet()) {
                for (String value : mostInfNeg.map.get(key).keySet()) {
                    answer = oracle.ask(key, value);
                    if (answer) {
                        logger.info("(" + key + "," + value + ") should be positive");
                        vector.put(expression, vector.get(expression)
                                - m.getSimilarity(source.getInstance(key),
                                target.getInstance(value), term1, term2));
                    }
                }
            }
        }
        //learn the negative bias and update known positives
        for (String key : mostInfNeg.map.keySet()) {
            for (String value : mostInfNeg.map.get(key).keySet()) {
                answer = oracle.ask(key, value);
                if (answer) {
                    vector.put("bias", vector.get("bias") - mostInfNeg.getSimilarity(key, value));
                    positives.add(key, value, 1.0);
                    examples++;
                }
            }
        }
        //Now update the configuration by using the learning rate
        boolean moreSteps;
        logger.info("Update vector:\n" + vector);
        if (examples > 0) {
            moreSteps = true;
            for (String expression : config.mapping.keySet()) {
                config.mapping.put(expression, config.mapping.get(expression)
                        + learningRate * vector.get(expression) / examples);
//                if (java.lang.Math.abs(vector.get(expression)) * learningRate > EPSILON) {
//                                        moreSteps = true;
//                }
            }
            //Now update the threshold
            config.threshold = config.threshold + learningRate * vector.get("bias");
            oldResults = results;
            results = mapper.getLinks(config.getExpression(), config.threshold);
            logger.info(getPRF());
        } else {
            logger.info("Classifications were all correct. No need to update");
            moreSteps = false;
        }
        return moreSteps;
    }

    public HashMap<String, Double> getPRF()
    {
        return Utils.getPRF(oracle.getMapping(), results);
    }
    /** Returns the final results of the learner
     *
     */
    public Mapping returnFinalResults() {
        return mapper.getLinks(config.getExpression(), config.threshold);
    }

    /** Returns the pair whose similarity is above and yet closest to the threshold 
     * 
     * @param m Input mapping
     * @param threshold Value of similarity threshold
     * @param tabu List of pairs that should not be considered
     * @return A pair of URIs
     */
    public static Mapping getMostInformativePositives(Mapping m, double threshold, Mapping tabu, int n) {
        logger.info("Getting most informative positives with threshold " + threshold);
        Mapping mostInfPos = new Mapping();
        double min = Double.MAX_VALUE, sim = 0;
        String result[] = new String[2];
        String uri1, uri2;
        for (int i = 0; i < n; i++) {
            uri1 = "";
            uri2 = "";
            min = Double.MAX_VALUE;
            sim = 0;
            for (String key : m.map.keySet()) {
                for (String value : m.map.get(key).keySet()) {
                    if (!tabu.contains(key, value) && !mostInfPos.contains(key, value)) {
                        sim = m.getSimilarity(key, value);
                        //Important: Takes >= because of definition of matching
                        if ((sim >= threshold) && (sim - threshold) < min) {
                            min = sim - threshold;
                            logger.info("Min went down to " + min);
                            uri1 = key;
                            uri2 = value;
                            logger.info("Min valid for <" + uri1 + "> <" + uri2 + "> with sim " + sim);
                        }
                    }
                }
            }
            if (!uri1.equals("")) {
                logger.info("Adding <" + uri1 + "> <" + uri2 + "> with sim " + (threshold + min));
                mostInfPos.add(uri1, uri2, threshold + min);
            } else {
                break;
            }
        }
        logger.info("Found " + mostInfPos + " most informative positives");
        return mostInfPos;
    }

    /** Returns the pair whose similarity is below and yet closest to the threshold
     *
     * @param m Input mapping
     * @param threshold Value of similarity threshold
     * @param tabu List of pairs that should not be considered
     * @return A pair of URIs
     */
    public static Mapping getMostInformativeNegatives(Mapping m, double threshold, Mapping tabu, int n) {
        Mapping mostInfNeg = new Mapping();
        double min = Double.MAX_VALUE, sim = 0;
        String uri1, uri2;
        for (int i = 0; i < n; i++) {
            uri1 = "";
            uri2 = "";
            min = Double.MAX_VALUE;
            sim = 0;
            for (String key : m.map.keySet()) {
                for (String value : m.map.get(key).keySet()) {
                    if (!tabu.contains(key, value) && !mostInfNeg.contains(key, value)) {
                        sim = m.getSimilarity(key, value);
                        //Important: Takes >= because of definition of matching
                        if ((sim < threshold) && (threshold - sim) < min) {
                            min = threshold - sim;
                            uri1 = key;
                            uri2 = value;
                            logger.info("Min went down to " + min);
                            logger.info("Min valid for <" + uri1 + "> <" + uri2 + "> with sim " + sim);
                        }
                    }
                }
            }
            if (!uri1.equals("")) {
                logger.info("Adding <" + uri1 + "> <" + uri2 + "> with sim " + (threshold + min));
                mostInfNeg.add(uri1, uri2, threshold - min);
            } else {
                break;
            }
        }
        logger.info("Found " + mostInfNeg + " most informative negatives");
        return mostInfNeg;
    }

    public Configuration getCurrentConfig() {
        return config;
    }

    /** Return the ratio of the known positives that occur in the result set
     * by total number of known positives. Seems to be buggy.
     * @return
     */
    public double getPrecision() {
        double tp = 0;
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
