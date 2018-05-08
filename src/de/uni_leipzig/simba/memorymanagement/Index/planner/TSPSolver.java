/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.planner;

import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Cluster;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Node;
import de.uni_leipzig.simba.memorymanagement.TSPSolver.GenericSimulatedAnnealing;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author ngonga
 */
public class TSPSolver {

    double epsilon = 10d;

    /*
     * Compute TSP graph from clustering result
     *
     */
    public double[][] getMatrix(List<Set<Node>> clusters, Map<String, Set<Integer>> itemClusterMap) {
        double[][] result = new double[clusters.size()][clusters.size()];

        //init matrix with epsilon
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.size(); j++) {
                result[i][j] = epsilon;
            }
        }

        //compute overlap
        for (String s : itemClusterMap.keySet()) {
            Set<Integer> relevantClusterIndexes = itemClusterMap.get(s);
            for (int i : relevantClusterIndexes) {
                for (int j : relevantClusterIndexes) {
                    if (i < j && result[i][j] == epsilon) {
                        Set<Node> iCluster = clusters.get(i);
                        Set<Node> jCluster = clusters.get(j);
                        Set<Node> resultCluster = new TreeSet<Node>();
                        for (Node n : iCluster) {
                            resultCluster.add(n);
                        }
                        resultCluster.retainAll(jCluster);
                        int weight = 0;
                        for (Node n : resultCluster) {
                            weight = weight + n.getWeight();
                        }
                        result[i][j] = 1d / (double) weight;
                        result[j][i] = 1d / (double) weight;
                    }
                }
            }
        }

        return result;
    }

    public static void printMatrix(double[][] m) {
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                System.out.print(m[i][j] + "\t");
            }
            System.out.println();
        }
    }

    public int[] getPath(double[][] m) {
        return (new GenericSimulatedAnnealing(m).simulatedAnnealing(1, 0.99, 0.1, false));
    }

    public int[] getPath(double[][] m, int iterations) {
        return (new GenericSimulatedAnnealing(m).simulatedAnnealing(iterations, 0.99, 0.1, false));
    }
    /**
     * Dependency matrix between clusters
     *
     * @param clusters
     * @return
     */
    public double[][] getMatrix(Map<Integer, Cluster> clusters) {

        //initialize with heavy edges
        double[][] matrix = new double[clusters.keySet().size()][clusters.keySet().size()];
        for (int i = 0; i < clusters.keySet().size(); i++) {
            for (int j = 0; j < clusters.keySet().size(); j++) {
                if (i != j) {
                    matrix[i][j] = epsilon;
                }
            }
        }

        for (int i : clusters.keySet()) {
            Cluster a = clusters.get(i);
            for (int j : clusters.keySet()) {
                if (i < j) {
                    int weight = 0;
                    Cluster b = clusters.get(j);
                    for (Node n : a.nodes) {
                        if (b.nodes.contains(n)) {
                            weight = weight + n.getWeight();
                        }
                    }
                    if (weight > 0) {
                        matrix[i][j] = 1d / weight;
                        matrix[j][i] = matrix[i][j];
                    }
                }
            }
        }
        //printMatrix(matrix);
        return matrix;
    }
}
