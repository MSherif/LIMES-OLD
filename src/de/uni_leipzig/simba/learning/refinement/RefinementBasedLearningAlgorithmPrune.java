package de.uni_leipzig.simba.learning.refinement;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.FileHandler;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.learning.refinement.evaluation.ResultLogger;
import de.uni_leipzig.simba.learning.refinement.operator.LengthLimitedRefinementOperator;
import de.uni_leipzig.simba.learning.refinement.operator.UpwardLengthLimitRefinementOperator;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.specification.LinkSpec;

public class RefinementBasedLearningAlgorithmPrune {
static Logger logger = Logger.getLogger("LIMES");
static FileHandler fileHandler;
 

	
	private LengthLimitedRefinementOperator operator;
	
	// all nodes in the search tree (used for selecting most promising node)
	private TreeSet<SearchTreeNode> nodes;
	private RefinementHeuristic heuristic;
	// root of search tree
	private SearchTreeNode startNode; // it is null
	/**Gamma score for root node to enabling revisting*/
	private double gammaScore = 0.3d;
	private double expPenalty = 0.9d;
	// all specs in the search tree plus those which were too weak (for fast redundancy check)
	private TreeSet<LinkSpec> specs; //m:note- here check the redundancy
	ResultLogger resLog;
	// needed for PFM computation...
	ExecutionPlanner planner;
	ExecutionEngine engine;
	EvaluationData evalData;
	/**PseudoFMeasure implemntation used**/
	PseudoMeasures pfm;
	/**Beta for the PseudoFMeasure**/
	double beta = 1d;
	int maxIteration = 500;
	// TODO test with pfms and real f how far we get
	
	
	// initialise algorithm
	public void init(EvaluationData evalData) {
		//m:code,initialize variables
		nodes= new TreeSet<SearchTreeNode>();
		specs= new TreeSet<LinkSpec>();
		heuristic = new DefaultRefinementHeuristic();
		operator = new UpwardLengthLimitRefinementOperator();
		operator.setEvalData(evalData);
		this.evalData = evalData;	
//		planner = new HeliosPlanner(evalData.getSourceCache(), evalData.getTargetCache());
		planner = new CanonicalPlanner();
		setUpExperiment();
		resLog = new ResultLogger(evalData);
		resLog.createFile();
	}
	
