package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

public class LruCache extends AbstractCache{
    /**
     * This list concerns in saving the keys of the cached values exist in the m_cacheMap.
     * Its way of management affects the way the cache works
     */
    protected final List<Object> m_access = new LinkedList<Object>();

	/**
	 * constructor calls the super constructor in AbstractCache
	 * It initializes the size and number of eviction in the cache
	 * @param size
	 * @param evictCount
	 */
	public LruCache(int size, int evictCount) {
        super(size, evictCount);
    }
	public LruCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}

	/* 
	 * This method calls get() to retrieve a value from the cache
	 */
	@Override
	public Cache getData(IndexItem index, Indexer indexer) {
		log4j.info("get value with key "+index.toString());
		return super.get(index, indexer);
	}

	/* 
	 * This method calls deleteData() to remove a value from the cache
	 */
	@Override
	public void deleteData(IndexItem index) {
		List<Object> removed = removeValues(index);
		if(removed!=null)
			log4j.debug("removed :" + index.toString());
		
	}
	
    @Override
    public List<Object> removeValues(Object value) {
        List<Object> removed = super.removeValues(value);//remove from cache and return list of keys
        for (Object key : removed) {// remove them from the m_access
        	m_access.remove(key);
		}
        return removed;
    }
	@Override
	public int getHits() {
		return m_hits;
	}

	@Override
	public int getMisses() {
		return m_misses;
	}


	/* 
	 * This method simulates the behavior of Least-Recently-Used.
	 * It checks if the item is in the cache by checking its key existence in m_access. 
	 * If yes it removes the item's key from the list then add it again at the end  of it. 
	 * This makes the least-recently-used item's key always in the front of the list.
	 */
	@Override
	protected void hitAccess(Object key) {
		 int index = m_access.indexOf(key);
	        if (index >= 0)
	            m_access.remove(index);
	        else
	            log4j.error("Was it realy a hit?");
	        m_access.add(key);
	        
	}


	/* 
	 * Evicts the 1st object or m_evictCount objects form m_access list.
	 * It moves over the keys' list and based on the order the corresponding
	 * values are removed from the cache itself
	 */
	@Override
	protected Object evict() {
		Object removed=null;
        Iterator<Object> it = m_access.iterator();
        for (int i = 0; i < m_evictCount; i++)
            if (it.hasNext()) {//cautious to exceed the cache list in case it is < m_evictCount
                Object o = it.next();
                removed=o;
                m_cacheMap.remove(o);
                it.remove();
            }
        return removed;
	}


	/* 
	 * This method is executed when put() method is called to add
	 * the new value's key in the key list
	 */
	@Override
	protected void putAccess(Object key) {
        int index = m_access.indexOf(key);
        if (index >= 0) {
            log4j.error("Key in cache!");
            m_access.remove(index);
        }
        m_access.add(key);
		
	}

}
