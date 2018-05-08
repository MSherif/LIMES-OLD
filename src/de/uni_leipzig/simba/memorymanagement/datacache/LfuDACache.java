/**
 * 
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import de.uni_leipzig.simba.cache.Cache;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;

/**
 * @author mofeed
 * @author rspeck
 */
public class LfuDACache extends AbstractGDCache{

	public LfuDACache(int size, int evictCount) {
		super(size, evictCount);
		// TODO Auto-generated constructor stub
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
        ((LFU_DA_Element) m_accessMap.get(key)).update();
		
	}

	@Override
	protected Object evict() {
		Object removed =null;
		 // set age and remove min element
        if (m_evictCount < m_accessMap.values().size()) {

            List<GDElement> evictElements = new LinkedList<GDElement>(m_accessMap.values());
            Collections.sort(evictElements);
            // LFU_DA_Element objects
            evictElements = evictElements.subList(0, m_evictCount);
            // min credit
            m_cacheAge = evictElements.get(0).getCredit();
            Iterator<Entry<Object, GDElement>> accessIter = m_accessMap.entrySet().iterator();
            while (accessIter.hasNext() && !evictElements.isEmpty()) {
                Entry<Object, GDElement> entry = accessIter.next();

                if (evictElements.contains(entry.getValue())) {
                    evictElements.remove(entry.getValue());
                    removed=entry.getKey();
                    m_cacheMap.remove(entry.getKey());
                    accessIter.remove();
                }
            }

        } else {
            m_cacheMap.clear();
            m_accessMap.clear();
        }
        return removed;
	}


    @Override
    public List<Object> removeValues(Object value) {

        List<Object> removed = super.removeValues(value);
        for (Object o : removed)
            m_accessMap.remove(o);

        return removed;
    }

    @Override
    public boolean test() {
        return (size() > m_cacheMaxSize ||
                m_accessMap.size() > m_cacheMaxSize || m_accessMap.size() > size()

        ) ? false : true;
    }
    
	@Override
	protected void putAccess(Object key) {
        m_accessMap.put(key, new LFU_DA_Element());
		
	}

	/**
     * Refined inner class for an element to compare on special value depends on
     * hits
     */
    protected class LFU_DA_Element extends GDElement {

        private Integer m_hits = 0;

        public LFU_DA_Element() {
            update();
        }

        public void update() {
            m_hits++;
            update(Double.valueOf(m_hits));
        }
    }
}
