package de.uni_leipzig.simba.learning.refinement.supervised;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.genetics.core.ALDecider;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.learning.refinement.RefinementBasedLearningAlgorithm;
import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;
import de.uni_leipzig.simba.learning.refinement.evaluation.EvaluationMemory;
import de.uni_leipzig.simba.specification.LinkSpec;
/**
 * Basic class to learn link specifications using an refinement operator in an supervised manner.
 * This means using training data, which is/has to be evaluated by an oracle.
 * @author Klaus Lyko
 *
 */
public class SupervisedRefinementAlgorithm extends RefinementBasedLearningAlgorithm {
	boolean improved = false;
	boolean useFScore = true;
	
	float portionTrimmed = 0.8f;
	
	Mapping trainingData; Mapping positiveLinks;
	Cache trimmedSource, trimmedTarget;
	ExecutionEngine trimmedEngine;
	
	Cache restSource, restTarget;
	ExecutionEngine restEngine;
	int loop = 0; 
	/** how many link specs to consider to retrieve training data*/
	int poolSize = 300; 
	/** how many mappings the orcale has to evaluate each cycle*/
	int inquerieSize = 10;
//	double highestAccuracy = 0.0;
	ALDecider alDecider = new ALDecider();
	boolean timeBasedTermination = false;
	boolean useFullCaches = false;
	int maxSteps = 100;
	/**
	 * 
	 * @param evalData
	 * @param inquerieSize
	 * @param timeBasedTermination set to use time based termination. Maxmimal duration as speziedied in init().
	 * @param improved if set we use a different quality measure: a combination of pfm and standard f.
	 */
	public SupervisedRefinementAlgorithm(EvaluationData evalData, int inquerieSize, boolean timeBasedTermination, boolean useFullCaches, boolean improved) {
		super(evalData);
		this.improved = improved;
		this.useFullCaches = useFullCaches;
		this.inquerieSize = inquerieSize;
		evalData.setName("hard_"+evalData.getName());
		trimmedSource = new MemoryCache();
		trimmedTarget = new MemoryCache();
		restSource = new MemoryCache();
		restTarget = new MemoryCache();
		positiveLinks = new Mapping();
		trainingData = new Mapping();
		this.timeBasedTermination = timeBasedTermination;
	}
	
	/**
	 * If called with true LION will use the MatthewCorrelation to compute the quality of candiates.
	 * @param yes true: use Matthew; false: use standard FScore.
	 */
	public void useMatthewCoefficient(boolean yes) {
		if(yes) {
			useFScore = false;
			useFullCaches = true;
		}
			
	}
	
	/**
	 * Checks termination criteria
	 * @param step number of performed steps
	 * @param dur time already spent in ms
	 * @return true if termination criteria is reached.
	 */
	private boolean terminationCriteriaReached(int step, long dur) {
		if(this.timeBasedTermination) {
			return (dur/1000)>=maxDuration;
		}
		return step>maxSteps;
	}
	
