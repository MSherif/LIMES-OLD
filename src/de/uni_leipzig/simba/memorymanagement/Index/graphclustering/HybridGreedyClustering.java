/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.graphclustering;

import de.uni_leipzig.simba.memorymanagement.Index.planner.TSPSolver;
import de.uni_leipzig.simba.memorymanagement.indexing.HR3IndexItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ngonga
 */
public class HybridGreedyClustering extends NaiveClustering {

    @Override
    public Map<Integer, Cluster> cluster(Graph g, int maxSize) {

        Comparator<Edge> comparator = new Comparator<Edge>() {
            public int compare(Edge e1, Edge e2) {
                return e1.getWeight() - e1.getSource().getWeight() - e1.getTarget().getWeight()
                        - e2.getWeight() + e2.getSource().getWeight() + e2.getTarget().getWeight(); // use your logic
            }
        };

//init
        Set<Edge> availableEdges = new HashSet<>();
        int count = 0;
        for (Edge e : g.getAllEdges()) {
            //System.out.println("<" + e.source.item.getId() + ">" + " => <" + e.target.item.getId() + ">");
            //if (!(e.source.item.getId().equals(e.target.item.getId()))) {
            availableEdges.add(e);
            //}
        }
        Map<Integer, Cluster> result = new HashMap<>();
        Cluster cluster;
        while (!availableEdges.isEmpty()) {
//            System.out.println("Available edges:" + availableEdges);
            int clusterSize;
            Set<Edge> emanatingEdges = new HashSet<>();
            //get largest available edge and start cluster       
            List<Edge> sortedEdges = new ArrayList<>(availableEdges);
            //get largest available edge and start cluster       
            Collections.sort(sortedEdges, comparator);
            Edge e = sortedEdges.get(0);

            cluster = new Cluster(count);
            result.put(count, cluster);
            count++;

            cluster.nodes.add(e.source);
            cluster.nodes.add(e.target);
            cluster.edges.add(e);
            //itemClusterMap.get(e.source.toString()).add(clusters.size());
            //itemClusterMap.get(e.target.toString()).add(clusters.size());
            clusterSize = e.source.getWeight() + e.target.getWeight();
            if (clusterSize > maxSize) {
                System.err.println("Basic condition for scheduling broken. Updating max size to " + clusterSize);
                maxSize = clusterSize;
            }
            availableEdges.remove(e);

            //take all edges between two nodes in the cluster and add them to the
            //edges of the cluster
            Set<Edge> toRemove = new HashSet<>();
            for (Edge e2 : availableEdges) {
                if (cluster.nodes.contains(e2.source) && cluster.nodes.contains(e2.target)) {
                    cluster.edges.add(e2);
                    toRemove.add(e2);
                }
            }

            for (Edge e2 : toRemove) {
                availableEdges.remove(e2);
            }

            boolean stop = false;

            //only continue to cluster if maxSize is not reached
            while (clusterSize < maxSize && !stop) {

                //find edges that emanate and are still available
                emanatingEdges.addAll(g.getEdges(e.source));
                emanatingEdges.addAll(g.getEdges(e.target));
                emanatingEdges.retainAll(availableEdges);

                Node n;

                //get largest available edge that emanates from cluster
                if (!emanatingEdges.isEmpty()) {
//                    System.out.println("Emanating edges: "+emanatingEdges);
                    stop = true;
//                    Set<Edge> emanatingToAdd = new HashSet<>();
//                    Set<Edge> emanatingToRemove = new HashSet<>();

                    for (Edge em : emanatingEdges) {
                        //get node that is not yet in cluster
                        if (cluster.nodes.contains(em.source)) {
                            n = em.target;
                        } else {
                            n = em.source;
                        }
                        //add node to cluster and update size if n fits
                        if (clusterSize + n.getWeight() <= maxSize) {
                            //System.out.println("Adding " + n);
                            availableEdges.remove(em);
                            cluster.edges.add(em);

                            //take all edges between two nodes in the cluster and add them to the
                            //edges of the cluster
                            toRemove = new HashSet<>();
                            for (Edge e2 : availableEdges) {
                                if (cluster.nodes.contains(e2.source) && cluster.nodes.contains(e2.target)) {
                                    cluster.edges.add(e2);
                                    toRemove.add(e2);
                                }
                            }

                            for (Edge e2 : toRemove) {
                                availableEdges.remove(e2);
                            }

                            if (!cluster.nodes.contains(n)) {
                                clusterSize = clusterSize + n.getWeight();
                                cluster.nodes.add(n);
                                //itemClusterMap.get(n.toString()).add(clusters.size());
                            }
//                            System.out.println("Weight is now " + clusterSize);
                            stop = false;
                            break;
                        }
                        //update emanating edges
//                        if (n != null) {
//                            emanatingEdges.addAll(g.getEdges(n));
//                            emanatingEdges.retainAll(availableEdges);
//                        }
                    }

                } else {
                    stop = true;
                    break;
                }
            }
//            System.out.println("Available edges:" + availableEdges);

            //result.add(cluster);
        }
        return result;
    }

    public static void main(String args[]) {

        Graph g = new Graph();
        HR3IndexItem i1 = new HR3IndexItem(5, "11A");
        HR3IndexItem i2 = new HR3IndexItem(2, "12A");
        HR3IndexItem i3 = new HR3IndexItem(3, "13A");
        HR3IndexItem i4 = new HR3IndexItem(1, "14A");
        HR3IndexItem i5 = new HR3IndexItem(3, "15A");
        HR3IndexItem i6 = new HR3IndexItem(3, "16A");

        g.addEdge(i1, i1);
        g.addEdge(i1, i2);
        g.addEdge(i2, i2);
        g.addEdge(i2, i3);
        g.addEdge(i3, i4);
        g.addEdge(i5, i6);

        Clustering gc = new EdgeGreedyClustering();
        Map<Integer, Cluster> clusters = gc.cluster(g, 10);
        System.out.println(clusters);
        //System.out.println(gc.clusters);
        //System.out.println(gc.itemClusterMap);
        TSPSolver tsp = new TSPSolver();
        int[] path = tsp.getPath(tsp.getMatrix(clusters));
        for (int i = 0; i < path.length; i++) {
            System.out.print(path[i] + " -> ");
        }
        System.out.println();
    }
}
