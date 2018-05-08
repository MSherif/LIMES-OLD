/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;

/**
 * Distribute tasks to threads regardless of its complexity
 * @author sherif
 *
 */
public class NaiveGeoLoadBalancer extends GeoLoadBalancer{
	private static final Logger logger = Logger.getLogger(GreedyGeoLoadBalancer.class.getName());

	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public NaiveGeoLoadBalancer(int maxThreadNr) {
		super(maxThreadNr);
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getBalancedLoad(java.util.Map, int)
	 */
	@Override
	public List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks) {
		logger.info("Starting NaiveGeoLoadBalancer ...");
		
//		long start = System.currentTimeMillis();
		

		// initialize result
		for(int threadId = 0 ; threadId < maxThreadNr ; threadId++ ){
			Multimap<GeoSquare, GeoSquare> initial = HashMultimap.create();
			balancedTaskBlocks.add(initial);
		}

		// distribute tasks
		int threadId = 0;
		for(GeoSquare s : tasks.keySet()){
			for(GeoSquare t : tasks.get(s)){
				balancedTaskBlocks.get(threadId).put(s, t);
				threadId = (threadId + 1) % maxThreadNr;
			}
		}
		
//		System.out.println("\tdistribute tasks time: " + (System.currentTimeMillis() - start) + "ms");
		return balancedTaskBlocks;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getName()
	 */
	@Override
	public String getName() {
		return "NaiveGeoLoadBalancer";
	}



}
