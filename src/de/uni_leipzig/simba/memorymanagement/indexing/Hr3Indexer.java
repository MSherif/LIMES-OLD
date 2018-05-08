/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.measures.space.EuclideanMetric;
import de.uni_leipzig.simba.measures.space.blocking.HR3Blocker;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Cluster;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Clustering;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Graph;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.NodeGreedyClustering;
import de.uni_leipzig.simba.memorymanagement.Index.planner.DataManipulationCommand;
import de.uni_leipzig.simba.memorymanagement.Index.planner.TSPPlanner;
import de.uni_leipzig.simba.memorymanagement.Index.planner.TSPSolver;
import de.uni_leipzig.simba.memorymanagement.Index.planner.execution.CacheAccessExecution;
import de.uni_leipzig.simba.memorymanagement.datacache.CacheType;
import de.uni_leipzig.simba.memorymanagement.datacache.DataCache;
import de.uni_leipzig.simba.memorymanagement.datacache.DataCacheFactory;
import de.uni_leipzig.simba.memorymanagement.datacache.SimpleCache;

import java.io.*;
import java.util.*;

/**
 * Indexes data for HR3 computation
 *
 * @author ngonga
 */
public class Hr3Indexer implements Indexer {

    public static String splitToken = "\t";
    int alpha;
    double threshold;
    public String baseFolder = "/home/mofeed/Projects/Caching/baseFolder/";
    public int beginColumn = 1;
    public int endColumn = 2;
    public Set<IndexItem> index;
    public Map<String, IndexItem> keyToIndexMap;
    public HR3Blocker blocker;

    /**
     * Initialize indexer
     *
     * @param alpha Alpha value
     * @param threshold Threshold for indexing
     */
    public Hr3Indexer(int alpha, double threshold) {
        this.alpha = alpha;
        this.threshold = threshold;
        init();
    }

    public Hr3Indexer(int alpha, double threshold, String baseFolder) {
        this.alpha = alpha;
        this.threshold = threshold;
        this.baseFolder = baseFolder;
        init();
    }