	// start algorithm
	public void start() {
		
		// highest real accuracy so far
		double highestAccuracy = 0.0;
		//highest best pseudo F-Measre
		double bestPFM = 0.0;
		SearchTreeNode nextNode;
		startNode = new SearchTreeNode(new LinkSpec(), gammaScore, expPenalty);
		//assign the same value to the supposed best PFM for the root

		startNode.addSpecificName("root");
		nodes.add(startNode);
		TreeSet<LinkSpec> refinements =null;
        logger.info("Start Node=[spec-->"+ startNode.getSpec()+": score -->"+startNode.getScore()+": best score -->"+startNode.getBestScore());
		int loop = 0;
		//TODO ask for this condition how valid it is
		while (!Terminate("iterate", loop)) 
		{
			// Remember best node
			/*SearchTreeNode best = getMaxNode();
			if(best == null) {
				best = startNode;
			}*/
			// Chose best node with highest score according to heuristics
	        logger.info("Selecting the node with max score to refine");
			nextNode = getNextNodeToExpand();
	        logger.info("Node to be refined =[spec-->"+ nextNode.getSpec()+": score -->"+nextNode.getScore()+": best score -->"+nextNode.getBestScore());
			//Max. expansion to be achieved (max value for a metric; atomic = 1)
			int horizExp = nextNode.getExpansion();
			logger.info("Loop "+loop+" expansion="+horizExp+ " refining node:"+nextNode+ "\n \twith score "+nextNode.getScore());
			//Apply refinement operator getting new refinements of this node
			refinements = refineNode(nextNode);
			logger.info("\t refined into "+refinements.size()+" specs..."+refinements );
			//Iterate all list of refinements
			//specified calculate its accuracy score,i.e.PFM
	        logger.info("Iterate over refinements to find the one with max best score");
			while(refinements.size() != 0) {
				// pick element from set of refinements
				LinkSpec refinement = refinements.pollFirst();

				int length = refinement.size();
				logger.info("horizExp= "+horizExp+" Length = "+refinement.size()+" ["+refinement+"]");				
				// we ignore all refinements with lower length and too high depth
				// (this also avoids duplicate node children)
				// @FIXME why this check?
				if(refinement.threshold<=0.08) {
					logger.error("Ignoring refinement "+refinement+"\n due to too low threshold");
					continue;
				}
				
				if(length >= horizExp) 
				{
					//Add the picked refinement as one of the parent children with accuracy calculation 
					//and 'just' mark it if it is weak node or not					
					SearchTreeNode added = addNode(refinement, nextNode, bestPFM);
					if(added!=null) //node successfully added to the tree
					{
							bestPFM = added.getBestScore();
						logger.info("Successfully added node "+refinement+"\n \t to "+nextNode);
					} 
					else 
					{
						logger.info("Not added node "+refinement+"\n \t to "+nextNode);
					}
				
					// adding nodes is potentially computationally expensive, so we have
					// to check whether max time is exceeded	
					if(terminationCriteriaSatisfied(loop)) {
						break;
					}

				}
				//TODO prune the tree (comment:pruning is done in the moment the spec proves less best achieved PFM as the node is not created)
		        logger.info("Start pruning levlel-2 scanning the tree");
				pruneSearchTree(bestPFM);

			}

			//resLog.logEntry(loop, nextNode, refinements, highestAccuracy);
	        logger.info("In iteration Nr. "+loop+"with best score (max best PFM) = "+bestPFM +"\n");
	        logger.info("List of nodes are:\n");
			displayFinalSpecs();
			loop++;
		}
		logger.info("Max. achieved Best PFM = "+ bestPFM+"\n");
		logger.info("Final Nodes spec and scores in iteration:\n");
		displayFinalSpecs();

			
	}
	private void pruneSearchTree(double bestPFM)
	{
		Iterator<SearchTreeNode> iterator = nodes.descendingIterator();
        logger.info("Check Weak Nodes");

		while(iterator.hasNext())
		{
			SearchTreeNode current = iterator.next();
	        logger.info("Check Weak =[spec-->"+ current.getSpec()+": score -->"+current.getScore()+": best score -->"+current.getBestScore());

			if(Math.abs(current.getBestScore()-bestPFM) > 0.01 && current.getParent() != null)// this node is weak adn not root
			{
				current.setWeakNode(true);
		        logger.info("Assigned to be Weak =[spec-->"+ current.getSpec()+": score -->"+current.getScore()+": best score -->"+current.getBestScore());
			}
		}
		iterator = nodes.descendingIterator();
		while(iterator.hasNext())
		{
			SearchTreeNode current = iterator.next();
			if(current.isWeakNode())// this node is weak adn not root
			{
				prune(current,"cutoff" );
			}
		}
		iterator = nodes.descendingIterator();
		while(iterator.hasNext())
		{
			SearchTreeNode current = iterator.next();
			if(current.isWeakNode())// this node is weak and not root
				nodes.remove(current);
		}
	}
	private void displayFinalSpecs()
	{
		Iterator<SearchTreeNode> iterator = nodes.descendingIterator();
		while(iterator.hasNext())
		{
			SearchTreeNode current = iterator.next();
	        logger.info("Node in tree =[spec-->"+ current.getSpec()+": score -->"+current.getScore()+": best score -->"+current.getBestScore());
		}
	}
	private void prune(SearchTreeNode weakNode,String pruneMethod)
	{
		if(pruneMethod.equals("shiftUp"))
		{
			//add the node children to it parent
			for(SearchTreeNode child : weakNode.getChildren() )
			{
				weakNode.getParent().addChild(child);
			}
			//remove the weak node from its parent children
			weakNode.getParent().getChildren().remove(weakNode);
			//remove from list of traversed nodes
			nodes.remove(weakNode);
			//remove its spec form the list
			specs.remove(weakNode.getSpec());
		}
		else if(pruneMethod.equals("cutoff"))
		{
			//remove it from the list of parent children
			weakNode.getParent().getChildren().remove(weakNode);
			//remove its connection to it parent
			weakNode.setParent(null);
			/*
			 * Note:
			 * Here the weak node spec. is not removed from the specs list, in purpose when it is generated again due to the same
			 * parent refinement it won't be added as it is already useless
			 */
		}
	}
	private boolean TerminateUsingNoise(double noise,int positiveEx)
	{
		Iterator<SearchTreeNode> iterator = nodes.descendingIterator();
		while(iterator.hasNext())
		{
			SearchTreeNode current = iterator.next();
			if(current.getScore() < noise * positiveEx)
				return false;
		}
		return true;
	}
	private boolean Terminate(String termType,double value)
	{
		//value can be max fmeasure required or max expamsion required
		if(termType.equals("fMeasure") || termType.equals("expansion"))
		{
			Iterator<SearchTreeNode> iterator = nodes.descendingIterator();
			while(iterator.hasNext())
			{
				SearchTreeNode current = iterator.next();
				if(current.getScore() > value )
					return true;
			}
			return false;
		}
		else if (termType.equals("iterate") && value < maxIteration)
			return false;
		return true;
	}

