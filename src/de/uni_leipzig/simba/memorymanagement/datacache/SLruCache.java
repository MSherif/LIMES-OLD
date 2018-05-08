/**
 * 
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.LinkedList;
import java.util.List;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

/**
 * @author mofeed
 *
 */
public class SLruCache extends AbstractCache{

	protected final List<Object> m_access       = new LinkedList<Object>();
	  protected final List<Object> m_accessSeg    = new LinkedList<Object>();
	    
	  /** percentage of cache memory to use for segment */
	  private final float          SEGMENT_BORDER = 0.5f;
	  /** # of elements in segment */
	 // private final int            SEGMENT_SIZE   = (int) (m_cacheMaxSize * SEGMENT_BORDER);
	  private final int            SEGMENT_SIZE   = (int) (capacity * SEGMENT_BORDER);

	  
	  /**
	     * Constructor calls super constructor with same parameters.
	     */
	    public SLruCache(int size, int evictCount) {
	        super(size, evictCount);
	    }
	  
		public SLruCache(int size, int evictCount,int capacity) {
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
	public int getHits() {
		return m_hits;
	}
	@Override
	public int getMisses() {
		return m_misses;
	}
	@Override
	protected void hitAccess(Object key) {
		   if (!m_access.remove(key))
	            m_accessSeg.remove(key);

	        m_accessSeg.add(key);
	        // here check if total size is greater than segment capacity and if so either remove one element at a time or remove set of them--> remove only one
	       /* if (m_accessSeg.size() > SEGMENT_SIZE)
	            m_access.add(m_accessSeg.remove(0));*/
	        //get total size of elements in protected segment
	        int total =0;
	        for (Object indexItem : m_accessSeg) {
	        	total+=((IndexItem)indexItem).getSize();
			}
	        if (total > SEGMENT_SIZE)// if total capacity of the protectedSeg exceeded the limit, remove the recent one from it and put it to unprotectedSeg only Once
	            m_access.add(m_accessSeg.remove(0));
	}
	@Override
	protected Object evict() {//removing from the whole cache and you do it first in unprotected and won't go to protrcted unless the former is empty and you have problem in size, so remove it totaly
		Object removed =null;
		for (int i = 0; i < m_evictCount; i++) {

            if (!m_access.isEmpty()) {
                Object o = m_access.remove(0);
                removed =o;
                m_cacheMap.remove(o);
            }

            else if (!m_accessSeg.isEmpty()) {
                Object o = m_accessSeg.remove(0);
                removed=o;
                m_cacheMap.remove(o);
            }
        }
		return removed;
	}
	@Override
	protected void putAccess(Object key) {
        m_access.add(key);		
	}

	@Override
    public boolean test() {
        return (size() > m_cacheMaxSize || (m_access.size() + m_accessSeg.size()) > m_cacheMaxSize) ? false : true;
    }
	
    @Override
    public List<Object> removeValues(Object value) {

        List<Object> removed = super.removeValues(value);
        m_access.removeAll(removed);
        m_accessSeg.removeAll(removed);

        return removed;
    }    
	    
	    
	    
}
