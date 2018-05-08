/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.graphclustering;

/**
 * Weighted edge in a graph
 * @author ngonga
 */
public class Edge implements Comparable {

    Node source;

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public int getWeight() {
        return weight;
    }
    Node target;
    int weight;

    public Edge(Node source, Node target, int weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    //we use the inverse natural order. Thus, a higher weight leads to a lower rank
    public int compareTo(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge) o;
            if(e.weight > weight) return +1;
            else if(e.weight < weight) return -1;
            else return 0;
        }
        return 0;
    }
    
    public String toString()
    {
        return "(S:"+source+", T:"+target+", W:"+weight+")";
    }
}
