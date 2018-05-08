/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;

/**
 * @author sherif
 *
 */
public class TaskComplexityThread  implements Runnable{
	protected Multimap<GeoSquare, GeoSquare> tasks;
	protected static Map<Map<GeoSquare,GeoSquare>, Long> tasks2Complexity = new HashMap<Map<GeoSquare,GeoSquare>, Long>();
	protected int start, size;
	public static Semaphore	allDone;


	/**
	 * @param subset
	 *@author sherif
	 */
	public TaskComplexityThread(Multimap<GeoSquare, GeoSquare> tasks) {
		this.tasks = tasks;
	}


	/**
	 * @param tasks2
	 * @param i
	 * @param subsetLength
	 *@author sherif
	 */
	public TaskComplexityThread(Multimap<GeoSquare, GeoSquare> tasks, int start, int size) {
		this.tasks = tasks;
		this.start = start;
		this.size = size;
	}


//		/* (non-Javadoc)
//		 * @see java.lang.Runnable#run()
//		 */
//		@Override
//		public void run() {
//			long start = System.currentTimeMillis();
//			try{
//				for(GeoSquare s : tasks.keySet()){
//					long sourceSquareComplexity = 0l;
//					for(Polygon p : s.elements){
//						sourceSquareComplexity += (long) p.points.size();
//					}
//					for(GeoSquare t : tasks.get(s)){
//						long targetSquareComplexity = 0l;
//						for(Polygon p : t.elements){
//							targetSquareComplexity += (long) p.points.size();
//						}
//						Long value = sourceSquareComplexity * targetSquareComplexity;
//						Map<GeoSquare, GeoSquare> key = new HashMap<GeoSquare, GeoSquare>();
//						key.put(s, t);
//						synchronized (tasks2Complexity) {
//							tasks2Complexity.put(key, value);
//						}
//					}
//				}
//				System.out.println("Task:" +Thread.currentThread().getName() + " input size = " + tasks.size() + " done in "+ (System.currentTimeMillis() - start) + "ms" );
//	//			synchronized (allDone) {
//				allDone.release();
//	//			}
//			}catch (Exception e) {
//				e.printStackTrace();
//			}
//		}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
//		long begin = System.currentTimeMillis();
		try{
			// deal only with the part starting from (start) to (start+size)
			int i = 0; 
			for(GeoSquare s : tasks.keySet()){
				if(i < start){
					i++;
				}else if(i > (start + size)){
					break;
				}else{
					i++;
					long sourceSquareComplexity = 0l;
					for(Polygon p : s.elements){
						sourceSquareComplexity += (long) p.points.size();
					}
					for(GeoSquare t : tasks.get(s)){
						long targetSquareComplexity = 0l;
						for(Polygon p : t.elements){
							targetSquareComplexity += (long) p.points.size();
						}
						Long value = sourceSquareComplexity * targetSquareComplexity;
						Map<GeoSquare, GeoSquare> key = new HashMap<GeoSquare, GeoSquare>();
						key.put(s, t);
						synchronized (tasks2Complexity) {
							tasks2Complexity.put(key, value);
						}
					}
				}
			}
			// synchronized (allDone) {
			allDone.release();
			// }
		}catch (Exception e) {
			e.printStackTrace();
		}
//		System.out.println("\t$$$$ TaskComplexityThread:" + (System.currentTimeMillis() - begin) + " ms" );
	}



	/**
	 * @return the mapping
	 */
	public static Map<Map<GeoSquare,GeoSquare>, Long> getTasks2Complexity() {
		return tasks2Complexity;
	}


	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}




