/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.SetOperations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ngonga
 */
public class DisjunctiveMeshSelfConfigurator extends MeshBasedSelfConfigurator{
    /**
     * Constructor
     *
     * @param source Source cache
     * @param target Target cache
     * @param beta Beta value for computing F_beta
     * @param minCoverage Minimal coverage for a property to be considered for
     * linking
     *
     */
    public DisjunctiveMeshSelfConfigurator(Cache source, Cache target, double minCoverage, double beta) {
        super(source, target, minCoverage, beta);        
    }
    
    /** Actually computes the union of all mappings
     * 
     * @param mappings
     * @return 
     */
    public static Mapping getUnion(List<Mapping> mappings) 
    {
       Mapping m = new Mapping();
       for(Mapping mapping: mappings)
       {
           m = SetOperations.union(m, mapping);
       }
       return m;
    }
    
    

    @Override
    public ComplexClassifier getZoomedHillTop(int gridPoints, int iterations, List<SimpleClassifier> sc) {
        logger.info("Beginning self-configuration process ... ");
        //first iteration
        if (gridPoints < 5) {
            gridPoints = 5;
        }
        ComplexClassifier cc = getHillTop(gridPoints, sc);
        ComplexClassifier bestCc = cc;
        double delta = 1.0 / ((double) gridPoints - 1.0);
        for (int i = 1; i < iterations; i++) {
            if (cc.fMeasure == 1) {
                return cc;
            }
            logger.info("Current F-score = " + cc.fMeasure);
            logger.info("Current delta = " + delta);

            List<Double> min = new ArrayList<Double>();
            List<Double> max = new ArrayList<Double>();

            for (int j = 0; j < cc.classifiers.size(); j++) {
                //fill min
                if (cc.classifiers.get(j).threshold >= delta) {
                    min.add(cc.classifiers.get(j).threshold - delta);
                } else {
                    min.add(0.0);
                }
                //fill max
                if (cc.classifiers.get(j).threshold + delta >= 1) {
                    max.add(1.0);
                } else {
                    max.add(cc.classifiers.get(j).threshold + delta);
                }
            }
            //get best classifier from the grid
            cc = getHillTop(min, max, gridPoints, cc.classifiers);
            // remember the best overall classifier
            if (bestCc.fMeasure <= cc.fMeasure) {
                bestCc = cc;
            } else {
                cc = bestCc;
            }
            delta = 2 * delta / ((double) gridPoints - 1);
        }
        logger.info("Final F-score = " + bestCc.fMeasure);
        return bestCc;
    }
    
    @Override
    public ComplexClassifier getZoomedHillTop(int gridPoints, long duration, List<SimpleClassifier> sc) {
        logger.info("Beginning self-configuration process ... ");
        //first iteration
        if (gridPoints < 5) {
            gridPoints = 5;
        }
        long start = System.currentTimeMillis();
        ComplexClassifier cc = getHillTop(gridPoints, sc);
        ComplexClassifier bestCc = cc;
        double delta = 1.0 / ((double) gridPoints - 1.0);
        for (int i = 1; i > 0; i++) {
            if (cc.fMeasure == 1) {
                return cc;
            }
            if((System.currentTimeMillis()-start)/1000 >= duration)
            	break;
            
            logger.info("Current F-score = " + cc.fMeasure);
            logger.info("Current delta = " + delta);

            List<Double> min = new ArrayList<Double>();
            List<Double> max = new ArrayList<Double>();

            for (int j = 0; j < cc.classifiers.size(); j++) {
                //fill min
                if (cc.classifiers.get(j).threshold >= delta) {
                    min.add(cc.classifiers.get(j).threshold - delta);
                } else {
                    min.add(0.0);
                }
                //fill max
                if (cc.classifiers.get(j).threshold + delta >= 1) {
                    max.add(1.0);
                } else {
                    max.add(cc.classifiers.get(j).threshold + delta);
                }
            }
            //get best classifier from the grid
            cc = getHillTop(min, max, gridPoints, cc.classifiers);
            // remember the best overall classifier
            if (bestCc.fMeasure <= cc.fMeasure) {
                bestCc = cc;
            } else {
                cc = bestCc;
            }
            delta = 2 * delta / ((double) gridPoints - 1);
        }
        logger.info("Final F-score = " + bestCc.fMeasure);
        return bestCc;
    }
    
