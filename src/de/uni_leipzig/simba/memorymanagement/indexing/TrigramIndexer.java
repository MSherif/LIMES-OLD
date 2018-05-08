/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Graph;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implements the indexer for trigram-based similarity
 * @author ngonga
 */
public class TrigramIndexer implements Indexer {

    public static String splitToken = "\t";
    public String baseFolder = "/home/mofeed/Projects/Caching/baseFolder/Trigram/";
    public int beginColumn = 1;
    public int endColumn = 1;
    public Set<IndexItem> index;
    public Map<String, IndexItem> keyToIndexMap;
    public double threshold;

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
     * Constructor. Simply creates the base folder and removes everything in it.
     * @param threshold Threshold for the similarity
     */
    public TrigramIndexer(double threshold) {
        this.threshold = threshold;
        init();
    }

    /**
     * Constructor as above
     *
     * @param baseFolder New base folder
     */
    public TrigramIndexer(String baseFolder, double threshold) {
        this.baseFolder = baseFolder;
        init();
    }

    /**
     * Runs the indexing and omits the header if header is set to true
     * @param input Input file for the data
     * @param header True => First line of file will be ignored
     */
    public void runIndexing(File input, boolean header) {
        String s = "";
        String fullLabel = "";
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
            Set<String> trigrams;
            int length;

            // read file and map strings to files. Names of files are trigram + 
            // length of string
            while (s != null) {
                split = s.split(splitToken);
                //String uri = split[0];
                trigrams = new HashSet<>();
                length = 0;
                if (endColumn > split.length - 1) {
                    endColumn = split.length - 1;
                }
                for (int i = beginColumn; i <= endColumn; i++) {
                    try {
                        trigrams.addAll(getTrigrams(split[i].toLowerCase()));
                        length = length + split[i].length();
                    } catch (Exception e2) {
                        System.err.println(s);
                        e2.printStackTrace();
                    }
                }
                //check whether we already have a file with that index
                //if not create file
                for (String label : trigrams) {
                    fullLabel = label.replaceAll("[^a-zA-Z0-9.-]", "_") + trigrams.size();
                    if (!fileNames.containsKey(fullLabel)) {
                        fileNames.put(fullLabel, 0);
//                        System.out.println(baseFolder + "/" + fullLabel);
                        File f = new File(baseFolder + "/" + fullLabel);
                        f.createNewFile();
                    }
                    //write data into file and increment the count of resources
                    write(s, fullLabel);
                    fileNames.put(fullLabel, fileNames.get(fullLabel) + 1);
                }
                s = in.readLine();
            }
            index = new HashSet<>();
            keyToIndexMap = new HashMap<>();
            //now create the index items
            for (String key : fileNames.keySet()) {
                IndexItem ii = new TrigramIndexItem(key, fileNames.get(key));
                index.add(ii);
                keyToIndexMap.put(key, ii);
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println(e.getMessage());
            System.err.println("Label: " + fullLabel);
            e.printStackTrace();
        }
        System.out.println("Index size = " + index.size());
        System.out.println(index);
        System.out.println("Index map size = " + keyToIndexMap.keySet().size());
    }

    public void runIndexing(File input) {
        runIndexing(input, true);
    }

    @Override
    public void runIndexing(File input, String folder) {
        baseFolder = folder;
        runIndexing(input, true);
    }

    /**
     * Gets the data for an index item from memory
     * @param ii Index item
     * @return Cache containing the corresponding data
     */
    public Cache get(IndexItem ii) {
        Cache mc = new MemoryCache();
        String uri;
        if (ii instanceof TrigramIndexItem) {
            String label = ii.getId();
            //get data from file and write to cache
            BufferedReader in =null;
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
            }
            finally{try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}

        }
        return mc;
    }

    /**
     * Generate a task graph for the given data
     * @return A task graph
     */
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

    /**
     * Computes trigrams for a given string
     * @param split String to process
     * @return Set of trigrams in the string
     */
    private Set<String> getTrigrams(String split) {
        Set<String> result = new HashSet<>();
        //empty string is empty
        if (split.length() == 0) {
            return result;
        }
        //else generate at least one trigram
        String label = "X" + split + "X";
        for (int i = 0; i < label.length() - 2; i++) {
            result.add(label.substring(i, i + 3));
        }
        return result;
    }

    /**
     * Append data to the file TODO: Redundant with HR3Indexer. Fix.
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

    public static void test() {
        double threshold = 0.8d;
        TrigramIndexer tix = new TrigramIndexer(threshold);
        tix.endColumn = 1;
        tix.runIndexing(new File("/home/mofeed/Projects/Caching/data.txt"/*"E:\\Work\\Java\\GeoBenchLab\\LinkBench\\dbpedia\\data.txt"*/), true);
        System.out.println(tix.get(new TrigramIndexItem("tis17", 0)));
        System.out.println(tix.generateTaskGraph()) ;
    }

    public static void main(String args[]) {
        test();
    }

    private Set<IndexItem> getItemsToCompare(IndexItem source) {
        String trigram = source.getId().substring(0, 3);
        int size = Integer.parseInt(source.getId().substring(3));
        int min = (int) Math.ceil(size * threshold);
        int max = (int) Math.floor(size / threshold);
        Set<IndexItem> result = new TreeSet<>();
        for (int i = min; i <= max; i++) {
            if (keyToIndexMap.containsKey(trigram + i)) {
                result.add(keyToIndexMap.get(trigram + i));
            }
        }
        return result;
    }
}
