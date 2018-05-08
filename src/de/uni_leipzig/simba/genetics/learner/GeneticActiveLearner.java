package de.uni_leipzig.simba.genetics.learner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.ProgramChromosome;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.genetics.core.ALDecider;
import de.uni_leipzig.simba.genetics.core.ALFitnessFunction;
import de.uni_leipzig.simba.genetics.core.ExpressionFitnessFunction;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.PropMapper;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.oracle.mappingreader.CSVMappingReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.specification.LinkSpec;

/**
 * Class to perform a learning of a link specification for two given knowledge
 * bases using genetic programming and an active learning approach. Needed
 * parameters are specified by the getParameters() method. Start learning with
 * an initial training data set using the learn() method. In case the training
 * data set is empty, the method will return a mapping of instances for the user
 * the evaluate, either by executing an initial link specification or by a
 * fallback solution. Once this is done, call the learn method again, to start a
 * learning. To retrieve the learned link specification call terminate();
 * 
* For the active learning approach the initial training data size is relatively
 * small. Be aware that the user is supposed to evaluate n cycles of returned
 * Mappings by the learn method, so the learning can decide upon more and
 * especially most informative data.
 * 
* TODO find generic way to use the class and property mapper. We assume this
 * step is done in advance.
 *
 * @author Klaus Lyko
 * 
*/
public class GeneticActiveLearner extends GeneticBatchLearner implements LinkSpecificationLearner {
    // shows if we need to create an initial population

    boolean started = false;
    ALFitnessFunction fitness;
    ALDecider alDecider = new ALDecider();
    // to keep track of all instances presented to user
    Mapping trainingData = new Mapping();
    GPPopulation pop;

    @Override
    protected void setUpFitness() throws InvalidConfigurationException {
        logger.info("Setting up fitness for active learning approach.");
        Configuration.reset();
        fitness = ALFitnessFunction.getInstance(config, o,
                "f-score", trainingDataSize);
        //	fitness.useFullCaches();
        fitness.useFScore();
        config.setFitnessFunction(fitness);
    }

