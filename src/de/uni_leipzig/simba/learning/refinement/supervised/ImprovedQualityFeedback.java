package de.uni_leipzig.simba.learning.refinement.supervised;

import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
/**
 * Just a class holding data to get complex quality for Supervised approach. That is the
 * standard F-Measure over trimmed caches and standard PFM over full caches.
 * @author Klaus Lyko
 *
 */
public class ImprovedQualityFeedback {
	public double pfmRest = 0d;
	public double fTrimmed = 0d;
	Mapping trimmedMap = new Mapping();
	Mapping restMap = new Mapping();
	float portionTrimmed = 0.3f;
	
	public ImprovedQualityFeedback() {
		this.portionTrimmed = 1;
	}
	
	/**
	 * Constructor to specify percent of real f-measure in quality computation
	 * @param portionTrimmed
	 */
	public ImprovedQualityFeedback(float portionTrimmed) {
		this.portionTrimmed = portionTrimmed;
	}
	public double combine() {
		return combine(fTrimmed, pfmRest);
	}
	
	public double combine(double f, double pfm) {
//		return (pfmRest+fTrimmed)/2;
		//    1
		//
		return pfmRest*(1f-portionTrimmed) + fTrimmed*portionTrimmed;
	}
	@Override
	public String toString() {
		return combine()+"(pfmRest="+pfmRest+" fTrimmed="+fTrimmed+")";
	}
}