	private SearchTreeNode getNextNodeToExpand() {
		// we expand the best node of those, which have not achieved 100% accuracy
		// already and have a horizontal expansion equal to their length
		// (rationale: further extension is likely to add irrelevant syntactical constructs)
//		logger.info("Get next node, treeset size="+nodes.size() );
        logger.info("Iterating over the nodes list to get the node with max score");

		Iterator<SearchTreeNode> it = nodes.descendingIterator();
		double maxScore=-1d;
		SearchTreeNode next = null; 
		while(it.hasNext()) 
		{
			
			SearchTreeNode node = it.next();
			if(node.getScore() > maxScore) 
			{
				maxScore = node.getScore();
				next = node;
			} 
			else 
			{// if there is a refinement with close accuracy score
				if(Math.abs(node.getScore() - maxScore) <= 0.01d && maxScore > 0) 
				{
					if(next == null || next.getExpansion()>node.getExpansion() && node.getSpec().threshold>0.1d) 
					{
						next = node;
						maxScore=node.getScore();
					}					
				}
			}
		}
		if(maxScore > -1d)
			return next;
		// this should practically never be called, since for any reasonable learning
		// task, we will always have at least one node with less than 100% accuracy
		return nodes.last();
		/*Iterator<SearchTreeNode> it = nodes.descendingIterator();
		double maxScore=-1d;
		SearchTreeNode next = null; 
		while(it.hasNext()) 
		{
			
			SearchTreeNode node = it.next();
			if(node.getScore() > maxScore) 
			{
				maxScore = node.getScore();
				next = node;
			} 
			else 
			{// if there is a refinement with close accuracy score
				if(Math.abs(node.getScore() - maxScore) <= 0.01d && maxScore > 0) 
				{
					if(next == null || next.getExpansion()>node.getExpansion() && node.getSpec().threshold>0.1d) 
					{
						next = node;
						maxScore=node.getScore();
					}					
				}
			}
		}
		if(maxScore > -1d)
			return next;
		// this should practically never be called, since for any reasonable learning
		// task, we will always have at least one node with less than 100% accuracy
		return nodes.last();*/
	}
	
	// expand node horizontically
	private TreeSet<LinkSpec> refineNode(SearchTreeNode node) {
		// we have to remove and add the node since its heuristic evaluation changes through the expansion
		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
		// otherwise you may see rarely occurring but critical false ordering in the nodes set)
		nodes.remove(node);

		int horizExp = node.getExpansion();
        logger.info("Refining the Node");

		TreeSet<LinkSpec> refinements = (TreeSet<LinkSpec>) operator.refine(node.getSpec(), horizExp+1);
		node.incExpansion();
		nodes.add(node);
		return refinements;
	}
	
