/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.graphclustering;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import java.util.*;

/**
 *
 * @author ngonga
 */
public class Graph {

    Set<Node> allNodes;
    Set<Edge> allEdges;
    Map<Edge, Set<Node>> edgeNodeMap;
    Map<String, Set<Edge>> nodeEdgeMap;
    Map<String, Node> nodeMap;

    public Graph() {
        allNodes = new HashSet<>();
        allEdges = new HashSet<>();
        edgeNodeMap = new HashMap<>();
        nodeEdgeMap = new HashMap<>();
        nodeMap = new HashMap<>();
    }

    public void addNode(IndexItem a) {
        if (nodeMap.containsKey(a.getId().toString())) {
        } else {
            Node n = new Node(a);
            allNodes.add(n);
            nodeMap.put(a.getId().toString(), n);
            nodeEdgeMap.put(a.getId().toString(), new TreeSet<Edge>());
        }
    }

    public void addEdgeOnly(IndexItem a, IndexItem b) {
        if (nodeMap.containsKey(a.getId().toString()) && nodeMap.containsKey(b.getId().toString())) {
            Node source = nodeMap.get(a.getId().toString());
            Node target = nodeMap.get(b.getId().toString());
            Set<Node> nodes = new TreeSet<>();
            nodes.add(source);
            nodes.add(target);
            Edge e = new Edge(source, target, source.getWeight() * target.getWeight());
            edgeNodeMap.put(e, nodes);
            if (!nodeEdgeMap.containsKey(a.getId().toString())) {
                nodeEdgeMap.put(a.getId().toString(), new TreeSet<Edge>());
            }
            if (!nodeEdgeMap.containsKey(b.getId().toString())) {
                nodeEdgeMap.put(b.getId().toString(), new TreeSet<Edge>());
            }
            nodeEdgeMap.get(a.getId().toString()).add(e);
            if (!a.getId().toString().equals(b.getId().toString())) {
                nodeEdgeMap.get(b.getId().toString()).add(e);
            }
            allEdges.add(e);
        }
        else
        {
            System.err.println("Could not find "+a.getId().toString()+" or "+b.getId().toString());
        }
    }

    public void addEdge(Node a, Node b, int weight) {
        Edge e = new Edge(a, b, weight);
        allEdges.add(e);
        Set<Node> nodes = new HashSet<>();
        nodes.add(a);
        nodes.add(b);
        allNodes.addAll(nodes);
        edgeNodeMap.put(e, nodes);
        if (!nodeEdgeMap.containsKey(a.toString())) {
            nodeEdgeMap.put(a.getItem().getId().toString(), new HashSet<Edge>());
        }
        if (!nodeEdgeMap.containsKey(b.toString())) {
            nodeEdgeMap.put(b.getItem().getId().toString(), new HashSet<Edge>());
        }
        nodeEdgeMap.get(a.getItem().getId().toString()).add(e);
        nodeEdgeMap.get(b.getItem().getId().toString()).add(e);
        //System.out.println(nodeEdgeMap);
    }

    public void addEdge(IndexItem a, IndexItem b) {
        addEdge(new Node(a), new Node(b), a.getSize() * b.getSize());
    }

    public Set<Node> getNodes(Edge e) {
        return edgeNodeMap.get(e);
    }

    public Set<Edge> getEdges(Node n) {
        return nodeEdgeMap.get(n.getItem().getId().toString());
    }

    public Set<Edge> getAllEdges() {
        return allEdges;
    }

    public Set<Node> getAllNodes() {
        return allNodes;
    }
    
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        for (Edge e: getAllEdges())
        {
            result.append(e.source + "\t" +e.target+"\t"+e.weight+"\n");
        }
        return result.toString();
    }
}
