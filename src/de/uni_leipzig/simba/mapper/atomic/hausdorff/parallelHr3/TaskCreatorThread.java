/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory.Type;

/**
 * @author sherif
 *
 */
public class TaskCreatorThread extends GeoHR3 implements Runnable{
	protected GeoIndex source;
	protected GeoIndex target;
	protected static Multimap<GeoSquare, GeoSquare> tasks = HashMultimap.create();
	public static Semaphore allDone;
	protected int start, size;
	
	/**
	 * @param distanceThreshold
	 * @param granularity
	 * @param hd
	 *@author sherif
	 */
	public TaskCreatorThread(float distanceThreshold, int granularity, Type hd, GeoIndex source, GeoIndex target, int start, int size) {
		super(distanceThreshold, granularity, hd);
		this.source = source;
		this.target = target;
		this.start = start;
		this.size = size;
	}


	/**
	 * @return the tasks
	 */
	public static Multimap<GeoSquare, GeoSquare> getTasks() {
		return tasks;
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
//		long begin = System.currentTimeMillis();
		try{
			int i = 0; // deal only with the part starting from (start) to (start+size)
			for (Integer latIndex : source.squares.keySet()) {
				if(i < start){
					i++;
				}else{
					for (Integer longIndex : source.squares.get(latIndex).keySet()) {
						if(i > (start + size)){
							break;
						}
						GeoSquare g1 = source.getSquare(latIndex, longIndex);
						Set<List<Integer>> squares = getSquaresToCompare(latIndex, longIndex, target);
						for (List<Integer> squareIndex : squares) {
							GeoSquare g2 = target.getSquare(squareIndex.get(0), squareIndex.get(1));
							if(!g1.elements.isEmpty() && !g2.elements.isEmpty()){
								synchronized (tasks) {
									tasks.put(g1, g2);
								}
							}

						}
						i++;
					}
				}
			}
//			synchronized (allDone) {
			allDone.release();
//			}
//			System.out.pr<intln("\t$$$$$ TaskCreatorThread: " + (System.currentTimeMillis()-begin) + " ms");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}




