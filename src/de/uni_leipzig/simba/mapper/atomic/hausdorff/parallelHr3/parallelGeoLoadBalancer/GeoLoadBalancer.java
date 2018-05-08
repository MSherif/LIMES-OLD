/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;

/**
 * @author sherif
 *
 */
public abstract class GeoLoadBalancer {
	public int maxThreadNr = 4; 
	protected Map<Map<GeoSquare, GeoSquare>,Long> tasks2Complexity;
	protected List<Multimap<GeoSquare, GeoSquare>> balancedTaskBlocks = new ArrayList<Multimap<GeoSquare,GeoSquare>>();


	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public GeoLoadBalancer(int maxThreadNr) {
		super();
		this.maxThreadNr = maxThreadNr;
	}

	/**
	 * @return
	 * @author sherif
	 */
	public abstract List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks);
	public abstract String getName();

	/**
	 * Compute tasksComplexity for each task
	 * and returning it as a Map
	 *
	 * @param tasks
	 * @author sherif
	 */
	protected Map<Map<GeoSquare, GeoSquare>, Long> computeTasksComplexity(Multimap<GeoSquare, GeoSquare> tasks) {
		long begin = System.currentTimeMillis();
		tasks2Complexity = new HashMap<Map<GeoSquare,GeoSquare>, Long>();
		for(GeoSquare s : tasks.keySet()){
			for(GeoSquare t : tasks.get(s)){
				Long value = s.size() * t.size();
				Map<GeoSquare, GeoSquare> key = new HashMap<GeoSquare, GeoSquare>();
				key.put(s, t);
				tasks2Complexity.put(key, value);
			}
		}
//		System.out.println("\t computeTasksComplexity took " + (System.currentTimeMillis() - begin) + " ms");
		return tasks2Complexity;
	}

	/**
	 * using multi-threading 
	 * Compute tasksComplexity for each task
	 * and returning it as a Map
	 *
	 * @param tasks
	 * @author sherif
	 */
	protected Map<Map<GeoSquare, GeoSquare>, Long> _computeTasksComplexityParallel(Multimap<GeoSquare, GeoSquare> tasks) {
		long begin = System.currentTimeMillis();
		TaskComplexityThread.allDone = new Semaphore(0);
		// Make and start all the threads, keeping them in a list.
		List<TaskComplexityThread> threads = new ArrayList<TaskComplexityThread>();
		int subsetLength = (int) Math.ceil((float) tasks.size() / maxThreadNr);

		Multimap<GeoSquare, GeoSquare> subset;
		Iterator<Entry<GeoSquare, GeoSquare>> itr = tasks.entries().iterator();
		while(itr.hasNext()){
			subset = HashMultimap.create();
			for(int i = 0 ; i < subsetLength && itr.hasNext() ; i++){
				Entry<GeoSquare, GeoSquare> e = itr.next();
				subset.put(e.getKey(), e.getValue());
			}
			TaskComplexityThread t = new TaskComplexityThread(subset);
			(new Thread(t)).start();
			threads.add(t);
		}

		// Wait to finish (this strategy is an alternative to join())
		try {
			TaskComplexityThread.allDone.acquire(maxThreadNr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		tasks2Complexity = TaskComplexityThread.getTasks2Complexity();
		System.out.println("\t _computeTasksComplexityParallel took " + (System.currentTimeMillis() - begin) + " ms");
		return tasks2Complexity;
	}

	/**
	 *
	 * using multi-threading 
	 * Compute tasksComplexity for each task
	 * and returning it as a Map
	 *
	 * @param tasks
	 * @author sherif
	 */
	//TODO find why it tacks more time than serial execution?
	protected Map<Map<GeoSquare, GeoSquare>, Long> computeTasksComplexityParallel(Multimap<GeoSquare, GeoSquare> tasks) {
		long begin = System.currentTimeMillis();
		TaskComplexityThread.allDone = new Semaphore(0);
		// Make and start all the threads, keeping them in a list.
		List<TaskComplexityThread> threads = new ArrayList<TaskComplexityThread>();
		int subsetLength = (int) Math.ceil((float) tasks.size() / maxThreadNr);
		for(int i = 0 ; i < tasks.size() ; i += subsetLength ){
			TaskComplexityThread t = new TaskComplexityThread(tasks, i, subsetLength);
			(new Thread(t)).start();
			threads.add(t);
		}

		// Wait to finish (this strategy is an alternative to join())
		try {
			TaskComplexityThread.allDone.acquire(maxThreadNr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		tasks2Complexity = TaskComplexityThread.getTasks2Complexity();
		System.out.println("\t computeTasksComplexityParallel took " + (System.currentTimeMillis() - begin) + " ms");
		return tasks2Complexity;
	}
	
	public double getMeanSquaredError(){//List<Multimap<GeoSquare, GeoSquare>>
		double mse = 0;
		double wholeTasksSize = 0;
		for(Multimap<GeoSquare, GeoSquare> load :  balancedTaskBlocks){
			for (Entry<GeoSquare, GeoSquare> e : load.entries()) {
				wholeTasksSize += e.getKey().size() * e.getValue().size();
			}
		}
		double averageBlockSize = wholeTasksSize / balancedTaskBlocks.size();
		for(int i =  0; i < balancedTaskBlocks.size(); i++){
			float currentSize = 0;
			for (Entry<GeoSquare, GeoSquare> e : balancedTaskBlocks.get(i).entries()) {
				currentSize += e.getKey().size() * e.getValue().size();
			}
			double error = currentSize - averageBlockSize;
//			float error = balancedLoad.get(i).size() - averageBlockSize;
			mse += error * error ;
		}
		mse /= balancedTaskBlocks.size();
		return mse;
		
	}

}
