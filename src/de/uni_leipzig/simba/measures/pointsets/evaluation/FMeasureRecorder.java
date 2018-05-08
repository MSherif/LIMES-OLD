/**
 * 
 */
package de.uni_leipzig.simba.measures.pointsets.evaluation;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.multilinker.MappingMath;

/**
 * class to manipulate mapping precision, recall and F measure
 * @author sherif
 *
 */
public class FMeasureRecorder {
	public double fMeasure;
	public double precision;
	public double recall;

	FMeasureRecorder(){
		fMeasure 	= 0;
		precision 	= 0;
		recall 		= 0;
	}

	/**
	 * @param fMeasure
	 * @param precision
	 * @param recall
	 *@author sherif
	 */
	public FMeasureRecorder(double fMeasure, double precision, double recall) {
		super();
		this.fMeasure 	= fMeasure;
		this.precision 	= precision;
		this.recall 	= recall;
	}

	FMeasureRecorder(Mapping mapping, int optimalSize){
		precision 	= MappingMath.computePrecision(mapping, optimalSize);
		recall 		= MappingMath.computeRecall(mapping, optimalSize);
		fMeasure 	= MappingMath.computeFMeasure(mapping, optimalSize);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FMeasureRecorder " +
				"[fMeasure=" + fMeasure + 
				", precision=" + precision + 
				", recall=" + recall + "]";
	}




}
