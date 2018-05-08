/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.transformation.learner;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.query.CsvQueryModule;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;
import de.uni_leipzig.simba.transformation.dictionary.Dictionary;
import de.uni_leipzig.simba.transformation.dictionary.Rule;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 * @author ngonga
 */
public class TransformationLearner {

    /**
     * Learns rules directly from keys
     *
     * @param m Mapping
     * @return Dictionary containing all the rules
     */
    public Dictionary learnRules(Mapping m) {
        Dictionary dict = new Dictionary();
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                dict.addRules(s, t);
            }
        }
        return dict;
    }

    /**
     * Learn rules from source to target cache
     *
     * @param source Source cache
     * @param target Target cache
     * @param m Mapping between entries of source and target cache
     * @param sourceProperty Source Property
     * @param targetProperty Target property
     * @return Dictionary
     */
    public Dictionary learnRules(Cache source, Cache target, Mapping m, String sourceProperty, String targetProperty) {
        Dictionary dict = new Dictionary();
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                Instance sourceInstance = source.getInstance(s);
                Instance targetInstance = target.getInstance(t);
                if (sourceInstance != null && targetInstance != null) {
                    TreeSet<String> sourceValues = source.getInstance(s).getProperty(sourceProperty);
                    TreeSet<String> targetValues = target.getInstance(t).getProperty(targetProperty);
                    for (String sV : sourceValues) {
                        for (String sT : targetValues) {
                            dict.addRules(sV, sT);
                        }
                    }
                }
            }
        }
        //System.out.println(dict);
        return dict;
    }

    /**
     * Learns the best rules (i.e., the rules with maximal coverage) from source
     * to target and vice-verse
     *
     * @param source Source cache
     * @param target Target cache
     * @param m Mapping between source and target
     * @param sourceProperty Source property for which the transformation is to
     * be learned
     * @param targetProperty Target property for which the transformation is to
     * be learned
     * @return Set of rules
     */
    public Set<Rule> learnAllRules(Cache source, Cache target, Mapping m, String sourceProperty, String targetProperty) {
        Set<Rule> source2target = new HashSet<Rule>();
        Dictionary ruleDict = learnRules(source, target, m, sourceProperty, targetProperty);
        System.out.println("+++" + ruleDict.getRulesWithMaxCoverage());
        source2target.addAll(ruleDict.getRulesWithMaxCoverage().keySet());
        Set<Rule> target2source = new HashSet<Rule>();
        target2source.addAll(learnRules(target, source, m.reverseSourceTarget(), targetProperty, sourceProperty).getRulesWithMaxCoverage().keySet());
        boolean add;
        for (Rule r : source2target) {
            r.setFromSourceToTarget(true);
        }
        for (Rule r : target2source) {
            r.setFromSourceToTarget(false);
        }
        for (Rule r1 : target2source) {
            add = true;
            for (Rule r2 : source2target) {
                if (r2.getSource().equals(r1.getTarget()) && r2.getTarget().equals(r1.getSource())) {
                    add = false;
                }
            }
            if (add) {
                source2target.add(r1);
            }

        }
        return source2target;
    }

    /**
     * Takes in a cache and applies the learned transformation rules
     *
     * @param r Rule of the form X -> Y
     * @param source Cache to be updated
     * @param property Property where the values should be applied
     * @param source Checks whether is the source of target cache. If true, all
     * X will be replaces by Y. Else all Y by X.
     * @return The new cache.
     */
    public Cache applyRuleToCache(Rule r, Cache source, String property) {
        ArrayList<String> uris = source.getAllUris();
        String newValue;
        for (String uri : uris) {
            Instance s = source.getInstance(uri);
            TreeSet<String> values = s.getProperty(property);
            TreeSet<String> newValues = new TreeSet<String>();
            for (String value : values) {
                newValue = value.replaceAll(Pattern.quote(r.getSource()), r.getTarget());
                newValues.add(newValue);
            }
            //overwrite the old values
//            System.out.println(uri +" "+values+" "+newValues);
            s.replaceProperty(property, newValues);
        }
        //System.out.println("===========\n"+c);
        return source;
    }

    /**
     * Checks whether a pair was the source of a rule before applying it.
     * Resolve circles in learning rules
     *
     * @param r Rule
     * @param source Source cache
     * @param target Target cache
     * @param sourceProperty Source property
     * @param targetProperty Target property
     * @param m Mapping
     * @return Updated cache
     */
    public Cache applyRuleToCacheUsingMapping(Rule r, Cache source, Cache target, String sourceProperty, String targetProperty, Mapping m) {
        Set<String> sourceUris = m.map.keySet();
        String newValue;
        int count = 0;
        boolean checkSourceValue, checkTargetValue;
        TreeSet<String> sourceValues;
        Set<String> targetUris, targetValues;
        Instance s, t;
        for (String sourceUri : sourceUris) {
            checkSourceValue = false;
            s = source.getInstance(sourceUri);
            sourceValues = s.getProperty(sourceProperty);
            targetUris = m.map.get(sourceUri).keySet();
            //first check whether the premise of the rule is fulfilled by a value
            for (String value : sourceValues) {
                if (value.contains(r.getSource())) {
                    checkSourceValue = true;
                    break;
                }
            }
            if (checkSourceValue) {
                checkTargetValue = false;
                for (String targetUri : targetUris) {
                    t = target.getInstance(targetUri);
                    targetValues = t.getProperty(targetProperty);
                    for (String targetValue : targetValues) {
                        if (targetValue.contains(r.getTarget())) {
                            checkTargetValue = true;
                            break;
                        }
                    }
                    if (checkTargetValue) {
                        break;
                    }
                }
                if (checkTargetValue) {

                    count++;
                    TreeSet<String> newValues = new TreeSet<String>();
                    for (String value : sourceValues) {
                        newValue = value.replaceAll(Pattern.quote(r.getSource()), r.getTarget());
                        newValues.add(newValue);
                    }
                    //overwrite the old values
//            System.out.println(uri +" "+values+" "+newValues);
                    s.replaceProperty(sourceProperty, newValues);
                }
            }
        }
        if (count == 0) {
            System.out.println("WARN: No source for " + r);
        }
        return source;
    }

    public void runLearningIteration(Cache source, Cache target, Mapping m, String sourceProperty, String targetProperty) {
        Set<Rule> rules = learnAllRules(source, target, m, sourceProperty, targetProperty);
        for (Rule r : rules) {
            if (r.isFromSourceToTarget()) {
                source = applyRuleToCache(r, source, sourceProperty);
            } else {
                target = applyRuleToCache(r, target, targetProperty);
            }
        }
    }

    public double runSelfConfig(Cache source, Cache target, Mapping reference) {
        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, 0.6, 1.0);