	@SuppressWarnings("unused")
	public Mapping learn(Mapping trainingData, int steps) {
		maxSteps = steps;
		long startTime = System.currentTimeMillis(); long  dur = 0L; 	int nextRootNode = 100;
		updateData(trainingData);
		alDecider.setKnown(trainingData);
		logger.error("Learning on "+positiveLinks.size()+" positive links out of " + this.trainingData.size());
		
		SearchTreeNode nextNode;
		if(loop <= 0) { // start of algorithm
			startNode = new SearchTreeNode(new LinkSpec(), gammaScore, expansionPenalty);
			startNode.addSpecificName("root");
			startNode.setExpansion(1);
//			startNode.incExpansion();
			nodes.add(startNode);
			//bMofeed
				startNode.nodeId = IdCounter++; //give id
				startNode.creationTime = globalTime ; //set creation time for node
				startNode.createHistoryevent(globalTime++); //add it to the history for attributes

		} else {
			// recalculate Qualities of all nodes
			for(SearchTreeNode node : nodes) {
				if(!node.isRoot()) {
					double score = 0d;
					if(!improved) {
						if(this.useFullCaches)
							score =  getQuality(getMapping(node.getSpec(), WhichMap.FULL));
						else
							score = getQuality(getMapping(node.getSpec(), WhichMap.TRIMMED));
					}						
					else
						score = getImprovedQuality(node.getSpec()).combine();
					node.setScore(score);
					if(score > bestScore) {
						best = node;
						bestScore = score;
					}
				}
			}
		}
		int step = 1;
		while(!terminationCriteriaReached(loop, dur)) {
		
			if(best == null) {
				best = startNode;
				memList.add(new EvaluationMemory(best, loop, System.currentTimeMillis()-startTime));
			}
			
		
			// chose best node according to heuristics
			if(hardRootExpansion) {
				if(loop==0 || loop==nextRootNode) {//0, 50, 100
				
				nextNode = startNode;
				if(loop == nextRootNode)
					nextRootNode *= 100; 
				logger.error("Loop "+loop+" expanding rootNode... Time passed "+(dur/1000)+" s");
				} else {
					nextNode = getNextNodeToExpand();	
				}
				
			} else {
				nextNode = getNextNodeToExpand();
			}
			logger.debug("BEST loop("+loop+")"+best);
			if(nextNode.isRoot()) {
				loopsRootExpanded.add(loop);
			}
			int horizExp = nextNode.getExpansion();
//			logger.info("Loop "+loop+" expansion="+horizExp+ " refining node:"+"\n \twith score "+nextNode.getScore()+ ": "+nextNode.toTreeString());
			// apply operator
			newBest = false;
			Set<LinkSpec> refinements = refineNode(nextNode, startTime);
			
			resLog.writeLogString(loop+": Expanded (sc:="+nextNode.getPenaltyScore()+" - maxF:="+nextNode.maxAcchievablePFM+")"+nextNode.specificName+" +node.spec"+nextNode.getSpec()+" into "+refinements.size()+" nodes.");
//			logger.info("\t refined into "+refinements.size());
			int specNr = 1;
			int nrOfChilds = 0;
			Iterator<LinkSpec> it = refinements.iterator();
			while(refinements.size() != 0 && it.hasNext()) {
				// pick element from set
				LinkSpec refinement = it.next();
				specNr++;

				if(refinement.threshold<=0.1) { 
					logger.debug("Ignoring refinement "+refinement+"\n due to too low threshold");
					continue;
				}
				SearchTreeNode added = addNode(refinement, nextNode);
				if(added!=null) {
					if(bestScore<added.getScore()) {
						bestScore = added.getScore();
						best = added;
						newBest = true;
						logger.debug("New highest accuracy: "+bestScore+" accieved by node "+added);
					}
					if(added.getScore() > nextNode.getScore()) {
						added.setReward(reward);
					}
					nrOfChilds++;
				}				
			}
			if(nrOfChilds==0 && !nextNode.isRoot()) {
				logger.debug("Not added any nodes. Ignoring node for further expansion");
				nextNode.expand = false;
			}
			if(newBest)
				memList.add(new EvaluationMemory(best, loop, System.currentTimeMillis()-startTime));
//			logger.info("Added "+nrOfChilds+" nodes to tree");
		if(debuggingInput) {
			System.out.println("Press enter for next loop");
		   InputStreamReader converter = new InputStreamReader(System.in);
		   BufferedReader in = new BufferedReader(converter);
		   try {
			in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}// debugging
			loop++;
			
			dur = System.currentTimeMillis()-startTime;
		} // end of innersteps
		steps++;
		memList.add(new EvaluationMemory(best, loop, System.currentTimeMillis()-startTime));
		return getMostInformativeLinks();
	}
	
	/**
	 * Method updates Caches and truePostives.
	 * @param oracleAnswer
	 */
	private void updateData(Mapping oracleAnswer) {
		for(String s : oracleAnswer.map.keySet()) {
			for(Entry<String, Double> e : oracleAnswer.map.get(s).entrySet()) {
				if(!useFullCaches||improved)
					try {
						Instance sI = evalData.getSourceCache().getInstance(s);
						trimmedSource.addInstance(sI);
					}catch(NullPointerException ex) {
						logger.error("Unable to locate source Instance "+s);
						ex.printStackTrace();
					}
				if(!useFullCaches||improved)
					try {
						Instance tI = evalData.getTargetCache().getInstance(e.getKey());
						trimmedTarget.addInstance(tI);
					}catch(NullPointerException ex) {
						logger.error("Unable to locate target Instance "+e.getKey());
						ex.printStackTrace();
					}
				if(e.getValue() > 0) {
					positiveLinks.add(s, e.getKey(), e.getValue());
				}
				trainingData.add(s, e.getKey(), e.getValue());
			}
		}
		if(!useFullCaches || improved) {
			trimmedEngine = new ExecutionEngine(
					trimmedSource, 
					trimmedTarget,
					evalData.getConfigReader().sourceInfo.var, 
					evalData.getConfigReader().targetInfo.var);
		} else {
			trimmedEngine = new ExecutionEngine(
					evalData.getSourceCache(), 
					evalData.getTargetCache(),
					evalData.getConfigReader().sourceInfo.var, 
					evalData.getConfigReader().targetInfo.var);
		}
		if(improved) {
			for(Instance i : evalData.getSourceCache().getAllInstances()) {
				if(!trimmedSource.containsInstance(i))
					restSource.addInstance(i);
			}
			for(Instance i : evalData.getTargetCache().getAllInstances()) {
				if(!trimmedTarget.containsInstance(i))
					restTarget.addInstance(i);
			}
			restEngine = new ExecutionEngine(
					restSource, 
					restTarget,
					evalData.getConfigReader().sourceInfo.var, 
					evalData.getConfigReader().targetInfo.var);
		}
		String s="\n";
		s+="sC.size()="+evalData.getSourceCache().size()+" tC.size()="+evalData.getTargetCache().size()+"\n";
		s+="trimmedSource.size()="+trimmedSource.size()+" trimmedTarget.size()="+trimmedTarget.size()+"\n";
		s+="restSource.size()="+restSource.size()+" restTarget.size()="+restTarget.size()+"\n";
		System.out.println(s);
		logger.error(s);
	}

	private Mapping getMostInformativeLinks() {
		logger.info("get most informative links... nodes.size()="+nodes.size());
		Comparator<SearchTreeNode> comparator = new SearchTreeScoreComparator();//best scores first
		List<SearchTreeNode> nodeList = new ArrayList<SearchTreeNode>(nodes.size());
		nodeList.addAll(nodes);
		int realPoolSize = Math.min(poolSize, nodeList.size());
		List<Mapping> mapList = new ArrayList<Mapping>(realPoolSize);
		//sort
		Collections.sort(nodeList, comparator);
		logger.info("get most informative links... sorted all nodes. getting full maps of "+realPoolSize+" specs");
		//getMappings
		for(int i=0; i<realPoolSize; i++) {
			SearchTreeNode node =  nodeList.get(i);

//			logger.info("getting full map of "+node.getSpec());
			mapList.add(i, getMapping(node.getSpec(), WhichMap.FULL));
		}
		logger.info("get most informative links... get Maps for "+realPoolSize+" candidates.");
		//compute inquieries
		List<Triple> inquery =  alDecider.getControversyCandidates(mapList, inquerieSize);
		Mapping getAnswerFor = new Mapping();
		
		for(Triple t: inquery) {
			getAnswerFor.add(t.getSourceUri(),  t.getTargetUri(), t.getSimilarity());
		}
		alDecider.setKnown(getAnswerFor);
		return getAnswerFor;
		
	}

	@Override
	protected double getQuality(Mapping map) {
		PRFCalculator prf = new PRFCalculator();//check iff to use positive links
			if(useFScore) {
				double f = prf.fScore(map, trainingData);
				logger.info("Computed FMeasure: "+f);
				return f;
			} else {
				double crossProd = evalData.getSourceCache().size() * evalData.getTargetCache().size();
				double mathew = prf.computeMatthewsCorrelation(map, trainingData, crossProd);
				logger.info("Computed Matthew Correlation: "+mathew);
				return mathew;
			}
	}
	/**
	 * Computes the improved combined quality function of a given Link Spec.
	 * The basic idea is to combine F Measure for trimmed Caches (holding those instances
	 * which are also present at the trainingData) and standard PFM-Measure for the rest
	 * Caches.
	 * @param ls
	 * @return
	 */
	protected ImprovedQualityFeedback getImprovedQuality(LinkSpec ls) {
		ImprovedQualityFeedback feedback = new ImprovedQualityFeedback(portionTrimmed);
		feedback.trimmedMap = getMapping(ls, WhichMap.TRIMMED);
		feedback.fTrimmed = getQuality(feedback.trimmedMap);
		feedback.restMap = getMapping(ls, WhichMap.REST);
		feedback.pfmRest = pfm.getPseudoFMeasure(restSource.getAllUris(),
				restTarget.getAllUris(), feedback.restMap, 
				beta);
//		logger.error("ImprovedQulaity= "+feedback+" on spec:"+ls);
		return feedback;
	}
	
	private Mapping getMapping(LinkSpec spec, WhichMap type) {
		if(spec.isEmpty()) {
			return new Mapping();
		}
		else {
			Mapping mapping = new Mapping();
			try {
				switch(type) {
					case FULL: mapping = getMapping(spec); break;
					case TRIMMED: mapping = trimmedEngine.runNestedPlan(planner.plan(spec)); break;
					case REST: mapping = restEngine.runNestedPlan(planner.plan(spec)); break;
				}
			}catch(Exception e) {
				System.err.print("Error executing spec "+spec);
				resLog.writeLogString("Error executing spec "+spec);
				e.printStackTrace();
			}
			return mapping;
		}
	}
	
	 @Override
	protected SearchTreeNode addNode(LinkSpec spec, SearchTreeNode parentNode) {
		
		// redundancy check (return if redundant)
//		Monitor monRed = MonitorFactory.start("redundancy");
//		boolean nonRedundant = specs.add(spec);
//		monRed.stop();
////		logger.info("adding Node: "+nonRedundant+": "+spec);
//		if(!nonRedundant) {
//			logger.info("redundandt");
//			return null;
//		}
		 ImprovedQualityFeedback acc = new ImprovedQualityFeedback(portionTrimmed);
		if(!improved) {
			acc = new ImprovedQualityFeedback(1);
		}
		if(!improved) {
			if(!useFullCaches)
				acc.trimmedMap = getMapping(spec, WhichMap.TRIMMED);
			else
				acc.trimmedMap = getMapping(spec, WhichMap.FULL);
			acc.pfmRest = 1d;
			acc.fTrimmed =  getQuality(acc.trimmedMap);		
		}
		else {
			acc = getImprovedQuality(spec);
		}
		SearchTreeNode node = new SearchTreeNode(parentNode, spec, acc.combine(), expansionPenalty);

		if(this.bestScore < acc.combine() || best.isRoot()) {
			bestScore = acc.combine();
//			logger.error("Setting new best"+best);
			best = node;
			newBest = true;
//			logger.error("New Best:("+bestScore+") "+best);
		}
		node.setExpansion(0);
		double maxAcievable = computeMaxAcchievableFMeasure(acc.trimmedMap);
		
			
		if(!improved)
			node.maxAcchievablePFM = maxAcievable;
		else {
			node.maxAcchievablePFM = acc.combine(maxAcievable, super.maxAcievableFScore(acc.restMap, restSource.size(), restTarget.size()));
		}
		boolean added = false;
		if(best != null && !best.isRoot())
			if(maxAcievable < best.getScore()) {
				logger.info("Not adding spec with score "+ node.getScore() + " due to maxAcievableTest");
	//			resLog.writeLogString("Not adding spec "+ node + " due to maxAcievableTest");
				return null;
			}
//		if(maxAcievable < parentNode.maxAcchievablePFM) {
//			logger.info("Not adding spec with score "+ node.getScore() +" due to maxAcievableTest with parent");
//			resLog.writeLogString("Not adding spec "+ node + " due to maxAcievableTest with parent");
//			return null;
//		}
		added = nodes.add(node);
		if(!added) {
//			Iterator<SearchTreeNode>nodes.iterator()
//			logger.error("Failed adding node "+node+" to treeset");
//			Iterator<SearchTreeNode> it = nodes.iterator();
//			while(it.hasNext()) {
//				SearchTreeNode o = it.next();
//				if(o.equals(node))
//					logger.error("Because it already contains:"+o);
//			}
			
			
			return null;
//			resLog.writeLogString("Failed adding node "+node+" to treeset");
		}
		else {
				node.nodeId = IdCounter++; //assign an id to the new node and increment it for future
				parentNode.addChild(node);
//			resLog.writeLogString("Added node (score="+node.getScore()+" | "+node.getPenaltyScore()+") with thresh"+node.getSpec().threshold+": "+node);
			logger.info("Node added score"+ node.getScore()+" max="+node.maxAcchievablePFM +"to treeset: "+node);
		}
		logger.debug("Adding node with with thresholdborders: "+node.lowThreshold+" - "+node.highThreshold);
		return node;
	}	
	
	 public void end() {
			String s="\n";
			s+=" SearchTree.size =" + nodes.size() + "\n";
			s+="sC.size()="+evalData.getSourceCache().size()+" tC.size()="+evalData.getTargetCache().size()+"\n";
			s+="trimmedSource.size()="+trimmedSource.size()+" trimmedTarget.size()="+trimmedTarget.size()+"\n";
			s+="restSource.size()="+restSource.size()+" restTarget.size()="+restTarget.size()+"\n";
			s+="using FScore as quality?"+useFScore+"\n";
			resLog.writeString(s);
			logger.error("Computing Scores");
			computeScores();
//			System.out.println("Generating graph for "+nodes.size()+" nodes\n");
//			FileUtils.writeStringToFile(new File("resources/results/searchTree.txt"), startNode.toTreeString());
//			resLog.writeTreeString(startNode.toTreeString());
	 }
	public static void runActiveLearningTests() {
		int inquerieSize = 10;
		DataSets allEvalData[] = {
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPACM,
				
		}; 
		for(DataSets ds : allEvalData) {
			EvaluationData data = DataSetChooser.getData(ds);
			data.setName("supervised_2thres_"+data.getName());
			System.out.println("Running dataset "+data.getName());
			logger.setLevel(Level.ERROR);
			SupervisedRefinementAlgorithm.hardRootExpansion = true;
			SupervisedRefinementAlgorithm algo = new SupervisedRefinementAlgorithm(data, inquerieSize, true, true, true);
			
			Mapping start = getStartData(data, inquerieSize);
			algo.init(data, 0.2, 0.95, 1.2, 600);
			
			Mapping toAsk = algo.learn(start, 200);
			for(int it = 0; it < 10; it++) {
				System.out.println("Running iteration "+it);
				System.out.println("Askinng oracle about "+toAsk);
				Mapping answer = getOracleAnswers(toAsk, data);
				logger.error("Asking oracle about "+toAsk.size()+" matches: Positive were "+answer.size());
				algo.logStatistics(it, toAsk, answer);
				toAsk = algo.learn(answer, 200);
			}
			algo.end();
		}// for data set
	}
	 
	public static void runBatchLearnTests(boolean matthew) {
		int inquerieSize = 100;
		DataSets allEvalData[] = {
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPACM,
		}; 
		for(DataSets ds : allEvalData) {
			EvaluationData data = DataSetChooser.getData(ds);
			data.setName("batch_2thres_"+data.getName());
			System.out.println("Running Batch learning on dataset "+data.getName());
//			logger.setLevel(Level.ERROR);
		
			Mapping start = getStartData(data, inquerieSize);
			
				SupervisedRefinementAlgorithm.hardRootExpansion = true;
				SupervisedRefinementAlgorithm algo = new SupervisedRefinementAlgorithm(data, inquerieSize, true, true, true);
				logger.setLevel(Level.ERROR);
				algo.useMatthewCoefficient(false);
				algo.init(data, 0.2, 0.8, 1.2, 60);
				Mapping toAsk = algo.learn(start, 10);
				algo.end();
			if(matthew) {	
				data.setName(data.getName()+"_matthew");
				SupervisedRefinementAlgorithm.hardRootExpansion = true;
				algo = new SupervisedRefinementAlgorithm(data, inquerieSize, true, true, true);
				algo.useMatthewCoefficient(true);
				algo.init(data, 0.2, 0.8, 1.2, 60);
				toAsk = algo.learn(start, 10);
				algo.end();
			}
			
		}// for data set

	}
	 
	public static void main(String args[]) {
	 	CommandLineParser parser = new BasicParser();
	    try {
			CommandLine cmd = parser.parse(getCLIOptions(), args);
			if(cmd.hasOption("active")) {
				runActiveLearningTests();
			}
			else {
				if(cmd.hasOption("matthew")) {
					runBatchLearnTests(true);
				}				
				runBatchLearnTests(false);
			}
				
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void logStatistics(int iteration, Mapping toAsk, Mapping answer) {
		resLog.writeString("Iteration "+iteration+" asked "+toAsk.size()+" matches true were: "+answer.size()+" trainingData.size()"+trainingData.size()+" Nr of positive links="+positiveLinks.size());
	}

	public static Mapping getStartData(EvaluationData data, int inquerieSize) {
		int count = 0;
		Mapping ref = data.getReferenceMapping();
		Mapping startData = new Mapping();
		for(String s : ref.map.keySet()) {
			for(String t : ref.map.get(s).keySet()) {
				startData.add(s, t, 1d); count++;
				break;
			}
			if(count>=inquerieSize)
				return startData;
		}
		return startData;
	}
	
	public static Mapping getOracleAnswers(Mapping m, EvaluationData data) {
		Mapping ref = data.getReferenceMapping();
		Mapping answer = new Mapping();
		int count = 0;
		for(String s : m.map.keySet()) {
			for(String t:m.map.get(s).keySet()) {
				if(ref.contains(s, t)) {
					answer.add(s,  t, m.map.get(s).get(t)); count++;
				} else {
					answer.add(s,  t, -1);
				}
			}
		}
		logger.info("Asking oracle about " + m.size()+" matches resulted in "+count+" true positives."); 
		return answer;
	}
	
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption("active", false, "set if the active learning should be tested.");
		options.addOption("matthew", false, "set if matthew correlation should be used for quality calculation");
		return options;
	}
	
	/**
	 * Implementation of max achievable F-Measure to avoid adding unuseful Link Specs to the search tree.
	 * The basic idea is: if we add all trainingData links to the mapping it could not get better then that.
	 * @param m Mapping of the Link Spec
	 * @return F-Measure of m union trainingData
	 */
	protected double computeMaxAcchievableFMeasure(Mapping m) {
//		m.map.putAll(trainingData.map);
		Mapping full = m.union(trainingData);
		PRFCalculator prf = new PRFCalculator();
		double val = prf.fScore(full, trainingData);
		
		
		if(val > 1.0) {
			logger.error("Computed maxF of > 1:"+val+" for node prec:="+prf.precision(full, trainingData)+" , rec:="+prf.recall(full, trainingData));
			val = 1;
		}
		return val;
	}
	
	/**
	 * To differentiate which Mappings to compute.
	 * @author Klaus Lyko
	 *
	 */
	public enum WhichMap {
		FULL, TRIMMED, REST
	}

	/**
	 * Set portion of QualityEvaluation based upon supervised part!
	 * @param f
	 */
	public void setTrimmedPortion(float f) {
		if(f>=0 && f<=1)
			portionTrimmed = f;
	}
}

