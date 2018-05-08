/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.ParallelPolygonGeoIndexer.GeoIndexerTheard;
import de.uni_leipzig.simba.measures.pointsets.SetMeasure;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory.Type;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.CentroidIndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.IndexedHausdorff;

/**
 * @author sherif
 *
 */
public class GeoHR3Thread  implements Runnable{
	protected Multimap<GeoSquare, GeoSquare> task;
	protected SetMeasure setMeasure;
	protected float distanceThreshold;
	protected static Mapping mapping = new Mapping();
	public static Semaphore	allDone;
	Map<String, Set<String>> computed = new HashMap<String, Set<String>>();




	/**
	 * create a task out of one pair of squares
	 * @param setMeasure
	 * @param distanceThreshold
	 * @param sourcesSquare
	 * @param targetSquare
	 *@author sherif
	 */
	public GeoHR3Thread(SetMeasure setMeasure, float distanceThreshold, GeoSquare sourcesSquare, GeoSquare targetSquare) {
		this.setMeasure = setMeasure;
		this.distanceThreshold = distanceThreshold;
		this.task = HashMultimap.create();
		this.task.put(sourcesSquare, targetSquare);
	}

	/**
	 * @param setMeasure2
	 * @param distanceThreshold2
	 * @param task
	 *@author sherif
	 */
	public GeoHR3Thread(SetMeasure setMeasure, float distanceThreshold, Multimap<GeoSquare, GeoSquare> task) {
		this.setMeasure = setMeasure;
		this.distanceThreshold = distanceThreshold;
		this.task = task;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
//		long start = System.currentTimeMillis();
		try{
			for(GeoSquare sourcesSquare : task.keySet()){
				for(GeoSquare targetSquare : task.get(sourcesSquare)){
					// only run if the hypercube actually exists
					for (Polygon a : sourcesSquare.elements) {
						for (Polygon b : targetSquare.elements) {
							if (!computed.containsKey(a.uri)) {
								computed.put(a.uri, new HashSet<String>());
							}
							if (!computed.get(a.uri).contains(b.uri)) {
								double d = setMeasure.computeDistance(a, b, distanceThreshold);
								if (d <= distanceThreshold) {
									synchronized (mapping) {
										mapping.add(a.uri, b.uri, 1/(1+d));
									}
								}
							}
							computed.get(a.uri).add(b.uri);
						}
					}

				}
			}
//			System.out.println("\t$$$$ GeoHR3Thread "+ (System.currentTimeMillis() - start) + "ms" );
			//			synchronized (allDone) {
			allDone.release();
			//			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * @return the mapping
	 */
	public static Mapping getMapping() {
		return mapping;
	}


	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}




