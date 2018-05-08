package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import java.util.Collections;
import java.util.LinkedList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.util.Clock;
/**
 * Class to run better evaluation. Providing all data through DataSetChooser class: loading caches and oracle, defining folders and such.
 * We should propably should also define a class for parameters for the execution: defining genetic parameters and such.
 * See main for usage, a look at the DataSetChooser is strongly recommended.
 * 
 * @author Klaus Lyko
 *
 */
public class PseudoEvaluation {
	static final Logger logger = Logger.getLogger("LIMES");
	
	public float mutation = 0.6f;
	public float crossover = 0.6f;
	public float reproduction = 0.5f;
	public int generations = 10;
	public int population = 20;
	double beta = 1d;
	public long maxDuration = 600; //in seconds
	LinkSpecGeneticLearnerConfig config;
	
	Clock clock;
	PseudoFMeasureFitnessFunction fitness;
	
	LinkedList<EvaluationPseudoMemory> perRunAndDataSet;
	/**Sorted by their sortNumber*/
	LinkedList<EvaluationPseudoMemory> perDataSet = new LinkedList<EvaluationPseudoMemory>();

	String extansion;

	IGPProgram bestEver; double bestEverFitness;
	
	public int maxRuns = 5;
	/**
	 * 
	 * @param baseFolder
	 * @param configFile
	 * @param referenceFile
	 * @throws InvalidConfigurationException 
	 */
	public void run(EvaluationData param, int run, PseudoMeasures measure, String nameExtansion) throws InvalidConfigurationException {
		bestEverFitness = Double.MAX_VALUE;
		if(nameExtansion!=null)
			this.extansion = nameExtansion;
//		logger.setLevel(Level.WARN);
		if(run==0) {
			System.exit(1);
		}
		System.out.println("Running run"+run+" on "+param.getConfigFileName());
		logger.info("Running run"+run+" on "+param.getConfigFileName());
		perRunAndDataSet = new LinkedList<EvaluationPseudoMemory>();
		clock = new Clock();
//		ConfigReader cR = new ConfigReader();
//		cR.validateAndRead((String)param.get("basefolder")+param.get("config"));
//		String inputType = "xml";
	
		ConfigReader cR = param.getConfigReader();
		Cache sC = param.getSourceCache();
		Cache tC = param.getTargetCache();
		Mapping reference = param.getReferenceMapping();

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

		ExpressionProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		

		Metric m = null;
		int gen = 0;
		for(gen=1; gen>=0; gen++) {
//			if(gen%10 == 0)
//				System.out.println("Running gen:"+gen+" of run:"+run);
			gp.evolve();
			m = processGeneration(gp, gen, run);
			if((clock.totalDuration()/1000)>maxDuration)
				break;
		}
//		m = processGeneration(gp, gen++, run);
//		System.out.println(m);
		Mapping bestMapping = fitness.getMapping(m.getExpression(), m.getThreshold());
//		bestMapping
		double prec, recall, fMeasure;
		PRFCalculator prf = new PRFCalculator();
		prec = prf.precision(bestMapping, reference);
		recall = prf.recall(bestMapping, reference);
		fMeasure = prf.fScore(bestMapping, reference);
		
		System.out.println("prec="+prec+" recall="+ recall+" fscore="+ fMeasure);
		System.out.println("Duration:"+clock.durationSinceClick());
		System.out.println("best Mapping size:"+bestMapping.size());
		System.out.println("reference size: "+reference.size());
		
		finishRun(param, run);
		if(param.getMaxRuns() == run) {
			finishDataSet(param);
		}
		fitness.destroy();
		fitness = null;
	}
	

	/**
	 * Makes necessary steps to finishes the run: sorts individuals into their position.
	 * @param param
	 * @param run
	 */
	private void finishRun(EvaluationData param, int run) {
		perDataSet.addAll(perRunAndDataSet);
		perRunAndDataSet.clear();
		fitness.destroy();
		LinkSpecGeneticLearnerConfig.reset();
	}

