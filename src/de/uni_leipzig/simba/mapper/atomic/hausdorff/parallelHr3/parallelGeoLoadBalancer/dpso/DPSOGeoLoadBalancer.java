/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.dpso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.DecimalFormat;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer;

/**
 * The main Idea here is to find both the most overloaded 
 * task block and the least underlaoded task block,
 * Based on blocks' complexity, sort tasks, find most and least loaded block 
 * Then, move tasks in order from most overloaded to the least underloaded block 
 * until best balancing found (between those 2 blocks). 
 * This technique can be repeated as many as requested.
 * 
 * @author sherif
 *
 */
public class DPSOGeoLoadBalancer extends GeoLoadBalancer{
	private static final Logger logger = Logger.getLogger(DPSOGeoLoadBalancer.class.getName());
	public int iterationCount = 1;
	protected List<LBTaskBlock> taskBlocks = new ArrayList<LBTaskBlock>(maxThreadNr);
	//	protected long maxTaskIndex = 0, maxTask = 0;
	//	protected long minTaskIndex = 0, minTask = Long.MAX_VALUE;
	protected long bestKnownFitness;
	protected float	fitnessThreshold = 0;


	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public DPSOGeoLoadBalancer(int maxThreadNr) {
		super(maxThreadNr);
	}

	/**
	 * @param maxThreadNr
	 *@author sherif
	 */
	public DPSOGeoLoadBalancer(int maxThreadNr, int iterationCount) {
		super(maxThreadNr);
		this.iterationCount = iterationCount;
	}

