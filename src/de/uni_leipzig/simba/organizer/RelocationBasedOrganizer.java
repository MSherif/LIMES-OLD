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
 * Relocates the exemplars computed by the LimesOrganizer to achieve better
 * distance approximations.
 * @author ngonga
 */
public class RelocationBasedOrganizer extends LimesOrganizer {

    HashMap<Instance, TreeSet<Instance>> newExemplarMap;
    long relocationTime = 0;
    Logger logger = Logger.getLogger("LIMES");
    //HashMap<Instance, ArrayList<Float>> newExemplarDistanceMap;
    @Override
    public String getName() {
        return "relocationBased";
    }

    /** Idea: Might have to do something completely different when computing the
     * exemplars. Instead of just using the distance, we might have to weight them
     * according to the number of children they have so as to tackle the density
     * problem that we seem to have. Apparently, some exemplars have tons of
     * children while some have only a few. Work for Stanley!
     * @param c
     * @param m
     * @param nrExemplars
     */
    @Override
    public void computeExemplars(Cache c, MetricFactory m, int nrExemplars) {
        HashMap<Instance, TreeSet<Instance>> updatedMap = new HashMap<Instance, TreeSet<Instance>>();
        super.computeExemplars(c, m, nrExemplars);
        // start measuring time for relocation
        // also measure number of comparisons
        int relocationComparisons = 0;
        long currentTime = System.currentTimeMillis();
        Iterator<Instance> exemplars = exemplarMap.keySet().iterator();
        Instance key, newExemplar;

        //1. reinitialize
        newExemplarMap = new HashMap<Instance, TreeSet<Instance>>();


        //2. fill the new maps
        while (exemplars.hasNext()) {
            key = exemplars.next();
            relocationComparisons = relocationComparisons + relocateExemplar(key, exemplarMap.get(key), m);
            logger.info("Relocation comparisons at " + relocationComparisons);
        }

        //3. rename the maps so that everything works as usual
        exemplarMap = newExemplarMap;
        relocationTime = (System.currentTimeMillis() - currentTime);

        //4. write some stats
        logger.info("Relocation took " + relocationTime / 1000 + " seconds.");
        logger.info("Relocation took " + relocationComparisons + " comparisons.");
        comparisons = comparisons + relocationComparisons;
        comparisonTime = comparisonTime + relocationTime;
    }

    private int relocateExemplar(Instance exemplar, TreeSet<Instance> children, MetricFactory m) {

        //if exemplar has no or only child then do nothing
        if (children.size() < 2) {
            newExemplarMap.put(exemplar, children);
            return 0;
        }
        // else
        logger.info("Input: " + exemplar + " -> " + children.size() + " children");
        ArrayList<Instance> childrenList = new ArrayList(children);
        childrenList.add(exemplar);
        int relocationComparisons = 0;

        int size = childrenList.size();
        double similarityMatrix[][] = new double[size][size];

        //1. Compute distance matrix
        for (int i = 0; i < size; i++) {
            similarityMatrix[i][i] = 0;
            for (int j = i + 1; j < size; j++) {
                similarityMatrix[i][j] = m.getSimilarity(childrenList.get(i), childrenList.get(j));
                similarityMatrix[j][i] = similarityMatrix[i][j];
                relocationComparisons++;
            }
        }

        //2. get instance at maximal total similarity to the others
        double maxSimilarity = 0;
        double similarity;
        int index = 0;
        for (int i = 0; i < size; i++) {
            similarity = 0;
            for (int j = 0; j < size; j++) {
                similarity = similarity + similarityMatrix[i][j];
            }
            if (similarity > maxSimilarity) {
                index = i;
                maxSimilarity = similarity;
            }
        }

        //3. swap old and new exemplar
        //children.remove(childrenList.get(index));
        //children.add(exemplar);

        //4. write the new distances
        childrenList.get(index).distance = -1;

        Iterator<Instance> iter = children.iterator();
        TreeSet<Instance> newChildren = new TreeSet<Instance>();
        Instance child;
        int indexOfChild;
        while (iter.hasNext()) {
            child = iter.next();
            indexOfChild = childrenList.indexOf(child);
            if (indexOfChild != index) {
                child.distance = 1 - similarityMatrix[index][indexOfChild];
                newChildren.add(child);
            }
        }

        logger.info("Output: " + childrenList.get(index) + " -> " + children.size() + " children");
        newExemplarMap.put(childrenList.get(index), newChildren);
        return relocationComparisons;
    }
}
