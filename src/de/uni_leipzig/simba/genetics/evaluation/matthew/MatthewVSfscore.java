package de.uni_leipzig.simba.genetics.evaluation.matthew;

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
import de.uni_leipzig.simba.genetics.evaluation.BasicEvaluator;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.learner.MappingCorrelation;
import de.uni_leipzig.simba.genetics.learner.WeightDecayCorrelation;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.util.Clock;

/**
 * Evaluation of FScore vs. Matthews Coefficient Correlation.
 * @author Klaus Lyko lyko.klaus@informatik.uni-leipzig.de
 *
 */
public class MatthewVSfscore extends BasicEvaluator{
	
	public static final  String AL ="AL_Normal";
	public static final  String AL_CLUSTERING ="AL_Cluster";
	public static final  String AL_WEIGHTDECAY ="AL_WD";
	public static String[] methods = {AL_CLUSTERING,AL_WEIGHTDECAY,AL};
	public static String methodType = AL;
	public static  double rValueWD = 2;
	public static  int edgeCountCluster= 3;
	
	public static String rawFitness;
	public static final String FScore = "fscore";
	public static final String MatthewsCoefficient = "matthews";
	public static String[] rawFitnesses = {MatthewsCoefficient, FScore};
	
	
	
	public static void main(String[] args) {
		DataSets[] datas = new DataSets[] {DataSets.DBLPACM, DataSets.ABTBUY, 
//				DataSets.PERSON1_CSV, DataSets.PERSON2_CSV, DataSets.RESTAURANTS_CSV
				};
		for(DataSets data : datas)
			runEval(data);
	}

	/**
	 * Runs an eval on a specific data set: compares Matthews Correlations and FScores using basic EAGLE approach.
	 * @param data
	 */
	public static void runEval(DataSets data) {
		for(String ff : rawFitnesses) {
			for(int pop : new int[]{20}) {
				MatthewVSfscore.rawFitness = ff;
				MatthewVSfscore eval = new MatthewVSfscore();
				MatthewVSfscore.rawFitness = ff;
				try {
					System.out.println("Running "+data+" with raw fitness: " + rawFitness +" for popSize="+pop);
					eval.run(data, pop, 50, 10, 10);
				} catch (InvalidConfigurationException e) {
					e.printStackTrace();
				}
			}
	
		}
	}
		
	
	/**
	 * Runs a specific evaluation using the techniques set by the static parameters.
	 * @param data The Dataset.
	 * @param popSize Population size.
	 * @param gens Number of generations run before asking for more data
	 * @param numberOfExamples Number instances to be labeled by the user each inquery.
	 * @param inquiries How many inquiries.
	 * @throws InvalidConfigurationException
	 */
	public void run(DataSets data, int popSize, int gens, int numberOfExamples, int inquiries) throws InvalidConfigurationException {
		logger.info("Running dataset "+data);
		currentDataSet = data;
		params = DataSetChooser.getData(data);
		String name = params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "AL_");
		params.setEvaluationResultFileName(name);
//		params.put(MapKey.EVALUATION_FILENAME,  name);
		
		
		
//		cR = new ConfigReader();
//		cR.validateAndRead(""+params.get(MapKey.BASE_FOLDER)+params.get(MapKey.CONFIG_FILE));
//		String verificationFile = ""+params.get(MapKey.BASE_FOLDER)+params.get(MapKey.DATASET_FOLDER)+params.get(MapKey.REFERENCE_FILE);
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
			fitness = new ALFitnessFunction(config, o, "f-score", numberOfExamples);
			if(rawFitness.equalsIgnoreCase(MatthewsCoefficient)) {
				fitness.useMCC();
			} else {
				fitness.useFScore();
			}
			Mapping start = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), numberOfExamples);
			System.out.println("Start data "+start);
			fitness.setReferenceMapping(start);
			fitness.trimKnowledgeBases(start);
			config.setFitnessFunction(fitness);
			
			GPProblem gpP = new ExpressionProblem(config);
			
			GPGenotype gp = gpP.create();
			Clock clock = new Clock();
			ALDecider aLD  = new ALDecider();
			
				
				for(int inquery = 1; inquery <= inquiries; inquery++) {
					logger.info("Inquery nr "+inquery+" on dataset"+data+" run "+run+ " method " +
							methodType);
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
							if (methodType.equals(AL_CLUSTERING)){
								MappingCorrelation cor= new MappingCorrelation(cR.sourceInfo,cR.targetInfo,
										fitness.getMetric(gp.getFittestProgram()).getExpression());
								toAsk = cor.getDisimilarMappings(aLD.getControversyCandidates(mapsOfPop), numberOfExamples,edgeCountCluster);
								aLD.retrieved.addAll(toAsk);
							}else if (methodType.equals(AL_WEIGHTDECAY)){
								WeightDecayCorrelation wdc = new WeightDecayCorrelation (cR.sourceInfo,cR.targetInfo,
										fitness.getMetric(gp.getFittestProgram()).getExpression());
								toAsk = wdc.getDisimilarMappings(aLD.getControversyCandidates(mapsOfPop), numberOfExamples,rValueWD);
								aLD.retrieved.addAll(toAsk);
							}else {
								toAsk =aLD.getControversyCandidates(mapsOfPop, numberOfExamples);
							}
											
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
		finishDataset(RUNS, popSize, gens, params, getNameExtension(popSize, gens));		
	}// end of run()
	
	public String getNameExtension(int popSize, int gens) {
		String nameExtansion = rawFitness+"_";
		nameExtansion += "AL_pop="+popSize+"_gens="+gens+"_meth="+methodType;
		if (methodType.equals(AL_CLUSTERING)){
			nameExtansion += "ECount="+edgeCountCluster;
		}else if (methodType.equals(AL_WEIGHTDECAY)){
			nameExtansion+="rValue="+rValueWD;
		}
		return nameExtansion;
	}
	
}
