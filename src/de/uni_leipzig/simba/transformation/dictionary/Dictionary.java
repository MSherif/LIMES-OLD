/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.transformation.dictionary;

import java.util.*;
import de.uni_leipzig.simba.transformation.stringops.StringOps;

/**
 *
 * @author ngonga
 */
public class Dictionary {

    Map<String, Map<String, Double>> sourceTargetMap;

    public Dictionary() {
        sourceTargetMap = new HashMap<String, Map<String, Double>>();
    }

    /**
     * Adds a rule to the dictionary
     *
     * @param r Rule to be added
     */
    public void addRule(Rule r) {
        if (sourceTargetMap.containsKey(r.source)) {
            if (sourceTargetMap.get(r.source).containsKey(r.target)) {
                sourceTargetMap.get(r.source).put(r.target, sourceTargetMap.get(r.source).get(r.target) + 1D);
            } else {
                sourceTargetMap.get(r.source).put(r.target, 1D);
            }
        } else {
            sourceTargetMap.put(r.source, new HashMap<String, Double>());
            sourceTargetMap.get(r.source).put(r.target, 1D);
        }
    }

    public int size() {
        return sourceTargetMap.size();
    }

    /**
     * DEPRECATED
     */
//    public void updateDictionary(String source, String target) {
//
//        List<String> sourceSubStrings = StringOps.getAllSubstrings(source);
//        List<String> targetSubStrings = StringOps.getAllSubstrings(target);
//        List<String> commonSubStrings = new ArrayList<>();
//        for (String s : sourceSubStrings) {
//            if (targetSubStrings.contains(s)) {
//                commonSubStrings.add(s);
//            }
//        }
//        for (int i = 0; i < sourceSubStrings.size(); i++) {
//
//            for (int j = 0; j < targetSubStrings.size(); j++) {
//                if (!sourceSubStrings.get(i).equalsIgnoreCase(targetSubStrings.get(j))) {
//                    addRule(new Rule(sourceSubStrings.get(i), targetSubStrings.get(j)));
//                }
//            }
//            addRule(new Rule(sourceSubStrings.get(i), Rule.EPSILON));
//        }
//    }
    public Set<String> generateRuleParts(String[] splits, Set<String> tabus) {
        Set<String> ruleParts = new HashSet<String>();
        for (int i = 0; i < splits.length; i++) {
            String ruleSource = "";
            for (int j = i; j < splits.length; j++) {
                if (!tabus.contains(splits[j])) {
                    ruleSource = ruleSource + StringOps.SEPARATOR + splits[j];
                    ruleParts.add(ruleSource.trim());
                } else {
                    break;
                }
            }
        }
        return ruleParts;
    }

    public void addRules(String source, String target) {
        String sourceSplit[] = source.split(StringOps.SEPARATOR);
        String targetSplit[] = target.split(StringOps.SEPARATOR);
        Set<String> commonSubStrings = new TreeSet<String>();

        for (String s : sourceSplit) {
            for (String t : targetSplit) {
                if (s.equals(t)) {
                    commonSubStrings.add(s);
                }
            }
        }

        Set<String> sourceRuleParts = generateRuleParts(sourceSplit, commonSubStrings);
        Set<String> targetRuleParts = generateRuleParts(targetSplit, commonSubStrings);

        for (String s : sourceRuleParts) {
            for (String t : targetRuleParts) {
                addRule(new Rule(s, t));
            }
//            addRule(new Rule(s, Rule.EPSILON));
        }
    }

    public Map<Rule, Double> getMostFrequentRules() {
        double max = 0;
        Map<Rule, Double> result = new HashMap<Rule, Double>();
        for (String source : sourceTargetMap.keySet()) {
            for (String target : sourceTargetMap.get(source).keySet()) {
                double value = sourceTargetMap.get(source).get(target);
                if (value > max) {
                    result = new HashMap<Rule, Double>();
                    result.put(new Rule(source, target), value);
                    max = value;
                } else if (value == max) {
                    result.put(new Rule(source, target), value);
                }
            }
        }
        return result;

    }

    public Map<Rule, Double> getRulesWithMaxCoverage() {
        double max = 0;
        Map<Rule, Double> result = new HashMap<Rule, Double>();
        for (String source : sourceTargetMap.keySet()) {
            for (String target : sourceTargetMap.get(source).keySet()) {
                double l1 = 0, l2 = 0;
                if (!source.equalsIgnoreCase(Rule.EPSILON)) {
                    l1 = source.split(StringOps.SEPARATOR).length;
                }
                if (!target.equalsIgnoreCase(Rule.EPSILON)) {
                    l2 = target.split(StringOps.SEPARATOR).length;
                }

                double value = sourceTargetMap.get(source).get(target) * (l1 + l2);
                if (value > max) {
                    result = new HashMap<Rule, Double>();
                    result.put(new Rule(source, target), value);
                    max = value;
                } else if (value == max) {
                    result.put(new Rule(source, target), value);
                }
            }
        }
        return result;

    }

    public String toString() {
        return sourceTargetMap.toString();
    }
}
