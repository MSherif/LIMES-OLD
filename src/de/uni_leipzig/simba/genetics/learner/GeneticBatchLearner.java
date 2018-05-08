package de.uni_leipzig.simba.genetics.learner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionProblem;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.util.PropMapper;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;

/**
 * Class to perform a learning of a link specification for to given knowledge bases 
 * using genetic algorithms. This is a batch learning approach. Needed parameters are
 * specified by the getParameters() method.
 * Start learning with an initial training data set using the learn() method. In case
 * the training data set is empty, the method will return a mapping of instances for the
 * user the evaluate. Once this is done, call the learn method again.
 * To retrieve the learned link specification call terminate();
 * TODO find generic way to use the class and property mapper. We assume this step is done by the calling method.
 * @author Klaus Lyko
 *
 */
public class GeneticBatchLearner implements LinkSpecificationLearner{
	static Logger logger = Logger.getLogger("LIMES");
	// parameters
	protected int popSize, gens, granularity, trainingDataSize;
	protected float mut, crossoverRate; protected boolean preserveFittest;
	protected PropertyMapping propMap;
	// holds data
//	protected Cache sC, tC;
	protected Oracle o;
	// to perform learning
	protected LinkSpecGeneticLearnerConfig config;
	protected ExpressionFitnessFunction fitness;
	protected KBInfo source, target;
	protected ConfigReader cR;
	protected GPProblem gpP;
	protected GPGenotype gp;
	// to keep track of learned metrics
	protected IGPProgram allBest; Metric metric;
	LinkedList<IGPProgram> bestOfCycles = new LinkedList<IGPProgram>();
	
	
	
	public void init(KBInfo source, KBInfo target,
			SupervisedLearnerParameters params) throws InvalidConfigurationException {
		// set parameters
		this.source = source;
		this.target = target;
		popSize = params.getPopulationSize();
		gens = params.getGenerations();
		mut = params.getMutationRate();
		crossoverRate = params.getCrossoverRate();

		preserveFittest = params.isPreserveFittestIndividual();
		propMap = params.getPropertyMapping();
		granularity = params.getGranularity();
		trainingDataSize = params.getTrainingDataSize();
		cR = params.getConfigReader();
		// setup
		setUp();
	}