    private void init() {
        //create base folder if it does not exist
        File folder = new File(baseFolder);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        } //create base folder
        else {
            folder.mkdirs();
        }
    }

    /**
     * Generates index for given character-separated file that contains spatial
     * data Return null in case of error
     *
     * @param input Character-separated file
     * @return Set of index items
     */
    public void runIndexing(File input, String folder, boolean header) {
        baseFolder = folder;
        runIndexing(input, header);
    }

    /**
     * Generates index for given character-separated file that contains spatial
     * data Return null in case of error
     *
     * @param input Character-separated file
     * @return Set of index items
     */
    public void runIndexing(File input, boolean header) {
        String s = "", label;
        try {
            // names of files in which the data is to be distributed. Basically the index
            Map<String, Integer> fileNames = new HashMap<>();
            BufferedReader in = new BufferedReader(new FileReader(input));
            s = in.readLine();
            //skip header
            if (header) {
                s = in.readLine();
            }
            String[] split;
            List<Integer> indexItem;
            int indexEntry;

            while (s != null) {
                split = s.split(splitToken);
                //String uri = split[0];
                indexItem = new ArrayList<>();
                label = "";
                if (endColumn > split.length - 1) {
                    endColumn = split.length - 1;
                }
                for (int i = beginColumn; i <= endColumn; i++) {
                    try {
                        indexEntry = (int) Math.floor((Double.parseDouble(split[i]) * alpha) / threshold);
                        indexItem.add(indexEntry);
                        label = label + indexEntry + "A";
                    } catch (Exception e2) {
                        System.err.println(s);
                        e2.printStackTrace();
                    }
                }
                //check whether we already have a file with that index
                //if not create file
                if (!fileNames.containsKey(label)) {
                    fileNames.put(label, 0);
                    File f = new File(baseFolder + "/" + label);
                    f.createNewFile();
                }
                //write data into file and increment the count of resources
                write(s, label);
                fileNames.put(label, fileNames.get(label) + 1);
                s = in.readLine();
            }
            index = new HashSet<>();
            keyToIndexMap = new HashMap<>();
            //now create the index items
            for (String key : fileNames.keySet()) {
                IndexItem ii = new HR3IndexItem(fileNames.get(key), key);
                index.add(ii);
                keyToIndexMap.put(key, ii);
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println(e.getMessage());
        }

//        System.out.println("Index size = " + index.size());
//        System.out.println("Index map size = " + keyToIndexMap.keySet().size());
        blocker = new HR3Blocker(alpha, endColumn - beginColumn + 1);
    }

    // We generate new index items with exactly the right id.
    public Set<IndexItem> getItemsToCompare(IndexItem ii) {
        ArrayList<ArrayList<Integer>> ids = blocker.getBlocksToCompare(((HR3IndexItem) ii).coordinates);
        Set<IndexItem> result = new TreeSet<>();
        for (ArrayList<Integer> id : ids) {
            String label = generateKey(id);
            if (keyToIndexMap.containsKey(label)) {
                result.add(keyToIndexMap.get(generateKey(id)));
            }
        }
        return result;
    }

    public Graph generateTaskGraph() {
        Graph g = new Graph();
        int edgeCount = 0;
        for (IndexItem source : index) {
            g.addNode(source);
        }
        for (IndexItem source : index) {
            Set<IndexItem> items = getItemsToCompare(source);
//            System.out.println("Blocks for item " + source + ": " + items);
            for (IndexItem target : items) {
                g.addEdgeOnly(source, target);
                edgeCount++;
            }
        }
//        System.out.println("Edge count should be " + edgeCount);
//        System.out.println("Edge count is " + g.getAllEdges().size());
        return g;
    }

    public String getKey(IndexItem ii) {
        String label = "";
        HR3IndexItem hr = (HR3IndexItem) ii;
        for (int i = 0; i < hr.coordinates.size(); i++) {
            label = label + hr.coordinates.get(i) + "A";
        }
        return label;
    }

    public String generateKey(List<Integer> coordinates) {
        String label = "";
        for (int i = 0; i < coordinates.size(); i++) {
            label = label + coordinates.get(i) + "A";
        }
        return label;
    }

    public Cache get(IndexItem ii) {
        Cache mc = new MemoryCache();
        String uri;
        BufferedReader in =null;
        if (ii instanceof HR3IndexItem) {
            String label = getKey(ii);
            //get data from file and write to cache
            try {
                in = new BufferedReader(new FileReader(new File(baseFolder + "/" + label)));
                String s = in.readLine(), split[];
                while (s != null) {
                    split = s.split(splitToken);
                    uri = split[0];
                    for (int i = 1; i < split.length; i++) {
                        mc.addTriple(uri, "p" + i, split[i]);
                    }
                    s = in.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally{try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}
        }
        return mc;
    }

    /**
     * Append data to the file
     *
     * @param s
     * @param fileName
     */
    private void write(String s, String fileName) {
        try {
            String fullName = baseFolder + "/" + fileName;
            FileWriter fw = new FileWriter(fullName, true); //the true will append the new data
            fw.write(s + "\n");//appends the string to the file
            fw.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    public static void main(String args[]) {

        double threshold = 0.8d;
        Hr3Indexer hr3 = new Hr3Indexer(4, threshold);
        hr3.endColumn = 2;
        hr3.runIndexing(new File("/home/mofeed/Projects/Caching/testdata1000.txt"), true);
        Graph g = hr3.generateTaskGraph();
        TSPPlanner planner = new TSPPlanner();
        System.out.println("Capacity\tRuntime\tHits\tMisses");
        List<String> data= new ArrayList<String>(); 
        data.add("Capacity\tRuntime\tHits\tMisses\n");
        String line="";
        for (int capacity = 100; capacity <= 1000; capacity += 100) {
            for (int j = 0; j < 5; j++) {
                //baseline
                long begin = System.currentTimeMillis();
                List<DataManipulationCommand> plan = planner.plan(g);
                DataCache dataCache = DataCacheFactory.createCache(CacheType.TimedSLru, Integer.MAX_VALUE, 1,capacity); // new SimpleCache(capacity);
                CacheAccessExecution cae = new CacheAccessExecution(dataCache, plan, new EuclideanMetric(), threshold, hr3);
                cae.run();
                long end = System.currentTimeMillis();
                System.out.print("Baseline (" + capacity + ")\t");
                System.out.print((end - begin) + "\t");
                System.out.print(dataCache.getHits() + "\t");
                System.out.print(dataCache.getMisses() + "\t");
                line="";
                line+=capacity + "\t"+(end - begin) + "\t"+dataCache.getHits() + "\t"+dataCache.getMisses() + "\t";

                begin = System.currentTimeMillis();
                int iterations = 0;

//        for (IndexItem ii : hr3.index) {
//            System.out.print(ii.getId() + " => " + ii.getSize());
                //System.out.println(hr3.get(ii));
//            System.out.println("\t to be compared with " + hr3.getItemsToCompare(ii));
//        }
//        System.out.println(g);
//            System.out.println("Initial nodes = " + g.getAllNodes().size());
//            System.out.println("Initial edges = " + g.getAllEdges().size());
                Clustering gc = new NodeGreedyClustering();
                Map<Integer, Cluster> clusters = gc.cluster(g, capacity);
//        System.out.println(clusters);
                //System.out.println(gc.clusters);
                //System.out.println(gc.itemClust   erMap);
                TSPSolver tsp = new TSPSolver();
                int[] path = tsp.getPath(tsp.getMatrix(clusters), iterations);
//            Set<Edge> edges = new HashSet<>();
//            Set<Node> nodes = new HashSet<>();
//            System.out.println("Path length = " + path.length);
//            for (int k = 0; k < path.length; k++) {
//                edges.addAll(clusters.get(path[k]).edges);
//                nodes.addAll(clusters.get(path[k]).nodes);
//            }
//
//            System.out.println("Edges = " + edges.size() + ", nodes = " + nodes.size());
//            for (Edge e : g.getAllEdges()) {
//                if (!edges.contains(e)) {
//                    System.out.println("Missing edge = " + e);
//                }
//            }
                plan = planner.plan(clusters, path);
//        System.out.println(plan);
                dataCache = new SimpleCache(capacity);
                cae = new CacheAccessExecution(dataCache, plan, new EuclideanMetric(), threshold, hr3);
                cae.run();
                end = System.currentTimeMillis();
                line+=capacity + "\t"+(end - begin) + "\t"+dataCache.getHits() + "\t"+dataCache.getMisses() + "\n";
                data.add(line);
                System.out.print(capacity + "\t");
                System.out.print((end - begin) + "\t");
                System.out.print(dataCache.getHits() + "\t");
                System.out.print(dataCache.getMisses() + "\n");
            }
        }
  //      writeData(data,"/home/mofeed/Projects/Caching/TimedSLruCacheOut");
    }

    public void runIndexing(File input) {
        runIndexing(input, false);
    }

    public void runIndexing(File input, String folder) {
        runIndexing(input, folder, false);
    }
    public static void writeData(List<String> data, String filePath)
    {
    	FileWriter fw=null;
    	BufferedWriter bw=null;

        	try {


    			File file = new File(filePath);

    			// if file doesnt exists, then create it
    			if (!file.exists()) {
    				file.createNewFile();
    			}

    			fw = new FileWriter(file.getAbsoluteFile());
    			bw = new BufferedWriter(fw);    	
    			for (String line : data) {
    				bw.write(line);
    			}
    			System.out.println("Done");

    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        	finally{    			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}
		
    }
}
