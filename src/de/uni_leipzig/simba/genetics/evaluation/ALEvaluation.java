package de.uni_leipzig.simba.genetics.evaluation;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.GPProgram;
import org.jgap.gp.impl.ProgramChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ALDecider;
import de.uni_leipzig.simba.genetics.core.ALFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoEvalFileLogger;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.util.Clock;

public class ALEvaluation {

    public static Logger logger = LoggerFactory.getLogger("LIMES");
    public static final int RUNS = 5;
    public static final int oracleQuestions = 10;
    public DataSets currentDataSet;
    EvaluationData params;
    public ALFitnessFunction fitness;
    Oracle o;
    ConfigReader cR;
    /**
     * List for the detailed file logs
     */
    List<EvaluationPseudoMemory> perRunMemory = new LinkedList<EvaluationPseudoMemory>();
    /**
     * List for the combined results
     */
    List<EvaluationPseudoMemory> perDatasetMemory = new LinkedList<EvaluationPseudoMemory>();
    private float mutationCrossRate = 0.6f;
    private float reproduction = 0.7f;
    /**
     * Restrict the number of full maps computed to get controversy instances?
     */
    public boolean restrictGetAllMaps = false;
    /**
     * If a restrictGetAllMaps is set set the maximum of maps to be considered
     */
    public int maxMaps = 20;

