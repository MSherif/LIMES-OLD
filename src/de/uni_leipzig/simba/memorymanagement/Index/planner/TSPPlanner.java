/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.planner;

import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Cluster;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Edge;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Graph;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.NaiveClustering;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Node;
import de.uni_leipzig.simba.memorymanagement.datacache.AbstractCache;
import de.uni_leipzig.simba.memorymanagement.datacache.CacheType;
import de.uni_leipzig.simba.memorymanagement.datacache.DataCacheFactory;
import de.uni_leipzig.simba.memorymanagement.indexing.Hr3Indexer;
import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;
import de.uni_leipzig.simba.memorymanagement.indexing.IndexerType;

import java.io.File;
import java.util.*;

//import com.hp.hpl.jena.sparql.lib.CacheFactory;

/**
 *
 * @author ngonga
 */
public class TSPPlanner implements DeduplicationPlanner {

	static AbstractCache cache=null;
    int comparisons;

    /**
     * Generate a data access and comparison plan based on the results of a
     * clustering
     *
     * @param clusters Set of clusters generated by a clustering algorithm
     * @param path Path to walk through the clusters
     * @return A list of commands to be executed by the engine
     */
    public List<DataManipulationCommand> plan(Map<Integer, Cluster> clusters, int[] path) {
        List<DataManipulationCommand> result = new ArrayList<>();
        if (!clusters.isEmpty()) {
            Cluster c;
            int totalSize = 0;

            //first get all elements of first entry into memory            
            c = clusters.get(path[0]);
            Set<IndexItem> oldItems = getItemsFromCluster(c);
            result.add(new DataManipulationCommand(DataOperator.LOAD, new ArrayList<>(oldItems)));
            //System.out.println("Current cache: " + oldItems);
            //get the computations
            addCompareOperations(c, result);

            Set<IndexItem> newItems, currentCache;
            for (int i = 1; i < path.length; i++) {
                c = clusters.get(path[i]);
                newItems = getItemsFromCluster(c);
                currentCache = getItemsFromCluster(c);
                // get nodes that have not yet been loaded
                newItems.removeAll(oldItems);
                // get nodes that should be flushed
                oldItems.removeAll(currentCache);
                //remove unnecessary data then load new data
                result.add(new DataManipulationCommand(DataOperator.FLUSH, new ArrayList<>(oldItems)));
                //the following might not be needed. Done automatically by the compare operations
                // result.add(new DataManipulationCommand(DataOperator.LOAD, new ArrayList<>(newItems)));
                //get computations
                //System.out.println("Current cache: " + currentCache);
                addCompareOperations(c, result);
                oldItems = currentCache;
            }
        }
        return result;
    }

    /**
     * Get items from nodes in cluster c
     *
     * @param c Cluster
     * @return Items from nodes n
     */
    public Set<IndexItem> getItemsFromCluster(Cluster c) {
        Set<IndexItem> items = new HashSet<>();
        for (Node n : c.nodes) {
            items.add(n.getItem());
        }
        return items;
    }

    /**
     * Adds comparisons to the list of commands to be executed
     *
     * @param c Cache
     * @param commands List of commands to update
     */
    public void addCompareOperations(Cluster c, List<DataManipulationCommand> commands) {
        for (Edge edge : c.edges) {
            IndexItem s = edge.getSource().getItem();
            IndexItem t = edge.getTarget().getItem();
            List<IndexItem> toCompare = new ArrayList<>();
            toCompare.add(s);
            toCompare.add(t);
            commands.add(new DataManipulationCommand(DataOperator.COMPARE, toCompare));
            //System.out.println("Compare: " + new DataManipulationCommand(DataOperator.COMPARE, toCompare));
        }
    }

