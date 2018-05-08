package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

/**
 * @author mofeed
 *
 */
public abstract class AbstractCache implements DataCache {



    /**
     * logger
     */
    public static Logger log4j = Logger.getLogger(AbstractCache.class);

    /**
     * cache
     * all are saved as Object
     */
    protected Map<Object, Object> m_cacheMap = new HashMap<Object, Object>();

    /**
     * cache size
     */
    protected final int m_cacheMaxSize;

    /**
     * evict count
     */
    protected final int m_evictCount;

    /**
     * evict count
     */
    protected int m_hits;

    /**
     * evict count
     */
    protected int m_misses;
    
    int currentSize;//current number of resources in the cache now
    int capacity;//max number of resources to store in the cache


    /**
     *
     * @param size
     * @param evictCount
     */
    public AbstractCache(int size, int evictCount) {
        m_cacheMaxSize = size;
        m_evictCount = evictCount;
        currentSize=m_hits = m_misses = 0;
    }
    public AbstractCache(int size, int evictCount, int theCapcity) {
        m_cacheMaxSize = size;
        m_evictCount = evictCount;
        capacity = theCapcity;
        currentSize=m_hits = m_misses = 0;
    }

    // coming methods are list of abstract methods to be implemented in concrete class
    @Override
    public abstract Cache getData(IndexItem index, Indexer indexer);

    @Override
    public abstract void deleteData(IndexItem index);

    @Override
    public abstract int getHits();

    @Override
    public abstract int getMisses();

    protected abstract void hitAccess(Object key);

   /* protected abstract void evict();
*/
    protected abstract Object evict();

    protected abstract void putAccess(Object key);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// These are list of methods common to all concrete classes and implemented in this abstract one
    /**
     * This method returns the value given the specified key from cache
     *
     * @param key the value to be retrieved
     * @return the value or null
     */
   /* public Cache get(Object key) {

        Object value = m_cacheMap.get(key);
        if (value != null)// that is hit
        {
            hitAccess(key);
            m_hits++; // added by me to suit the wrapping needed
        } else // added by me to suit the wrapping needed
        {
            m_misses++;
        }

        if (log4j.isDebugEnabled()) {
            if (value != null) {
                log4j.debug("hit:" + key.toString());
            } else {
                log4j.debug("miss: " + key.toString());
            }
        }

        return (Cache)value;
    }*/
    public Cache get(IndexItem key, Indexer indexer) {//changed from 

        Object value = m_cacheMap.get(key);
        if (value != null)// that is hit
        {
            log4j.info("get: value existed");

            hitAccess(key);
            m_hits++; // added by me to suit the wrapping needed
        } else // added by me to suit the wrapping needed
        {
            log4j.info("get: value missed");

            m_misses++;
            put(key, indexer.get(key));// put the IndexItem and its values from the indexer
            value = m_cacheMap.get(key);// after it is added
        }

        if (log4j.isDebugEnabled()) {
            if (value != null) {
                log4j.debug("hit:" + key.toString());
            } else {
                log4j.debug("miss: " + key.toString());
            }
        }

        return (Cache)value;//cast the retrieved object to unbox the cache required
    }

    /**
     * @return the specified size of the cache
     */
    public int maxSize() {
        return m_cacheMaxSize;
    }

    /**
     * This method adds new item to the cache. It checks if the size limit is
     * reached. If yes it calls evict to remove some values of the cache based
     * on its behavior afterwards, it put the new value's key into key list,
     * then the values itself is added to the cache
     *
     * @param key the index of the hypercube
     * @param val the hypercube itself
     * @return null if it was first time to be in the cache or the previously
     * and replaced values associated to this key
     */
/*    public Object put(Object key, Object val) {

        if (m_cacheMaxSize > 0) {
            if (size() >= m_cacheMaxSize) {
                evict();
            }
            putAccess(key);

            // debug info
            if (log4j.isDebugEnabled()) {
                log4j.debug("put:" + key.toString());
            }

            return m_cacheMap.put(key, val);
        }
        return null;
    }*/
    
    public Object put(Object key, Object val) {
    	IndexItem removed= null;
        if (m_cacheMaxSize > 0) {
            log4j.info("put"+currentSize+"+"+((IndexItem) key).getSize()+"<>"+ capacity);
            int hypercubeSize= ((IndexItem) key).getSize();
        	 while ((currentSize + hypercubeSize) > capacity) {
        		 removed = (IndexItem)evict();
                 log4j.info("EVICT "+removed);
        		 if(removed!=null)// for cases like FIFO2Chance if it is not removed for a second chance it will return null
        			 currentSize = currentSize - removed.getSize();
             }
            putAccess(key);//just to update the accessibility key map order based on the cache strategy

            // debug info
            if (log4j.isDebugEnabled()) {
                log4j.debug("put:" + key.toString());
            }
            currentSize = currentSize + ((IndexItem)key).getSize();
            return m_cacheMap.put(key, val);//add the object as key and value to the cache 
        }
        return null;
    }

    /**
     * It removes specific value from the cache
     *
     * @param value to be removed
     * @return list of values removed already from the cache
     */
    public List<Object> removeValues(Object value) {

        List<Object> removed = new ArrayList<Object>();
        Iterator<Object> iter = m_cacheMap.keySet().iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (m_cacheMap.get(o).equals(value)) {
                removed.add(o);
                iter.remove();
            }
        }
        return removed;
    }

    /**
     * this method checks if certain key exist in cache
     *
     * @param key
     * @return true if exist false otherwise
     */
    public boolean contains(Object key) {
        if (m_cacheMap.keySet().contains(key)) {
            return true;
        }
        return false;
    }

    /**
     * @return the size of the current cache (number of elements currently
     * exist)
     */
    public int size() {
        return m_cacheMap.size();
    }

    /*  public boolean test() {
     return false;
     }*/
    /**
     * This method gives a list of all values exist in the cache currently
     *
     * @return collection of current values in cache
     */
    public Collection<Object> values() {
        return m_cacheMap.values();
    }

    public Collection<Object> keys() {
        return m_cacheMap.keySet();
    }

    public boolean test() {
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_cacheMap.toString();
    }
}
