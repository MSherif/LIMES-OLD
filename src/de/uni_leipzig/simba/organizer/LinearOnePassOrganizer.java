/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.organizer;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.metricfactory.MetricFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author Axel
 */
public class LinearOnePassOrganizer extends LimesOrganizer {

    public double similarityThreshold = 0.2;

    @Override
    //we could misuse nrExemplars by using it as the percentage for the similarity
    //threshold. I.e. 1 -> 0.01 while 100 ->1
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars) {
        Logger logger = Logger.getLogger("LIMES");
        logger.info("Similarity threshold for exemplars was set to " + similarityThreshold);
        long startTime = System.currentTimeMillis();
        uris = c.getAllUris();
        exemplarMap = new HashMap<Instance, TreeSet<Instance>>();
        double max, theoreticalMax, min, distance;
        int index;
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
        exemplarList.add(exemplar);
        exemplarMap.put(exemplar, new TreeSet<Instance>());
        // compute distance to all other instances
        ArrayList<Double> distances = new ArrayList<Double>();
        double value;
        for (int i = 1; i < uris.size(); i++) {
            //logger.info(i+" Processing " + uris.get(i) + "with " + exemplarList.size() + " exemplars");
            max = 0;
            index = -1;
            for (int j = 0; j < exemplarList.size(); j++) {
                value = m.getSimilarity(exemplarList.get(j), c.getInstance(uris.get(i)));
                comparisons++;
                if (value >= max) {
                    max = value;
                    index = j;
                }
            }

            if (max <= similarityThreshold || index == -1) {
                //logger.info("New Exemplar was created for " + uris.get(i));
                exemplarList.add(c.getInstance(uris.get(i)));
                //logger.info("Number of exemplars is up to " + exemplarList.size());
                exemplarMap.put(c.getInstance(uris.get(i)), new TreeSet<Instance>());
            } else {
                //logger.info("Adding " + uris.get(i) + " to " + exemplarList.get(index).getUri());
                c.getInstance(uris.get(i)).distance = 1 - max;
                exemplarMap.get(exemplarList.get(index)).add(c.getInstance(uris.get(i)));
            }
        }

        //System.out.println(exemplarMap);
        logger.info("Reorganizing cache took " + comparisons + " comparisons.");
        logger.info("Reorganizing cache generated " + exemplarList.size() + " exemplars.");
        logger.info("Reorganizing was carried out in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");

        //for (Instance instance : exemplarMap.keySet()) {
        //    logger.info(instance.getUri() + "->" + exemplarMap.get(instance).size());
        //}
    }

    @Override
    public void computeExemplars(Cache c, MetricFactory m) {
        int size = (int) java.lang.Math.sqrt((double) c.size());
        computeExemplars(c, m, size);
    }

    @Override
    public String getName() {
        return "LinearOrganizerOnePass";
    }
}