    public List<DataManipulationCommand> run(File data, Map<String, String> parameters, IndexerType indexerType, int capacity) {
        comparisons = 0;
        List<DataManipulationCommand> result = new ArrayList<>();
        Indexer indexer;

        //first init indexers
        if (indexerType == IndexerType.HR3) {
            indexer = new Hr3Indexer(Integer.parseInt(parameters.get("alpha")),
                    Double.parseDouble(parameters.get("threshold")));
        } else {
            System.err.println("Error generating indexer. Using HR3");
            indexer = new Hr3Indexer(Integer.parseInt(parameters.get("alpha")),
                    Double.parseDouble(parameters.get("threshold")));
        }

        // now index source and target
        indexer.runIndexing(data);

        // now generate, cluster and tsp the task graph        
        Graph g = indexer.generateTaskGraph();
        System.out.println(g);
        NaiveClustering gc = new NaiveClustering();
//        System.out.println("Nodes in graph:" + g.getAllNodes().size());
//        System.out.println("Edges in graph:" + g.getAllEdges().size());
        Map<Integer, Cluster> clusters = gc.cluster(g, capacity);
//        System.out.println("Clusters: " + gc.getClusters());
        TSPSolver tsp = new TSPSolver();
        //TSPSolver.printMatrix(tsp.getMatrix(gc.getClusters(), gc.getItemClusterMap()));
        int[] path = tsp.getPath(tsp.getMatrix(clusters));
        //for(int i=0; i<path.length; i++)
        //    System.out.print(path[i]+" => "+gc.getClusters().get(path[i])+"\t");
        //System.out.println();
        List<Cluster> clusterSequence = new ArrayList<>();

        // get the sequence of clusters
        for (int i = 0; i < path.length; i++) {
            clusterSequence.add(clusters.get(path[i]));
        }

        //transform this sequence into commands for the engine
        List<IndexItem> oldItems = new ArrayList<>(), newItems;
        if (!clusterSequence.isEmpty()) {
            int totalSize = 0;
            //first get all elements of first entry into memory
            for (Node n : clusterSequence.get(0).nodes) {
                oldItems.add(n.getItem());
                totalSize = totalSize + n.getItem().getSize();
            }
            result.add(new DataManipulationCommand(DataOperator.LOAD, oldItems));
//            System.out.println("Size = " + totalSize);

            // now for all the remaining in the cluster sequence
            Set<Edge> visitedEdges = new HashSet<Edge>();

            for (int i = 1; i < clusterSequence.size(); i++) {

                //first get the corresponding items
                newItems = new ArrayList<IndexItem>();
                for (Node n : clusterSequence.get(i).nodes) {
                    newItems.add(n.getItem());
                }

                //now check which items should be flushed, i.e. those in old items but not in new items
                List<IndexItem> toFlush = new ArrayList<IndexItem>();
                int delta = 0;
                for (IndexItem ii : oldItems) {
                    if (!newItems.contains(ii)) {
                        toFlush.add(ii);
                        delta = delta - ii.getSize();
                    }
                }
                if (!toFlush.isEmpty()) {
                    result.add(new DataManipulationCommand(DataOperator.FLUSH, toFlush));
                }

                //check which items should be added
                //now check which items should be flushed, i.e. those in old items but not in new items
                List<IndexItem> toAdd = new ArrayList<IndexItem>();
                for (IndexItem ii : newItems) {
                    if (!oldItems.contains(ii)) {
                        toAdd.add(ii);
                        delta = delta + ii.getSize();
                    }
                }
                if (!toAdd.isEmpty()) {
                    result.add(new DataManipulationCommand(DataOperator.LOAD, toAdd));
                }

                // now add the execution commands
                result.addAll(getComputationsFromGraph(g, clusterSequence.get(i).nodes, visitedEdges));
                //overwrite new items with old items
                oldItems = newItems;
                totalSize = totalSize + delta;
//                System.out.println("Total size of cache = " + totalSize);
            }
        }
//        System.out.println("Number of comparisons: " + comparisons);
        return result;
    }

    /**
     * Generates plan based on a graph of tasks
     * @param g Input graph
     * @return Sequence of tasks
     */
    public List<DataManipulationCommand> plan(Graph g) {
        List<DataManipulationCommand> commands = new ArrayList<DataManipulationCommand>();
        for (Edge e : g.getAllEdges()) {
            List<IndexItem> items = new ArrayList();
            items.add(e.getSource().getItem());
            items.add(e.getTarget().getItem());
            commands.add(new DataManipulationCommand(DataOperator.COMPARE, items));
        }
        return commands;
    }

