package de.uni_leipzig.simba.genetics.core;

import org.jgap.gp.IGPProgram;

import de.uni_leipzig.simba.data.Mapping;

public interface IFitnessFunction {

	public Mapping getMapping(String expression, double accThreshold, boolean full);
	
//	public double calculateRawMeasure(IGPProgram p);

	public double calculateRawFitness(IGPProgram p);

	public double calculateRawMeasure(IGPProgram bestHere);
}
