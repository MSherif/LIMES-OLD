package de.uni_leipzig.simba.genetics.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoEvalFileLogger;
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.util.Clock;

/**
 * Improved EAGLEs Batch Evaluation: 2 termination criteria:
 * 1st:  max. number of generations overall := gen*inqueries
 * 2nd: timebased. 
 * 
 * Runs EAGLE batched run times and logs results both atomic and means.
 * @author Klaus Lyko
 *
 */
public class BatchEvaluation {
	public static Logger logger = LoggerFactory.getLogger("LIMES");
	public String logFileNameplus = "";
	public static final int RUNS = 5;
	public static final int oracleQuestions = 5;
	public DataSets currentDataSet;
	EvaluationData params;
	public ExpressionFitnessFunction fitness;
	Oracle o;
	ConfigReader cR;
	/**List for the combined results*/
	List<EvaluationPseudoMemory> perDatasetMemory = new LinkedList<EvaluationPseudoMemory>();
	private float mutationCrossRate = 0.6f;
	private float reproduction = 0.7f;
	Clock clock;
	/**use time based termination criteria?*/
	public boolean timeBased = false;
	/**seconds to run if termination criteria was set to time*/
	public long maxDuration = 600;//in seconds
	public int maxGens = 100;
	public static float percTrainingData = 0.2f;
	public boolean useFullCaches = false;
	
	/**
	 * Set termination criteria to maxDuration, and specify whether to use full caches for learning.
	 * @param maxDuration
	 * @param useFullCaches
	 */
	public void runTimeBased(long maxDuration, boolean useFullCaches) {
		this.timeBased = true;
		this.maxDuration = maxDuration;
		this.useFullCaches = useFullCaches;
	}
	
	/**
	 * 
	 * @param data
	 * @param popSize
	 * @param gens
	 * @param inqueries
	 * @param questions
	 * @throws InvalidConfigurationException
	 */
	public Statistics run(DataSets data, int popSize, int gens, int inqueries, int questions, Mapping trainingData) throws InvalidConfigurationException {
		logger.info("Running dataset "+data);
		currentDataSet = data;
		params = DataSetChooser.getData(data);
		String name = params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "_BATCH_");
		params.setEvaluationResultFileName(name);
		cR = params.getConfigReader();
        o = new SimpleOracle(params.getReferenceMapping());
//        for(int inquery = 1; inquery <= inqueries; inquery++) {	
			for(int run = 1; run <= RUNS; run++	) {
				LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, params.getPropertyMapping());
				config.sC = params.getSourceCache();
				config.tC = params.getTargetCache();
				config.setSelectFromPrevGen(reproduction);
				config.setCrossoverProb(mutationCrossRate);
				config.setMutationProb(mutationCrossRate);
				config.setReproductionProb(reproduction);
				config.setPopulationSize(popSize);
				config.setPreservFittestIndividual(true);
				config.setAlwaysCaculateFitness(true);
				// new pop && data				
//				Mapping trainingData = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), Math.min(inqueries*questions, (int)(o.getMapping().size()*percTrainingData)));
//				logger.info(params.getName()+" TrainingData.size() = "+trainingData.size()+"  == Min("+inqueries*questions+"  ,   "+ (int)(o.getMapping().size()*percTrainingData)+")");
				
				fitness = ExpressionFitnessFunction.getInstance(config, o.getMapping(), "f-score", (inqueries*questions));
				fitness.setReferenceMapping(trainingData);
				fitness.useFullCaches(useFullCaches);
				if(!useFullCaches) {
					fitness.trimKnowledgeBases(trainingData);
				}
				config.setFitnessFunction(fitness);
				
				GPProblem gpP = new ExpressionProblem(config);
				GPGenotype gp = gpP.create();
				clock = new Clock();
				maxGens = gens*inqueries;
				// which termination criteria to use?
				int gen = 1;
				long start = System.currentTimeMillis();
				long dur = 0;
				System.out.println("EAGLE: running "+gen+" gen of "+currentDataSet.name());
				while(!terminationCriteriaReached(dur, gen)) {
					gp.evolve();
					gen++;
					if(gen % 100 == 0){
						logger.error("EAGLE: running "+gen+" gen of "+currentDataSet.name());
						System.out.println("EAGLE: running "+gen+" gen of "+currentDataSet.name());
					}
					
					dur = System.currentTimeMillis() - start;
				}
				logger.info("EAGLE performed "+gen+" generations in " +maxDuration+" seconds on dataset "+data.name());
				
				processRun(gp, run, (gens*inqueries), trainingData.size());
				fitness.destroy();
				LinkSpecGeneticLearnerConfig.reset();
			}
