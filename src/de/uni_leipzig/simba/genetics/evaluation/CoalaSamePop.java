package de.uni_leipzig.simba.genetics.evaluation;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.impl.GPGenotype;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.ALFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;

public class CoalaSamePop extends CoalaEvaluation {
	
	
	@Override
	public void run(DataSets data, int popSize, int gens, int numberOfExamples, int inquiries) throws InvalidConfigurationException {
		currentDataSet = data;
		params = DataSetChooser.getData(data);
		String name = params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "AL_");
		params.setEvaluationResultFileName(name);
		cR = params.getConfigReader();
		o = new SimpleOracle(params.getReferenceMapping());		
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, params.getPropertyMapping());
		config.setSelectFromPrevGen(reproduction);
		config.setCrossoverProb(mutationCrossRate);
		config.setMutationProb(mutationCrossRate);
		config.setReproductionProb(reproduction);
		config.setPopulationSize(popSize);
		config.setPreservFittestIndividual(true);
		config.setAlwaysCaculateFitness(true);
		
		fitness = new ALFitnessFunction(config, o, "f-score", numberOfExamples);
		fitness.useFScore();
		Mapping start = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), numberOfExamples);
		System.out.println("Start data "+start);
		fitness.setReferenceMapping(start);
		fitness.trimKnowledgeBases(start);
		
		config.setFitnessFunction(fitness);
		GPProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		// first evolve together
		gp.evolve(gens);
//		for(String correlationMethod : methods) {
//			for(int run = 0; run < RUNS; run++) {// per method run 5 runs 
//				
//			}
//		}
//		
//		for(int i = 0; i < 3; i++) {
//			
//		}
		
	}
	

}
