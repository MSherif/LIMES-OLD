/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;

/**
 * Particle Swarm Optimization load balancer
 * Here, particles (p) are tasks (pair of blocks to be compared),
 * and each thread (t_k) handles a set of tasks T_k = {p_i, p_(i+1), ...}.
 * particle's velocity (v_i) enable particle p_i to move between different T_i,
 * Fitness function = min{|t_max|-|t_min|},
 * where |T_k| is the size of tasks handled by thread k.
 * 
 * @author sherif
 *
 */
public class PSOGeoLoadBalancer extends GeoLoadBalancer{
	private static final Logger logger = Logger.getLogger(PSOGeoLoadBalancer.class.getName());
	public int maxIterationNr = 5;
	protected Map<GeoSquare, GeoSquare> leastUnderloadedTask;
	protected Map<GeoSquare, GeoSquare> mostOverloadedTask;
	protected long  	leastUnderloadedTaskComplexity = Long.MAX_VALUE;
	protected long		mostOverloadedTaskComplexity = 0;
	protected long 	bestKnownFitness = Long.MAX_VALUE;
	protected float	fitnessThreshold = 0;

	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public PSOGeoLoadBalancer(int maxThreadNr) {
		super(maxThreadNr);
	}
	
	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public PSOGeoLoadBalancer(int maxThreadNr, int maxIterationNr) {
		super(maxThreadNr);
		this.maxIterationNr = maxIterationNr;
	}

