/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.dpso;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoSquare;

/**
 * @author sherif
 *
 */
public class LBTask implements Comparable<LBTask>{
	protected GeoSquare sourceGeoSqare;
	protected GeoSquare targetGeoSqare;
	protected long complexity;
	
	/**
	 * @param sourceGeoSqare
	 * @param targetGeoSqare
	 *@author sherif
	 */
	public LBTask(GeoSquare sourceGeoSqare, GeoSquare targetGeoSqare) {
		super();
		this.sourceGeoSqare = sourceGeoSqare;
		this.targetGeoSqare = targetGeoSqare;
		this.complexity = computeComplexity();
	}
	
	
	/**
	 * @return
	 * @author sherif
	 */
	private long computeComplexity() {
		return sourceGeoSqare.size() * targetGeoSqare.size();
	}


	/**
	 * @return the sourceGeoSqare
	 */
	public GeoSquare getSourceGeoSqare() {
		return sourceGeoSqare;
	}
	
	/**
	 * @param sourceGeoSqare the sourceGeoSqare to set
	 */
	public void setSourceGeoSqare(GeoSquare sourceGeoSqare) {
		this.sourceGeoSqare = sourceGeoSqare;
	}
	
	/**
	 * @return the targetGeoSqare
	 */
	public GeoSquare getTargetGeoSqare() {
		return targetGeoSqare;
	}

	/**
	 * @param targetGeoSqare the targetGeoSqare to set
	 */
	public void setTargetGeoSqare(GeoSquare targetGeoSqare) {
		this.targetGeoSqare = targetGeoSqare;
	}


	/**
	 * @return the complexity
	 */
	public long getComplexity() {
		return complexity;
	}


	/**
	 * @param complexity the complexity to set
	 */
	public void setComplexity(long complexity) {
		this.complexity = complexity;
	}
	
	@Override
    public int compareTo(LBTask t){
        if (this.getComplexity() > t.getComplexity())
            return 1;
        else if (this.getComplexity() == t.getComplexity())
            return 0;
        else 
            return -1;
    }


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 
//				"LBTask [sourceGeoSqare=" + sourceGeoSqare + ", targetGeoSqare="
//				+ targetGeoSqare + ", complexity=" + 
				complexity + "";
//				+ "]";
	}
	
	
}
