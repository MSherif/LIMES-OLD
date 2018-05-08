package de.uni_leipzig.simba.genetics.learner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;

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
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.KBInfo;

public class UnsupervisedLearner implements UnsupervisedLinkSpecificationLearner {
    static Logger logger = Logger.getLogger("LIMES");
    KBInfo source;
    KBInfo target;
    Cache sC, tC;
    UnSupervisedLearnerParameters params;
    protected LinkSpecGeneticLearnerConfig config;
    protected PseudoFMeasureFitnessFunction fitness;
    protected GPProblem gpP;
    protected GPGenotype gp;
    // to keep track of learned metrics
    protected IGPProgram allBest;
    Metric metric;
    protected List<String> specifications = new ArrayList<String>();

    @Override
    public void init(KBInfo source, KBInfo target, UnSupervisedLearnerParameters parameters)
	    throws InvalidConfigurationException {
	this.source = source;
	this.target = target;
	this.params = parameters;
	setUp();
    }

    @Override
    public Mapping learn() {
	System.out.println("Start learning");
	for (int gen = 1; gen < params.getGenerations(); gen++) {
	    gp.evolve();
	    IGPProgram currentBest = determinFittest(gp);
	    Metric currentBestMetric = getMetric(currentBest);
	    logger.info(currentBestMetric.toString());
	    //TODO: if you want to save only the best LS of each generation,
	    //then comment the following line
	    specifications.add(currentBestMetric.toString());

	}

	allBest = determinFittest(gp);
	return fitness.calculateMapping(allBest);
    }

    @Override
    public Metric terminate() {
	specifications.add(getMetric(allBest).toString());
	writeSpecifications("VILLAGES");
	return getMetric(allBest);
    }

    @Override
    public PseudoFMeasureFitnessFunction getFitnessFunction() {
	return fitness;
    }

    protected void writeSpecifications(String DatasetName) {
	String BaseDirectory = "datasets/" + DatasetName + "/specifications/";
	File dirName = new File(BaseDirectory);
	if (!dirName.isDirectory()) {
	    try {
		dirName.mkdir();
	    } catch (SecurityException se) {
	    }
	}
	File f = new File(BaseDirectory + "specifications.txt");
	FileWriter writer = null;
	try {
	    if (f.exists()) {
		writer = new FileWriter(f, true);
	    } else {
		writer = new FileWriter(f);
	    }
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	for (String spec : specifications) {
	    try {
		writer.append(spec);
		writer.append("\n");
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	}
	try {
	    writer.flush();
	    writer.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

    /* setUp */
    /**
     * Performs initial setUp steps.
     * 
     * @throws InvalidConfigurationException
     */
    protected void setUp() throws InvalidConfigurationException {
	// control property mapping
	logger.info(params.getPropertyMapping());
	if (params.getPropertyMapping() == null)
	    params.setPropertyMapping(new PropertyMapping());
	if (!params.getPropertyMapping().wasSet()) {
	    logger.warn("No Property Mapping set we use a fallback solution.");
	    if (params.getConfigReader() == null)
		params.getPropertyMapping().setDefault(source, target);
	    else {
		// or we find a way to use the class mapper.
		params.getPropertyMapping().setDefault(source, target);
	    }
	}

	config = new LinkSpecGeneticLearnerConfig(source, target, params.getPropertyMapping());
	config.setPopulationSize(params.getPopulationSize());
	config.setCrossoverProb(params.getCrossoverRate());
	config.setMutationProb(params.getMutationRate());
	config.setPreservFittestIndividual(params.isPreserveFittestIndividual());
	config.setReproductionProb(params.getReproductionRate());

	sC = HybridCache.getData(source);

	tC = HybridCache.getData(target);

	fitness = PseudoFMeasureFitnessFunction.getInstance(config, params.getPseudoFMeasure(), sC, tC);
	// fitness = new PseudoFMeasureFitnessFunction(config, measure, sC, tC);
	fitness.setBeta(params.getPFMBetaValue());
	config.setFitnessFunction(fitness);
	gpP = new ExpressionProblem(config);
	gp = gpP.create();
    }
    //
    // /**
    // * does whatever should be done each generation.
    // * @param gp
    // * @param gen
    // */
    // public IGPProgram processGeneration(GPGenotype gp, int gen) {
    // IGPProgram pBest = determinFittest(gp);
    // return pBest;
    // }

    private IGPProgram determinFittest(GPGenotype gp) {

	GPPopulation pop = gp.getGPPopulation();
	pop.sortByFitness();

	IGPProgram bests[] = { gp.getFittestProgramComputed(), pop.determineFittestProgram(),
		// gp.getAllTimeBest(),
		pop.getGPProgram(0), };
	IGPProgram bestHere = null;
	double fittest = Double.MAX_VALUE;

	for (IGPProgram p : bests) {
	    if (p != null) {
		double fitM = fitness.calculateRawFitness(p);
		if (fitM < fittest) {
		    fittest = fitM;
		    bestHere = p;
		}
	    }
	}
	/* consider population if neccessary */
	if (bestHere == null) {
	    logger.debug("Determing best program failed, consider the whole population");
	    System.err.println("Determing best program failed, consider the whole population");
	    for (IGPProgram p : pop.getGPPrograms()) {
		if (p != null) {
		    double fitM = fitness.calculateRawFitness(p);
		    if (fitM < fittest) {
			fittest = fitM;
			bestHere = p;
		    }
		}
	    }
	}

	int i = 0;
	for (IGPProgram p : pop.getGPPrograms()) {
	    if (p != null) {
		double fitM = fitness.calculateRawFitness(p);
		Metric metric = getMetric(p);
		// logger.info(++i+". (fit="+fitM +") Program: "+metric);
	    }
	}

	return bestHere;
    }

    private Metric getMetric(IGPProgram p) {
	Object[] args = {};
	ProgramChromosome pc = p.getChromosome(0);
	return (Metric) pc.getNode(0).execute_object(pc, 0, args);
    }

    public static void main(String args[]) {
	// HashMap<MapKey, Object> data =
	// DataSetChooser.getData(DataSets.PERSON1_CSV);
	for (int i = 0; i < 5; i++) {
	    EvaluationData data = DataSetChooser.getData(DataSets.VILLAGES);
	    UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(data.getConfigReader(),
		    data.getPropertyMapping());
	    params.setGenerations(20);
	    params.setPopulationSize(20);
	    // params.setPreserveFittestIndividual(false);
	    UnsupervisedLinkSpecificationLearner learner = new UnsupervisedLearner();
	    try {
		learner.init(params.getConfigReader().sourceInfo, params.getConfigReader().targetInfo, params);
	    } catch (InvalidConfigurationException e) {
		e.printStackTrace();
	    }
	    Mapping m = learner.learn();
	    Metric result = learner.terminate();
	    System.out.println("Finished learning. Best Mapping has size :" + m.size());
	    System.out.println("Best Metric: " + result);
	}

    }
}