    @Override
    public Mapping learn(Mapping trainingData) {
        if (!started && (trainingData == null || trainingData.size() == 0)) {
            logger.info("Get start data...");
            return getStartingData(fitness);
        } else {
            logger.info("filling with trainingData " + trainingData.size());
            // handle the training data
            fillOracle(trainingData);

            if (!started || gp == null) {
                try {
                    logger.info("Creating initial population");
                    gp = gpP.create();
                    started = true;
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("Starting additional learning cycle.");
            }

            for (int gen = 0; gen < this.gens; gen++) {
                // evolve
                gp.evolve();
                
                        pop = gp.getGPPopulation();
                        for(int i=0; i<pop.getPopSize(); i++)
//                            System.out.println(">>" + Metric.getMetric(pop.getGPProgram(i)));
                        
                gp.calcFitness();
                pop = gp.getGPPopulation();
                if (gp.getFittestProgramComputed().getFitnessValue() == 0d) {
                    logger.info("Stopping evolution because we already discovered the best possible program");
                    break;
                }
            }
            System.out.println("Evolution finished ... ");
            determineMetric();
            return getMappingForOutput(trainingData, fitness);
        }
    }

    @Override
    protected Mapping getMappingForOutput(Mapping trainingData, ExpressionFitnessFunction fitness) {
        // first get all Mappings for the current population
        logger.info("Getting mappings for output");
        //	GPPopulation pop = this.gp.getGPPopulation();
        pop.sortByFitness();
        HashSet<Metric> metrics = new HashSet<Metric>();
        List<Mapping> candidateMaps = new LinkedList<Mapping>();
        // and add the best
        if (metric != null && metric.isValid()) {
            metrics.add(metric);
        }
        for (IGPProgram p : pop.getGPPrograms()) {
            Metric m = getMetric(p);
            if (m != null && m.isValid() && !metrics.contains(m)) {
                //logger.info("Adding metric "+m);
                metrics.add(m);
            }
        }
        // fallback solution if we have too less candidates
        if (metrics.size() <= 1) {
            return super.getMappingForOutput(trainingData, fitness);
        }

        // get mappings for all distinct metrics
        logger.info("Getting " + metrics.size() + " full mappings to determine controversy matches...");
        for (Metric m : metrics) {
            if (m.isValid() && m.getExpression().indexOf("falseProp") == -1) {
                candidateMaps.add(fitness.getMapping(m.getExpression(), m.getThreshold(), true));
            }
        }
        // get most controversy matches
        logger.info("Getting " + this.trainingDataSize + " controversy match candidates from " + candidateMaps.size() + " maps...");;
        List<Triple> controversyMatches = alDecider.getControversyCandidates(candidateMaps, trainingDataSize);
        // construct answer
        Mapping answer = new Mapping();
        for (Triple t : controversyMatches) {
            answer.add(t.getSourceUri(), t.getTargetUri(), t.getSimilarity());
        }
        return answer;
    }

    protected void determineMetric() {
        pop.sortByFitness();
        Metric best = null;
        double fit = Double.MAX_VALUE;
        IGPProgram bestProg = null;
        for (IGPProgram p : pop.getGPPrograms()) {
            if (p != null && fit > p.getFitnessValue()) {
                try {
                    fit = p.getFitnessValue();
                    Metric mp = getMetric(p);
                    if (mp.isValid()) {
                        best = mp;
                        bestProg = p;
                    }
                } catch (IllegalStateException e) {
                    continue;
                }
            }
        }
        if (best != null && bestProg != null) {
            allBest = bestProg;
            metric = best;
            bestOfCycles.add(bestProg);
            logger.info("Fittest Program is set to: " + metric + " with a fitness value of " + allBest.getFitnessValue());
        }
    }

    /**
     * To get the Metric of a program.
     *
     * @param p
     * @return
     */
    protected Metric getMetric(IGPProgram p) {
        try {
            Object[] args = {};
            ProgramChromosome pc = p.getChromosome(0);
            metric = (Metric) pc.getNode(0).execute_object(pc, 0, args);
            if (metric.isValid()) {
                return metric;
            } else {
                //	logger.info("Program is not valid.");
                return metric;
            }
        } catch (Exception e) {
            logger.warn("Error executing program. Exception:\n" + e.getMessage());
            return null;
        }
    }

    private void fillOracle(Mapping trainingData) {
        Mapping cachedMatches = new Mapping();
        for (Entry<String, HashMap<String, Double>> e1 : trainingData.map.entrySet()) {
            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                if (e2.getValue() > 0) {
                    cachedMatches.add(e1.getKey(), e2.getKey(), 1d);
                }
                // remember already explored matches...
                this.trainingData.add(e1.getKey(), e2.getKey(), e2.getValue());
            }
        }
        // add reference data
        fitness.addToReference(cachedMatches);
        // trim down caches for evolution only to asked instances
        fitness.fillCachesIncrementally(trainingData);
        // and propagate already asked instances
        alDecider.setKnown(trainingData);
    }

    @Override
    public ExpressionFitnessFunction getFitnessFunction() {
        return fitness;
    }