	/**
	 * Describes parameters needed to perform a learning of a link specification sing this learner.
	 * @return Map with the following entries: 
	 * 	<table border="1">
	 * 	 <thead><tr>
	 * 		<th>Name</th>
	 * 		<th>Type</th>
	 * 		<th>Description</th>
	 *   </tr></thead>
	 *   <tbody>
	 *    <tr>	
	 *      <td>populationSize</td>
	 *   	<td>int</td>
	 *   	<td>Maximum number of individual per generation.</td>
	 *    </tr>
	 *    <tr>	
	 *      <td>generations</td>
	 *   	<td>int</td>
	 *   	<td>How many generations to evolve.</td>
	 *    </tr>
	 *    <tr>	
	 *      <td>mutationRate</td>
	 *   	<td>float</td>
	 *   	<td>Probability of an individual to mutate. Value between 0f and 1f.</td>
	 *    </tr>
	 *    <tr>	
	 *      <td>crossoverRate</td>
	 *   	<td>float</td>
	 *   	<td>Probability of an crossover operation to occur during evolution. Value between 0f and 1f.</td>
	 *    </tr>
	 *    <tr>	
	 *      <td>preserveFittest</td>
	 *   	<td>boolean</td>
	 *   	<td>Copy fittest individuals to the next generation?</td>
	 *    </tr>
	 *    <tr>	
	 *      <td>propertyMapping</td>
	 *   	<td>PropertyMapping</td>
	 *   	<td>Specify which properties to compare, thereby limiting execution time. If no property mapping is provided 
	 *   		we assume all properties to be String properties and map them according to their index.</td>
	 *    </tr>
	 *    <tr>
	 *    	<td>trainingDataSize</td>
	 *    	<td>int</td>
	 *    	<td>How many possible matches a user have to decide upon a question cycle.</td>
	 *    </tr>
	 *    <tr>
	 *    	<td>granularity</td>
	 *    	<td>integer</td>
	 *    	<td>OPTIONAL: Granularity for euclidean measures.</td>
	 *    </tr>
	 *    <tr>
	 *    	<td>config</td>
	 *    	<td>ConfigReader</td>
	 *    	<td>Instance of ConfigReader to retrieve initial metric expression. Used to get initial training data.</td>   
	 *    </tr>
	 *   </tbody>
	 *  </table>
	 */
	public static HashMap<String, Class> getParameters() {
		HashMap<String, Class> param = new HashMap<String, Class>();
		param.put("populationSize", int.class);
		param.put("generations", int.class);
		param.put("mutationRate", float.class);
		param.put("crossoverRate", float.class);
		param.put("preserveFittest", boolean.class);
		param.put("propertyMapping", PropertyMapping.class);
		param.put("trainingDataSize", int.class);
		param.put("granularity", int.class);
		param.put("config", ConfigReader.class);
		return param;
	}

	
	public Mapping learn(Mapping trainingData) {
		if(trainingData == null || trainingData.size()==0) {
			return getStartingData(fitness);
		}else {
			// only consider Mappings with sim>0 as true
			fillOracle(trainingData);
			fitness.trimKnowledgeBases(trainingData);
			fitness.setReferenceMapping(o.getMapping());
			
			try {
				gp = gpP.create();
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
			for(int gen = 0; gen < this.gens; gen++) {
				// evolve
				gp.evolve();
				gp.calcFitness();
			}
			determineMetric();
			return getMappingForOutput(trainingData, fitness);
		}
	}

	/**
	 * Method is called by learn() to getting additional links for the user
	 * to decide upon based on the so far learned metric.
	 * @param trainingData Original data used in the learning process before. Just to
	 *		 avoid asking a user about the same stuff twice.
	 * @return A Mapping of instances for the user to evaluated.
	 */
	protected Mapping getMappingForOutput(Mapping trainingData, ExpressionFitnessFunction fitness) {
		logger.info("Getting Mapping for output");
		if(metric == null || !metric.isValid())
			return new Mapping();
		else {
			logger.info("trying to get a full map");
			Mapping ret = new Mapping();
			Mapping all = fitness.getMapping(metric.getExpression(), metric.getThreshold(), true);
			for(Entry<String,HashMap<String, Double>> e1 : all.map.entrySet())
				for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
					if(ret.size()>=this.trainingDataSize)
						return ret;
					if(!trainingData.contains(e1.getKey(), e2.getKey()))
						ret.add(e1.getKey(), e2.getKey(), e2.getValue());
				}
			return ret;
		}		
	}

	
	public Metric terminate() {		
		for(IGPProgram p : bestOfCycles) {
			if(fitness.calculateRawFitness(p)<fitness.calculateRawFitness(allBest)) {
				allBest = p;
				metric = getMetric(p);
			}
		}
		if(!metric.isValid())
			System.out.println("WARNING false solution.");
		return metric;
	}
	
