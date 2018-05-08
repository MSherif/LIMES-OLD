/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.graphclustering;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for algorithms that cluster tasks into clusters that can be contained
 * in the memory at one go
 * @author ngonga
 */
public interface Clustering {
    Map<Integer, Cluster> cluster(Graph g, int capacity);
    Set<Cluster> getClusters();
    //Map<String, Set<Integer>> getItemClusterMap();
}
