package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;


import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.core.PseudoFMeasureFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.util.Clock;

/**
 * Class for running evaluations on PseudoFMeasures, based upon certain beta values.
 * @author Klaus Lyko
 *
 */
public class PseudoBaseEvaluator {
	protected static final Logger logger = Logger.getLogger("LIMES");

	public static final String SEP = ";";
	public static final String subFolder = "betaEval/";
	public float mutation = 0.3f;
	public float crossover = 0.5f;
	public float reproduction = 0.5f;
	public int generations = 20; 
	public int population = 100;
	
	protected LinkSpecGeneticLearnerConfig config;
	
	protected PseudoFMeasureFitnessFunction fitness;
	
	protected LinkedList<EvaluationPseudoMemory> perRunAndDataSet;
//	/**Sorted by their sortNumber*/
//	LinkedList<EvaluationPseudoMemory> perDataSet = new LinkedList<EvaluationPseudoMemory>();

	protected String extansion;

	protected IGPProgram bestEver; protected double bestEverFitness;
	
	public int maxRuns = 5;
	
	public PseudoBaseEvaluator() {
		this("");
	}
	
	public PseudoBaseEvaluator(String name) {
		System.out.println("Setting up environment");
		File folder = new File(DataSetChooser.getEvalFolder()+subFolder);
		if(!folder.exists() || !folder.isDirectory()) {
			System.err.println("Cannot read eval folder exiting...");
			System.exit(-1);
		}
		boolean b = setUpOutStream(DataSetChooser.getEvalFolder(), name);
		b = b&setupErrorStream(DataSetChooser.getEvalFolder(), name);
		if(!b) {
			System.err.println("There were errors setting out and error stream");
		}
	}
	
	/**
	 * Runs 5 runs for each dataset and each beta value.
	 * @param param
	 * @param measure
	 * @param allBetaValues Array of betaValues to test.
	 * @throws Exception 
	 */
	public void run(EvaluationData param, Measure measure, Double[] allBetaValues) throws Exception {		
		List<EvaluationPseudoCombinedMemory> perDataSetMem = new LinkedList<EvaluationPseudoCombinedMemory>();
		bestEver = null;
		System.out.println("Start Evaluating "+param.getName()+ " with measure "+measure.getName());
		for(double beta : allBetaValues) {
			System.out.println("Start Evaluating "+param.getName()+" for "+measure.getName()+" with beta = "+beta);
			EvaluationPseudoCombinedMemory perBeta = new EvaluationPseudoCombinedMemory(beta);
//			List<EvaluationPseudoMemory> perBetaMem = new LinkedList<EvaluationPseudoMemory>();
			for(int run = 1; run <= maxRuns; run++) {
				System.out.println("Running run "+run+" on "+param.getName());
				// minor run method sets the mapping, pseudo measure, fitness, runTime.
				EvaluationPseudoMemory perRun = run(param, run, measure, beta);
//				// add to compute means
				perBeta.add(evaluate(param, perRun));	
			}
			// remember
			perDataSetMem.add(perBeta);			
			System.out.println(perBeta.toString(SEP));
		}
		System.out.println("Finished Evaluating "+param.getName());
		// write log file for dataset and pseudoMeasure
		String range = "B"+allBetaValues[0]+"-"+allBetaValues[allBetaValues.length-1];
		System.out.println(range);
		writeFile(param, measure, perDataSetMem, range);
	}

