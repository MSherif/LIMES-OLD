/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.dpso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import com.google.common.collect.TreeMultiset;

/**
 * @author sherif
 *
 */
public class LBTaskBlock implements Comparable<LBTaskBlock>{
	protected List<LBTask> tasks;
	protected long complexity;
	
	
	/**
	 * @return the complexity
	 */
	public long getComplexity() {
		return complexity;
	}

	/**
	 * 
	 *@author sherif
	 */
	public LBTaskBlock() {
		super();
		this.tasks = new ArrayList<LBTask>();
		this.complexity = 0;
	}
	
	/**
	 * @param tasks
	 *@author sherif
	 */
	public LBTaskBlock(List<LBTask> tasks) {
		super();
		this.tasks = tasks;
		this.complexity = computeComplexity();
	}
	
	public void add(LBTask task){
		tasks.add(task);
		complexity += task.complexity;
	}
	
	

	public void remove(LBTask task){
		tasks.remove(task);
		complexity -= task.complexity;
	}

	/**
	 * @return
	 * @author sherif
	 */
	private long computeComplexity() {
		complexity = 0;
		for (LBTask task : tasks) {
			complexity += task.complexity;
		}
		return complexity;
	}

	/**
	 * @return the tasks
	 */
	public List<LBTask> getTasks() {
		return tasks;
	}


	/**
	 * @param tasks the tasks to set
	 */
	public void setTasks(List<LBTask> tasks) {
		this.tasks = tasks;
	}
	
	@Override
    public int compareTo(LBTaskBlock tb){
        if (this.getComplexity() > tb.getComplexity())
            return 1;
        else if (this.getComplexity() == tb.getComplexity())
            return 0;
        else 
            return -1;
    }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
//		String s = "[";
//				for(LBTask t : tasks){
//					s += t.toString() + ", ";
//				}
//				return s+ "]";
		return "complexity:" + complexity + "<tasks.size():" + tasks.size() + ">";
	}
	
	public void sort(){
		Collections.sort(tasks);
	}
}
