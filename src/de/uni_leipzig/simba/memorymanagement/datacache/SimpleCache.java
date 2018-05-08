/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Simple implementation of DataCache. Should work in combination with planner.
 *
 * @author ngonga
 */
public class SimpleCache implements DataCache {

    Map<IndexItem, Cache> cache;//m_cacheMap
    Queue<IndexItem> activeObjects;//m_access
    int maxSize;
    int currentSize;
    int hits, misses;

    // Could be extended with max number of items
    public SimpleCache(int capacity) {//why map is passed to cache should it be internally created
        cache = new HashMap<>();
        activeObjects = new ArrayBlockingQueue<IndexItem>(capacity);
        maxSize = capacity;
        currentSize = 0;
        hits = 0;
        misses = 0;
    }

    /**
     * Gets resources from the cache. If the resource is not in the cache, then
     * it is added
     *
     * @param ii Index whose corresponding data is to be retrieved
     * @param indexer Indexer for converting data read from HDD to Cache. 
     * @return Data (get)
     */
    public Cache getData(IndexItem ii, Indexer indexer) {
        if (!activeObjects.contains(ii)) {
            misses = misses + ii.getSize();
            //need to remove an object
            while (currentSize + ii.getSize() > maxSize) {
                IndexItem removeMe = activeObjects.poll();
                cache.remove(removeMe);
                currentSize = currentSize - removeMe.getSize();
            }
            activeObjects.add(ii);
            cache.put(ii, indexer.get(ii));
            currentSize = currentSize + ii.getSize();
        } else {
            hits = hits + ii.getSize();
        }
        return cache.get(ii);
    }

    /**
     * Removes an object from the cache )(evict)
     *
     * @param index Index to be removed
     */
    public void deleteData(IndexItem index) {
        if (activeObjects.contains(index)) {
            activeObjects.remove(index);
            cache.remove(index);
            currentSize = currentSize - index.getSize();
        }
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }
}
