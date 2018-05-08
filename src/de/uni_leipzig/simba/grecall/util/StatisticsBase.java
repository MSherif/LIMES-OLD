package de.uni_leipzig.simba.grecall.util;

public class StatisticsBase {
	public float runtime;
	public float mappingSize;
	public float selectivity;


	public StatisticsBase() {
		this.runtime = 0;
    	this.mappingSize = 0;
    	this.selectivity = 0;

    }
	
    public StatisticsBase(float runtime,float mappingSize, float selectivity) {
    	this.runtime = runtime;
    	this.mappingSize = mappingSize;
    	this.selectivity = selectivity;
    	
    	
    }
}
	