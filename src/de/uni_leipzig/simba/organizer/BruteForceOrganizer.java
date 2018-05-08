/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.organizer;
import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.metricfactory.MetricFactory;
import de.uni_leipzig.simba.data.Instance;
import java.util.HashMap;
import java.util.ArrayList;
/**
 *
 * @author ngonga
 */
public class BruteForceOrganizer implements Organizer {

    Cache cache;
    int comparisons;
    double time;
    /**
     * Uses the brute force approach and compares all instances i in cache with
     * instance to find all instances i such that sim(i, instance) > threshold
     * @param instance A source instance
     * @param threshold Similarity threshold for computations
     * @param m MetricFactory to be used for comparisons
     * @return A map that contains the simiarity between the input instance and
     * all instances in the target knowledge base whose similarity is greater than
     * the threshold.
     */
    public HashMap<String, Double> getSimilarInstances(Instance instance, double threshold, MetricFactory m)
    {
        long currentTime = System.currentTimeMillis();
        HashMap<String, Double> map = new HashMap<String, Double>();
        ArrayList<String> uris = cache.getAllUris();
        double sim;
        for(int i=0; i<uris.size(); i++)
        {
            sim = m.getSimilarity(instance, cache.getInstance(uris.get(i)));
            if(sim >= threshold)
                map.put(uris.get(i), sim);
            comparisons++;
        }
        time = time + (System.currentTimeMillis() - currentTime);
        return map;
    }

    /** Does not compute any exemplars. Just stores the cache.
     *
     * @param c Cache
     * @param m Metric Factory to use for similarity computations
     * @param nrExemplars Proposed number of exemplars
     */
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars) {
        cache = c;
        comparisons = 0;
        time = 0;
    }

    /** Does not compute any exemplars. Just stores the cache.
     *
     * @param c Cache
     * @param m Metric Factory to use for similarity computations
     */
    public void computeExemplars(Cache c, MetricFactory m) {
        cache = c;
        comparisons = 0;
        time = 0;
    }

    public int getComparisons() {
        return comparisons;
    }

    public double getComparisonTime() {
        return time/1000;
    }

    public String getName() {
        return "bruteForce";
    }
}
