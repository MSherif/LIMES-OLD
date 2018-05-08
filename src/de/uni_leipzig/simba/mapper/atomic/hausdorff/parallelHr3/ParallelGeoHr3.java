/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.jgap.impl.GreedyCrossover;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonReader;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GreedyGeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.PSOGeoLoadBalancer;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory.Type;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.CentroidIndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.IndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.NaiveHausdorff;

/**
 * @author sherif
 *
 */
public class ParallelGeoHr3 extends GeoHR3 {

	public long loadBalancerTime;
	public int maxThreadNr;
	protected GeoLoadBalancer geoLoadBalancer;
	public long tasksCreationTime;

	/**
	 * @param distanceThreshold
	 * @param granularity
	 * @param hd
	 *@author sherif
	 */
	public ParallelGeoHr3(float distanceThreshold, int granularity, Type hd, GeoLoadBalancer geoLoadBalancer, int maxThreadsNr) {
		super(distanceThreshold, granularity, hd);
		this.geoLoadBalancer = geoLoadBalancer; 
		this.maxThreadNr = maxThreadsNr;
	}

	public GeoIndex assignSquares(Set<Polygon> input) {
		ParallelPolygonGeoIndexer  parallelPolygonGeoIndexer = new ParallelPolygonGeoIndexer(input, delta, maxThreadNr);
		parallelPolygonGeoIndexer.run();
		return parallelPolygonGeoIndexer.getIndex();
	}


