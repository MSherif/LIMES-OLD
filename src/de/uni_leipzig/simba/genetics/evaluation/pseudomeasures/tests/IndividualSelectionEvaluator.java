package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.tests;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.GPProgram;

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
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoBaseEvaluator;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.SizeAwarePseudoFMeasure;
import de.uni_leipzig.simba.selfconfig.SizeUnawarePseudoFMeasure;


public class IndividualSelectionEvaluator extends PseudoBaseEvaluator {

		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		int win1 = 0;
		int win2 = 0;
		
		public IndividualSelectionEvaluator() {			
			population = 20;
			generations = 40;
			maxRuns = 1;
			df.applyPattern( "#,###,######0.000" );
		}
		
		public void runTest(EvaluationData param, int run, Measure measure, double beta) throws InvalidConfigurationException {
			bestEverFitness = Double.MAX_VALUE;
			if(run==0) {
				System.exit(1);
			}
			System.out.println("Running run"+run+" on "+param.getName());
			logger.info("Running run"+run+" on "+param.getName());
			perRunAndDataSet = new LinkedList<EvaluationPseudoMemory>();
		
			ConfigReader cR =  param.getConfigReader();
//			cR.validateAndRead((String)param.get(MapKey.BASE_FOLDER)+param.get(MapKey.CONFIG_FILE));
//			String inputType = "xml";
		
			
			
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
//			fitness = new PseudoFMeasureFitnessFunction(config, measure, sC, tC);
			fitness.setBeta(beta);
			config.setFitnessFunction(fitness);

			ExpressionProblem gpP = new ExpressionProblem(config);
			GPGenotype gp = gpP.create();
			

			IGPProgram p1 = null;
			IGPProgram p2 = null;
			for(int gen=1; gen<=generations; gen++) {
				if(gen%10 == 0)
					System.out.println("Running gen:"+gen+" of run:"+run);
				gp.evolve();
				long dur1,dur2;
				long start = System.currentTimeMillis();
				
				
				p1=processGeneration(gp, gen, run);
				dur1 = System.currentTimeMillis() - start;				
				start = System.currentTimeMillis();
				p2=processGeneration2(gp, gen, run);
				dur2 = System.currentTimeMillis() - start;
				eval(fitness, reference, p1, p2, dur1, dur2);
			}
			//Metric m = getMetric(gp.getAllTimeBest());
//			System.out.println(m);
			
			
//			finishRun(param, run);
//			if((Integer)param.get("maxruns")==run) {
//				finishDataSet(param);
//			}
			fitness.destroy();
			fitness = null;
			Configuration.reset();
		}
		
		public IGPProgram processGeneration2(GPGenotype gp, int gen, int run) {
			IGPProgram pBest;// = (GPProgram) gp.getFittestProgram();
			GPPopulation pop = gp.getGPPopulation();
			pop.sortByFitness();
			double fittest = Double.MAX_VALUE;
			pBest = pop.determineFittestProgram();
			fittest = fitness.calculateRawFitness(pBest);
			// to do recalculate older programs
			for(GPProgram p:(GPProgram[])pop.getGPPrograms())
			{
				double actFit = fitness.calculateRawFitness(p);
				if(fittest>actFit && getMetric(p).getExpression().indexOf("falseProp") == -1) {
					pBest = p;
					fittest = actFit;
//					System.out.println("Setting to fitter program then JGAP");
				}			
			}
			return pBest;
		}
		
		public void eval(PseudoFMeasureFitnessFunction fitness, Mapping reference, IGPProgram p1, IGPProgram p2, long dur1, long dur2) {
			Metric m1 = getMetric(p1);
			Metric m2 = getMetric(p2);
			Mapping map1 = fitness.getMapping(m1.getExpression(), m1.getThreshold());
			Mapping map2 = fitness.getMapping(m2.getExpression(), m2.getThreshold());
			double fMeasure1, fMeasure2;
			PRFCalculator prf = new PRFCalculator();
//			prec = prf.computePrecision(bestMapping, reference);
//			recall = prf.computeRecall(bestMapping, reference);
			fMeasure1 = prf.fScore(map1, reference);
			fMeasure2 = prf.fScore(map2, reference);
			if(Math.abs(fMeasure1 - fMeasure2) >= 0.05d) {
				if(fMeasure1>fMeasure2)
					win1++;
				else
					win2++;
			}
			
			System.out.println("f1="+df.format(fMeasure1)+"["+df.format(fitness.calculatePseudoMeasure(p1))+"]"+"("+dur1+") -- f2="+df.format(fMeasure2)+"["+df.format(fitness.calculatePseudoMeasure(p2))+"]"+"("+dur2+")");
			
		}
		
		public static void main(String args[]) {
//			Double[] allBetaValues = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
//					1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0};
			IndividualSelectionEvaluator eval = null;

			Map<DataSets, Measure> setup = getEvaluationData();	
			for(Entry<DataSets, Measure> test : setup.entrySet()) {
				EvaluationData param = DataSetChooser.getData(test.getKey());
				try {
					if(eval == null) {
						eval = new IndividualSelectionEvaluator();
					}
						System.out.println("eval.run("+param.getName()+", "+test.getValue().getName()+", allBetaValues)\n");
						logger.info("eval.run("+param.getName()+", "+test.getValue().getName()+", allBetaValues)\n");
						eval.runTest(param, 1, test.getValue(), 1.0d);
				} catch (Exception e) {
					e.printStackTrace();
				continue;
				}
			}
		}
		
		
		/**
		 * Maps DataSets to their Measure they have to be tested with. 
		 * @return
		 */
		public static Map<DataSets, Measure> getEvaluationData() {
			Measure sa = new SizeAwarePseudoFMeasure();
			Measure su = new SizeUnawarePseudoFMeasure();
			Map<DataSets, Measure> testSetup = new HashMap<DataSets, Measure>();
//			testSetup.put(DataSets.PERSON1_CSV, su);
			testSetup.put(DataSets.PERSON1_CSV, sa);
//			testSetup.put(DataSets.PERSON2_CSV, su);
			testSetup.put(DataSets.PERSON2_CSV, sa);
//			testSetup.put(DataSets.RESTAURANTS_CSV, su);
			testSetup.put(DataSets.RESTAURANTS_CSV, sa);
//			testSetup.put(DataSets.ABTBUY, sa);
			testSetup.put(DataSets.ABTBUY, su);
			testSetup.put(DataSets.AMAZONGOOGLE, sa);
//			testSetup.put(DataSets.AMAZONGOOGLE, su);
			return testSetup;
		}
}