    /**
     *
     * @param data The Dataset.
     * @param popSize Population size.
     * @param gens Number of generations run before asking for more data
     * @param numberOfExamples Number instances to be labeled by the user each
     * inquery.
     * @param inquiries How many inquiries.
     * @throws InvalidConfigurationException
     */
    public void run(DataSets data, int popSize, int gens, int numberOfExamples, int inquiries) throws InvalidConfigurationException {
        logger.info("Running dataset " + data);
        currentDataSet = data;
        params = DataSetChooser.getData(data);
//      String name = ((String) params.get("evalfilename")).replaceAll("Pseudo_eval_", "AL_");
        String name = params.getEvaluationResultFileName().replaceAll("Pseudo_eval_", "AL_");
        params.setEvaluationResultFileName(name);
        //fitness = ALFitnessFunction.

        cR = params.getConfigReader();
        o = new SimpleOracle(params.getReferenceMapping());
        for (int run = 1; run <= RUNS; run++) {
            LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, params.getPropertyMapping());
            config.setSelectFromPrevGen(reproduction);
            config.setCrossoverProb(mutationCrossRate);
            config.setMutationProb(mutationCrossRate);
            config.setReproductionProb(reproduction);
            config.setPopulationSize(popSize);
            config.setPreservFittestIndividual(true);
            config.setAlwaysCaculateFitness(true);
            config.sC = params.getSourceCache();
            config.tC = params.getTargetCache();
            logger.info("Starting run " + run);
            fitness = new ALFitnessFunction(config, o, "f-score", numberOfExamples);
            fitness.useFScore();
            Mapping start = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), numberOfExamples);
            System.out.println("Start data " + start);
            fitness.setReferenceMapping(start);
            fitness.trimKnowledgeBases(start);
            config.setFitnessFunction(fitness);

            GPProblem gpP = new ExpressionProblem(config);
            GPGenotype gp = gpP.create();
            Clock clock = new Clock();
            ALDecider aLD = new ALDecider();

            for (int inquery = 1; inquery <= inquiries; inquery++) {
                logger.info("Inquery nr " + inquery + " on dataset" + data + " run " + run + "");
                for (int gen = 0; gen < gens; gen++) {
                    gp.evolve();
                    gp.calcFitness();
                }
                examineGen(run, inquery * gens, gp, clock);
                // query Oracle
                if (inquery != inquiries) {
                    List<Mapping> mapsOfPop = getMaps(gp.getGPPopulation());
                    if (mapsOfPop.size() > 0) {
                        List<Triple> toAsk = aLD.getControversyCandidates(mapsOfPop, numberOfExamples);
                        logger.info("Asking about " + toAsk.size() + " instances.");
                        if (toAsk.size() == 0) {
                            aLD.maxCount += 100;
                        }
                        Mapping oracleAnswers = new Mapping();
                        for (Triple t : toAsk) {
                            if (o.ask(t.getSourceUri(), t.getTargetUri())) {
                                oracleAnswers.add(t.getSourceUri(), t.getTargetUri(), 1d);
                            }
                        }
                        fitness.addToReference(oracleAnswers);
                        fitness.fillCachesIncrementally(toAsk);
                    }// if we got anything to ask about
                }
            }// per inquery
            finishRun();
            fitness.destroy();
            LinkSpecGeneticLearnerConfig.reset();
        }// per run		
        finishDataset(RUNS, popSize, gens, params);
    }// end of run()

    /**
     * Examines this generation and memorizes best program so far.
     *
     * @param run
     * @param gen
     * @param gp
     * @param clock
     */
    private void examineGen(int run, int gen, GPGenotype gp, Clock clock) {
        IGPProgram pBest;// = (GPProgram) gp.getFittestProgram();
        GPPopulation pop = gp.getGPPopulation();
        pop.sortByFitness();
        double fittest = Double.MAX_VALUE;
        pBest = pop.determineFittestProgram();
        fittest = fitness.calculateRawFitness(pBest);
        // to do recalculate older programs
        for (GPProgram p : (GPProgram[]) pop.getGPPrograms()) {
            double actFit = fitness.calculateRawFitness(p);
            if (fittest > actFit && getMetric(p).getExpression().indexOf("falseProp") == -1) {
                pBest = p;
                fittest = actFit;
                System.out.println("Setting to fitter program then JGAP");
            }
        }
        boolean getBetterOld = false;
        for (EvaluationPseudoMemory mem : perRunMemory) {
            // re use older ones
            double oldFit = fitness.calculateRawFitness(mem.program);
            if (oldFit < fittest) {
                fittest = oldFit;
                pBest = mem.program;
                getBetterOld = true;
            }
        }
        if (getBetterOld) {
            System.out.println("Reusing older program...");
//			pop.addFittestProgram((IGPProgram) pBest.clone());
        }
        EvaluationPseudoMemory mem = new EvaluationPseudoMemory(run, gen, 
        		getMetric(pBest), 
        		fittest, 
        		1d-fittest,
        		clock.totalDuration());
        mem.knownInstances = fitness.getReferenceMapping().size();
        mem.program = pBest;
        perRunMemory.add(mem);
    }

    /**
     * Method to write the detailed log of this dataset log
     */
    private void finishRun() {
        logger.info("Fininshing run...");
        // get full Maps and calculate prf
        for (EvaluationPseudoMemory mem : perRunMemory) {
            Mapping fullMap = fitness.getMapping(mem.metric.getExpression(), mem.metric.getThreshold(), true);
            mem.fmeasue = PRFCalculator.fScore(fullMap, o.getMapping());
            mem.recall = PRFCalculator.recall(fullMap, o.getMapping());
            mem.precision = PRFCalculator.precision(fullMap, o.getMapping());
        }
        // add data to the complete memory
        this.perDatasetMemory.addAll(perRunMemory);// clear per run Memory
        perRunMemory.clear();
    }

    @SuppressWarnings("unchecked")
    private void finishDataset(int maxRuns, int popSize, int gens, EvaluationData params2) {
        // sort and calc the 
        Collections.sort(perDatasetMemory);
//        PseudoEvalFileLogger fileLog = new PseudoEvalFileLogger((String) params2.get(MapKey.EVALUATION_RESULTS_FOLDER), (String) params2.get(MapKey.EVALUATION_FILENAME));
        PseudoEvalFileLogger fileLog = new PseudoEvalFileLogger(params2.getEvauationResultFolder(), params2.getEvaluationResultFileName());
        fileLog.nameExtansion = "AL_pop=" + popSize + "_gens=" + gens;
        fileLog.log(perDatasetMemory, maxRuns, gens, params2);
        perDatasetMemory.clear();
    }

    public List<Mapping> getMaps(GPPopulation pop) {
        List<Mapping> allMaps = new LinkedList<Mapping>();
        Set<String> expressions = new HashSet<String>();
        List<IGPProgram> considered = new LinkedList<IGPProgram>();
        int max = pop.size();
        if (restrictGetAllMaps) {
//			pop.sortByFitness();
            pop.sort(new Comparator<IGPProgram>() {

//                @Override
                public int compare(IGPProgram o1, IGPProgram o2) {
                    if (o1.getFitnessValue() < o2.getFitnessValue()) {
                        return -1;
                    }
                    if (o1.getFitnessValue() > o2.getFitnessValue()) {
                        return 1;
                    }
                    return 0;
                }
            });
            max = Math.min(maxMaps, pop.size());
        }
        for (int i = 0; i < max; i++) {
            considered.add(pop.getGPProgram(i));
        }
        for (int i = 0; i < Math.min(pop.size(), 20); i++) {
            for (IGPProgram gProg : considered) {
                String expr = "falseProp";
                double d = 0.9d;
                try {
                    Metric metric = getMetric(gProg);
                    expr = metric.getExpression();
                    d = metric.getThreshold();
                } catch (IllegalStateException e) {
                    continue;
                }
                if (expr.indexOf("falseProp") > -1) {
                    continue;
                }
                if (expressions.contains(expr + d)) {
                    maxMaps++;
                    continue;
                } else {
                    expressions.add(expr + d);
                }
                try {
                    allMaps.add(fitness.getMapping(expr, d, true));
                } catch (OutOfMemoryError bounded) {
                    logger.info("Encountered Memory error...");
                    continue;
                }
            }
        }
        return allMaps;
    }

    private Metric getMetric(IGPProgram p) {
        Object[] args = {};
        ProgramChromosome pc = p.getChromosome(0);
        return (Metric) pc.getNode(0).execute_object(pc, 0, args);
    }

    /**
     * Restrict the number of individuals executed (full mappings to be
     * calculated) to generate new data for a user to label to the given value;
     *
     * @param maximum The maximal number of (different) individuals in the
     * population to be considered
     */
    public void restrictNumberOfMaps(int maximum) {
        if (maximum > 0) {
            restrictGetAllMaps = true;
            maxMaps = maximum;
        }
    }

    public static void main(String args[]) {
    	DataSets datas[] = {
				DataSets.DBLPACM,
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
////				MTC
//				DataSets.DBPLINKEDMDB,
//				DataSets.DBLPSCHOLAR,
				
		}; 
    	DataSets toRun[] = datas;
    	if (args.length == 1) {
            int pos = Integer.parseInt(args[0]);	
            toRun = new DataSets[]{
                    datas[pos]}; 
        }
            
        try {
            // for every dataset
            for (DataSets data : toRun) {
                // pop: 20 vs. 100
                for (int pop : new int[]{20}) {
                    ALEvaluation eval = new ALEvaluation();
                    eval.run(data, pop, 10, 10, 10);
                }
            }
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
