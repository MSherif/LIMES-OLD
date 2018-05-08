package de.uni_leipzig.simba.genetics.evaluation;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.GPProgram;
import org.jgap.gp.impl.ProgramChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.*;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoEvalFileLogger;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.util.Clock;

/**
 * Profides central methods common for all evaluators.
 * @author Klaus Lyko
 *
 */
public class BasicEvaluator {

	public static Logger logger = LoggerFactory.getLogger("LIMES");
	public static final int RUNS = 5;
	public static final int oracleQuestions = 10;
	
	public DataSets currentDataSet;
	protected EvaluationData params;
	public ExpressionFitnessFunction fitness;
	protected Oracle o;
	protected ConfigReader cR;
	/**List for the detailed file logs*/
	protected List<EvaluationPseudoMemory> perRunMemory = new LinkedList<EvaluationPseudoMemory>();
	/**List for the combined results*/
	protected List<EvaluationPseudoMemory> perDatasetMemory = new LinkedList<EvaluationPseudoMemory>();
	public float mutationCrossRate = 0.6f;
	public float reproduction = 0.7f;
	public List<Mapping> startedMappings;
	/**
	 * Restrict the number of full maps computed to get controversy instances? 
	 */
	public boolean restrictGetAllMaps = true;
	/**
	 * If a restrictGetAllMaps is set set the maximum of maps to be considered
	 */
	public int maxMaps = 20;

	/**
	 * Examines this generation and memorizes best program so far.
	 * @param run
	 * @param gen
	 * @param gp
	 * @param clock
	 */
	public void examineGen(int run, int gen, GPGenotype gp, Clock clock) {
		IGPProgram pBest;// = (GPProgram) gp.getFittestProgram();
		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();
		double fittest = Double.MAX_VALUE;
		pBest = pop.determineFittestProgram();
		fittest = fitness.calculateRawFitness(pBest);
		// to do recalculate older programs
		for(GPProgram p:(GPProgram[])pop.getGPPrograms())
		{
			double actFit = fitness.calculateRawFitness(p);
			if(fittest>actFit && getMetric(p).getExpression().indexOf("falseProp") == -1) {
				pBest = p;
				fittest = actFit;
				System.out.println("Setting to fitter program then JGAP");
			}			
		}
		boolean getBetterOld = false;
		for(EvaluationPseudoMemory mem : perRunMemory) {
			// re use older ones
			double oldFit = fitness.calculateRawFitness(mem.program);
			if(oldFit < fittest) {
				fittest = oldFit;
				pBest = mem.program;
				getBetterOld = true;
			}
		}
		if(getBetterOld) {
			System.out.println("Reusing older program...");
//			pop.addFittestProgram((IGPProgram) pBest.clone());
		}
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, gen, getMetric(pBest), 
				fittest, 
				1d-fittest,
				clock.totalDuration());
		mem.knownInstances = fitness.getReferenceMapping().size();
		mem.program = pBest;
		perRunMemory.add(mem);
	}



	/**
	 * Method to write the detailed log of this dataset log
	 */
	public void finishRun() {
		logger.info("Fininshing run...");
		// get full Maps and calculate prf
		logger.info("size of eval data:"+perRunMemory.size());
		for(EvaluationPseudoMemory mem : perRunMemory) {
			
			Mapping fullMap = fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold(), true);
			PRFCalculator pRF = new PRFCalculator();
			mem.fmeasue = pRF.fScore(fullMap, o.getMapping());
			mem.recall = pRF.recall(fullMap, o.getMapping());
			mem.precision = pRF.precision(fullMap, o.getMapping());
			logger.info("mem"+mem.runTime+"instances"+mem.knownInstances+"\t"+mem.fmeasue);
			
		}
		// add data to the complete memory
		this.perDatasetMemory.addAll(perRunMemory);// clear per run Memory
		perRunMemory.clear();
	}
	
	@SuppressWarnings("unchecked")
	public void finishDataset(int maxRuns, int popSize, int gens, EvaluationData params, String nameExtension) {
		// sort and calc the 
		Collections.sort(perDatasetMemory);
//		PseudoEvalFileLogger fileLog = new PseudoEvalFileLogger((String)params.get(MapKey.EVALUATION_RESULTS_FOLDER), (String)params.get(MapKey.EVALUATION_FILENAME));
		PseudoEvalFileLogger fileLog =  new PseudoEvalFileLogger(params.getEvauationResultFolder(), params.getEvaluationResultFileName());
		fileLog.nameExtansion = nameExtension;
		fileLog.log(perDatasetMemory, maxRuns, gens, params);
		perDatasetMemory.clear();
	}
	
	private Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}
	
	public List<Mapping> getMaps(GPPopulation pop) {
		List<Mapping> allMaps = new LinkedList<Mapping>();
		Set<String> expressions = new HashSet<String>();
		List<IGPProgram> considered = new LinkedList<IGPProgram>();
		int max = pop.size();
		if(restrictGetAllMaps) {
			pop.sortByFitness();
			pop.sort(new Comparator<IGPProgram>() {
//				@Override
				public int compare(IGPProgram o1, IGPProgram o2) {
					if(o1.getFitnessValue()<o2.getFitnessValue())
						return -1;
					if(o1.getFitnessValue()>o2.getFitnessValue())
						return 1;
					return 0;
				}
			});
			max = Math.min(maxMaps, pop.size());	
		} 
		for(int i = 0; i<max; i++) {
			considered.add(pop.getGPProgram(i));
		}
	//	for(int i = 0; i<Math.min(pop.size(), 20); i++) {		
			for(IGPProgram gProg : considered) {
				String expr="falseProp";
				double d=0.9d;
				try {	
					Metric metric = getMetric(gProg);
					expr = metric.getExpression();
					d = metric.getThreshold();
				}
				catch(IllegalStateException e)  {
					continue;
				}
				if(expr.indexOf("falseProp")>-1)
					continue;
				if(expressions.contains(expr+d)) {
					maxMaps++;
					continue;
				}
				else
					expressions.add(expr+d);
				try {
					allMaps.add(fitness.getMapping(expr, d, true));
				} catch (OutOfMemoryError bounded) {
					logger.info("Encountered Memory error...");
					continue;
				}
			}
		//}
		return allMaps;
	}
}
