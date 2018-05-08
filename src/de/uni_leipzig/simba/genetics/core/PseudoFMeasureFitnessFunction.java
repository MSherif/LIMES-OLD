package de.uni_leipzig.simba.genetics.core;

import org.apache.log4j.Logger;
//import java.beans.PropertyChangeSupport;
//import java.beans.PropertyChangeListener;

import org.jgap.gp.GPFitnessFunction;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.ProgramChromosome;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.specification.LinkSpec;

/**
 * Fitness function to evolve metric expression using a PseudoMeasue
 * @author Lyko
 *
 */
public class PseudoFMeasureFitnessFunction extends GPFitnessFunction implements IFitnessFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7114137172832439294L;
	static Logger logger = Logger.getLogger("LIMES");
	Cache sourceCache, targetCache;
	LinkSpecGeneticLearnerConfig config;
	SetConstraintsMapper mapper;
	Measure measure;
	ExecutionEngine engine;
	Filter f;
	double beta = 1.0d;
	private static PseudoFMeasureFitnessFunction instance = null;
	
	private PseudoFMeasureFitnessFunction(LinkSpecGeneticLearnerConfig a_config, Measure measure, Cache c1, Cache c2) {
		config = a_config;
		sourceCache = c1;
		targetCache = c2;
		f = new LinearFilter();
		mapper = SetConstraintsMapperFactory.getMapper( "simple", a_config.source, a_config.target, 
				sourceCache, targetCache, f, 2);
		this.measure = measure;
	
		engine = new ExecutionEngine(c1, c2, a_config.source.var, a_config.target.var);
	}
	
	@Override
	protected double evaluate(IGPProgram program) {
		return calculateRawFitness(program); 
	}
	
	/**
	 * Determine fitness of the individual p;
	 * @param p
	 * @return 1-PseudoFMeasure. Or if something wents wrong either 5d, iff p isn't fulfilling all constraints. 8d if executing p results in memory error.
	 */
	public double calculateRawFitness(IGPProgram p) {
		double pseudoFMeasure = calculatePseudoMeasure(p);
		if(!(pseudoFMeasure>=0d && pseudoFMeasure<=1d)) {
			Object[] args = {};
			ProgramChromosome pc = null;
			pc = p.getChromosome(0);
			Metric metric = (Metric)pc.getNode(0).execute_object(pc, 0, args);
			logger.error("Pseudo Measure was not in [0,1]");
			System.out.println("Pseudo Measure for ("+metric+") was not in [0,1]");
			System.err.println("Pseudo Measure for ("+metric+") was not in [0,1]");
		}
		if(pseudoFMeasure>=0)
			return Math.abs(1.0d-pseudoFMeasure);
		else {
			return Math.abs(pseudoFMeasure)+1;
		}
	}
	
	public Mapping calculateMapping(IGPProgram p) {
		// execute individual
				Object[] args = {};
				ProgramChromosome pc = null;
				pc = p.getChromosome(0);
				Mapping actualMapping = new Mapping();
				Metric metric = (Metric)pc.getNode(0).execute_object(pc, 0, args);
				String expr = metric.getExpression();
				double accThreshold = metric.getThreshold();
				// get Mapping 
				try{
					actualMapping = getMapping(expr, accThreshold);
				}
				catch(java.lang.OutOfMemoryError e) {
					e.printStackTrace(); // should not happen
					System.err.println(e.getMessage());
					return new Mapping();
				}
				return actualMapping;
	}
	
	public Double calculatePseudoMeasure(IGPProgram p) {
		return measure.getPseudoFMeasure(sourceCache.getAllUris(), targetCache.getAllUris(), calculateMapping(p), beta);
	}
	
	/**
	 * Executes metric to get mapping for given metric.
	 * @param metric Metric String.
	 * @param threshold Acceptance threshold: 0<=threshold<=1.
	 * @return Mapping m={sURI, tURI} of all pairs who satisfy the metric.
	 */
	public Mapping getMapping(String metric, double threshold) {
		try {
			CanonicalPlanner planner = new CanonicalPlanner();
			LinkSpec spec = new LinkSpec(metric, threshold);
			return engine.runNestedPlan(planner.plan(spec));
//			return mapper.getLinks(metric, threshold);
		} catch(Exception e) {
			e.printStackTrace();
			String out = "Error getMapping() in PFM (" +  config.source.id + " - " + config.target.id +") with metric: "+metric+"<="+threshold+" \n"+ e.getMessage();
			System.err.println(out);
			logger.error(out);
			return new Mapping();
		}
		
	}
	
	/**Singleton pattern*/
	public static PseudoFMeasureFitnessFunction getInstance(LinkSpecGeneticLearnerConfig a_config, Measure measure, Cache c1, Cache c2) {
		if(instance == null) {
			return instance = new PseudoFMeasureFitnessFunction(a_config, measure, c1, c2);
		} else {
			return instance;
		}
	}
	/**Needed between several runs*/
	public void destroy() {
		instance = null;
	}
	
	public Measure getMeasure() {
		return measure;
	}

	public void setMeasure(Measure measure) {
		this.measure = measure;
	}
	public double getBeta() {
		return this.beta;
	}
	public void setBeta(double beta) {
		this.beta = beta;
	}

	@Override
	public Mapping getMapping(String expression, double accThreshold,
			boolean full) {
		return getMapping(expression, accThreshold);
	}

	public double calculateRawMeasure(IGPProgram p) {
		return calculatePseudoMeasure(p);
	}
	
//	public void addPropertyChangeListener(PropertyChangeListener l) {
//		changes.addPropertyChangeListener(l);
//	}
//	
//	public void removePropertyChangeListener(PropertyChangeListener l) {
//		changes.removePropertyChangeListener(l);
//	}
}