    /**
     * Test main shows usage.
     *
     * @param args
     */
    public static void main(String args[]) {
    	//runLinkedMDB_DBPedia("/home/akswadmin/Mofeed/GeneticLearningPart/GeneticJars/MoviesJars/05May2014GAL501Phase1Spec/specMovs.xml");
    	runExample();
    }
    
    
    public static void runExample() {

        Map<String, Double> results= new TreeMap<String, Double>();
    	
        String configFile = "/home/akswadmin/Mofeed/05May2014GAL501Phase1Spec/specMovs.xml";
        ConfigReader cR = new ConfigReader();
        cR.validateAndRead(configFile);
        Oracle o = OracleFactory.getOracle("/home/akswadmin/Mofeed/05May2014GAL501Phase1Spec/All501.csv", "csv", "simple");
        int size = 10;
        //EvaluationData eData = DataSetChooser.getData(DataSets.DBLPACM);
        // gets default property Mapping
        //PropertyMapping propMap = eData.getPropertyMapping();//PropMapper.getPropertyMapping(configFile);
        PropertyMapping propMap = new PropertyMapping();
		propMap.addStringPropertyMatch("rdfs:label", "rdfs:label");
		propMap.addStringPropertyMatch("linkedmdb:director","dbpedia-owl:director");
		propMap.addStringPropertyMatch("linkedmdb:initial_release_date","dbpedia-owl:releaseDate");

		//propMap.addDatePropertyMatch("linkedmdb:initial_release_date","dbpedia-owl:releaseDate");
		System.out.println(propMap);

        LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_LEARNER);
        // params for the learner
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(20);
	      params.setGenerations(100);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(true);
	      params.setTrainingDataSize(size);
	      params.setGranularity(2);
        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // initial data
        answer = learner.learn(new Mapping());
        Mapping oracleAnswer = new Mapping();
        // looking for answers from oracle aka user
        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                if (o.ask(e1.getKey(), e2.getKey())) {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                } else {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                }
            }
        }
    	Mapping fin=null;
    	String answerSTR="",expression="",threhold="";
        for (int cycle = 0; cycle < 10; cycle++) {
            System.out.println("Performing learning cycle " + cycle);
            // learn
            answer = learner.learn(oracleAnswer);
            // get best solution so far:
            answerMetric = learner.terminate();
            if (answerMetric.isValid()) {
                PRFCalculator prfC = new PRFCalculator();
                double fS = prfC.precision(o.getMapping(), learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true));
                System.out.println("Cycle " + cycle + " Best = " + answerMetric + "\n\tF-Score = " + fS);
                results.put("Cycle("+cycle+") Best : " + answerMetric.toString()+"\n\tF-Score ",fS);
            } else {
                Logger.getLogger("Limes").warn("Method returned no valid metric!");
            }
            System.out.println("Gathering more data from user ... ");
            oracleAnswer = new Mapping();
            for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
                for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                    if (o.ask(e1.getKey(), e2.getKey())) {
                        oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                    } else {
                        oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                    }
                }
            }
            fin = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
            answerSTR = answer.toString();
            expression = answerMetric.getExpression().toString();
            threhold =String.valueOf(answerMetric.getThreshold());
        }
        System.out.println(fin);
        System.out.println(answerSTR);
        System.out.println(expression);
        System.out.println(threhold);
        for( Entry<String, Double> entry : results.entrySet()){
			//System.out.println(entry.getKey()+": "+entry.getValue());
		}
    }
    
    public static void runLinkedMDB_DBPedia(String filePath) {
    	Map<String, Double> results= new TreeMap<String, Double>();
    	
    	ConfigReader cR = new ConfigReader();
    	cR.validateAndRead(filePath);
		KBInfo s = cR.getSourceInfo();
		KBInfo t = cR.getTargetInfo();
		Cache sC = HybridCache.getData(s);
		Cache tC = HybridCache.getData(t);
		
		ExecutionEngine engine = new ExecutionEngine(sC, tC, "?a", "?b");
		
		double threshold = 0.2d;
		String expr = "cosine(a.dbpedia-owl:director/rdfs:label, b.linkedmdb:director/rdfs:label)";
		String expr2 = "levensthein(a.rdfs:label,b.rdfs:label)";
		LinkSpec spec = new LinkSpec(expr2, threshold);
		CanonicalPlanner planner = new CanonicalPlanner();
		Mapping exampleMatch = engine.runNestedPlan(planner.plan(spec));

		System.out.println("Mapped to "+exampleMatch.size()+" links");
	
	
//		
		PropertyMapping propMap = new PropertyMapping();
		propMap.addStringPropertyMatch("rdfs:label", "rdfs:label");
		propMap.addStringPropertyMatch( "dbpedia-owl:director/rdfs:label","linkedmdb:director/rdfs:label");
		propMap.addDatePropertyMatch("dbpedia-owl:releaseDate", "linkedmdb:initial_release_date");

	
		CSVMappingReader mr = new CSVMappingReader();
//		Mapping loaded = mr.getMapping("resources/All501.csv");
		Mapping ref=Mapping.getBestOneToOneMappings(exampleMatch);
		int nrOfPositives = 0;
		Mapping start  = new Mapping();
		Cache knownSource = new MemoryCache();
		Cache knownTarget = new MemoryCache();
		for(String sUri : ref.map.keySet()) {
			Instance sI = sC.getInstance(sUri);
			if(sI != null)
				knownSource.addInstance(sI);
			for(String tUri : ref.map.get(sUri).keySet()) {
				Instance tI = tC.getInstance(tUri);
				if(tI != null) {
					knownTarget.addInstance(tI);
					if(nrOfPositives<50) {
						start.add(sUri, tUri, 1d);
						System.out.println("Mapping "+sI+" to "+tI);
						nrOfPositives++;
					}
					
				}
			}
		}
		
		System.out.println("Caches smallered into sizes" + knownSource.size()+" and "+knownTarget.size());
//		System.exit(1);
		
		Oracle o = new SimpleOracle(ref);
		
		LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_LEARNER);
			SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
		      params.setPopulationSize(10);
		      params.setGenerations(10);
		      params.setMutationRate(0.5f);
		      params.setPreserveFittestIndividual(true);
		      params.setTrainingDataSize(5);
		      params.setGranularity(2);
	        Mapping answer;
	        Metric answerMetric;
	        try {
	            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
	        } catch (InvalidConfigurationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        // initial data
	        learner.setCaches(knownSource, knownTarget);
	        answer = learner.learn(start);
	        learner.setCaches(knownSource, knownTarget);
	        
	        Mapping oracleAnswer = new Mapping(); // init data
		    int mapCount = 0;
		    
	        // looking for answers from oracle aka user
	        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
	            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
	                if (o.ask(e1.getKey(), e2.getKey())) {
	                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
	                } else {
	                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
	                }
	            }
	        }

	        for (int cycle = 0; cycle <2; cycle++) {
	            System.out.println("Performing learning cycle " + cycle);
	            // learn
	            answer = learner.learn(oracleAnswer);
	            // get best solution so far:
	            answerMetric = learner.terminate();
	           
//	            if (answerMetric.isValid()) {
	                PRFCalculator prfC = new PRFCalculator();
	                Mapping result = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
	                double fS = prfC.precision(o.getMapping(), result);
	                
	                String resultString = "Cycle("+cycle+") Best : " + answerMetric.toString()+" Mapping size="+result.size()+
	                		" sC.size()="+learner.getFitnessFunction().getSourceCache().size()+" tC.size()="+learner.getFitnessFunction().getTargetCache().size()+"\n\tF-Score ";
	                System.out.println(resultString);
	                results.put(resultString, fS);
//	            } else {
//	                Logger.getLogger("Limes").warn("Method returned no valid metric!");
//	            }
//	            System.out.println("Gathering more data from user ... ");
	            oracleAnswer = new Mapping();
	            for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
	                for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
	                    if (o.ask(e1.getKey(), e2.getKey())) {
	                        oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
	                    } else {
	                        oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
	                    }
	                }
	            }
	        }
	        for( Entry<String, Double> entry : results.entrySet()){
				System.out.println(entry.getKey()+": "+entry.getValue());
			}
	        System.out.println("Full Caches: sC.size()="+sC.size()+" tC.size()0"+tC.size());
	        
    }

    protected void printPop(GPPopulation pop) {
        int i = 0;
        for (IGPProgram p : pop.getGPPrograms()) {
//            System.out.println(++i + ":" + getMetric(p));
        }
    }

    @Override
    public Metric terminate() {
        determineMetric();
        return metric;
    }
    
    public void setCaches(Cache sC, Cache tC) {
    	logger.info("Manually setting caches");
		fitness.setCaches(sC, tC);
	}
}
