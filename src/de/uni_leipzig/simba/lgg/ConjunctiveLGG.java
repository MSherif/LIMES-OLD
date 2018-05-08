/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.lgg;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.selfconfig.Experiment;
import de.uni_leipzig.simba.selfconfig.LinearSelfConfigurator;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class ConjunctiveLGG implements LGG {

    public boolean STRICT = true;
    public int ITERATIONS_MAX = 1000;
    public double MIN_THRESHOLD = 0.4;
    public double learningRate = 0.9;
    Map<String, Double> sourcePropertiesCoverageMap; //coverage map for latter computations
    Map<String, Double> targetPropertiesCoverageMap;//coverage map for latter computations
    static Logger logger = Logger.getLogger("LIMES");
    double minCoverage;
    Cache source, target;
    Set<String> measures;
    Mapping reference;

    /**
     * ** TODO 
     * 1- Get relevant source and target resources from sample 
     * 2- Sample source and target caches 
     * 3- Run algorithm on samples of source and target 
     * 4- Get mapping function 
     * 5- Execute on the whole
     */
    /**
     * Constructor
     *
     * @param source
     * @param target
     * @param examples
     * @param minCoverage
     */
    public ConjunctiveLGG(Cache source, Cache target, Mapping examples, double minCoverage) {
        sourcePropertiesCoverageMap = LinearSelfConfigurator.getPropertyStats(source, minCoverage);
        targetPropertiesCoverageMap = LinearSelfConfigurator.getPropertyStats(target, minCoverage);
        this.minCoverage = minCoverage;
        this.source = source;
        this.target = target;
        measures = new HashSet<>();
        measures.add("jaccard");
        measures.add("trigrams");
        reference = examples;
    }

    public List<ExtendedClassifier> getAllInitialClassifiers() {
    	logger.info("Geting all initial classifiers ...");
        List<ExtendedClassifier> initialClassifiers = new ArrayList<>();
        for (String p : sourcePropertiesCoverageMap.keySet()) {
            for (String q : targetPropertiesCoverageMap.keySet()) {
                for (String m : measures) {
//                    System.out.println("Getting classifier for " + p + ", " + q + ", " + m + ".");
                    ExtendedClassifier cp = getInitialClassifier(p, q, m);
                    //only add if classifier covers all entries
                    initialClassifiers.add(cp);
                }
            }
        }
        logger.info("Done computing all initial classifiers.");
        return initialClassifiers;
    }

    /* (non-Javadoc)
     * @see de.uni_leipzig.simba.lgg.LGG#getMapping()
     * 
     * run conjunctive merge
     */
    public Mapping getMapping() {
        Mapping result;
        List<ExtendedClassifier> classifiers = getAllInitialClassifiers();
        result = classifiers.get(0).mapping;
        for (int i = 1; i < classifiers.size(); i++) {
            result = SetOperations.union(result, classifiers.get(i).mapping);
        }
        return result;
    }
    
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.lgg.LGG#getMetricExpression()
	 */
	@Override
	public String getMetricExpression() {
		String result = new String();
        List<ExtendedClassifier> classifiers = getAllInitialClassifiers();
        result = classifiers.get(0).getMetricExpression();
        for (int i = 1; i < classifiers.size(); i++) {
        	result += "OR(" + result+ "," + classifiers.get(i).getMetricExpression() + ")|0";
        }
        return result;
	}
    
//	childMetricExpr += "OR(" + childSubMetricExpr.get(0)+ "," + childSubMetricExpr.get(1) + ")|0";


    /**
     * Computes the atomic classifiers by finding the highest possible F-measure
     * achievable on a given property pair
     *
     * @param source Source cache
     * @param target Target cache
     * @param sourceProperty Property of source to use
     * @param targetProperty Property of target to use
     * @param measure Measure to be used
     * @param reference Reference mapping
     * @return Best simple classifier
     */
    private ExtendedClassifier getInitialClassifier(String sourceProperty, String targetProperty, String measure) {
        double maxOverlap = 0;
        double theta = 1.0;
        Mapping bestMapping = new Mapping();
        PRFCalculator prf = new PRFCalculator();
        for (double threshold = 1d; threshold > MIN_THRESHOLD; threshold = threshold * learningRate) {
            Mapping mapping = execute(sourceProperty, targetProperty, measure, threshold);
            double overlap = prf.recall(mapping, reference);
            if (maxOverlap < overlap) //only interested in largest threshold with recall 1
            {
                bestMapping = mapping;
                theta = threshold;
                maxOverlap = overlap;
                bestMapping = mapping;
//                System.out.println("Works for " + sourceProperty + ", " + targetProperty + ", " + threshold + ", " + overlap);
            }
        }
        ExtendedClassifier cp = new ExtendedClassifier(measure, theta);
        cp.fMeasure = maxOverlap;
        cp.sourceProperty = sourceProperty;
        cp.targetProperty = targetProperty;
        cp.mapping = bestMapping;
        return cp;
    }

    //runs an atomic mapper
    public Mapping execute(String sourceProperty, String targetProperty, String measure, double threshold) {
        String measureExpression = measure + "(x." + sourceProperty + ", y." + targetProperty + ")";
        Instruction inst = new Instruction(Instruction.Command.RUN, measureExpression, threshold + "", -1, -1, -1);
        ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
        return ee.executeRun(inst);
    }

    /**
     * Computes a sample of the reference dataset for experiments
     */
    public static Mapping sampleReference(Mapping reference, double fraction) {
        int mapSize = reference.map.keySet().size();
        if (fraction > 1) {
            fraction = 1 / fraction;
        }
        int size = (int) (mapSize * fraction);
        Set<Integer> index = new HashSet<>();
        //get random indexes
        for (int i = 0; i < size; i++) {
            int number;
            do {
                number = (int) (mapSize * Math.random()) - 1;
            } while (index.contains(number));
            index.add(number);
        }

        //get data
        Mapping sample = new Mapping();
        int count = 0;
        for (String key : reference.map.keySet()) {
            if (index.contains(count)) {
                sample.map.put(key, reference.map.get(key));
            }
            count++;
        }
        return sample;
    }

    public static void main(String args[]) {
        ////      //DBLP-ACM
        Cache source = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP2.csv");
        Cache target = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-ACM/ACM.csv");
        Mapping reference = Experiment.readReference("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv");
        Mapping sample = sampleReference(reference, 0.1);
        ConjunctiveLGG clgg = new ConjunctiveLGG(source, target, reference, 0.6);
        Mapping result = clgg.getMapping();
        System.out.println("scource=" + source.size() + " target=" + target.size() + "ref= " + reference.size());

        PRFCalculator prf = new PRFCalculator();
        System.out.println("Precision = " + prf.precision(result, reference));
        System.out.println("Recall = " + prf.recall(result, reference));
        System.out.println("F-measure = " + prf.fScore(result, reference));
    }


}
