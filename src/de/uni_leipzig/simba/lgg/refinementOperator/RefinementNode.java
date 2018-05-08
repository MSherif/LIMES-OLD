/**
 * 
 */
package de.uni_leipzig.simba.lgg.refinementOperator;


import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.mapper.SetOperations;

/**
 * @author sherif
 *
 */
public class RefinementNode implements Comparable<RefinementNode> {
	private static final Logger logger = Logger.getLogger(RefinementNode.class.getName());

	public double precision = -Double.MAX_VALUE;
	public double recall = -Double.MAX_VALUE;
	public double fMeasure = -Double.MAX_VALUE;
	public double maxFMeasure = 1d;
	public Mapping map = new Mapping();
	public String metricExpression = new String();
	public static double rMax = -Double.MAX_VALUE;

	public static boolean saveMapping = true;
//	public static boolean usePseudoFMeasure = false;


	/**
	 * @param fMeasure
	 * @param map
	 * @param metricExpression
	 *@author sherif
	 */
	public RefinementNode(double fMeasure, Mapping map, String metricExpression) {
		super();
		this.fMeasure = fMeasure;
		this.map = map;
		this.metricExpression = metricExpression;
	}

	/**
	 * @param precision
	 * @param recall
	 * @param map
	 * @param metricExpression
	 *@author sherif
	 */
	public RefinementNode(Mapping map, String metricExpression, Mapping refMap) {
		super();
		this.precision 	= PRFCalculator.precision(map, refMap);
		this.recall 	= PRFCalculator.recall(map, refMap);
		this.fMeasure = (this.precision == 0 && this.recall == 0) ? 0 : 2 * precision * recall / (precision + recall);
		double pMax = computeMaxPrecision(map, refMap);
		this.maxFMeasure = 2 * pMax * rMax / (pMax + rMax);
		this.map = saveMapping ? map : null;
		this.metricExpression = metricExpression;
	}


	/**
	 * Note: basically used for unsupervised version of WOMBAT 
	 * @param map
	 * @param metricExpression
	 * @param fMeasure
	 */
	public RefinementNode(Mapping map, String metricExpression, double fMeasure) {
		super();
		this.fMeasure = fMeasure;
		this.map = saveMapping ? map : null;
		this.metricExpression = metricExpression;

	}

	private double computeMaxPrecision(Mapping map, Mapping refMap) {
		Mapping falsePos = new Mapping();
		for(String key: map.map.keySet()){
			for(String value : map.map.get(key).keySet()){
				if(refMap.map.containsKey(key) || refMap.reversedMap.containsKey(value)){
					falsePos.add(key, value, map.map.get(key).get(value));
				}
			}
		}
		Mapping m = SetOperations.difference(falsePos, refMap);
		return (double)refMap.size()/(double)(refMap.size() + m.size());
	}




	/**
	 * 
	 *@author sherif
	 */
	public RefinementNode() {
		// TODO Auto-generated constructor stub
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 
				metricExpression + 
				//				this.hashCode()+
//				" (P = " + precision + ", " + "R = " + recall + ", " + "F = " + fMeasure + ")";
		" (F = " + fMeasure + ")";
	}


	/* (non-Javadoc)
	 * Compare RefinementNodes based on fitness
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(RefinementNode o) {
		return (int) (fMeasure - o.fMeasure);

	}





	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 * @author sherif
	 */
	public double getMaxFMeasure() {
		// TODO Auto-generated method stub
		return 0;
	}
}
