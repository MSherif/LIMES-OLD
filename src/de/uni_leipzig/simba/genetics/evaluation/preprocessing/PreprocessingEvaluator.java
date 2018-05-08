package de.uni_leipzig.simba.genetics.evaluation.preprocessing;

import java.util.LinkedList;
import java.util.List;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.impl.GPGenotype;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.genetics.core.ALDecider;
import de.uni_leipzig.simba.genetics.core.ALFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.PreprocessingFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.BasicEvaluator;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.util.Clock;

/**
 * Evaluates EEL - the Enhanced EAGLE Learner approach to also learn Preprocessing.
 * Basically compares the performance of EAGLE and EEL over several w.r.t. both quality and runtime.
 * @author Klaus Lyko
 *
 */
public class PreprocessingEvaluator extends BasicEvaluator{

	public static final  String EAGLE ="Eagle";
	public static final  String EEL ="Eel";
	public static String[] methods = {EAGLE,EEL};
//	public static String methodType;

	public static final int RUNS = 5;
	/**
	 * 
	 * @param data The Dataset.
	 * @param popSize Population size.
	 * @param gens Number of generations run before asking for more data
	 * @param numberOfExamples Number instances to be labeled by the user each inquery.
	 * @param inquiries How many inquiries.
	 * @throws InvalidConfigurationException
	 */
	public void run(DataSets data, int popSize, int gens, int numberOfExamples, int inquiries, String method) throws InvalidConfigurationException {
		logger.info("Running dataset "+data);
		currentDataSet = data;
		params = DataSetChooser.getData(data);
		String name = (method+"_"+params.getName()+"_pop="+popSize+".csv");
		params.setEvaluationResultFileName(name);
		//fitness = ALFitnessFunction.
//		cR = new ConfigReader();
//		cR.validateAndRead(""+params.get(MapKey.BASE_FOLDER)+params.get(MapKey.CONFIG_FILE));
//		String verificationFile = ""+params.get(MapKey.BASE_FOLDER)+params.get(MapKey.DATASET_FOLDER)+params.get(MapKey.DATASET_FOLDER);
//		o = OracleFactory.getOracle(verificationFile, verificationFile.substring(verificationFile.lastIndexOf(".")+1), "simple");
//		cR = (ConfigReader) params.get(MapKey.CONFIG_READER);
//		o = new SimpleOracle((Mapping)params.get(MapKey.REFERENCE_MAPPING));
		cR = params.getConfigReader();
		o = new SimpleOracle(params.getReferenceMapping());
		for(int run = 1; run <= RUNS; run++) {
			LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, params.getPropertyMapping());
			config.setSelectFromPrevGen(reproduction);
			config.setCrossoverProb(mutationCrossRate);
			config.setMutationProb(mutationCrossRate);
			config.setReproductionProb(reproduction);
			config.setPopulationSize(popSize);
			config.setPreservFittestIndividual(true);
			config.setAlwaysCaculateFitness(true);
			logger.info("Starting run "+run);
			
			if(method.equalsIgnoreCase(EEL))
				fitness = new PreprocessingFitnessFunction(config, o, "f-score", numberOfExamples);
			else
				fitness = new ALFitnessFunction(config, o, "f-score", numberOfExamples);
			fitness.useFScore();
			Mapping start = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), numberOfExamples);
			System.out.println("Start data "+start);
			fitness.setReferenceMapping(start);
			fitness.trimKnowledgeBases(start);
			config.setFitnessFunction(fitness);
			GPProblem gpP;
			if(method.equalsIgnoreCase(EEL))
				gpP = new ExpressionProblem(config, true);
			else
				gpP = new ExpressionProblem(config, false);			
			GPGenotype gp = gpP.create();
			Clock clock = new Clock();
			ALDecider aLD  = new ALDecider();
			
				
				for(int inquery = 1; inquery <= inquiries; inquery++) {
					logger.info("Inquery nr "+inquery+" on dataset"+data+" run "+run+ " method " +
							method);
					for(int gen = 0; gen < gens; gen++) {
						gp.evolve();
						gp.calcFitness();
					}
					examineGen(run, inquery*numberOfExamples, gp, clock);
					// query Oracle
					if(inquery != inquiries) {
						List<Mapping> mapsOfPop = getMaps(gp.getGPPopulation());
						if(mapsOfPop.size() > 0) {	
							List<Triple> toAsk = new LinkedList<Triple>(); 
							toAsk =aLD.getControversyCandidates(mapsOfPop, numberOfExamples);
														
							logger.info("Asking about "+toAsk.size()+" instances.");
							if(toAsk.size() == 0)
								aLD.maxCount += 100;
							Mapping oracleAnswers = new Mapping();
							for(Triple t : toAsk) {					
								if(o.ask(t.getSourceUri(), t.getTargetUri()))
									oracleAnswers.add(t.getSourceUri(), t.getTargetUri(), 1d);
							}
							fitness.addToReference(oracleAnswers);
							fitness.fillCachesIncrementally(toAsk);	
						}// if we got anything to ask about
					}	//inqueries != queries			
				}// per inquery
			finishRun();
			fitness.destroy();
			LinkSpecGeneticLearnerConfig.reset();
		}// per run
		finishDataset(RUNS, popSize, gens, params, "");		
	}// end of run()

	
	public static void runEval(DataSets data) {
		for(String method : new String[]{EAGLE}) {
			for(int pop : new int[]{20,100}) { // expect that performance overhead decreases with more individuals
				PreprocessingEvaluator eval = new PreprocessingEvaluator();
				try {
					System.out.println("Running "+data+" with method: " +  method +" for popSize="+pop);
					eval.run(data, pop, 10, 10, 10, method);
				} catch (InvalidConfigurationException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String args[]) {
		DataSets[] datas = new DataSets[] {
//				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
//				DataSets.DBLPACM,
//				DataSets.DBPLINKEDMDB,
//				DataSets.PERSON1_CSV,
//				DataSets.PERSON2_CSV,
//				DataSets.RESTAURANTS_CSV,
		};
		for(DataSets data : datas)
			runEval(data);
//		System.out.println(java.lang.Runtime.getRuntime().maxMemory()/1024/1024+"-"+Long.MAX_VALUE); 
	}
}
