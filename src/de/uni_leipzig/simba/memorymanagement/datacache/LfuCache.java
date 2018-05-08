/**
 * 
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

/**
 * @author mofeed
 *
 */
public class LfuCache  extends AbstractCache{
    /**
     * cluster (map key) to hits (map value).
     */
    protected final Map<Object, Integer> m_access = new LinkedHashMap<Object, Integer>();

	public LfuCache(int size, int evictCount) {
		super(size, evictCount);
	}
	public LfuCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}

	@Override
	public Cache getData(IndexItem index, Indexer indexer) {
		return super.get(index, indexer);
	}

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

	@Override
	protected void hitAccess(Object key) {
	       Integer id = m_access.get(key);
	        if (id != null)
	            m_access.put(key, ++id);
	        else {
	            m_access.put(key, 0);
	            log4j.error("Can't find key. Was it realy a hit?");
	        }
	}

	@Override
	protected Object evict() {
		Object removed = null;
	      if (m_evictCount < m_access.size()) {//there are already items to evict

	            List<Integer> minHits = new ArrayList<Integer>(m_access.values());
	            Collections.sort(minHits);
	            minHits = minHits.subList(0, m_evictCount);

	            Iterator<Entry<Object, Integer>> accessIter = m_access.entrySet().iterator();
	            while (accessIter.hasNext() && !minHits.isEmpty()) {
	                Entry<Object, Integer> entry = accessIter.next();

	                if (minHits.contains(entry.getValue())) {
	                    minHits.remove(entry.getValue());//remove the Cache that contains the resources related to this IndexItem
	                    accessIter.remove();
	            		removed=entry.getKey();// should be the indexItem which is the key in our cache
	                    m_cacheMap.remove(entry.getKey());
	                }
	            }

	        } else {
	            m_access.clear();
	            m_cacheMap.clear();
	        }
	      return removed;
	}

	@Override
	protected void putAccess(Object key) {
        Integer id = m_access.get(key);
        if (id != null)
            m_access.put(key, ++id);
        else
            m_access.put(key, 0);	
	}
	
	   @Override
	    public boolean test() {
	        if (size() > m_cacheMaxSize || m_access.size() > size() || m_access.size() > m_cacheMaxSize)
	            return false;
	        return true;
	    }

	    @Override
	    public String toString() {
	        return super.toString() + "\n" + m_access.toString();
	    }

}