	/**
	 * Writes the output after all runs are done.
	 */
	@SuppressWarnings("unchecked")
	public void finishDataSet(EvaluationData param) {
		Mapping reference = param.getReferenceMapping();
		Collections.sort(perDataSet);
		for(EvaluationPseudoMemory mem : perDataSet) {
			Mapping map = fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold());
			// For real F-measures use best 1-to-1 mapping
			Mapping map_1to1  = Mapping.getBestOneToOneMappings(map);
			double prec, recall, fMeasure, prec_1to1, recall_1to1, fMeasure_1to1;
			prec = PRFCalculator.precision(map, reference);
			recall = PRFCalculator.recall(map, reference);
			fMeasure = PRFCalculator.fScore(map, reference);
			mem.precision=prec;
			mem.recall=recall;
			mem.fmeasue=fMeasure;
			
			prec_1to1 = PRFCalculator.precision(map_1to1, reference);
			recall_1to1 = PRFCalculator.recall(map_1to1, reference);
			fMeasure_1to1 = PRFCalculator.fScore(map_1to1, reference);
			mem.precision_1to1=prec_1to1;
			mem.recall_1to1=recall_1to1;
			mem.fmeasue_1to1=fMeasure_1to1;
		}
		// log it
		PseudoEvalFileLogger fileLog = new PseudoEvalFileLogger(param.getEvauationResultFolder(), param.getEvaluationResultFileName());
		fileLog.nameExtansion = extansion+"_Beta-"+beta+"_"; 
		fileLog.log(perDataSet, maxRuns, this.generations, param);
//		if(!fileLog.log(perDataSet, maxRuns, this.generations, param)) {
////			System.exit(1);
//			perDataSet.clear();
//		}
		perDataSet.clear();
		perRunAndDataSet.clear();
	}

	/**
	 * Logs the best individual this generation
	 * @param gp
	 * @param gen
	 * @param run
	 */
	public Metric processGeneration(GPGenotype gp, int gen, int run) {
		System.out.println("Processing generation "+gen);
		IGPProgram pBest = determinFittest(gp);
		if(gp!=null) 
			perRunAndDataSet.add(new EvaluationPseudoMemory(run, gen, getMetric(pBest), fitness.calculateRawFitness(pBest), fitness.calculatePseudoMeasure(pBest), clock.totalDuration()));
		return getMetric(pBest);
	}
	
	private IGPProgram determinFittest(GPGenotype gp) {
		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();
		
		IGPProgram bests[] = { gp.getFittestProgramComputed(),
			pop.determineFittestProgram(),
			gp.getAllTimeBest(),
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
		if(fittest<bestEverFitness) {
				bestEverFitness = fittest;
				gp.getGPPopulation().addFittestProgram(bestEver);
				return bestHere;
		} else {
		
		/**
		* @FIXME return fittest ever computed
		*/
			this.bestEver = bestEver;
		}
		if(bestEver == null) {
			bestEver = bestHere;
		}
		return bestHere;
	}
	
	
	
	public Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}
	
	public static void main(String args[]) {
		
		PseudoEvaluation eval = new PseudoEvaluation();
		PseudoMeasures measure;
		String nameExtansion="bestever";
		logger.setLevel(Level.ERROR);
		DataSets[] data = {
				DataSets.PERSON1,
//				DataSets.PERSON2,
//				DataSets.RESTAURANTS,
//				DataSets.DBLPACM, 
//				DataSets.ABTBUY,
//				DataSets.AMAZONGOOGLE
				};
		/**Which measures*/
		measure= new PseudoMeasures();
		nameExtansion = "_EAGLE_PFM_";
		try {
			
			for(DataSets dataset:data) {
				System.out.println(nameExtansion+": Running dataset " + dataset);
				EvaluationData param = DataSetChooser.getData(dataset);
				for(int run =1; run<=eval.maxRuns; run++) {
					param.setMaxRuns(eval.maxRuns);
					eval.run(param, run, measure, nameExtansion);
				}
			}
			
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
