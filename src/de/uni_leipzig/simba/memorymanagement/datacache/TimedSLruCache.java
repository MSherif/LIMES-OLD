/**
 * 
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;

/**
 * @author mofeed
 *
 */
public class TimedSLruCache  extends SLruCache{
	/*BiMap<String, Long> m_accessTimed = HashBiMap.create ();
	BiMap<String, Long> m_accessSegTimed = HashBiMap.create ();
*/
	BiMap<Object, Long> m_accessTimed = HashBiMap.create ();
	BiMap<Object, Long> m_accessSegTimed = HashBiMap.create ();

	long cacheTimer = 0; //with each cache access it is incremented except for eviction
	long evictIndex = 0;
	long evictSegIndex = 0;

	
	/** percentage of cache memory to use for segment */
    private final float          SEGMENT_BORDER = 0.5f;
    /** # of elements in segment */
  //  private final int            SEGMENT_SIZE   = (int) (m_cacheMaxSize * SEGMENT_BORDER);
    private final int            SEGMENT_SIZE   = (int) (capacity * SEGMENT_BORDER);

	
	public TimedSLruCache(int size, int evictCount) {
		super(size, evictCount);
	}
	public TimedSLruCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}
	/**
     * Evicts the 1st object or m_evictCount objects form m_access list.
     */
    @Override
    protected Object evict() {
    	Object removed=null;

    	int evictCount = m_evictCount;
		BiMap<Long,Object> inverse_m_accessTimed = m_accessTimed.inverse();
		BiMap<Long,Object> inverse_m_accessSegTimed = m_accessSegTimed.inverse();
		
        while(evictCount !=0 ) 
        {
        	//evict first from normal list which i all older than in Seg
        	if (!inverse_m_accessTimed.isEmpty()) {
        		if(inverse_m_accessTimed.containsKey(evictIndex))
        		{
        			Object key = inverse_m_accessTimed.remove(evictIndex++);
        	    	removed=key;
        			m_cacheMap.remove(key);
                	evictCount--;
        		}
        		else
        			evictIndex++;
            }
            else if (!inverse_m_accessSegTimed.isEmpty()) {
            	if(inverse_m_accessSegTimed.containsKey(evictSegIndex))
            	{
            		Object key = inverse_m_accessSegTimed.remove(evictSegIndex++);
        	    	removed=key;
            		m_cacheMap.remove(key);
                	evictCount--;
            	}
            	else
            		evictSegIndex++;
            }
            else
            	break;// both are empty, evict size greater than number of elements in both
        }
        return 	removed;

    }
    
    @Override
    protected void hitAccess(Object key) {
     
    	if(m_accessTimed.remove(key)== null)
     		m_accessSegTimed.remove(key);
    	if(m_accessSegTimed.size()==0) // first time to insert in the segment
    		evictSegIndex = cacheTimer; // initialize it to clock tick of the first time insertion instead of starting from 0
     	m_accessSegTimed.put(key, cacheTimer++);
     		
        int total =0;
        for (Object indexItem : m_accessSegTimed.keySet()) {
        	total+=((IndexItem)indexItem).getSize();
		}
    	if (total > SEGMENT_SIZE)
     	{
     		BiMap<Long,Object> inverse_m_accessSegTimed =m_accessSegTimed.inverse();
     		while(!inverse_m_accessSegTimed.containsKey(evictSegIndex))
     			evictSegIndex++;
     		Object movedKey = inverse_m_accessSegTimed.remove(evictSegIndex++); // remove from seg
            m_accessTimed.put(movedKey,cacheTimer++); // add to access list with new time
     	}
     /*	if (m_accessSegTimed.size() > SEGMENT_SIZE)
     	{
     		BiMap<Long,Object> inverse_m_accessSegTimed =m_accessSegTimed.inverse();
     		while(!inverse_m_accessSegTimed.containsKey(evictSegIndex))
     			evictSegIndex++;
     		Object movedKey = inverse_m_accessSegTimed.remove(evictSegIndex++); // remove from seg
            m_accessTimed.put(movedKey,cacheTimer++); // add to access list with new time
     	}*/
    }
    @Override
    protected void putAccess(Object key) {
    	m_accessTimed.put(key, cacheTimer++); // Add it with its new timing
    }
    
    @Override
    public List<Object> removeValues(Object value) {
        List<Object> removed = super.removeValues(value);
        for (Object key : removed) {
        	m_accessTimed.remove(key);
        	m_accessSegTimed.remove(key);
		}
 /*       m_accessTimed.clear();
        m_accessSegTimed.clear();*/
        return removed;
    }
    @Override
    public boolean test() {
        return (size() > m_cacheMaxSize || (m_accessTimed.size() + m_accessSegTimed.size()) > m_cacheMaxSize) ? false : true;
    }
}
