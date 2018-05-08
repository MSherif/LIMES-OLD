/**
 * 
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;           //just to update the accessibility key map order based on the cache strategy



/**
 * @author mofeed
 *
 */
public class TimedLruCache extends LruCache{
	//BiMap<String, Integer> keyIndices = HashBiMap.create ();
	private Map<Object, Integer> keysTimeSpots = new HashMap<Object, Integer>();
	private Map<Integer, Object> keysTimeSpotsInverse = new HashMap<Integer, Object>();



	int cacheTime = 0; //with each cache access it is incremented except for eviction
	int evictIndex = 0; // it depends on the fact that each item inserted in cache is recorded in keyIndices based on cacheTime in that moment which increases sequentially
	
    /**
     * logger
     */
    public static Logger log4j = Logger.getLogger(LruCache.class);
    
	public TimedLruCache(int size, int evictCount) {
		super(size, evictCount);
	}
	public TimedLruCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}
	/**
     * Evicts the 1st object or m_evictCount objects form m_access list.
     */
    @Override
    protected Object evict() {
    	Object removed=null;
    	int evictCount = 1;// m_evictCount;
        while(evictCount !=0 && keysTimeSpotsInverse.size() > 0) // for the number of elements to be evicted, second cond. in case of evict > existing elemtn nr.
        {
        	if(keysTimeSpotsInverse.containsKey(evictIndex)) // if the entry of the required index exists
        	{
                log4j.info("eviction number: "+ evictCount);
        		removed = keysTimeSpotsInverse.get(evictIndex); //get the associated key
        		
                log4j.info("Timed_Lru: Item (key) to remove"+((IndexItem)removed).toString());
                
        		m_cacheMap.remove(removed);// remove the item from the cache
        		keysTimeSpots.remove(removed); //remove from keys:time
        		keysTimeSpotsInverse.remove(evictIndex); //remove from reverse map
        		
        		log4j.info("evict"+m_cacheMap.size()+"#"+keysTimeSpots.size());
        		if(m_cacheMap.size()!=keysTimeSpots.size())
        			System.exit(0);
 //               log4j.info("Timed_Lru: removed from cache and keyIndices");
               log4j.info(m_cacheMap.keySet());
                log4j.info(keysTimeSpots.keySet());
                evictCount--; //one is put
        	}
    		evictIndex++; // update to the next evicted item index
        }
/*        while(evictCount !=0 && keysTimeSpotsInverse.size() > 0) // for the number of elements to be evicted, second cond. in case of evict > existing elemtn nr.
        {
        	if(keysTimeSpotsInverse.containsKey(evictIndex)) // if the entry of the required index exists
        	{
                log4j.info("eviction number: "+ evictCount);
        		removed = keysTimeSpotsInverse.get(evictIndex); //get the associated key
        		
                log4j.info("Timed_Lru: Item (key) to remove"+((IndexItem)removed).toString());
                
        		m_cacheMap.remove(removed);// remove the item from the cache
        		keysTimeSpots.remove(removed); //remove from keys:time
        		keysTimeSpotsInverse.remove(evictIndex); //remove from reverse map
        		
        		log4j.info("evict"+m_cacheMap.size()+"#"+keysTimeSpots.size());
        		if(m_cacheMap.size()!=keysTimeSpots.size())
        			System.exit(0);
 //               log4j.info("Timed_Lru: removed from cache and keyIndices");
               log4j.info(m_cacheMap.keySet());
                log4j.info(keysTimeSpots.keySet());


        		evictCount--;
        	}
    		evictIndex++; // update to the next evicted item index
        }*/
        return removed;
    }
    /*
     * This method updates cache recently used indices in case of cache hit situation.
     * It should update the existing item's index to new one reflecting its new usage moment
     * key : the key of the stored item in cache (lat:long) 
     * */
    @Override
    protected void hitAccess(Object key) {
		log4j.info("hitAccess"+m_cacheMap.size()+"#"+keysTimeSpots.size());
     	if(keysTimeSpots.containsKey(key))///////////////////!!
    	{
     		keysTimeSpotsInverse.remove(keysTimeSpots.get(key));
     		keysTimeSpots.remove(key);
     		
    		keysTimeSpots.put(key, cacheTime);// update the index of this cache key
    		keysTimeSpotsInverse.put(cacheTime, key);
    		
    		cacheTime++; //update cache timing
    		log4j.info("hitAccess"+m_cacheMap.size()+"#"+keysTimeSpots.size());
    		if(m_cacheMap.size()!=keysTimeSpots.size())
    			System.exit(0);
    	}
    	else
    	{
            log4j.error("Was it realy a hit?");
            System.exit(0);
    	}
        log4j.info("Timed_Lru:hitAccess: keyIndices"+keysTimeSpots);
        log4j.info("Timed_Lru:hitAccess: cache_Map"+m_cacheMap.keySet());

    }

    /* 
     * This method updates cache recently used indices in case of cache put situation. (new item)
     * It should add the new item along with its insertion timing as index in indices list
     * The insertion of the item in the cache happens in the caller method 
     */
    /* 
     * @key: the key of the item in the cache (lat:long) 
     */
    @Override
    protected void putAccess(Object key) {
		log4j.info("hitAccess"+m_cacheMap.size()+"#"+keysTimeSpots.size());

    	if(keysTimeSpots.containsKey(key))
    	{
    		log4j.info("Key in cache!");
     		keysTimeSpotsInverse.remove(keysTimeSpots.get(key));
     		keysTimeSpots.remove(key); // remove its entry from keys:indices table
    	}
		log4j.info("putAccess"+m_cacheMap.size()+"#"+keysTimeSpots.size());

		keysTimeSpots.put(key, cacheTime);// Add it with its new timing
 		keysTimeSpotsInverse.remove(keysTimeSpots.get(key));// remove previous time tick ( it can't be replaced with the new time tick in map)
		keysTimeSpotsInverse.put(cacheTime, key); 
		
        log4j.info("Timed_Lru:hitAccess: keyIndices"+keysTimeSpots);
        log4j.info("Timed_Lru:hitAccess: cache_Map"+m_cacheMap.keySet());
        
		if(m_cacheMap.size()!=keysTimeSpots.size()-1)
			System.exit(0);
        cacheTime++;
    }
    @Override
	public void deleteData(IndexItem index) {
		List<Object> removed = removeValues(index);
		if(removed!=null)
			log4j.debug("removed :" + index.toString());
	}
    @Override
    public List<Object> removeValues(Object value) {
        List<Object> removed = super.removeValues(value);
        //m_access.removeAll(removed);
        for (Object key : removed) {
            keysTimeSpotsInverse.remove(keysTimeSpots.get(key));
            keysTimeSpots.remove(key);
		}

        return removed;
    }