	// add node to search tree if it is not too weak
	// returns true if node was added and false otherwise
	private SearchTreeNode addNode(LinkSpec spec, SearchTreeNode parentNode, double bestPFM) {
	
		// redundancy check (return if redundant)
		boolean nonRedundant = specs.add(spec);
		logger.info("adding Node: "+nonRedundant+": "+spec);
		if(!nonRedundant) {
			return null;
		}
		//Calculate 'Real' F-measure for this specification

		double accuracy = getPseudoFMeasure(spec,"pfm");
		if(accuracy == -1) {
			return null;
		}   
		logger.info("The score for the refienement with spec="+spec.getShortendFilterExpression());

		//calculate 'Best' F-Measure (named here best accuracy) can be achieved by this spec
		double specBestaccuracy = getMaxPseudoFMeasure(spec);
				if(accuracy == -1) {
					return null;
				}
       logger.info("The best score for the refinement with spec ="+ spec.getShortendFilterExpression());
       logger.info("Checking the refinementif to be added to the tree or not (pruning Level-1)");

		if(!maxAcievableFScore(spec, bestPFM)) {
			logger.error("Not adding spec "+ spec + " due to maxAcievableTest");
			return null;
				}
		logger.error("Add spec = "+ spec + " due to maxAcievableTest");
		//create a node for the refined spec. and adding it to its parent
		SearchTreeNode node = new SearchTreeNode(parentNode, spec, accuracy, expPenalty);

		node.maxAcchievablePFM=specBestaccuracy;
		node.addSpecificName(parentNode.getSpecificName()+":"+(parentNode.getChildren().size()+1));
		// link to parent (unless start node)
		if(parentNode == null) {
			startNode = node;
		} else {
			parentNode.addChild(node);
		}
	
		boolean added = nodes.add(node);
		if(!added)
			logger.error("Failed adding node "+node+" to treeset");
		else {
			logger.info("Node added to "+ node +" treeset");
		}
		return node;
	}	
	/**
	 * Just a dirty termination check.
	 * @param loop
	 * @return
	 */
	private boolean terminationCriteriaSatisfied(int loop) {
		if(loop >=10)
			return true;
		return false;
	}	
/*	public static void main( String[] args )
	{
		RefinementBasedLearningAlgorithm runner= new RefinementBasedLearningAlgorithm();
		runner.init();
		runner.start();
	}*/
	/**
	 * Method computes precision on an LinkSpec
	 * @param spec
	 * @return Double precision [0,1]
	 */
	private double getPrecision(LinkSpec spec) {
		Double res = 0d;
		if(spec.isEmpty()) {
			logger.info("getPrecision of empty LS: return 0");
			return 0;
		}
			
//		logger.info("Executing LinkSpec "+spec);
		Mapping mapping = engine.runNestedPlan(planner.plan(spec));
		res = pfm.getPseudoPrecision(evalData.getSourceCache().getAllUris(),
				evalData.getTargetCache().getAllUris(), mapping);
		logger.info("Executed Spec\n"+spec+"\nMapping size is "+mapping.size()+ " Precision="+res);
		return res;
	}
	/**
	 * Method computes PFM on an LinkSpec
	 * @param spec
	 * @return Double PFM [0,1]
	 */
	private double getPseudoFMeasure(LinkSpec spec, String type) {
		Double res = 0d;
		//first get the real mapping according to the specification
		Mapping mapping = engine.runNestedPlan(planner.plan(spec));
		if(spec.isEmpty()) {
				logger.info("getScore of empty LS: return 0");
				return 0;
			}
				
			res = pfm.getPseudoFMeasure(evalData.getSourceCache().getAllUris(),
					evalData.getTargetCache().getAllUris(), mapping, 
					1d);
			logger.info("Executed Spec\n"+spec+"\nMapping size is "+mapping.size()+ " PFM="+res);
		return res;
	}
	