//        DisjunctiveMeshSelfConfigurator bsc = new DisjunctiveMeshSelfConfigurator(source, target, 0.6, 1.0);
        bsc.setMeasure("reference");

//        bsc.MIN_THRESHOLD = 0.2;
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
        ComplexClassifier cc = bsc.getZoomedHillTop(5, 10, cp);
        Mapping m2 = cc.mapping;
        Mapping m3 = m2.getBestOneToNMapping();
        PRFCalculator prf = new PRFCalculator();
        double p = prf.precision(m2, reference);
        double r = prf.recall(m2, reference);
        String output = p + "\t" + r + "\t" + (2 * p * r / (p + r)) + "\n";
        System.out.println(output);
        return 2 * p * r / (p + r);
    }

    public static void main(String args[]) {
        KBInfo S = new KBInfo();
        S.endpoint = "E:/Work/Data/EAGLE/dbpedia-linkedmdb/source.csv";
        KBInfo T = new KBInfo();
        T.endpoint = "E:/Work/Data/EAGLE/dbpedia-linkedmdb/target.csv";
        CsvQueryModule qm = new CsvQueryModule(S);
        qm.setSeparation("\t");
        Cache source = new HybridCache();
        qm.fillAllInCache(source);

        CsvQueryModule qm2 = new CsvQueryModule(T);
        Cache target = new HybridCache();
        qm2.setSeparation("\t");
        qm2.fillAllInCache(target);

        Mapping reference = Mapping.readFromCsvFile("E:/Work/Data/EAGLE/dbpedia-linkedmdb/reference.csv");

        TransformationLearner tl = new TransformationLearner();

//        tl.runSelfConfig(source, target, reference);

        List<Double> result = new ArrayList<Double>();
        for (int i = 0; i < 20; i++) {
//            tl.runLearningIteration(source, target, m, "director", "director");
            Set<Rule> rules = tl.learnAllRules(source, target, reference, "director", "director");
            System.out.println(">> S -> T:" + rules);

            for (Rule rule : rules) {
                if (rule.isFromSourceToTarget()) {
//                    System.out.println(source);
                    source = tl.applyRuleToCacheUsingMapping(rule, source, target, "director", "director", reference);
//                    source = tl.applyRuleToCache(rule, source, "director");
//                    System.out.println(source);
//                    System.exit(1);
                } else {
                    target = tl.applyRuleToCacheUsingMapping(rule, target, source, "director", "director", reference.reverseSourceTarget());
//                    target = tl.applyRuleToCache(rule, target, "director");
                }
            }

            rules = tl.learnAllRules(source, target, reference, "label", "label");
            System.out.println(">>" + rules);

            for (Rule rule : rules) {
                if (rule.isFromSourceToTarget()) {
                    source = tl.applyRuleToCacheUsingMapping(rule, source, target, "label", "label", reference);
                } else {
                    target = tl.applyRuleToCacheUsingMapping(rule, target, source, "label", "label", reference.reverseSourceTarget());
                }
            }
            String measure = "trigrams";
            String property = "label";
            String measureExpression = measure+"(x." + property + ", y." + property + ")";
            ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");

            for (double iter = 0; iter < 5; iter++) {
                Instruction inst1 = new Instruction(Instruction.Command.RUN, measureExpression, 1 - 0.1 * iter + "", -1, -1, -1);
                Mapping data1 = ee.executeRun(inst1);
                property = "director";
                measureExpression = measure+"(x." + property + ", y." + property + ")";
                Instruction inst2 = new Instruction(Instruction.Command.RUN, measureExpression, 1 - 0.1 * iter + "", -1, -1, -1);
                Mapping data2 = ee.executeRun(inst2);
                Mapping data = SetOperations.intersection(data1, data2);
                
                PRFCalculator prf = new PRFCalculator();
                double p = prf.precision(data, reference);
                double r = prf.recall(data, reference);
                System.out.println("Iteration " + i + "\t Threshold " + (1 - 0.1 * iter) + "\t" + p + "\t" + r + "\t" + (2 * p * r / (p + r)));
            }
//            result.add(tl.runSelfConfig(source, target, reference));
        }
        System.out.println(result);
    }
}
