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

public class AbtBuyPseudoMeasureTester extends PseudoBaseEvaluator {
	
	DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
	
	public AbtBuyPseudoMeasureTester() {
		super();
		population = 10;
		generations = 10;
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
	
//		ConfigReader cR = new ConfigReader();
//		cR.validateAndRead((String)param.get(MapKey.BASE_FOLDER)+param.get(MapKey.CONFIG_FILE));
//		String inputType = "xml";
		ConfigReader cR = param.getConfigReader();
//		o = new SimpleOracle((Mapping)params.get(MapKey.REFERENCE_MAPPING));
		
	
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
		

//		IGPProgram p1 = null;
//		IGPProgram p2 = null;
		for(int gen=1; gen<=generations; gen++) {
			
			System.out.println("Running gen:"+gen+" of run:"+run);
			gp.evolve();
			GPPopulation pop= gp.getGPPopulation();
			pop.sortByFitness();
			int z = 0;
			for(IGPProgram p : pop.getGPPrograms()) {
				String nr = ""+gen+"."+(++z);
				processIndividual(nr, p, sC, tC, reference);
			}
			IGPProgram p1 = processGeneration(gp, gen, run);
			logger.info("BEST: " + p1);
		}
		fitness.destroy();
		fitness = null;
		Configuration.reset();
	}

	
	/**
	 * Get insights to individuals - how is the pseudo measure etc.
	 * @param p
	 */
	public void processIndividual(String nr, IGPProgram p, Cache sC, Cache tC, Mapping ref) {
		Mapping map = fitness.calculateMapping(p);
		Measure m = fitness.getMeasure();
				
		double pfm = m.getPseudoFMeasure(sC.getAllUris(), tC.getAllUris(), map, 1.0d);
		double pp = m.getPseudoPrecision(sC.getAllUris(), tC.getAllUris(), map);
		double pr = m.getPseudoRecall(sC.getAllUris(), tC.getAllUris(), map);
		
		
		Metric met = getMetric(p);
		
		PRFCalculator c = new PRFCalculator();
		String out = nr+"."+met + " (fit="+df.format(fitness.getFitnessValue(p))+"):\npfm=" +
				""+df.format(pfm)+"["+df.format(c.fScore(map, ref))+"]"+" pp="+df.format(pp)+" pr="+df.format(pr);
		System.out.println(out);
		logger.info(out);
	}
	
	/**
	 * Maps DataSets to their Measure they have to be tested with. 
	 * @return
	 */
	public static Map<DataSets, Measure> getEvaluationData() {
		Measure sa = new SizeAwarePseudoFMeasure();
		Measure su = new SizeUnawarePseudoFMeasure();
		Map<DataSets, Measure> testSetup = new HashMap<DataSets, Measure>();
//		testSetup.put(DataSets.PERSON1_CSV, su);
//		testSetup.put(DataSets.PERSON1_CSV, sa);
//		testSetup.put(DataSets.PERSON2_CSV, su);
//		testSetup.put(DataSets.PERSON2_CSV, sa);
//		testSetup.put(DataSets.RESTAURANTS_CSV, su);
//		testSetup.put(DataSets.RESTAURANTS_CSV, sa);
		testSetup.put(DataSets.ABTBUY, sa);
		testSetup.put(DataSets.ABTBUY, su);
//		testSetup.put(DataSets.AMAZONGOOGLE, sa);
//		testSetup.put(DataSets.AMAZONGOOGLE, su);
		return testSetup;
	}
	
	
	public static void main(String args[]) {
//		Double[] allBetaValues = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
//				1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0};
		AbtBuyPseudoMeasureTester eval = null;

		Map<DataSets, Measure> setup = getEvaluationData();	
		for(Entry<DataSets, Measure> test : setup.entrySet()) {
			EvaluationData param = DataSetChooser.getData(test.getKey());
			try {
				if(eval == null) {
					eval = new AbtBuyPseudoMeasureTester();
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
}