	//	/* (non-Javadoc)
	//	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getBalancedLoad(com.google.common.collect.Multimap)
	//	 */
	//	@Override
	//	public List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks) {
	//		logger.info("Starting DPSO LoadBalancer ...");
	//
	//		long start = System.currentTimeMillis();
	//
	//		start = System.currentTimeMillis();
	//		for(int threadId = 0 ; threadId < maxThreadNr ; threadId++ ){
	//			taskBlocks.add(threadId, new LBTaskBlock());
	//		}
	//
	//		int threadId = 0;
	//		for(GeoSquare s : tasks.keySet()){
	//			for(GeoSquare t : tasks.get(s)){
	//				taskBlocks.get(threadId).add(new LBTask(s,t));
	//				threadId = (threadId + 1) % maxThreadNr;
	//			}
	//		}
	//		//		System.out.println("\tTasks distribution time: " + (System.currentTimeMillis() - start) + "ms");
	//
	//		for(int itrNr = 0 ; itrNr < iterationCount ; itrNr++){
	//			// Sort blocks and get most and least loaded blocks, compute difference in complexity
	//			Collections.sort(taskBlocks);
	//			LBTaskBlock minBlock = taskBlocks.get(0);
	//			LBTaskBlock maxBlock = taskBlocks.get(taskBlocks.size()-1);
	////			System.out.println("==> minBlock:" + minBlock.complexity);
	////			System.out.println("==> maxBlock:" + maxBlock.complexity);
	//			long diff = Math.abs(maxBlock.getComplexity() - minBlock.getComplexity());
	////			System.out.println("Diff="+diff);
	//			
	//			List<LBTask> movedTasks = new ArrayList<LBTask>();
	//			long maxBlockNewComplixity = 0;
	//			long minBlockNewComlixity = 0;
	//			// Sort maxBlock. Until balanced, move one task at time to minBlock
	//			taskBlocks.get(taskBlocks.size()-1).sort();
	//			maxBlock = taskBlocks.get(taskBlocks.size()-1);
	//			for (LBTask task : maxBlock.tasks) {
	//				maxBlockNewComplixity += maxBlock.complexity - task.complexity;
	//				minBlockNewComlixity  += minBlock.complexity + task.complexity;
	//				long newDiff = Math.abs(maxBlockNewComplixity - minBlockNewComlixity);
	////				System.out.println("newDiff="+newDiff);
	//				if(newDiff >= diff){
	//					break;
	//				}else{
	//					movedTasks.add(task);
	//					diff = newDiff; 
	//				}
	//			}
	//			// move tasks from maxBlock to MinBlock
	//			for (LBTask mt : movedTasks) {
	//				taskBlocks.get(0).add(mt);
	//				taskBlocks.get(taskBlocks.size()-1).remove(mt);
	//			}
	////			System.out.println("==> minBlock:" + taskBlocks.get(0).complexity);
	////			System.out.println("==> maxBlock:" + taskBlocks.get(taskBlocks.size()-1).complexity);
	//		}
	//		int index = 0;
	//
	//		//convert to balancedTaskBlocks structure
	//		for(LBTaskBlock taskBlock: taskBlocks){
	//			Multimap<GeoSquare, GeoSquare> initial = HashMultimap.create();
	//			balancedTaskBlocks.add(index, initial);
	//			for(LBTask task : taskBlock.tasks){
	//				balancedTaskBlocks.get(index).put(task.sourceGeoSqare, task.targetGeoSqare);
	//			}
	//			index++;
	//		}
	//		return balancedTaskBlocks;
	//	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getName()
	 */
	@Override
	public String getName() {
		return "NewLoadBalancer";
	}


	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GeoLoadBalancer#getBalancedLoad(com.google.common.collect.Multimap)
	 */
	@Override
	public List<Multimap<GeoSquare, GeoSquare>> getBalancedLoad(Multimap<GeoSquare, GeoSquare> tasks) {
		logger.info("Starting DPSO LoadBalancer ...");

		long start = System.currentTimeMillis();

		//		start = System.currentTimeMillis();
		for(int threadId = 0 ; threadId < maxThreadNr ; threadId++ ){
			taskBlocks.add(threadId, new LBTaskBlock());
		}

		int threadId = 0;
		for(GeoSquare s : tasks.keySet()){
			for(GeoSquare t : tasks.get(s)){
				taskBlocks.get(threadId).add(new LBTask(s,t));
				threadId = (threadId + 1) % maxThreadNr;
			}
		}
		//		System.out.println("\tTasks distribution time: " + (System.currentTimeMillis() - start) + "ms");
		int oldMinBlockIndex = -1, oldMaxBlockIndex = -1, iteration = 1;
		int minBlockIndex = genMinBlockIndex(taskBlocks);
		int maxBlockIndex = genMaxBlockIndex(taskBlocks);
		while(minBlockIndex != oldMinBlockIndex && maxBlockIndex != oldMaxBlockIndex){
			System.out.println("------------- " + iteration++ + " -----------");
			oldMinBlockIndex = minBlockIndex;
			oldMaxBlockIndex = maxBlockIndex;
			LBTaskBlock minBlock = taskBlocks.get(minBlockIndex);
			LBTaskBlock maxBlock = taskBlocks.get(maxBlockIndex);
			
			System.out.println("=> minBlock:" + minBlock.complexity);
			System.out.println("=> maxBlock:" + maxBlock.complexity);
			long C=0, L=0; for(LBTaskBlock b:taskBlocks){C += b.complexity; L+= b.tasks.size();}
			DecimalFormat df = new DecimalFormat("#,###");
			System.out.println("taskBlocks(" + taskBlocks.size() + "):" + taskBlocks + "totalComplixity:" +df.format(C)+ "<totalTaskNr:"+L + ">");
			
			
			
			long diff = Math.abs(maxBlock.getComplexity() - minBlock.getComplexity());
			//			System.out.println("Diff="+diff);

			List<LBTask> movedTasks = new ArrayList<LBTask>();
			long maxBlockNewComplixity = 0;
			long minBlockNewComlixity = 0;
			// Sort maxBlock. Until balanced, move one task at time to minBlock
			maxBlock.sort();
			for (LBTask task : maxBlock.tasks) {
				maxBlockNewComplixity += maxBlock.complexity - task.complexity;
				minBlockNewComlixity  += minBlock.complexity + task.complexity;
				long newDiff = Math.abs(maxBlockNewComplixity - minBlockNewComlixity);
				//				System.out.println("newDiff="+newDiff);
				if(newDiff >= diff){
					break;
				}else{
					movedTasks.add(task);
					diff = newDiff; 
				}
			}
			// move tasks from maxBlock to MinBlock
			for (LBTask mt : movedTasks) {
				taskBlocks.get(minBlockIndex).add(mt);
				taskBlocks.get(maxBlockIndex).remove(mt);
			}
			minBlockIndex = genMinBlockIndex(taskBlocks);
			maxBlockIndex = genMaxBlockIndex(taskBlocks);
			
			System.out.println("==> minBlock:" + minBlock.complexity);
			System.out.println("==> maxBlock:" + maxBlock.complexity);
			C=0; L=0; for(LBTaskBlock b:taskBlocks){C += b.complexity; L+= b.tasks.size();}
			System.out.println("taskBlocks(" + taskBlocks.size() + "):" + taskBlocks + "totalComplixity:" +df.format(C)+ "<totalTaskNr:" +L+ ">");
		}
		//		}
		int index = 0;

		//convert to balancedTaskBlocks structure
		for(LBTaskBlock taskBlock: taskBlocks){
			Multimap<GeoSquare, GeoSquare> initial = HashMultimap.create();
			balancedTaskBlocks.add(index, initial);
			for(LBTask task : taskBlock.tasks){
				balancedTaskBlocks.get(index).put(task.sourceGeoSqare, task.targetGeoSqare);
			}
			index++;
		}
		return balancedTaskBlocks;
	}

	/**
	 * @param taskBlocks2
	 * @return
	 * @author sherif
	 */
	private int genMinBlockIndex(List<LBTaskBlock> taskBlocks) {
		long min = Long.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < taskBlocks.size(); i++) {
			long c = taskBlocks.get(i).getComplexity();
			if(c < min){
				min = c;
				minIndex = i;
			}
		}
		return minIndex;
	}

	/**
	 * @param taskBlocks2
	 * @return
	 * @author sherif
	 */
	private int genMaxBlockIndex(List<LBTaskBlock> taskBlocks) {
		long max = -Long.MAX_VALUE;
		int maxIndex = -1;
		for (int i = 0; i < taskBlocks.size(); i++) {
			long c = taskBlocks.get(i).getComplexity();
			if(c > max){
				max = c;
				maxIndex = i;
			}
		}
		return maxIndex;
	}

}













