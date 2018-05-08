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
 *
 * @author ngonga
 */
public class DensityBasedOrganizer extends LimesOrganizer {

    @Override
    public String getName() {
        return "densityBased";
    }

    @Override
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars) {
        Logger logger = Logger.getLogger("LIMES");
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

        ///////////////////////////////////////
        //now for the second exemplar
        ///////////////////////////////////////
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

        //assign new exemplar to list
        exemplar = c.getInstance(uris.get(index));
        distances = new ArrayList<Double>();

        for (int i = 0; i < c.size(); i++) {
            distance = 1 - m.getSimilarity(exemplar, c.getInstance(uris.get(i)));
            comparisons++;
            distances.add(distance);
        }
        exemplarDistanceMap.put(exemplar, distances);
        exemplarList.add(exemplar);

        // compute the mapping between nodes and the two exemplars to get the weight
        // for the distance computation
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
                if (min > distance) {
                    min = distance;
                    index = j;
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

        ///////////////////////////////////////
        //now for the others
        ///////////////////////////////////////
        while (exemplarList.size() < nrExemplars) {
            max = 0;
            index = 0;
            theoreticalMax = exemplarList.size() * exemplarList.size();
            // get instance at maximal total weighted distance of all exemplars
            for (int i = 0; i < c.size(); i++) {
                distance = 0;
                //if (!exemplarList.contains(c.getInstance(uris.get(i)))) {
                    for (int j = 0; j < exemplarList.size(); j++) {
                        //key difference to LimesOrganizer. The distance is a function of
                        //the number of nodes assigned to the exemplar. The more nodes
                        //it has assigned, the higher its gravity to exemplars, i.e., the
                        //longer the distances in its subspace
                        distance = distance
                                + exemplarDistanceMap.get(exemplarList.get(j)).get(i) 
                                / (double) exemplarMap.get(exemplarList.get(j)).size();
                        lookups = lookups + 2;
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
                //}
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

// compute the mapping between nodes and exemplars to get the weight
            // for the distance computation
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

        }

        //give out the distribution of nodes to exemplars
        /*
        logger.info("Computed " + exemplarList.size() + " exemplars");
        Iterator<Instance> iter = exemplarMap.keySet().iterator();
        Instance instance;
        while (iter.hasNext()) {
        instance = iter.next();
        logger.info(instance + " -> " + exemplarMap.get(instance).size());
        }
         */


        logger.info("Reorganizing cache took " + comparisons + " comparisons.");
        logger.info("Computed " + exemplarList.size() + " exemplars.");

        logger.info("Reorganizing was carried out in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");
        
        Iterator<Instance> iter = exemplarMap.keySet().iterator();
        while (iter.hasNext()) {
        Instance key = iter.next();
        logger.info(key.getUri() +" -> "+exemplarMap.get(key).size());
        }
         
    }
}
