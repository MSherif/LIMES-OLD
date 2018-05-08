package de.uni_leipzig.simba.ukulele;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPFitnessFunction;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;

import com.hp.hpl.jena.rdf.model.Property;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.IFitnessFunction;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.keydiscovery.model.CandidateNode;
import de.uni_leipzig.simba.keydiscovery.rockerone.Rocker;
import de.uni_leipzig.simba.ukulele.RockerRunner.Arg;
import de.uni_leipzig.simba.util.Clock;

/**
 * @author Klaus Lyko <klaus.lyko@informatik.uni-leipzig.de>
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Evaluation {

//	public GPFitnessFunction fitness; //FIXME UnsupFitness extends Express.
	double bestEverFitness = Double.MAX_VALUE;
	IGPProgram bestEver;
	Clock clock;
//	LinkedList<EvaluationPseudoMemory> perRunAndDataSet;
	RunSolution solution;
	
	public void runEAGLEExperimentSupervised(EvaluationData data, Set<CandidateNode> nodesA, 
			Set<CandidateNode> nodesB, EvaluationParameter param,
			Mapping trainingData, int run			
			) throws InvalidConfigurationException, IOException {
		//build EAGLEs propertyMapping based upon ROCKERs keys
		bestEverFitness = Double.MAX_VALUE;
		PropertyMapping propMap = buildPropertyMappingForEAGLE(nodesA, nodesB);
		data.setPropertyMapping(propMap);
		// init EAGLE
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(data.getConfigReader().sourceInfo, data.getConfigReader().targetInfo, data.getPropertyMapping());
		config.sC = data.getSourceCache();
		config.tC = data.getTargetCache();
//		config.setSelectFromPrevGen(param.reproductionProbability);
		config.setCrossoverProb(param.mutationProbability);
		config.setMutationProb(param.mutationProbability);
		config.setReproductionProb(param.reproductionProbability);
		config.setPopulationSize(param.population);
		config.setPreservFittestIndividual(true);
		config.setAlwaysCaculateFitness(true);		
		
		ExpressionFitnessFunction fitness = ExpressionFitnessFunction.getInstance(config, trainingData, "f-score", 
				trainingData.size());
		fitness.setReferenceMapping(trainingData);
		fitness.useFullCaches(param.useFullCaches);
		if(!param.useFullCaches) {
			fitness.trimKnowledgeBases(trainingData);
		}
		config.setFitnessFunction(fitness);
		//FIXME make this outside ?
		File logFile = new File("resources/results/RockEAGLE_"+data.getName()+".csv");
		solution = new RunSolution(data, fitness, logFile);
		clock = new Clock();
		
		GPProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		// evolve generations
		for(int gen = 0; gen<param.generations; gen++) {
			gp.evolve();
			// inspect generation: fitter solution?
			processGeneration(gp, run, gen, fitness);
		}		
		// log last one
		System.out.println("Creating last...fit="+bestEverFitness+" fitness_recalc="+fitness.calculateRawFitness(bestEver)+" pfm="+fitness.calculateRawMeasure(bestEver));
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, param.generations, getMetric(bestEver),
				bestEverFitness, fitness.calculateRawMeasure(bestEver), clock.totalDuration());
		solution.perRunAndDataSet.add(mem);
		// compute F-Measures, AUC values and log to file
		solution.processRun(run,true);		
		fitness.destroy();
		config.reset();
	}
	
	
	public void runPlainEAGLESupervised(EvaluationData data, EvaluationParameter param,
			Mapping trainingData, int run			
			) throws InvalidConfigurationException, IOException {
		bestEverFitness = Double.MAX_VALUE;
		//build EAGLEs propertyMapping n-to-m based
		PropertyMapping propMap = buildNtoMPropertyMapping(data);
		data.setPropertyMapping(propMap);
		// init EAGLE
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(data.getConfigReader().sourceInfo, data.getConfigReader().targetInfo, data.getPropertyMapping());
		config.sC = data.getSourceCache();
		config.tC = data.getTargetCache();
//		config.setSelectFromPrevGen(param.reproductionProbability);
		config.setCrossoverProb(param.mutationProbability);
		config.setMutationProb(param.mutationProbability);
		config.setReproductionProb(param.reproductionProbability);
		config.setPopulationSize(param.population);
		config.setPreservFittestIndividual(true);
		config.setAlwaysCaculateFitness(true);		
		ExpressionFitnessFunction fitness = ExpressionFitnessFunction.getInstance(config, trainingData, "f-score", trainingData.size());
		fitness.setReferenceMapping(trainingData);
		fitness.useFullCaches(param.useFullCaches);
		if(!param.useFullCaches) {
			fitness.trimKnowledgeBases(trainingData);
		}
		config.setFitnessFunction(fitness);
		//FIXME make this outside ?
		File logFile = new File("resources/results/EAGLE_"+data.getName()+".csv");
		solution = new RunSolution(data, fitness, logFile);
		clock = new Clock();
		
		GPProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		// evolve generations
		for(int gen = 0; gen<param.generations; gen++) {
			gp.evolve();
			// inspect generation: fitter solution?
			processGeneration(gp, run, gen, fitness);
		}		
		// log last one
		EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, param.generations, getMetric(bestEver),
				bestEverFitness, fitness.calculateRawMeasure(bestEver), clock.totalDuration());
		solution.perRunAndDataSet.add(mem);
		// compute F-Measures, AUC values and log to file
		solution.processRun(run, true);		
		
	}
	

	
	/**
	 * Computes the best of this generation, if it's better then the global best log it
	 * @param gp
	 * @param run
	 * @param gen
	 * @return current best program
	 */
	protected IGPProgram processGeneration(GPGenotype gp, int run, int gen, IFitnessFunction fitness) {
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
					bestHere = (IGPProgram) p.clone();
				}
			}
		}
		// if we reached a new better solution
		if(fittest<bestEverFitness) {
			bestEverFitness = fittest;
			gp.getGPPopulation().addFittestProgram(bestEver);
			bestEver = bestHere;
			double pfm = fitness.calculateRawMeasure(bestHere);
			EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, gen, getMetric(bestHere),
					fittest, pfm, clock.totalDuration());
			System.out.println("New best gen="+gen+" : "+fittest+" => "+mem.metric);
			solution.perRunAndDataSet.add(mem);
			return bestHere;
		}
		if(bestEver == null) { // for initial generation
			bestEver = bestHere;
		}
		return bestHere;
	}
	
	/**
	 * Generates LIMES' link specification for GP program
	 * @param p
	 * @return
	 */
	protected Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}
	
	public static void main(String args[]) {
		Evaluation eval = new Evaluation();
		double coverage = 0.8;
		eval.setOutStreams("UKELE_supervised");
		DataSets datas[] = {				
				DataSets.PERSON1,
				DataSets.PERSON2, DataSets.RESTAURANTS_FIXED, DataSets.DBLPACM, 
				DataSets.ABTBUY, DataSets.AMAZONGOOGLE, DataSets.DBPLINKEDMDB, DataSets.DBLPSCHOLAR				 
		};
		// for every data set
		for(DataSets ds : datas) {
			EvaluationData data = DataSetChooser.getData(ds);
			
			// runs rocker
			List<Set<CandidateNode>> rockerResult = RockerRunner.runRocker(data, coverage);
			// init parameter, training data
			EvaluationParameter params = new EvaluationParameter();
			
			// TODO here goes the key selection
			for(Arg arg : RockerRunner.getRockers().keySet()) {
				Rocker rocker = RockerRunner.getRockers().get(arg).getRockerObject();
				System.out.println("=== " + arg.getName() + " ===");
				for(CandidateNode cn : rockerResult.get(arg.getPosition())) {
					System.out.println(cn + " is composed by:");
					for(Property p : cn.getProperties()) {
						CandidateNode atomic = rocker.getAlgo().getAtomicCandidate(p);
						System.out.println("\t" + p.getURI() + " --weight--> " + atomic.getScore());
					}
				}
			}
			
			
			try {
				for(int run=1; run <= params.runs; run++) { // number of runs
					eval.reset();
					int trainingDataSize = (int) (data.getReferenceMapping().size()*0.3);
					Mapping trainingData = ExampleOracleTrimmer.getRandomTrainingData(data.getReferenceMapping(), trainingDataSize);
					// evaluate EAGLE based upon ROCKER
					eval.runEAGLEExperimentSupervised(data, rockerResult.get(0), rockerResult.get(1), params, trainingData, run);
					// evaluate plain EAGLE
					eval.runPlainEAGLESupervised(data, params, trainingData, run);
				}
				
			} catch (InvalidConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// XXX I commented this because it was giving warnings.
			// debug builds EAGLEs PropertyMapping based on ROCKER
//			PropertyMapping propMap = buildPropertyMappingForEAGLE(rockerResult.get(0), rockerResult.get(1));
//			System.out.println(propMap);
		}
		
		
	}
	

	
	
	/**
	 * Build EAGLES propertyMapping out of ROCKERs keys
	 * @param nodesA
	 * @param nodesB
	 * @return
	 */
	public static PropertyMapping buildPropertyMappingForEAGLE(Set<CandidateNode> nodesA, 
			Set<CandidateNode> nodesB) {
		PropertyMapping propMap = new PropertyMapping();
		//match all source and target candidate keys as PropertyMapping
		for(CandidateNode nodeA : nodesA) {
			for(String propA : nodeA.getPropertySet())
				for(CandidateNode nodeB : nodesB) {
					for(String propB : nodeB.getPropertySet()) {
						propMap.addStringPropertyMatch(propA.replaceAll("\"", ""), propB.replaceAll("\"", ""));
					}
				}
		}
		return propMap;
	}
	
	/**
	 * Generates a n-to-m mapping of all n source Properties and all m target properties.
	 * @param data
	 * @return
	 */
	public static PropertyMapping buildNtoMPropertyMapping(EvaluationData data) {
		PropertyMapping map = new PropertyMapping();
		for(String sp : data.getConfigReader().getSourceInfo().properties) {
			for(String tp : data.getConfigReader().getTargetInfo().properties) {
				map.addStringPropertyMatch(sp, tp);
			}
		}
		
		return map;
	}
	
	
	public void setOutStreams(String name) {
		try {
			File stdFile = new File(name+"_stdOut.txt");
			PrintStream stdOut;
			stdOut = new PrintStream(new FileOutputStream(stdFile, false));
			File errFile = new File(name+"_errOut.txt");
			PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
			System.setErr(errOut);
			System.setOut(stdOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void reset(){
		double bestEverFitness = Double.MAX_VALUE;
	}
}
