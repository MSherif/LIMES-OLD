package de.uni_leipzig.simba.genetics.evaluation;

import org.jgap.gp.IGPProgram;

public class EvalLogMem {
	public int gen;
	public IGPProgram fittestProg;
	public double fitness;
	public double avgFitness;
	public long dur;
	
	public double FScore;
	public double recall;
	public double precision;
	
	/**
	 * Log memory to keep track of best Solutions per generation.
	 * @param g Number of generation.
	 * @param p Program to remember.
	 * @param fp Fitness value.
	 * @param fg Average fitness of generation.
	 */
	public EvalLogMem(int g, IGPProgram p, double fp, double fg) {
		super();
		gen=g;
		fittestProg = p;
		fitness = fp;
		avgFitness = fg;
	}
	/**
	 * 
	 * @param g Number of generation.
	 * @param p Program to remember.
	 * @param fp Fitness value.
	 * @param fg Average fitness of generation.
	 * @param d duration
	 */
	public EvalLogMem(int g, IGPProgram p, double fp, double fg, long d) {
		this(g, p, fp, fg);
		dur = d;
	}
	public EvalLogMem clone() {
		return new EvalLogMem(gen, fittestProg, fitness, avgFitness);		
	}
	
	public String toString() {
		String o = "";
		o = this.fittestProg+" fitness="+this.fitness;
		return o;
	}
}
