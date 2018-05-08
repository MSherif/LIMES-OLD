package de.uni_leipzig.simba.genetics.evaluation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.GPProgram;
import org.jgap.gp.impl.ProgramChromosome;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;

import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.util.Clock;

// COMMENT: Had to comment out line 227 due to LIMES not compilig with that line :(

public class Baseliner {

    Logger logger = Logger.getLogger("LIMES");
    String configFile, verificationFile;
    public static int pop = 20;
    public static int gens = 100;
    float mutCross = 0.6f;
    float reprod = 0.7f;
    Clock clock;
    LinkSpecGeneticLearnerConfig config;
    ExpressionFitnessFunction fF;
    Cache sC, tC;
    List<EvaluationPseudoMemory> memList;
    ConfigReader cR;
    Mapping reference;
    IGPProgram bestEver;
    double bestEverFitness = Double.MAX_VALUE;

    public static void main(String args[]) {
        if (args.length == 2) {
            DataSets d = DataSets.DBPLINKEDMDB;
            int gen = Integer.parseInt(args[0]);
            int pop = Integer.parseInt(args[1]);
           
            try {
            	Baseliner.gens = gen;
            	Baseliner.pop = pop;
                EvaluationData params = DataSetChooser.getData(d);
                params.setEvaluationResultFileName("BaseLine_" + pop + "pop_" + gen + "gens_" + d + ".csv");
                Baseliner bL = new Baseliner();
                bL.run(params);
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            DataSets data[] = {DataSets.DBPLINKEDMDB, DataSets.PERSON1, DataSets.RESTAURANTS};
            //	DataSets.ABTBUY, DataSets.DBLPACM};
            //	DataSets.AMAZONGOOGLE, DataSets.DBLPSCHOLAR,
            //	DataSets.DBPLINKEDMDB, DataSets.DRUGS};
            for (DataSets d : data) {
                Baseliner liner = new Baseliner();
                try {
                    EvaluationData params = DataSetChooser.getData(d);
                    params.setEvaluationResultFileName("BaseLine_" + pop + "pop_" + gens + "gens_" + d + ".csv");
                    liner.run(params);
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run(EvaluationData params) throws InvalidConfigurationException {
    	memList = new LinkedList<EvaluationPseudoMemory>();
        clock = new Clock();
        cR = params.getConfigReader();
        PropertyMapping propMap = params.getPropertyMapping();
        sC = params.getSourceCache();
        tC = params.getTargetCache();
        this.reference = params.getReferenceMapping();
        config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, propMap);
        config.setCrossoverProb(mutCross);
        config.setMutationProb(mutCross);
        config.setReproductionProb(reprod);
        config.setPopulationSize(pop);

        GPProblem gpP = new ExpressionProblem(config);
        fF = ExpressionFitnessFunction.getInstance(config, params.getReferenceMapping(), "f-score", 0);
        fF.useFullCaches(true);
        config.setFitnessFunction(fF);

        GPGenotype gp = gpP.create();

        // evolve
        for (int i = 0; i < gens; i++) {
            gp.evolve();
            processGen(i, gp);
        }
        // finalize
        finishRun(params.getEvauationResultFolder(), params.getEvaluationResultFileName());
        // destroy objects and references.
        destroy();
    }

    /**
     *
     */
    private void finishRun(String folder, String fileName) {
        for (EvaluationPseudoMemory mem : memList) {
            mem = evaluateInstance(mem);
        }
        Collections.sort(memList);
        writeFile(folder, fileName);
    }

    /**
     * Compute precision, recall, fMeasure
     *
     * @param mem
     * @return
     */
    private EvaluationPseudoMemory evaluateInstance(EvaluationPseudoMemory mem) {
        Mapping map = fF.getMapping(mem.metric.getExpression(), mem.metric.getThreshold(), true);
        mem.precision = PRFCalculator.precision(map, reference);
        mem.recall = PRFCalculator.recall(map, reference);
        mem.fmeasue = PRFCalculator.fScore(map, reference);
        return mem;
    }

    /**
     * Handle actions taken for generation nr.
     *
     * @param nr
     * @param gp
     */
    private void processGen(int nr, GPGenotype gp) {
//		gp.calcFitness();
//		GPProgram fittest = (GPProgram) gp.getFittestProgram();
        GPProgram fittest = (GPProgram) determinFittest(gp);
        double avgFitness = 0f;
        avgFitness = gp.getTotalFitness();
        gp.outputSolution(gp.getAllTimeBest());

        logger.info("Processing generation " + nr + " avergeFitness " + avgFitness / pop);
        if (fittest != null && getMetric(fittest) != null) {
        	double fit =  fittest.calcFitnessValue();
            logger.info("best fitness:" + fit + " " + getMetric(fittest));
            EvaluationPseudoMemory mem = new EvaluationPseudoMemory(1, nr, getMetric(fittest), 
            		fit, 
            		1d-fit,
            		clock.durationSinceClick());
            memList.add(mem);
        } else {
            logger.info("Got no valid fittest program.");
        }
    }

    private void writeFile(String folder, String fileName) {
        EvalFileLogger fileLogger = EvalFileLogger.getInstance();
        fileLogger.createFile(folder, fileName, true);
        fileLogger.writeHead();
        for (EvaluationPseudoMemory mem : memList) {
            writeInstance(fileLogger, mem);
        }
    }

    private void writeInstance(EvalFileLogger fileLogger, EvaluationPseudoMemory best) {
        fileLogger.writeWithDuration(best.generation, 0d, best.metric.toString(), best.fitness,
                best.fmeasue, best.recall, best.precision, best.runTime);
    }

    /**
     * get Metric to program*
     */
    private Metric getMetric(GPProgram prog) {
        ProgramChromosome pc = prog.getChromosome(0);
        Object args[] = {};
        try {
            Metric metric = (Metric) pc.getNode(0).execute_object(pc, 0, args);
            return metric;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private IGPProgram determinFittest(GPGenotype gp) {
        GPPopulation pop = gp.getGPPopulation();
        pop.sortByFitness();

        IGPProgram bests[] = {
            gp.getFittestProgramComputed(),
            pop.determineFittestProgram(),
            gp.getAllTimeBest(),
            pop.getGPProgram(0),
            pop.getGPProgram(pop.getPopSize() - 1)
        };
        IGPProgram bestHere = null;
        double fittest = Double.MAX_VALUE;
        for (IGPProgram p : bests) {
            if (p != null) {
                double fitM = fF.calculateRawFitness(p);
                if (fitM < fittest) {
                    fittest = fitM;
                    bestHere = p;
                }
            }
        }
        if (fittest < bestEverFitness) {
            bestEverFitness = fittest;
          // bestEver = (IGPProgram) bestHere.clone();
            gp.getGPPopulation().addFittestProgram(bestEver);
            return bestHere;
        }
        return bestEver;
    }

    private void destroy() {
        this.sC = null;
        this.tC = null;
        fF.destroy();
        Configuration.reset();
        config = null;
        memList.clear();
        fF = null;

    }
}