	/**
	 * Runs GeoHR3 for source and target dataset. Uses the set SetMeasure
	 * implementation. FastHausdorff is used as default
	 *
	 * @param sourceData Source polygons
	 * @param targetData Target polygons
	 * @return Mapping of polygons
	 */
	public Mapping run(Set<Polygon> sourceData, Set<Polygon> targetData) {
		long begin = System.currentTimeMillis();
		GeoIndex source = assignSquares(sourceData);
		GeoIndex target = assignSquares(targetData);
		long end = System.currentTimeMillis();
		indexingTime = end - begin;
//		System.out.println("Parallel Geo-Indexing took: " + indexingTime + " ms");
		if(verbose){
			System.out.println("Geo-Indexing took: " + indexingTime + " ms");
			System.out.println("|Source squares|= " + source.squares.keySet().size());
			System.out.println("|Target squares|= " + target.squares.keySet().size());
			System.out.println("Distance Threshold = " + distanceThreshold);
			System.out.println("Angular Threshold = " + angularThreshold);
			System.out.println("Parallel index = " + source);
		}
		
		if (setMeasure instanceof CentroidIndexedHausdorff) {
			((CentroidIndexedHausdorff) setMeasure).computeIndexes(sourceData, targetData);
		} else if (setMeasure instanceof IndexedHausdorff) {
			PolygonIndex targetIndex = new PolygonIndex();
			targetIndex.index(targetData);
			((IndexedHausdorff) setMeasure).targetIndex = targetIndex;
		}

		
		//create tasks as pairs of squares to be compared
		Multimap<GeoSquare, GeoSquare> tasks = createTasks(source, target); 
//		Multimap<GeoSquare, GeoSquare> tasks = createTasksParallel(source, target);
//		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5");
//		for(Entry<GeoSquare, GeoSquare> e : tasks.entries()){
//			System.out.println(e.getKey().size() * e.getValue().size());
//		}

		// Divide tasks equal likely between threads 
		begin = System.currentTimeMillis();
		List<GeoHR3Thread> threads = new ArrayList<GeoHR3Thread>();
		System.out.println("************************>>>>>>>>>>" + tasks.size());
		List<Multimap<GeoSquare, GeoSquare>> balancedLoad = geoLoadBalancer.getBalancedLoad(tasks);
		loadBalancerTime = System.currentTimeMillis() - begin;
//		System.out.println("(Overhead) " + geoLoadBalancer.getName() + " took: " + loadBalancerTime + " ms");
		
//		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//		for (Multimap<GeoSquare, GeoSquare> multimap : balancedLoad) {
//			System.out.println(multimap.size());
//		}
		
		
		// Run concurrently
		begin = System.currentTimeMillis();
		GeoHR3Thread.allDone = new Semaphore(0);
		for(Multimap<GeoSquare, GeoSquare> task : balancedLoad){
			GeoHR3Thread t = new GeoHR3Thread(setMeasure, distanceThreshold, task);
					(new Thread(t)).start();
					threads.add(t);
		}
		
		// Wait until all threads finished
		try {
			GeoHR3Thread.allDone.acquire(maxThreadNr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		System.out.println("Whole threads execution took: " + (System.currentTimeMillis() - begin) + " ms");
		return GeoHR3Thread.getMapping();
	}

	/**
	 * @param source
	 * @param target
	 * @return
	 * @author sherif
	 */
	protected Multimap<GeoSquare, GeoSquare> createTasks(GeoIndex source, GeoIndex target) {
		long begin;
		begin = System.currentTimeMillis();
		Multimap<GeoSquare, GeoSquare> tasks = HashMultimap.create();
		for (Integer latIndex : source.squares.keySet()) {
			for (Integer longIndex : source.squares.get(latIndex).keySet()) {
				GeoSquare g1 = source.getSquare(latIndex, longIndex);
				Set<List<Integer>> squares = getSquaresToCompare(latIndex, longIndex, target);
				for (List<Integer> squareIndex : squares) {
					GeoSquare g2 = target.getSquare(squareIndex.get(0), squareIndex.get(1));
					if(!g1.elements.isEmpty() && !g2.elements.isEmpty()){
						tasks.put(g1, g2);
					}
				}
			}
		}
		System.out.println("(Overhead) Tasks creation took: " + (System.currentTimeMillis() - begin) + " ms");
		return tasks;
	}
	
	/**
	 * use multi-threading to create tasks
	 * @param source
	 * @param target
	 * @return
	 * @author sherif
	 */
	protected Multimap<GeoSquare, GeoSquare> createTasksParallel(GeoIndex source, GeoIndex target) {
		long begin = System.currentTimeMillis();
		TaskCreatorThread.allDone = new Semaphore(0);
		
		List<TaskCreatorThread> threads = new ArrayList<TaskCreatorThread>();
		int subsetLength = (int) Math.ceil((double) source.squares.size() / maxThreadNr);
		for(int i = 0 ; i < source.squares.size() ; i += subsetLength ){
			TaskCreatorThread t = new TaskCreatorThread(this.distanceThreshold, this.granularity, null, source, target, i, subsetLength);
			(new Thread(t)).start();
			threads.add(t);
		}
		
		try {
			TaskCreatorThread.allDone.acquire(maxThreadNr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		tasksCreationTime = System.currentTimeMillis() - begin;
//		System.out.println("(Overhead) Parallel Tasks creation took: " + tasksCreationTime + " ms");
		return TaskCreatorThread.getTasks();
	}


	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {

		System.out.println("-------------------- GeoHR3 --------------------");
		Set<Polygon> sourcePolygonSet = PolygonReader.readPolygons(args[0], Integer.parseInt(args[1]));
		long startTime = System.currentTimeMillis();
		GeoHR3 geoHr3 = new GeoHR3(0.5f, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(new NaiveHausdorff()));
//		geoHr3.verbose = true;
		Mapping m1 = geoHr3.run(sourcePolygonSet, sourcePolygonSet);
//		System.out.println("Result Mapping: \n" + m1);
		long hr3Time = System.currentTimeMillis() - startTime;
		System.out.println("GeoHr3 total time = " + hr3Time + "ms");

		System.out.println("-------------------- Parallel GeoHR3 --------------------");
		for(int threadNr = 2; threadNr <= 2 ; threadNr *= 2){
			System.out.println("Number of Threads = " + threadNr);
			startTime = System.currentTimeMillis();
//			ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(1f, GeoHR3.DEFAULT_GRANULARITY, 
//					SetMeasureFactory.getType(new NaiveHausdorff()), new NaiveGeoLoadBalancer(threadNr), threadNr);
//			ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(1f, GeoHR3.DEFAULT_GRANULARITY, 
//					SetMeasureFactory.getType(new NaiveHausdorff()), new GreedyGeoLoadBalancer(threadNr), threadNr);
			ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(1f, GeoHR3.DEFAULT_GRANULARITY, 
					SetMeasureFactory.getType(new NaiveHausdorff()), new PSOGeoLoadBalancer(threadNr), threadNr);
			
			
			System.out.println("ParallelGeoHr3 Constructor time = " + (System.currentTimeMillis() - startTime) + "ms");
			
//			parallelGeoHr3.verbose = true;
			Mapping m2 = parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
			long parallelHr3Time = System.currentTimeMillis() - startTime;
			System.out.println("ParallelGeoHr3 total time = " + parallelHr3Time + "ms");
			double speadUp = ((double)(hr3Time - parallelHr3Time) / hr3Time) * 100;
			System.out.println("Speed up = " + speadUp + "%");
//			startTime = System.currentTimeMillis();
//			 m2 = parallelGeoHr3_2.run(sourcePolygonSet, sourcePolygonSet);
//			 System.out.println("ParallelGeoHr3 total time = " + (System.currentTimeMillis() - startTime) + "ms");
//			 startTime = System.currentTimeMillis();
//			 m2 = parallelGeoHr3_3.run(sourcePolygonSet, sourcePolygonSet);
//			System.out.println("Result Mapping: \n" + m2);
//			System.out.println("ParallelGeoHr3 total time = " + (System.currentTimeMillis() - startTime) + "ms");
			
		}
	}

}