	/**
	 * Runs a specific passage.
	 * @param baseFolder
	 * @param configFile
	 * @param referenceFile
	 * @throws InvalidConfigurationException 
	 */
	protected EvaluationPseudoMemory run(EvaluationData param, int run, Measure measure, double beta) throws InvalidConfigurationException {
		bestEverFitness = Double.MAX_VALUE;
		bestEver = null;
		System.out.println("Running passage "+run+" on "+param.getConfigFileName()+ " with beta="+beta);
		perRunAndDataSet = new LinkedList<EvaluationPseudoMemory>();
		Clock clock = new Clock();

		ConfigReader cR = param.getConfigReader();
		Cache sC = param.getSourceCache();
		Cache tC = param.getTargetCache();
		PropertyMapping pM = param.getPropertyMapping();
	
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, pM);
		config.setCrossoverProb(crossover);
		config.setMutationProb(mutation);
		config.setReproductionProb(reproduction);
		config.setPopulationSize(population);
		config.setPreservFittestIndividual(true);
		fitness = PseudoFMeasureFitnessFunction.getInstance(config, measure, sC, tC);
//		fitness = new PseudoFMeasureFitnessFunction(config, measure, sC, tC);
		fitness.setBeta(beta);
		config.setFitnessFunction(fitness);
		// evolve
		ExpressionProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		for(int gen=1; gen<generations; gen++) {
			gp.evolve();
			if(gen % 5 == 0 || gen == 1) {
				processGeneration(gp, gen, run);
				System.out.println("Finished generation "+gen+" / "+generations);
			}
		}
		// last generation: determine the best
		IGPProgram bestProgram  = processGeneration(gp, generations, run);
		if(bestProgram == null)
			logger.error("No best Program found");
		Metric bestMetric = getMetric(bestProgram);
		if(bestMetric == null)
			logger.error("No bestMetric found");
		else
			logger.info("Best metric: "+bestMetric);
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, generations, bestMetric, 
				fitness.calculateRawFitness(bestProgram),
				fitness.calculatePseudoMeasure(bestProgram),
				clock.totalDuration());
		mem.pseudoFMeasure = fitness.calculatePseudoMeasure(bestProgram);
		mem.betaValue = beta;
		mem.fullMapping = fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold());
		fitness.destroy();
		fitness = null;
		Configuration.reset();
		LinkSpecGeneticLearnerConfig.reset();
		return mem;
	}


	/**
	 * does whatever should be done each generation.
	 * @param gp
	 * @param gen
	 * @param run
	 */
	public IGPProgram processGeneration(GPGenotype gp, int gen, int run) {		
		IGPProgram pBest = determinFittest(gp);
	return pBest;
	}
	
	private IGPProgram determinFittest(GPGenotype gp) {
		
		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();
	
		IGPProgram bests[] = { 
				gp.getFittestProgramComputed(),
				pop.determineFittestProgram(),
//				gp.getAllTimeBest(),
				pop.getGPProgram(0),
		};
		IGPProgram bestHere = null;
		double fittest = Double.MAX_VALUE;
		
		for(IGPProgram p : bests) {
			if(p != null) {
				double fitM =	fitness.calculateRawFitness(p);
				if(fitM<fittest) {
					fittest = fitM;
					bestHere = p;
				}
			}
		}
		/*consider population if neccessary*/
		if(bestHere == null) {
			logger.debug("Determing best program failed, consider the whole population");
			System.err.println("Determing best program failed, consider the whole population");
			for(IGPProgram p : pop.getGPPrograms()) {
				if(p != null) {
					double fitM =	fitness.calculateRawFitness(p);
					if(fitM<fittest) {
						fittest = fitM;
						bestHere = p;
					}
				}
			}
		}
		return bestHere;
	}


	// ---Little assistance methods --------------------------------------------------------------
	
	/**
	 * To set the real fmeasure, recall and precision for both the full map and its best 1-to-1 map.
	 * @param param
	 * @param perRun
	 * @return
	 */
	private EvaluationPseudoMemory evaluate(EvaluationData param,
			EvaluationPseudoMemory mem) {
		Mapping reference = param.getReferenceMapping();
		Double prec, recall, fMeasure, prec_1to1, recall_1to1, fMeasure_1to1;
		prec = PRFCalculator.precision(mem.fullMapping, reference);
		recall = PRFCalculator.recall(mem.fullMapping, reference);
		fMeasure = PRFCalculator.fScore(mem.fullMapping, reference);
		mem.precision=prec;
		mem.recall=recall;
		mem.fmeasue=fMeasure;
		// also compute results for best 1-1-Map
		Mapping full_1to1 = Mapping.getBestOneToOneMappings(mem.fullMapping);
		prec_1to1 = PRFCalculator.precision(full_1to1, reference);
		recall_1to1 = PRFCalculator.recall(full_1to1, reference);
		fMeasure_1to1 = PRFCalculator.fScore(full_1to1, reference);
		mem.precision_1to1=prec_1to1;
		mem.recall_1to1=recall_1to1;
		mem.fmeasue_1to1=fMeasure_1to1;		
		return mem;
	}
	
	
	
	public Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}
	

	/**
	 * Writes the results for a dataset and a specific measure to a logging file.
	 * @param param
	 * @param measure
	 * @param perDataSetMem
	 * @param range String to specify the range of betas in file name.
	 * @throws IOException
	 */
	private void writeFile(EvaluationData param, Measure measure, 
			List<EvaluationPseudoCombinedMemory> perDataSetMem, String range) throws IOException {
		String fileName = measure.getName()+"_"+param.getEvaluationResultFileName()+"-"+range+".csv";
		File file = new File(param.getEvauationResultFolder()+subFolder+fileName);
		
		FileWriter writer = new FileWriter(file, false);
		writer.write(EvaluationPseudoCombinedMemory.getColumnHeader(SEP));
		writer.write(System.getProperty("line.separator"));
		writer.flush();
		for(EvaluationPseudoCombinedMemory mem: perDataSetMem) {
			writer.write(mem.toString(SEP));
			writer.write(System.getProperty("line.separator"));
			writer.flush();
		}
		writer.close();
	}
	
	private boolean setupErrorStream(String folder, String name) {
		try {
			System.out.println("Setting up error stream...");
			File f = new File(folder+subFolder+name+"_errorLog.txt");
			f.delete();
			f.createNewFile();			
			PrintStream err = new PrintStream(f);			
			System.setErr(err);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean setUpOutStream(String folder, String name) {
		try {
			System.out.println("Setting up out stream...");
			File f = new File(folder+subFolder+name+"_log.txt");
			f.delete();
			f.createNewFile();			
			PrintStream out = new PrintStream(f);			
			System.setOut(out);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}	
	
}