	/**
	 * Helper function to retrieve a learned metric expression. Whether we take the allTimeBest
	 * solution or iterate over the latest population.
	 */
	protected void determineMetric() {		
		allBest = gp.getAllTimeBest();
		try {
			metric = getMetric(allBest);
			if(!metric.isValid() || allBest.getFitnessValue()>=1d)
				getBestPossibleSolution();
			else {
				bestOfCycles.add(allBest);				
			}
			logger.info("Fittest Program is set to: "+metric+" with a fitness value of "+allBest.getFitnessValue()); 
		}catch(Exception e)  {
			getBestPossibleSolution();
		}		
	}
	/**
	 * Find the best solution by iterating over the current population.
	 */
	protected void getBestPossibleSolution() {
		logger.info("The fittest propgram is no vaild solution. So we look for annother one.");
		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();
		Metric best = null; double fit = Double.MAX_VALUE;
		IGPProgram bestProg = null;
		for(IGPProgram p : pop.getGPPrograms()) {
			if(p!= null && fit>p.getFitnessValue())
				try {
					fit = p.getFitnessValue();
					Metric mp = getMetric(p);
					if(mp.isValid()) {
						best = mp;
						bestProg = p;
					}					
				} catch(IllegalStateException e) {
					continue;
				}
		}
		if(best != null && bestProg != null) {
			allBest = bestProg;
			metric = best;
			bestOfCycles.add(bestProg);
			logger.info("Fittest Program is set to: "+metric+" with a fitness value of "+allBest.getFitnessValue());
		}
	}
	
	private Metric getMetric(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (Metric) pc.getNode(0).execute_object(pc, 0, args);
	}
	
	/**
	 * Return an initial mapping for a user to decide upon, if no initial
	 * training data was provided for the learn() method.
	 * @return A Mapping of URIs of source and target.
	 */
	protected Mapping getStartingData(ExpressionFitnessFunction fitness) {
		if(cR != null && cR.metricExpression != null && cR.acceptanceThreshold >= 0 &&  cR.acceptanceThreshold <= 1)
			return getTrainingDataFromInitialConfig(fitness);
		Cache sC;
		Cache tC;
		sC = fitness.getSourceCache();
		tC = fitness.getTargetCache();
		
		// if both knowledge bases are .csv files we can't call a Class Mapper.
	//	if(source.type.equalsIgnoreCase("csv") && target.type.equalsIgnoreCase("csv")) {
			Mapping m = new Mapping();
			logger.info("Get random initial training data.");
			sC.resetIterator(); tC.resetIterator();
			while(m.size()<Math.max(10, trainingDataSize)) {				
				Instance inst1 = sC.getNextInstance();
				Instance inst2 = tC.getNextInstance();
				if(inst1 != null && inst2 != null) {
					m.add(inst1.getUri(), inst2.getUri(), 0d);
				}
			}
		return m;
	//	}
	}
	/**
	 * We can use an initial config to retrieve training data.
	 * @return Mapping with 
	 */
	private Mapping getTrainingDataFromInitialConfig(ExpressionFitnessFunction fitness) {
		logger.info("Retrieving training data by initial config.");
		Mapping full = fitness.getMapping(cR.metricExpression, cR.acceptanceThreshold, true); // we may also use the the PPJoinPlusController instead.
		if(full.size() == 0) {
			logger.warn("Initial training data retrieved with "+cR.metricExpression +" >= "+ cR.acceptanceThreshold +" is empty!");
			return getRandomTrainingData();
		}
		Mapping answer = new Mapping();
		for(Entry<String, HashMap<String, Double>> e1: full.map.entrySet()) {
			for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
				if(answer.size() >= trainingDataSize)
					return answer;
				if(e2.getValue()<1) {
					// just consider the lesser sure ones - they're more informative
					answer.add(e1.getKey(), e2.getKey(), e2.getValue());					
				}
			}
		}
		if(answer.size() == 0) {
			for(Entry<String, HashMap<String, Double>> e1: full.map.entrySet()) {
				for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
					if(answer.size() >= trainingDataSize)
						return answer;
					if(e2.getValue()<=1) {
						// just consider the lesser sure ones - they're more informative
						answer.add(e1.getKey(), e2.getKey(), e2.getValue());					
					}
				}
			}
		}
		return answer;
	}

	private Mapping getRandomTrainingData() {
		Mapping answer = new Mapping();
		for(int i = 0; i<trainingDataSize; i++) {
			System.out.println(fitness);
			ArrayList<Instance> sInst = fitness.getSourceCache().getAllInstances();
			ArrayList<Instance> tInst = fitness.getTargetCache().getAllInstances();
			if(sInst.size()==0 || tInst.size()==0)
				return new Mapping();
			Random rand  = new Random();
			Instance i1 = sInst.get(rand.nextInt(sInst.size()));
			Instance i2 = tInst.get(rand.nextInt(tInst.size()));
			while(i1 == null)
				i1 = sInst.get(rand.nextInt(sInst.size()));
			while(i2 == null)
				i2 = tInst.get(rand.nextInt(tInst.size()));
			answer.add(i1.getUri(), i2.getUri(), 0.5d);
		}
		return answer;
	}

	/**
	 * Performs initial setUp steps.
	 * @throws InvalidConfigurationException
	 */
	protected void setUp() throws InvalidConfigurationException {
		// control property mapping
		if(propMap == null)
			propMap = new PropertyMapping();
		if(!propMap.wasSet()) {
			logger.warn("No Property Mapping set we use a fallback solution.");
			if(cR == null)
				propMap.setDefault(source, target);
			else {
				// or we find a way to use the class mapper.
				propMap.setDefault(source, target);				
			}
		}
		o = new SimpleOracle();
		o.loadData(new Mapping());
		// configure evolution
		config = new LinkSpecGeneticLearnerConfig(source, target, propMap);
		setUpFitness();
		config.setPopulationSize(popSize);
		config.setCrossoverProb(crossoverRate);
		config.setMutationProb(mut);
		config.setPreservFittestIndividual(preserveFittest);

		
		gpP = new ExpressionProblem(config);
		gp = gpP.create();
	}
	
	/**
	 * Learner specific fitness settings.
	 * @throws InvalidConfigurationException
	 */
	protected void setUpFitness() throws InvalidConfigurationException {
		Configuration.reset();
		fitness = ExpressionFitnessFunction.getInstance(config, o,
				"f-score", trainingDataSize);
		fitness.useFullCaches(true);
		fitness.useFScore();		
		config.setFitnessFunction(fitness);
	}
	
	/**
	 * We assume we get a Mapping with matches and non-matches. We assume matches have similarity
	 * values greater then 0, whereas, non-matches have a similarity of 0.
	 * As the Oracle as implemented in Limes assumes everything inside it's mapping a match
	 * we have to trim the training data, to those instance with similarity values greater then 0.
	 * @param trainingData Mapping possible holding non-machtes, aka matches with a similarity value of 0.
	 */
	private void fillOracle(Mapping trainingData) {
		Mapping cachedMatches = new Mapping();
		for(Entry<String, HashMap<String, Double>> e1: trainingData.map.entrySet()) {
			for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
				if(e2.getValue()>0)
					cachedMatches.add(e1.getKey(), e2.getKey(), 1d);
			}
		}
		o = new SimpleOracle();
		o.loadData(cachedMatches);
	}
		
		
	
	
