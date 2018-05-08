package de.uni_leipzig.simba.genetics.selfconfig;


import org.jgap.InvalidConfigurationException;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.core.PseudoFMeasureFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.learner.UnSupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;

/**
 * Basic self configuration using GP and a defined PseudoMeasure
 * @author Lyko
 *
 */
public class BasicGeneticSelfConfigurator implements GeneticSelfConfigurator {

	int  pop, gens;
	KBInfo sInfo, tInfo;
	Cache sC, tC;
	double beta = 1.0d;
	float mutation = 0.4f;
	float crossover = 0.4f;
	float reproduction = 0.5f;
	PropertyMapping propMapping;
	Measure measure;
	
	PseudoFMeasureFitnessFunction fitness;
	
	Mapping bestMapping = new Mapping();
	
	public Metric learn(UnSupervisedLearnerParameters parameters) throws InvalidConfigurationException {
		setParams(parameters);
		
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(sInfo, tInfo, propMapping);
		config.setCrossoverProb(crossover);
		config.setMutationProb(mutation);
		config.setReproductionProb(reproduction);
		config.setPopulationSize(pop);
		config.setPreservFittestIndividual(true);
		fitness = PseudoFMeasureFitnessFunction.getInstance(config, measure, sC, tC);
		fitness.setBeta(beta);
		config.setFitnessFunction(fitness);

		ExpressionProblem gpP = new ExpressionProblem(config);
		GPGenotype gp = gpP.create();
		
		for(int gen=1; gen<=gens; gen++) {
//			if(gen%10 == 0)
				System.out.println("Running gen:"+gen);
			gp.evolve();                    
                        GPPopulation pop = gp.getGPPopulation();
                        for(int i=0; i<pop.getPopSize(); i++)
                            System.out.println(">>" + Metric.getMetric(pop.getGPProgram(i)));
		}		
		return getFittest(gp);
	}

	/**
	 * Return fittest of the gp.
	 * @param gp
	 * @return
	 */
	private Metric getFittest(GPGenotype gp) {
		IGPProgram fittest = determinFittest(gp);
		if(fittest != null) {
			Metric metric = Metric.getMetric(fittest);
			bestMapping = this.fitness.getMapping(metric.getExpression(), metric.getThreshold());
			return metric;
		} else {
			return null;
		}
		
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
		return bestHere;
	}
	
	@Override
	public Mapping getMapping() {
		return bestMapping;
	}
	
	/**
	 * Sets Parameters
	 * @param params
	 * @return
	 */
	private boolean setParams(UnSupervisedLearnerParameters params) {
		// TODO make check if parameters are set correctly
		// required
		sInfo = params.getConfigReader().sourceInfo;
		tInfo = params.getConfigReader().targetInfo;
		pop = params.getPopulationSize();
		gens = params.getGenerations();
		
		sC = HybridCache.getData(sInfo);
		tC =  HybridCache.getData(tInfo);
		propMapping = params.getPropertyMapping();
		measure = params.getPseudoFMeasure();
		// optionals
		mutation = params.getMutationRate();
		crossover = params.getCrossoverRate();
		beta = params.getPFMBetaValue();		
		return true;
	}
	
	
	public static void main(String args[]) {
		EvaluationData param = DataSetChooser.getData(DataSets.RESTAURANTS_CSV);
		UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(param.getConfigReader(), param.getPropertyMapping());
		params.setPFMBetaValue(1.0d);
		params.setGenerations(100);
		params.setPopulationSize(10);
		params.setCrossoverRate(0.4f);
		params.setMutationRate(0.4f);
		params.setPseudoFMeasure(new PseudoMeasures());
		BasicGeneticSelfConfigurator configer = new BasicGeneticSelfConfigurator();
		try {
			 Metric m = configer.learn(params);
			 Mapping map = configer.getMapping();
			 System.out.println("Best Link Specificion:  " + m);
			 System.out.println("Its map has " + map.size() + " links.");
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

}
