/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.graphclustering;

import java.util.HashSet;
import java.util.Set;

/**
 * Clusters gather a set of nodes and edges that are equivalent to data entries
 * and the corresponding tasks
 * @author ngonga
 */
public class Cluster {
    public Set<Node> nodes;
    public Set<Edge> edges;
    public int id;
    public Cluster(int number)
    {
        nodes = new HashSet<>();
        edges = new HashSet<>();
        id = number;
    }
    
    /** 
     * Get weight of the cluster
     * @return Total weight of nodes in cluster
     */
    public int getWeight()
    {
        int weight = 0;
        for(Node n:nodes)
            weight = weight + n.getWeight();
        return weight;
    }
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Nodes: ");
        for(Node n: nodes)
            buffer.append("["+n.toString()+","+n.getWeight()+"] ");
        buffer.append("\nEdges");
        for(Edge e: edges)
            buffer.append(e.toString()+", ");
        buffer.append("\n");
        return buffer.toString();
    }
}