    List<DataManipulationCommand> getComputationsFromGraph(Graph g, Set<Node> nodes, Set<Edge> visitedEdges) {
        List<DataManipulationCommand> commands = new ArrayList<DataManipulationCommand>();//store list og command like load, compare
        List<Node> nodeList = new ArrayList(nodes); //store list of graph's nodes
        //for each node in the graph
        for (int i = 0; i < nodeList.size(); i++) {
        	//for each node predecessor to this node including itself
            for (int j = 0; j <= i; j++) {
            	//get set of edges related to this node
                Set<Edge> edges = g.getEdges(nodeList.get(i));
                for (Edge e : edges) {
                    if (g.getNodes(e).contains(nodeList.get(j)) && !visitedEdges.contains(e)) {
/*                        List<IndexItem> items = new ArrayList<IndexItem>();
                        if(cache.contains(nodeList.get(i).getItem().getId()));
                        items.add(nodeList.get(i).getItem());
                        items.add(nodeList.get(j).getItem());
                        commands.add(new DataManipulationCommand(DataOperator.COMPARE, items));
                        visitedEdges.add(e);
                        comparisons++;*/
                        List<IndexItem> items = new ArrayList<IndexItem>();
                        IndexItem NodeiId = nodeList.get(i).getItem();
                        IndexItem NodejId = nodeList.get(j).getItem();
                        //get the item from cache
                        if(cache.contains(NodeiId.getId()))
                        {
                        	items= new ArrayList<IndexItem>();
                        	items.add(NodeiId);
                            commands.add(new DataManipulationCommand(DataOperator.LOAD, items));
                        }
                        else if (cache.contains(NodejId.getId()))
                        {
                        	items= new ArrayList<IndexItem>();
                        	items.add(NodejId);
                            commands.add(new DataManipulationCommand(DataOperator.LOAD, items));
                        }
                        else
                        {
                           	items= new ArrayList<IndexItem>();
                        	items.add(NodeiId);
                        	items.add(NodejId);
                            commands.add(new DataManipulationCommand(DataOperator.COMPARE, items));
                        }
                        visitedEdges.add(e);
                        comparisons++;
                    }
                }
            }
        }
        return commands;
    }
    
    List<DataManipulationCommand> getComputationsFromGraph2(Graph g, Set<Node> nodes, Set<Edge> visitedEdges) {
        List<DataManipulationCommand> commands = new ArrayList<DataManipulationCommand>();//store list og command like load, compare
        List<Node> nodeList = new ArrayList(nodes); //store list of graph's nodes
        //for each node in the graph
        for (int i = 0; i < nodeList.size(); i++) {
        	//for each node predecessor to this node including itself
            for (int j = 0; j <= i; j++) {
            	//get set of edges related to this node
                Set<Edge> edges = g.getEdges(nodeList.get(i));
                for (Edge e : edges) {
                    if (g.getNodes(e).contains(nodeList.get(j)) && !visitedEdges.contains(e)) {
/*                        List<IndexItem> items = new ArrayList<IndexItem>();
                        if(cache.contains(nodeList.get(i).getItem().getId()));
                        items.add(nodeList.get(i).getItem());
                        items.add(nodeList.get(j).getItem());
                        commands.add(new DataManipulationCommand(DataOperator.COMPARE, items));
                        visitedEdges.add(e);
                        comparisons++;*/
                    	List<IndexItem> items = new ArrayList<IndexItem>();
                        if(cache.contains(nodeList.get(i).getItem().getId()));
                        items.add(nodeList.get(i).getItem());
                        items.add(nodeList.get(j).getItem());
                        commands.add(new DataManipulationCommand(DataOperator.COMPARE, items));
                        visitedEdges.add(e);
                        comparisons++;
                    }
                }
            }
        }
        return commands;
    }

    public static void test() {
    	cache =  (AbstractCache) DataCacheFactory.createCache(CacheType.FIFO, 10, 1,1000);
        TSPPlanner tsp = new TSPPlanner();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("alpha", "4");
        parameters.put("threshold", "1");
        System.out.println(tsp.run(new File("/home/mofeed/Projects/Caching/testdata1000.txt"),
                parameters, IndexerType.HR3, 20));
    }

    public static void main(String args[]) {
        test();
    }
}