/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.organizer;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.ArrayList;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.metricfactory.MetricFactory;
import de.uni_leipzig.simba.data.Instance;
import org.apache.log4j.*;

/**
 * This class implements the core of LIMES. It compute the exemplars for the
 * knowledge base in cache c by using a given metric a
 * @author ngonga
 */
public class LimesOrganizer implements Organizer {

    HashMap<Instance, TreeSet<Instance>> exemplarMap;
    HashMap<Instance, ArrayList<Double>> exemplarDistanceMap;
    ArrayList<String> uris;
    public int comparisons;
    public int lookups;
    public long comparisonTime;

    public LimesOrganizer() {
        comparisons = 0;
        lookups = 0;
        comparisonTime = 0;
    }

    /**
     * @return The number of comparisons that the organizer carried out so far.
     */
    public int getComparisons() {
        return comparisons;
    }

    /**
     * Computes the exemplars for the data contained in the cache c.
     * @param c Cache containing the instances
     * @param m Metric for computing the exemplars
     * @param nrExemplars Number of exemplars
     */
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars) {
        long startTime = System.currentTimeMillis();
        uris = c.getAllUris();

        double max, theoreticalMax, min, distance;
        int index = 0;
        // ensure that we have at least two exemplars
        if (nrExemplars <= 2) {
            nrExemplars = 2;
        }
        // ensure that we don't have more exemplars than entities
        if (nrExemplars > c.size()) {
            nrExemplars = c.size();
        }

        // list of all exemplars and their distance to entities
        exemplarDistanceMap = new HashMap<Instance, ArrayList<Double>>();
        // list of all exemplars
        ArrayList<Instance> exemplarList = new ArrayList<Instance>();

        // set the first exemplar
        Instance exemplar = c.getInstance(uris.get(0));

        // compute distance to all other instances
        ArrayList<Double> distances = new ArrayList<Double>();
        max = 0;
        for (int i = 0; i < uris.size(); i++) {
            distance = 1 - m.getSimilarity(exemplar, c.getInstance(uris.get(i)));
            comparisons++;
            distances.add(distance);
            /* We might need the radius when we reorganize, yet it is not needed for
             * this function
            if (distance > max) {
            max = distance;
            }
             */
        }
        //e.radius = max;
        exemplarDistanceMap.put(exemplar, distances);
        exemplarList.add(exemplar);
        //now for all others

        while (exemplarList.size() < nrExemplars) {
            max = 0;
            index = 0;
            theoreticalMax = exemplarList.size();
            // get instance at maximal total distance of all exemplars
            for (int i = 0; i < c.size(); i++) {
                distance = 0;
                for (int j = 0; j < exemplarList.size(); j++) {
                    distance = distance + exemplarDistanceMap.get(exemplarList.get(j)).get(i);
                    lookups++;
                }
                if (distance > max) {
                    max = distance;
                    index = i;
                    //does not change the number of comparisons, yet reduces the
                    // number of lookups significantly
                    if (max == theoreticalMax) {
                        break;
                    }
                }
            }

            //index of instance at maximal distance of all exemplar is index. Make
            // that to a new exemplar

            exemplar = c.getInstance(uris.get(index));
            distances = new ArrayList<Double>();

            for (int i = 0; i < c.size(); i++) {
                distance = 1 - m.getSimilarity(exemplar, c.getInstance(uris.get(i)));
                comparisons++;
                distances.add(distance);
            }
            exemplarDistanceMap.put(exemplar, distances);
            exemplarList.add(exemplar);
        }

        // finally map the exemplars to the nodes of which they are exemplars
        // first create empty maps
        exemplarMap = new HashMap<Instance, TreeSet<Instance>>();
        for (int i = 0; i < exemplarList.size(); i++) {
            exemplarMap.put(exemplarList.get(i), new TreeSet<Instance>());
        }

        //then assign each exemplar to a set of nodes
        for (int i = 0; i < uris.size(); i++) {
            distance = 0;
            min = 1;
            index = 0;
            for (int j = 0; j < exemplarList.size(); j++) {
                distance = exemplarDistanceMap.get(exemplarList.get(j)).get(i);
                lookups++;
                //System.out.println("Distance from "+exemplarList.get(j) +" to "+terms.get(i)+" is "+distance);
                if (min > distance) {
                    min = distance;
                    index = j;
                    //System.out.println("Updated min to "+distance+" and exemplar to "+exemplarList.get(j));
                }
            }
            // distance = 0 means that we just measured the similarity of the exemplar
            // with itself. No need to store the exemplar in the queue of nodes that are most similar to it
            // save space and time complexity
            if (min > 0) {
                c.getInstance(uris.get(i)).distance = min;
                exemplarMap.get(exemplarList.get(index)).add(c.getInstance(uris.get(i)));
            }
        }
        Logger logger = Logger.getLogger("LIMES");
        logger.info("Reorganizing cache took " + comparisons + " comparisons.");
        logger.info("Reorganizing was carried out in " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds.");

        /*
        Iterator<Instance> iter = exemplarMap.keySet().iterator();

        while (iter.hasNext()) {
            Instance key = iter.next();
            logger.info(key.getUri() + " -> " + exemplarMap.get(key));
        }
         *
         */

    }

    /**
     * Computes the exemplars for the data contained in the cache c. Sets the number
     * of exemplars to sqrt(c.size())
     * @param c Cache containing the instances
     * @param m Metric for computing the exemplars
     */
    public void computeExemplars(Cache c, MetricFactory m) {
        int size = (int) java.lang.Math.sqrt((double) c.size());
        computeExemplars(c, m, size);
    }

    /** This method retrieves all the instances whose similarity to the input instance
     * is above the threshold. This method use the following:
     * the approximation of the similarity is always larger than the
     * real similarity. Thus, if the approximation gets smaller than
     * the threshold, we know that the real value will also be smaller
     * than the threshold. Furthermore, the list is sorted. Thus, we know that
     * if the approximation for an instance is larger than the threshold
     * then it will be the case for all the instances that follow
     * @param instance Instance from the target knowledge base of which we need the similar instances
     * in the source knowlegde base
     * @param threshold Threshold for the similarity
     * @param metric MetricFactory to use for similarity computations
     * @return Similarity of instance and all instances contained in the target
     * whose similarity with instance is greater than threshold.
     */
    public HashMap<String, Double> getSimilarInstances(Instance instance, double threshold,
            MetricFactory metric) {
        long startTime = System.currentTimeMillis();
        // Iterator for all the exemplars
        Iterator<Instance> exemplarIterator = exemplarMap.keySet().iterator();
        Instance exemplar, example;
        TreeSet<Instance> instancesInSubspace;
        HashMap<String, Double> result = new HashMap<String, Double>();
        boolean end;

        //run for each exemplar
        while (exemplarIterator.hasNext()) {
            exemplar = exemplarIterator.next();
            instancesInSubspace = exemplarMap.get(exemplar);
            Iterator<Instance> examplesIterator = instancesInSubspace.iterator();
            double basicSim = metric.getSimilarity(instance, exemplar);
            double sim;
            comparisons++;

            //if basicSim >= threshold, then we should write out the exemplar
            if (basicSim >= threshold) {
                result.put(exemplar.getUri(), basicSim);
            }

            end = false;
            while (examplesIterator.hasNext()) {
                // get next example
                example = examplesIterator.next();
                // compute similarity approximation and terminate as soon as
                // approximation is below threshold
                if (basicSim + example.distance >= threshold) {
                    sim = metric.getSimilarity(instance, example);
                    comparisons++;
                    if (sim >= threshold) {
                        result.put(example.getUri(), sim);
                    }
                }
                /**
                else {
                    break;
                }*/
            }
        }
        comparisonTime = comparisonTime + (System.currentTimeMillis() - startTime);
        return result;
    }

    public double getComparisonTime()
    {
        return comparisonTime/1000;
    }

    public String getName() {
        return "limes";
    }

}