	//	/* (non-Javadoc)
	//	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getBalancedLoad(com.google.common.collect.Multimap)
	//	 */
	//	@Override
	//	public List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks) {
	//				System.out.println("=== PSOGeoLoadBalancer ===");
	//		long start = System.currentTimeMillis();
	//		// Initialize the particles' positions (distribute tasks equal likely to threads)
	//		// Initialize the particle's best known position to its initial position: pi ← xi
	//		start = System.currentTimeMillis();
	//		for(int threadId = 0 ; threadId < maxThreadNr ; threadId++ ){
	//			Multimap<GeoSquare, GeoSquare> initial = HashMultimap.create();
	//			balancedLoad.add(threadId, initial);
	//		}
	//		int threadId = 0;
	//		for(GeoSquare s : tasks.keySet()){
	//			for(GeoSquare t : tasks.get(s)){
	//				balancedLoad.get(threadId).put(s, t);
	//				threadId = (threadId + 1) % maxThreadNr;
	//			}
	//		}
	//		//		System.out.println("\tTasks distribution time: " + (System.currentTimeMillis() - start) + "ms");
	//
	//		// compute fitness function of initial particles
	//		long maxLoad = 0, minLoad = Long.MAX_VALUE, c = 0;
	//		Multimap<GeoSquare, GeoSquare> maxLoadBlock, minLoadBlock;
	//		for(Multimap<GeoSquare, GeoSquare> taskLoad : balancedLoad){
	//			c = 0;
	//			for(Entry<GeoSquare, GeoSquare> e: taskLoad.entries()){
	//				c += e.getKey().size() * e.getValue().size(); 
	//			}
	//			if(c < minLoad){
	//				minLoad = c;
	//				minLoadBlock = taskLoad;
	//				
	//			}
	//			if(c > maxLoad){
	//				maxLoad = c;
	//				maxLoadBlock = taskLoad;
	//			}
	//		}
	//		bestKnownFitness = maxLoad - minLoad;
	//		//		System.out.println("\tbestKnownFitness = " + bestKnownFitness);
	//
	//		//move particles to get better fitness (move one task per taskLoad)
	//		List<Multimap<GeoSquare, GeoSquare>> newPos = new ArrayList<Multimap<GeoSquare,GeoSquare>>(balancedLoad);
	//		for(int i = 0 ; i < maxIterationNr && bestKnownFitness > fitnessThreshold ; i++){
	//			for(int index = 0 ; index < balancedLoad.size() ; index++){
	//				Multimap<GeoSquare, GeoSquare> taskLoad = balancedLoad.get(index);
	////				if(taskLoad.entries().iterator().hasNext()){
	////					Entry<GeoSquare, GeoSquare> t = taskLoad.entries().iterator().next();
	//				for(Entry<GeoSquare, GeoSquare> t : taskLoad.entries()){
	//					int velocity = (int)(Math.random() * maxThreadNr);
	//					if(velocity != index){
	////						newPos.get(index).remove(t.getKey(), t.getValue());
	//						newPos.get(velocity).put(t.getKey(), t.getValue());
	//						c = 0;
	//						for(Entry<GeoSquare, GeoSquare> e: newPos.get(velocity).entries()){
	//							c += e.getKey().size() * e.getValue().size(); 
	//						}
	//						if(c < minLoad){
	//							minLoad = c;
	//							minLoadBlock = taskLoad;
	//						}
	//						if(c > maxLoad){
	//							maxLoad = c;
	//							maxLoadBlock = taskLoad;
	//						}
	//						long newFitness = maxLoad - minLoad;
	//						
	//						if(newFitness < bestKnownFitness){
	//							System.out.println("\t new fitness = " + newFitness + ", bestKnownFitness = " +bestKnownFitness);
	//							bestKnownFitness = newFitness;
	//							balancedLoad = newPos;
	//							newPos.get(index).remove(t.getKey(), t.getValue());
	////							newPos.get(velocity).put(t.getKey(), t.getValue());
	//						}else{
	//							newPos.get(velocity).remove(t.getKey(), t.getValue());
	//						}
	//					}
	//				}
	//				
	//			}
	////			long newFitness = maxLoad - minLoad;
	////			System.out.println("\t new fitness = " + newFitness + ", bestKnownFitness = " +bestKnownFitness);
	////			if(newFitness < bestKnownFitness){
	////				bestKnownFitness = newFitness;
	////				balancedLoad = newPos;
	////
	////			}
	//		}
	//		return balancedLoad;
	//	}



	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getBalancedLoad(com.google.common.collect.Multimap)
	 */
	@Override
	public List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks) {
		logger.info("Starting PSOGeoLoadBalancer ...");

		long start = System.currentTimeMillis();

		//Compute tasks' complexity
		//computeTasksComplexity(tasks);
		//System.out.println("\t computeTasksComplexity : " + (System.currentTimeMillis() - start) + " ms");

		//computeMostOverUndeloadedTasks();

		// Initialize the particles' positions (distribute tasks equal likely to threads)
		// Initialize the particle's best known position to its initial position: pi ← xi
		start = System.currentTimeMillis();
		for(int threadId = 0 ; threadId < maxThreadNr ; threadId++ ){
			Multimap<GeoSquare, GeoSquare> initial = HashMultimap.create();
			balancedTaskBlocks.add(threadId, initial);
		}
		int threadId = 0;
		for(GeoSquare s : tasks.keySet()){
			for(GeoSquare t : tasks.get(s)){
				balancedTaskBlocks.get(threadId).put(s, t);
				threadId = (threadId + 1) % maxThreadNr;
			}
		}
		//		System.out.println("\tTasks distribution time: " + (System.currentTimeMillis() - start) + "ms");

		// compute fitness function of initial particles
		bestKnownFitness = fitness(balancedTaskBlocks);
		//		System.out.println("\tbestKnownFitness = " + bestKnownFitness);

		//		//move particles to get better fitness (from most overloaded task to the least underloaded one)
		//		List<Multimap<GeoSquare, GeoSquare>> newPos = new ArrayList<Multimap<GeoSquare,GeoSquare>>(bestKnownPos);
		//		for(int i = 0 ; i < maxIterationNr && bestKnownFitness > fitnessThreshold ; i++){
		//			int velocity = (int)(Math.random() * (maxThreadNr + 1));
		//			newPos.remove(leastUnderloadedTask);
		//			newPos.remove(mostOverloadedTask);
		//			leastUnderloadedTask.entrySet().iterator().next();
		//		}

		//move particles to get better fitness (move one task per taskLoad)
		List<Multimap<GeoSquare, GeoSquare>> newPos = new ArrayList<Multimap<GeoSquare,GeoSquare>>(balancedTaskBlocks);
		for(int itrNr = 0 ; itrNr < maxIterationNr && bestKnownFitness > fitnessThreshold ; itrNr++){
			for(int index = 0 ; index < balancedTaskBlocks.size() ; index++){
				Multimap<GeoSquare, GeoSquare> taskLoad = balancedTaskBlocks.get(index);
				if(taskLoad.entries().iterator().hasNext()){
					Entry<GeoSquare, GeoSquare> t = taskLoad.entries().iterator().next();
					int velocity = (int)(Math.random() * maxThreadNr);
					if(velocity != index){
						newPos.get(index).remove(t.getKey(), t.getValue());
						newPos.get(velocity).put(t.getKey(), t.getValue());
					}
				}
			}
			long f = fitness(newPos);
//			System.out.println("\t new fitness = " + f + ", bestKnownFitness = " +bestKnownFitness);
			if(f < bestKnownFitness){
				bestKnownFitness = f;
				balancedTaskBlocks = newPos;
			}
		}
		return balancedTaskBlocks;
	}

	/**
	 * compute the most overloaded and underloaded tasks
	 * 
	 * @author sherif
	 */
	private void computeMostOverUndeloadedTasks() {
		for(Entry<Map<GeoSquare, GeoSquare>, Long> e : tasks2Complexity.entrySet()){
			Map<GeoSquare, GeoSquare> key = e.getKey();
			long value = e.getValue();
			if(value > mostOverloadedTaskComplexity){
				mostOverloadedTaskComplexity = value;
				mostOverloadedTask = key;
			}
			if(value < leastUnderloadedTaskComplexity){
				leastUnderloadedTaskComplexity = value;
				leastUnderloadedTask = key;
			}
		}
	}

	/**
	 * Fitness function = min{|T_max|-|T_min|},
	 * where |T_n| is the size of tasks handled by thread n.
	 * 
	 * @param bestKnownPos
	 * @return
	 * @author sherif
	 */
	private long fitness(List<Multimap<GeoSquare, GeoSquare>> taskBlocks) {
		long maxTask = 0, minTask = Long.MAX_VALUE, c = 0;

		for(Multimap<GeoSquare, GeoSquare> taskBlock : taskBlocks){
			c = 0;
			for(Entry<GeoSquare, GeoSquare> task : taskBlock.entries()){
				c += task.getKey().size() * task.getValue().size(); 
			}
			if(c < minTask){
				minTask = c;
			}
			if(c > maxTask){
				maxTask = c;
			}
		}
		return (maxTask - minTask);
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getName()
	 */
	@Override
	public String getName() {
		return "PSOGeoLoadBalancer";
	}

}













