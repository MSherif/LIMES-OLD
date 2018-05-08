/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.datacache;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

/**
 * Interface for indexes
 * @author ngonga
 */
public interface DataCache {
    Cache getData(IndexItem index, Indexer indexer);//get
    void deleteData(IndexItem index);//evict
    int getHits();
    int getMisses();
}