//		}
		return finishDataSet(popSize, gens);
		//write data
	}
	
	private boolean terminationCriteriaReached(long dur, int gen) {
		if(this.timeBased) {
			return (dur/1000) >= maxDuration;
		}
		return gen>maxGens;
	}
	
	/**
	 * Writes results to files: both atomics over all runs and means
	 * @param popSize
	 * @param gens
	 */
	@SuppressWarnings("unchecked")
	private Statistics finishDataSet(int popSize, int gens) {
		Collections.sort(perDatasetMemory);
		PseudoEvalFileLogger fileLog = new PseudoEvalFileLogger(params.getEvauationResultFolder(),params.getEvaluationResultFileName());
		fileLog.nameExtansion = "Batch_pop="+popSize+"_gens="+gens+logFileNameplus;
		Statistics stat = fileLog.log(perDatasetMemory, RUNS, gens, params);
		perDatasetMemory.clear();
		return stat;
	}

	/**
	 * Computes atomic results of a single run: stored in List perDatasetMemory
	 * @param gp
	 * @param run
	 * @param gens
	 * @param trainingSize
	 */
	private void processRun(GPGenotype gp, int run, int gens, int trainingSize) {
		List<IGPProgram> bests = new LinkedList<IGPProgram>();
		bests.add(gp.getAllTimeBest());
		bests.add(gp.getFittestProgram());
		bests.add(gp.getFittestProgramComputed());
		GPPopulation pop = gp.getGPPopulation();
		IGPProgram pBest = pop.determineFittestProgram();
		double bestFit = fitness.calculateRawFitness(pBest);
		for(IGPProgram p : bests) {
			if(p != null)
				if(fitness.calculateRawFitness(p)<bestFit) {
					bestFit = fitness.calculateRawFitness(p);
					pBest = p;
				}
		}		
		Metric m = fitness.getMetric(pBest);
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, gens, m, 
				bestFit, 
				1d-bestFit,
				clock.totalDuration());
		mem.knownInstances = trainingSize;
		mem.program = pBest;
		Mapping full =  fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold(), true);
		PRFCalculator comp = new PRFCalculator();
		double fM = comp.fScore(full, o.getMapping());
		double rec =  comp.recall(full, o.getMapping());
		double prec = comp.precision(full, o.getMapping());
		mem.fmeasue = (fM != Double.NaN)? fM : 0d;
		mem.recall = (rec != Double.NaN) ? rec : 0d;
		mem.precision = (prec != Double.NaN) ? prec : 0d;
		// log also best 1to1s
		Mapping full_1to1 = Mapping.getBestOneToOneMappings(full);
		double fM_1to1 = comp.fScore(full_1to1, o.getMapping());
		double rec_1to1 =  comp.recall(full_1to1, o.getMapping());
		double prec_1to1 = comp.precision(full_1to1, o.getMapping());
		mem.fmeasue_1to1 = (fM_1to1 != Double.NaN)? fM_1to1 : 0d;
		mem.recall_1to1 = (rec_1to1 != Double.NaN) ? rec_1to1 : 0d;
		mem.precision_1to1 = (prec_1to1 != Double.NaN) ? prec_1to1 : 0d;
		this.perDatasetMemory.add(mem);
	}

	
	public static void main(String args[]) throws FileNotFoundException {
//		DataSets data = DataSets.DBPLINKEDMDB;
		DataSets[] datas = new DataSets[] {
				DataSets.PERSON1,
//				DataSets.PERSON2,
//				DataSets.RESTAURANTS,
//				DataSets.DBLPACM, 
				DataSets.ABTBUY, 
//				DataSets.AMAZONGOOGLE, 
		};try {
			for(DataSets data : datas) {
				BatchEvaluation evaluation = new BatchEvaluation();		
				evaluation.setOutStreams(data.name());
				evaluation.runTimeBased(600, false);// set termination criteria: time based
				// new pop && data				
				int inqueries = 10;
				int questions = 10;
				EvaluationData params = DataSetChooser.getData(data);
				
				Mapping trainingData = ExampleOracleTrimmer.trimExamplesRandomly(params.getReferenceMapping(), Math.min(inqueries*questions, (int)(params.getReferenceMapping().size()*percTrainingData)));
				logger.info(params.getName()+" TrainingData.size() = "+trainingData.size()+"  == Min("+inqueries*questions+"  ,   "+ (int)(params.getReferenceMapping().size()*percTrainingData)+")");
				
				evaluation.run(data, 20, 10, inqueries, questions, trainingData);
			}			
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public void setOutStreams(String name) throws FileNotFoundException {
		File stdFile = new File(name+"_eagle_stdOut.txt");
		PrintStream stdOut = new PrintStream(new FileOutputStream(stdFile, false));
		File errFile = new File(name+"_eagle_errOut.txt");
		PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
		System.setErr(errOut);
		System.setOut(stdOut);
	}

}
