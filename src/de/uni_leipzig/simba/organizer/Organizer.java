/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.organizer;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.metricfactory.MetricFactory;
import de.uni_leipzig.simba.data.Instance;
import java.util.HashMap;
/**
 * Interface for oganizers
 * @author ngonga
 */
public interface Organizer {
    /** Computes a set of exemplars of size nrExemplars
     *
     * @param c Cache containing the knowledge base
     * @param m MetricFactory to use during the exemplar computation
     * @param nrExemplars Number of exemplars
     */
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars);

    /** Computes a set of exemplars and decides automatically on the right size
     *
     * @param c Cache containing the knowledge base
     * @param m MetricFactory to use during the exemplar computation
     */
    public void computeExemplars(Cache c, MetricFactory m);

    /** Returns a map containing the URI (ID) and the similarity of each instance i of the
     * knowledge base stored in c such that sim(instance, i) >= threshold
     * @param instance
     * @param threshold
     * @param metric
     * @return A map containing the URI (ID) and the similarity of each instance i
     * such that sim(instance, i) >= threshold
     */
    public HashMap<String, Double> getSimilarInstances(Instance instance, double threshold,
            MetricFactory metric);

    /** Returns the number of comparisons carried out by the organizer
     *
     * @return Number of comparisons
     */
    public int getComparisons();
    
    /** Returns the time spent on comparing
     * 
     * @return Time spent on comparing
     */
    public double getComparisonTime();

    public String getName();

}
