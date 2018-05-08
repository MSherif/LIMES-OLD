/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.memorymanagement.Index.graphclustering.Graph;
import java.io.File;

/**
 * Takes in a data stream that models a cache and indexes it. Returns an
 * index items. Those can be read to retrieve the data they refer to.
 * @author ngonga
 */
public interface Indexer {
    // index the data in a file
    void runIndexing(File input);
    void runIndexing(File input, String folder);
    // get data from mass storage and write it in a cache
    Cache get(IndexItem ii);
    Graph generateTaskGraph();
}