    @Override
    public ComplexClassifier getHillTop(List<Double> min, List<Double> max, int n, List<SimpleClassifier> sc) {
        logger.info("Getting hill top for dimensions described in " + sc);
        //first generate coordinates of points in the mesh
        //these basically give the thresholds for each of the classifiers
        List<List<Double>> coordinates = generateCoordinates(min, max, n);
        logger.info("Generated "+coordinates.size()+" coordinates ...");
        //then generate mappings for each of the classifiers
        // the access to the map is classifier_index -> threshold -> mapping
        Map<Double, Map<Double, Mapping>> mappings = new HashMap<Double, Map<Double, Mapping>>();
        for (int i = 0; i < sc.size(); i++) {
            mappings.put(new Double(i), getMappings(min.get(i), max.get(i), n, sc.get(i)));
        }
        //get list of best classifiers
        double bestF = -1;
        List<List<Double>> highestPoints = new ArrayList<List<Double>>();
        Mapping bestMapping = new Mapping();
        Mapping m = new Mapping();
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> meshPoint = coordinates.get(i);
            List<Mapping> currentMappings = new ArrayList<Mapping>();
            //get the mappings for the current meshpoint
            for (int j = 0; j < meshPoint.size(); j++) {
                // take the jth classifier and its threshold
                if (meshPoint.get(j) > 0) {
                    currentMappings.add(mappings.get(new Double(j)).get(meshPoint.get(j)));
                }
            }
            double f;
            if (currentMappings.isEmpty()) {
                f = 0.0;
            } else {
                m = getUnion(currentMappings);
                f = computeQuality(m);
//                f = _measure.getPseudoFMeasure(source.getAllUris(), target.getAllUris(), m, beta);
            }
            if (f > bestF) {
                highestPoints = new ArrayList<List<Double>>();
                highestPoints.add(meshPoint);
                bestF = f;
                bestMapping = m;
            } else if (f == bestF) {
                highestPoints.add(meshPoint);
            }
        }
        //we have found the best points. Return the point with the highest total coordinates
        double bestSum = -1;
        List<Double> bestPoint = null;
        //if solution unique return it
        if (highestPoints.size() == 1) {
            bestPoint = highestPoints.get(0);
        } //else return solution with highest sum of thresholds. Reason is simply
        //that we want to be biased towards precision
        else {
            if (STRATEGY.toLowerCase().startsWith("max")) {
                for (int i = 0; i < highestPoints.size(); i++) {
                    List<Double> point = highestPoints.get(i);
                    double sum = 0;
                    for (int j = 0; j < point.size(); j++) {
                        sum = sum + point.get(j);
                    }
                    if (sum > bestSum) {
                        bestPoint = point;
                        bestSum = sum;
                    }
                }
            } else {
                bestSum = sc.size();
                for (int i = 0; i < highestPoints.size(); i++) {
                    List<Double> point = highestPoints.get(i);
                    double sum = 0;
                    for (int j = 0; j < point.size(); j++) {
                        sum = sum + point.get(j);
                    }
                    if (sum < bestSum) {
                        bestPoint = point;
                        bestSum = sum;
                    }
                }
            }
        }
        List<SimpleClassifier> scList = new ArrayList<SimpleClassifier>();
//
//        if (bestPoint.size() < sc.size()) {
//            logger.fatal("Size error");
//            logger.fatal(bestPoint);
//            logger.fatal(sc);
//            System.exit(1);
//        } else {
//            logger.info(">>" + sc);
//            logger.info(">>" + bestPoint);
//        }
        for (int i = 0; i < sc.size(); i++) {
            if (bestPoint.get(i) > 0) {
                scList.add(sc.get(i).clone());
                scList.get(scList.size() - 1).threshold = bestPoint.get(i);
            }
        }
        ComplexClassifier cc = new ComplexClassifier(scList, bestF);
        cc.mapping = bestMapping;
        System.out.println("Best Classifier: " + cc.classifiers);
        System.out.println("Highest Point: " + bestPoint);
        //System.out.println("Best Mapping: " + cc.mapping);
        System.out.println("FMeasure = " + bestF);
        return cc;
    }
    
    /** Runs classifiers and retrieves the correspoding mappings
     * 
     * @param classifiers List of classifiers
     * @return Mapping generated by the list of classifiers
     */
    @Override
    public Mapping getMapping(List<SimpleClassifier> classifiers) {
        List<Mapping> mappings = new ArrayList<Mapping>();
        for (int i = 0; i < classifiers.size(); i++) {
            Mapping m = executeClassifier(classifiers.get(i), classifiers.get(i).threshold);
            mappings.add(m);
        }
        Mapping result = getUnion(mappings);
        //System.out.println("***" + classifiers + "\n" + mappings + " => " + result + "***");
        return result;
    }
}