/*	//BiMap<String, Integer> keyIndices = HashBiMap.create ();
	BiMap<Object, Integer> keyIndices = HashBiMap.create ();


	int cacheTime = 0; //with each cache access it is incremented except for eviction
	int evictIndex = 0; // it depends on the fact that each item inserted in cache is recorded in keyIndices based on cacheTime in that moment which increases sequentially
	
    *//**
     * logger
     *//*
    public static Logger log4j = Logger.getLogger(LruCache.class);
    
	public TimedLruCache(int size, int evictCount) {
		super(size, evictCount);
	}
	public TimedLruCache(int size, int evictCount,int capacity) {
		super(size, evictCount,capacity);
	}
	*//**
     * Evicts the 1st object or m_evictCount objects form m_access list.
     *//*
    @Override
    protected Object evict() {
    	Object removed=null;
    	int evictCount = m_evictCount;
 //       log4j.info("Timed_Lru: evict 1 item using inverseKeys  <Integer,Obejct>");

		BiMap<Integer,Object> inverseKeyIndices = keyIndices.inverse();

        while(evictCount !=0 && inverseKeyIndices.size() > 0) // for the number of elements to be evicted, second cond. in case of evict > existing elemtn nr.
        {
        	if(inverseKeyIndices.containsKey(evictIndex)) // if the entry of the required index exists
        	{
                log4j.info("eviction number: "+ evictCount);
        		removed = inverseKeyIndices.get(evictIndex);
                log4j.info("Timed_Lru: Item (key) to remove"+((IndexItem)removed).toString());
        		m_cacheMap.remove(removed);// remove the item from the cache
        		keyIndices.remove(removed);
        		
        		log4j.info(m_cacheMap.size()+"#"+keyIndices.size());
        		if(m_cacheMap.size()!=keyIndices.size())
        			System.exit(0);
 //               log4j.info("Timed_Lru: removed from cache and keyIndices");
               log4j.info(m_cacheMap.keySet());
                log4j.info(keyIndices.keySet());


        		evictCount--;
        	}
    		evictIndex++; // update to the next evicted item index
        }
        return removed;
    }
    
     * This method updates cache recently used indices in case of cache hit situation.
     * It should update the existing item's index to new one reflecting its new usage moment
     * key : the key of the stored item in cache (lat:long) 
     * 
    @Override
    protected void hitAccess(Object key) {
    	//super.hitAccess(key);
 //       log4j.info("Timed_Lru: check is it in keyIndices");
     	if(keyIndices.containsKey(key))///////////////////!!
    	{
     		keyIndices.remove(key);
    		keyIndices.put(key, cacheTime);// update the index of this cache key
    		cacheTime++; //update cache timing
    		log4j.info(m_cacheMap.size()+"#"+keyIndices.size());
    		if(m_cacheMap.size()!=keyIndices.size())
    			System.exit(0);
    	}
    	else
    	{
            log4j.error("Was it realy a hit?");
            System.exit(0);
    	}
        log4j.info("Timed_Lru:hitAccess: keyIndices"+keyIndices);
        log4j.info("Timed_Lru:hitAccess: cache_Map"+m_cacheMap.keySet());

    }

     
     * This method updates cache recently used indices in case of cache put situation. (new item)
     * It should add the new item along with its insertion timing as index in indices list
     * The insertion of the item in the cache happens in the caller method 
     
     
     * @key: the key of the item in the cache (lat:long) 
     
    @Override
    protected void putAccess(Object key) {
    	if(keyIndices.containsKey(key))
    	{
    		log4j.info("Key in cache!");
    		keyIndices.remove(key); // remove its entry from keys:indices table
    	}
 //		log4j.info("AbstractCache:putAccess: add"+ key+" to the cache");
		log4j.info(m_cacheMap.size()+"#"+keyIndices.size());

    	keyIndices.put(key, cacheTime); // Add it with its new timing
        log4j.info("Timed_Lru:hitAccess: keyIndices"+keyIndices);
        log4j.info("Timed_Lru:hitAccess: cache_Map"+m_cacheMap.keySet());
		if(m_cacheMap.size()!=keyIndices.size()-1)
			System.exit(0);
        cacheTime++;
    }

    @Override
    public List<Object> removeValues(Object value) {
        List<Object> removed = super.removeValues(value);
        m_access.removeAll(removed);
        keyIndices.clear();
        return removed;
    }*/
	
}
