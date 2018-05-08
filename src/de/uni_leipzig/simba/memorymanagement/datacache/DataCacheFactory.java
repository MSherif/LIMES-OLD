package de.uni_leipzig.simba.memorymanagement.datacache;

public class DataCacheFactory {
	public static int cacheSize=10;
	public static int evictCount=1;
	public static int capacity=1;

	
	public static DataCache createCache(CacheType cacheType, int size, int evCount, int Capacity)
	{
		cacheSize= size;
		evictCount = evCount;
		capacity = Capacity;
		return createCache(cacheType);
	}
	
	public static DataCache createCache(CacheType cacheType)
	{
		if(cacheType.equals(CacheType.LRU))
			return new LruCache(cacheSize, evictCount, capacity);
		else if (cacheType.equals(CacheType.SLRU))
			return new SLruCache(cacheSize, evictCount, capacity);
		else if(cacheType.equals(CacheType.LFU))
			return new LfuCache(cacheSize, evictCount, capacity);
		else if(cacheType.equals(CacheType.LFUDA))
			return new LfuDACache(cacheSize, evictCount);
		else if(cacheType.equals(CacheType.FIFO2nd))
			return new Fifo2ChanceCache(cacheSize, evictCount, capacity);
		else if(cacheType.equals(CacheType.TimedLru))
			return new TimedLruCache(cacheSize, evictCount, capacity);
		else if (cacheType.equals(CacheType.TimedSLru))
			return new TimedSLruCache(cacheSize, evictCount, capacity);
		else
			return new FifoCache(cacheSize, evictCount, capacity);

	}
}
