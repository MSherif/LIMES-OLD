package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

public class FifoCache extends AbstractCache{
	
    protected Queue<Object> m_access = new LinkedList<Object>();


	public FifoCache(int size, int evictCount) {
		super(size, evictCount);
	}
	
	public FifoCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}

	@Override
	public Cache getData(IndexItem index, Indexer indexer){
		return super.get(index, indexer);
	}

	@Override
	public void deleteData(IndexItem index) {
		List<Object> removed = removeValues(index);
		if(removed!=null)
			log4j.debug("removed :" + index.toString());		
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
		//nothing to do in FIFO as nothing will change based on getting the value
		
	}

	@Override
	protected Object evict() {
		Object removed =null;
        for (int i = 0; i < m_evictCount && !m_access.isEmpty(); i++) {
            removed = m_access.poll();
            m_cacheMap.remove(removed);
        }	
        return removed;
	}

	@Override
	protected void putAccess(Object key) {
        m_access.add(key);		
	}
    @Override
    public List<Object> removeValues(Object value) {
        List<Object> remove = super.removeValues(value);
        m_access.removeAll(remove);
        return remove;
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
