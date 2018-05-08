package de.uni_leipzig.simba.genetics.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;

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
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.util.Clock;

/**
 * Batch learner used for evaluation:  already calculate means
 * @author Klaus
 *
 */
public class ImprovedBatchEvaluation {
	public static final String SEP = ";";
	int gens = 50;
	static int RUNS = 5;
	int inqueries = 10;
	int examplesPerInquiry = 10;
	public float mutation = 0.6f;
	public float crossover = 0.6f;
	public float mutationCrossRate = 0.6f;
	public float reproduction = 0.7f;
	static final Logger logger = Logger.getLogger("LIMES");

	
	public static void main(String args[]) {
		DataSets[] datas = new DataSets[] {
				DataSets.DBPLINKEDMDB, DataSets.DRUGS,
				DataSets.DBLPACM, DataSets.ABTBUY, DataSets.AMAZONGOOGLE, DataSets.DBLPSCHOLAR
		};
		try {
			for(DataSets data : datas) {
				for(int population = 20; population <= 100; population += 80) {
					ImprovedBatchEvaluation learner = new ImprovedBatchEvaluation();
					learner.run(data, RUNS, population, 10, 10);
				}
			}
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Runs batch learner.
	 * @param data Dataset
	 * @param numberOfRuns how man runs to do on each dataset
	 * @param popSize
	 * @param numberOfInqueries Number of inquiries (last oracleSize = numberOfInqueries*questionsperInquery)
	 * @param questionsperInquery number of questions to user per inquiry
	 * @throws InvalidConfigurationException
	 */
	public void run(DataSets data, int numberOfRuns, int popSize, 
			int numberOfInqueries, int questionsperInquery) throws InvalidConfigurationException {
		logger.info("Running dataset "+data);
		EvaluationData params = DataSetChooser.getData(data);
		
		String name = ""+params.getEvauationResultFolder()+params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "_BATCH_pop="+popSize+"_");
		
		ConfigReader cR = params.getConfigReader();
		Oracle o = new SimpleOracle(params.getReferenceMapping());
		
		File evalFile = null;
		try {
			evalFile = createFile(name, popSize, numberOfRuns);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int inquery = 1; inquery <= numberOfInqueries; inquery++ ) {
			
			List<EvaluationPseudoMemory> perInqery = new LinkedList<EvaluationPseudoMemory>();
			for(int run = 1; run<= numberOfRuns; run++) {
				EvaluationPseudoMemory memOfRun = run(params, cR, o, run, popSize, inquery, questionsperInquery);
				perInqery.add(memOfRun);
			}
			if(evalFile != null)
			{
				
				try {
					writeEntry(evalFile, o.getMapping(), perInqery);
				} catch (IOException e) {
					logger.error("Unable to write file: "+name);
					e.printStackTrace();
					System.exit(1);
				}	
			}else {
				logger.error("Unable to write file: "+name);
				System.exit(1);
			}
		}
		
	}
	/**
	 * Method to run a specifc inquiry Number from outside. Writes it to a file with the specified name expansion
	 * @param nameExpansion to not(!) override other files 
	 * @param data
	 * @param numberOfRuns
	 * @param popSize
	 * @param concreteInquiry
	 * @param questionsperInquery
	 * @throws InvalidConfigurationException
	 */
	public void runRest(String nameExpansion, DataSets data, int numberOfRuns, int popSize, 
			int concreteInquiry, int questionsperInquery) throws InvalidConfigurationException {
		logger.info("Running dataset "+data);
		EvaluationData params = DataSetChooser.getData(data);
		String name = ""+params.getEvauationResultFolder()+params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "_"+nameExpansion+"_BATCH_pop="+popSize+"_");
		ConfigReader cR = params.getConfigReader();
		Oracle o = new SimpleOracle(params.getReferenceMapping());
		
		File evalFile = null;
		try {
			evalFile = createFile(name, popSize, numberOfRuns);
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<EvaluationPseudoMemory> perInqery = new LinkedList<EvaluationPseudoMemory>();
		for(int run = 1; run<= numberOfRuns; run++) {
			EvaluationPseudoMemory memOfRun = run(params, cR, o, run, popSize, concreteInquiry, questionsperInquery);
			perInqery.add(memOfRun);
		}
		if(evalFile != null)
		{
			
			try {
				writeEntry(evalFile, o.getMapping(), perInqery);
			} catch (IOException e) {
				logger.error("Unable to write file: "+name);
				e.printStackTrace();
				System.exit(1);
			}	
		}else {
			logger.error("Unable to write file: "+name);
			System.exit(1);
		}
	}	
	
	/**
	 * Computes all means writes result to the file.
	 * @param evalFile File to write into
	 * @param reference Reference mapping needed  to compute fScore, recall and precision.
	 * @param data List of EvaluationPseudoMemory to compute the means out of.
	 * @throws IOException
	 */
	private void writeEntry(File evalFile, Mapping reference, List<EvaluationPseudoMemory> data) throws IOException {
		Statistics oracle = new Statistics();
		Statistics fitness = new Statistics();
		Statistics fscore = new Statistics();
		Statistics prec = new Statistics();
		Statistics recall = new Statistics();
		Statistics duration = new Statistics();
		FileWriter writer = new FileWriter(evalFile, true);
		int gen = 0;
		for(EvaluationPseudoMemory mem : data) {
			gen = mem.generation;
			oracle.add(mem.knownInstances);
			fitness.add(mem.fitness);
			PRFCalculator prfc = new PRFCalculator();
			fscore.add(prfc.fScore(mem.fullMapping, reference));
			prec.add(prfc.precision(mem.fullMapping, reference));
			recall.add(prfc.recall(mem.fullMapping, reference));
			duration.add(mem.runTime);
		}
		String line = ""+gen+SEP+oracle.mean+SEP+oracle.standardDeviation+SEP+fitness.mean+SEP+fitness.standardDeviation+SEP+
				fscore.mean+SEP+fscore.standardDeviation+SEP+prec.mean+SEP+prec.mean+SEP+recall.mean+SEP+recall.standardDeviation+SEP+
				duration.mean+SEP+duration.standardDeviation;
		writer.write(line);
		writer.write(System.getProperty("line.separator"));
		writer.flush();
		writer.close();
	}

	/**
	 * Tries to (over)ride the file and the heading of the table.
	 * @param name path to the File
	 * @param popSize
	 * @param runs
	 * @return File reference
	 * @throws IOException
	 */
	private File createFile(String name, int popSize, int runs) throws IOException {
		// first line :: pop, runs
		// gen + oracleSize + fitness(m) + fscore(m) + prec(m) + recall(m)+ duration(m) 
		File file = new File(name);
		FileWriter writer = new FileWriter(file, false);
		String head = "population= "+ popSize + " runs= "+runs; 
		writer.write(head);
		writer.write(System.getProperty("line.separator"));
		head = "gen"+SEP+"oralce"+SEP+"oracle_std"+SEP+"fitness"+SEP+"fitness_std"+SEP+"f-score"+SEP+"f-score_std"+SEP+"prec"+SEP+"prec_std"+SEP+"recall"+SEP+"recall_std"+SEP+"duration"+SEP+"duration_std";
		writer.write(head);
		writer.write(System.getProperty("line.separator"));
		writer.flush();
		writer.close();
		return file;
	}
	


	/**
	 * We have to run RUNS runs 
	 * @throws InvalidConfigurationException 
	 * 
	 */	
	private EvaluationPseudoMemory run(EvaluationData params, ConfigReader cR, Oracle o, int run, int popSize, 
			int inquery, int questionsperInquery) throws InvalidConfigurationException {
		String name = params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "_BATCH_");
		params.setEvaluationResultFileName(name);
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, params.getPropertyMapping());
		config.setSelectFromPrevGen(reproduction);
		config.setCrossoverProb(mutationCrossRate);
		config.setMutationProb(mutationCrossRate);
		config.setReproductionProb(reproduction);
		config.setPopulationSize(popSize);
		config.setPreservFittestIndividual(true);
		config.setAlwaysCaculateFitness(true);
		// new pop && data				
		int oralceSize = inquery * questionsperInquery;
		Mapping trainingData = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), oralceSize);
		ExpressionFitnessFunction fitness = ExpressionFitnessFunction.getInstance(config, o.getMapping(), "f-score", oralceSize);
		fitness.trimKnowledgeBases(trainingData);
		config.setFitnessFunction(fitness);
		
		GPProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		Clock clock = new Clock();
		for(int gen = 1; gen <= (gens*inquery); gen++) {
			gp.evolve();
//			System.out.println(getMetric(gp.getFittestProgram()));
		}
		IGPProgram bestProgram = processRun(gp, fitness);
		Metric bestMetric = getMetric(bestProgram);
		
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, (gens*inquery), bestMetric, 
				fitness.calculateRawFitness(bestProgram), 
				1d-fitness.calculateRawFitness(bestProgram),
				clock.totalDuration());
		mem.program = bestProgram;
		mem.knownInstances = trainingData.size();
		mem.fullMapping = fitness.getMapping(bestMetric.getExpression(), bestMetric.getThreshold(), true);
		
		fitness.destroy();
		fitness = null;
		LinkSpecGeneticLearnerConfig.reset();
		return mem;
	}
	
	
	private Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}

	/**
	 * Get fittest program of an evolved genotype
	 * @param gp
	 * @param fitness
	 * @return
	 */
	private IGPProgram processRun(GPGenotype gp, ExpressionFitnessFunction fitness) {
		List<IGPProgram> bests = new LinkedList<IGPProgram>();
		bests.add(gp.getAllTimeBest());
		bests.add(gp.getFittestProgram());
		bests.add(gp.getFittestProgramComputed());
		GPPopulation pop = gp.getGPPopulation();
		IGPProgram pBest = pop.determineFittestProgram();
		double bestFit = fitness.calculateRawFitness(pBest);
		for(IGPProgram p : bests) {
			if(p != null)
				if(fitness.calculateRawFitness(p) < bestFit && fitness.calculateRawFitness(p) >= 0) {
					bestFit = fitness.calculateRawFitness(p);
					pBest = p;
				}
		}		
		return pBest;
	}
}