	/**
	 * Method to create executrion engine
	 */
	private void setUpExperiment() {
		pfm = new PseudoMeasures();
		heuristic.setEvaluationData(evalData);
		operator.setEvalData(evalData);
		// are source/targetVars neccessary?
		engine = new ExecutionEngine(evalData.getSourceCache(), 
				evalData.getTargetCache(), 
				evalData.getConfigReader().sourceInfo.var, 
				evalData.getConfigReader().targetInfo.var);
	}
	////////////////////////////////////////
	boolean checkTreeScores(double upperlimit)
	{
		for (SearchTreeNode treeNode : nodes) {
			if(treeNode.getScore() < upperlimit ) 
				return false;
		}
		return true;
	}
	SearchTreeNode getMaxNode()
	{
		double maxScore=-1;
		SearchTreeNode maxTreeNode = null;
		for (SearchTreeNode treeNode : nodes) {
			if(treeNode.getScore() > maxScore ) 
				{
					maxTreeNode = treeNode;
					maxScore = treeNode.getScore();
				}
		}
		return maxTreeNode;
	}
	///////////////////////////////////////
	private double getMaxPseudoFMeasure(LinkSpec spec) {
		double max_prec = 0;
		double max_rec = 0;
		
		
		// first get actual Mapping
		Mapping actMapping = engine.runNestedPlan(planner.plan(spec));
		// all source instances with atleast 1 map partner
		double lambda_ij = actMapping.map.keySet().size();
		// all target instances with atleast 2 map patner
		double lambda_ji = actMapping.reverseSourceTarget().map.keySet().size();
	
		if(lambda_ij > lambda_ji) {
//			logger.error("Switching lambdas: lambda_ij="+lambda_ij+" lambda_ji="+lambda_ji+". Switching now...");
			double mem = lambda_ij;
			lambda_ij = lambda_ji;
			lambda_ji = mem;
		}
		double K_i = evalData.getSourceCache().getAllUris().size();
		double K_j = evalData.getTargetCache().getAllUris().size();
		double M_ij = actMapping.size();
//		logger.error("K_i="+K_i+"  K_j="+K_j+" M_ij="+M_ij);
		double test_1 = (2*K_i + 2*lambda_ji - 2*actMapping.size())/(K_i+K_j);
//		logger.error("test_1="+test_1);
		
		if((beta*beta) > test_1 ){			
//			logger.error("Using 1st definition of maxPFM:");
			max_prec = (K_i+K_j) / (2*(M_ij + K_j - lambda_ji));
			max_rec = 1;
		}
		else {			
//			logger.error("Using 2nd definition of maxPFM:");
			max_prec = (2*K_i + lambda_ji - lambda_ij) / (2*(M_ij+K_i-lambda_ij));
			max_rec = (2*K_i + lambda_ji - lambda_ij)/(K_i+K_j);
		}
	
		double max_PFM = (1+beta*beta)*max_prec / ((beta*beta*max_prec) + max_rec);
//		logger.error("Computed max_PFM="+max_PFM+" for a mapping of size "+M_ij+" over K_i="+K_i+" and K_j="+K_j );
		return max_PFM;
	}
private boolean maxAcievableFScore(LinkSpec spec, double bestAchievedPFM) {
		
	//	double realPFM = getPseudoFMeasure(spec,"pfm");
//		logger.error("Computing maximal achievable for a spec with actual PFM="+realPFM+" ...");
		// variables
		boolean answer = true;
		double max_PFM  = getMaxPseudoFMeasure(spec);
		if(max_PFM < bestAchievedPFM) {
			return false;
		}
		return answer;
	}
///////////////////////////////////////////////////////////////////////////////////
	public static void main(String args[]) {
		try {
//	            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p: %l: %m%n");
	            FileAppender fileAppender = new FileAppender(layout, "/home/mofeed/Desktop/MyLogFile"/*configFile.replaceAll(".xml", "")*/ + ".log", false);
	            fileAppender.setLayout(layout);
	            logger.addAppender(fileAppender);
	            logger.setLevel(Level.DEBUG);
	            logger.info("logger is workin");
	
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		

        
		EvaluationData data = DataSetChooser.getData(DataSets.DBLPACM);
		RefinementBasedLearningAlgorithmPrune algo = new RefinementBasedLearningAlgorithmPrune();
		algo.init(data);
		algo.start();
		
//		LinkSpec and = new LinkSpec();
//		and.operator= Operator.OR;
//		and.threshold = 0.4d;
//		
//		LinkSpec atom1 = new LinkSpec();
//		atom1.threshold = 0.5;
//		atom1.filterExpression = "levenshtein(x.title,y.title)";
//		atom1.parent = and;
//		and.addChild(atom1);
//		
//		LinkSpec atom2 = new LinkSpec();
//		atom2.threshold= 0.3d;
//		atom2.filterExpression = "trigrams(x.authors,y.authors)";
//		atom2.parent = and;
//		and.addChild(atom2);
		
//		algo.getPseudoFMeasure(and);
	}
}