/*	public static void main(String args[]) {
		String configFile = "Examples/GeneticEval/PublicationData.xml";
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile);
		Oracle o = OracleFactory.getOracle("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv", "csv", "simple");
		//Mapping ref = ExampleOracleTrimmer.trimExamplesRandomly(o.getMapping(), 50);	
	
		PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);	
	//	System.out.println(propMap.getStringPropMapping());
		
		LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.BATCH_LEARNER);
//		GeneticBatchLearner learner = new GeneticBatchLearner();
//		HashMap<String, Object> param = new HashMap<String, Object>();
//		param.put("populationSize", 20);
//		param.put("generations", 100);
//		param.put("mutationRate", 0.5f);
//		param.put("preserveFittest", true);
//		param.put("propertyMapping", propMap);
//		param.put("trainingDataSize", 50);
//		param.put("granularity", 2);
//		param.put("config", cR);
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(20);
	      params.setGenerations(100);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(false);
	      params.setTrainingDataSize(50);
	      params.setGranularity(2);
		Mapping answer; Metric answerMetric;
		try {
			learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			answer = learner.learn(new Mapping());
			Mapping oracleAnswer = new Mapping();
			
			for(Entry<String, HashMap<String, Double>> e1: answer.map.entrySet()) {
				for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
					if(o.ask(e1.getKey(), e2.getKey())) {
						oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
					}else {
						oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
					}
				}
			}
			System.out.println(oracleAnswer);
			for(int cycle=0; cycle<10; cycle++) {
				System.out.println("");
				learner.learn(oracleAnswer);
				logger.info("Learned Cycle "+cycle+" now terminating...");
				answerMetric = learner.terminate();
				PRFComputer prfC = new PRFComputer();
				Mapping instanceMap = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
				Mapping oracleMap = o.getMapping();
				logger.info("Computing Mapping of instanceMap:"+instanceMap.size()+" and oracleMap"+oracleMap.size());
				double fS=prfC.computePrecision(oracleMap, instanceMap);
				System.out.println("Cycle "+cycle+"  -  "+answerMetric);
				System.out.println("Cycle "+cycle+"  -  F-Score = "+fS);
			}
	}
*/
	public static void main(String args[]) {
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		String paramConfigFile = args[0];
    	String paramOracleFile = args[1];
    	String paramDataFile = args[2];
    	int paramPopulationSize = Integer.parseInt(args[3]);
    	int paramGenerations = Integer.parseInt(args[4]);
    	
		String configFile = paramConfigFile;
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile);
		Oracle o = OracleFactory.getOracle(paramOracleFile, "csv", "simple");
		PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);	
		LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.BATCH_LEARNER);
        int size = 20;

		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(paramPopulationSize);
	      params.setGenerations(paramGenerations);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(true);
	      params.setTrainingDataSize(size);
	      params.setGranularity(2);
	      
		Mapping answer; Metric answerMetric;
		try {
			learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			answer = learner.learn(new Mapping());
			Mapping oracleAnswer = new Mapping();
			
			for(Entry<String, HashMap<String, Double>> e1: answer.map.entrySet()) {
				for(Entry<String, Double> e2 : e1.getValue().entrySet()) {
					if(o.ask(e1.getKey(), e2.getKey())) {
						oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
					}else {
						oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
					}
				}
			}
			System.out.println(oracleAnswer);
			Mapping fin=null;
			for(int cycle=0; cycle<10; cycle++) {
				System.out.println("");
				learner.learn(oracleAnswer);
				logger.info("Learned Cycle "+cycle+" now terminating...");
				answerMetric = learner.terminate();
				PRFCalculator prfC = new PRFCalculator();
				Mapping instanceMap = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
				Mapping oracleMap = o.getMapping();
				logger.info("Computing Mapping of instanceMap:"+instanceMap.size()+" and oracleMap"+oracleMap.size());
				double fS=prfC.precision(oracleMap, instanceMap);
				System.out.println("Cycle "+cycle+"  -  "+answerMetric);
				System.out.println("Cycle "+cycle+"  -  F-Score = "+fS);
				fin=instanceMap;
			}
			System.out.println("The resulted Mapping based on learned metric and threshold");
			System.out.println(fin);
			String x =fin.toString();
	    	List<String> data = new ArrayList<String>();
	    	data.add(x);
	    	//BEio.WriterFile.writeToFile(data, paramDataFile/*"/home/mofeed/Desktop/testfile"*/);
	    	Date date2 = new Date();
			System.out.println(dateFormat.format(date));
			System.out.println(dateFormat.format(date2));

	}

	public ExpressionFitnessFunction getFitnessFunction() {
		return fitness;
	}
	
	public void setCaches(Cache sC, Cache tC) {
		fitness.setCaches(sC, tC);
	}

	@Override
	public void init(KBInfo source, KBInfo target,
			SupervisedLearnerParameters parameters, Cache sourceCache,
			Cache targetCache) throws InvalidConfigurationException {
		init(source, target, parameters);
		setCaches(sourceCache, targetCache);		
	}
}
