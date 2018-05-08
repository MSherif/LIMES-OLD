package de.uni_leipzig.simba.ukulele;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.impl.GPGenotype;

import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.PseudoFMeasureFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.keydiscovery.model.CandidateNode;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.util.Clock;

public class UnSupEvaluation extends Evaluation {

	
	
	public void runEAGLEExperimentUnSupervised(EvaluationData data, Set<CandidateNode> nodesA, 
			Set<CandidateNode> nodesB, EvaluationParameter param,
			int run			
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
		PseudoFMeasureFitnessFunction fitness = PseudoFMeasureFitnessFunction.getInstance(config, new PseudoMeasures(), data.getSourceCache(), data.getTargetCache());
//		fitness.setReferenceMapping(trainingData);
//		fitness.useFullCaches(param.useFullCaches);
//		if(!param.useFullCaches) {
//			fitness.trimKnowledgeBases(trainingData);
//		}
		config.setFitnessFunction(fitness);
		//FIXME make this outside ?
		File logFile = new File("resources/results/RockEAGLE_unsup_"+data.getName()+".csv");
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
	
	
	public void runPlainEAGLEUnSupervised(EvaluationData data, EvaluationParameter param,
			int run			
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
		PseudoFMeasureFitnessFunction fitness = PseudoFMeasureFitnessFunction.getInstance(config, new PseudoMeasures(), data.getSourceCache(), data.getTargetCache());
//		fitness.setReferenceMapping(trainingData);
//		fitness.useFullCaches(param.useFullCaches);
//		if(!param.useFullCaches) {
//			fitness.trimKnowledgeBases(trainingData);
//		}
		config.setFitnessFunction(fitness);
		//FIXME make this outside ?
		File logFile = new File("resources/results/EAGLE_unsup__"+data.getName()+".csv");
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

	
	
	public static void main(String[] args) {
		UnSupEvaluation eval = new UnSupEvaluation();
		double coverage = 0.8;
		eval.setOutStreams("UKELE_unsup");
			DataSets datas[] = {
					DataSets.PERSON1,				
					DataSets.PERSON2, DataSets.RESTAURANTS_FIXED, DataSets.DBLPACM,
					DataSets.ABTBUY, 
					DataSets.DBPLINKEDMDB, 
					DataSets.AMAZONGOOGLE, 
					DataSets.DBLPSCHOLAR 
			};
		// for every data set
		for(DataSets ds : datas) {
			EvaluationData data = DataSetChooser.getData(ds);
			
			// runs rocker
			List<Set<CandidateNode>> rockerResult = RockerRunner.runRocker(data,coverage);
			// init parameter, training data
			EvaluationParameter params = new EvaluationParameter();
			
			// TODO here goes the key selection
//			Rocker rocker = RockerRunner.getRockerObject();
//			String[] name = {"source", "target"};
//			for(int i=0; i<=1; i++) {
//				System.out.println("=== " + name[i] + " ===");
//				for(CandidateNode cn : rockerResult.get(i)) {
//					System.out.println(cn + " is composed by:");
//					for(Property p : cn.getProperties()) {
//						CandidateNode atomic = rocker.getAlgo().getAtomicCandidate(p);
//						System.out.println("\t" + p.getURI() + " --weight--> " + atomic.getScore());
//					}
//				}
//			}
			
			
			try {
				for(int run=1; run <= params.runs; run++) { // number of runs
					eval.reset();
//					int trainingDataSize = (int) (data.getReferenceMapping().size()*0.3);
//					Mapping trainingData = ExampleOracleTrimmer.getRandomTrainingData(data.getReferenceMapping(), trainingDataSize);
					// evaluate EAGLE based upon ROCKER
					eval.runEAGLEExperimentUnSupervised(data, rockerResult.get(0), rockerResult.get(1), params, run);
					// evaluate plain EAGLE
					eval.runPlainEAGLEUnSupervised(data, params, run);
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
	
	

}
