/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.lgg;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.lgg.refinementOperator.RefinementNode;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.selfconfig.Experiment;
import de.uni_leipzig.simba.selfconfig.LinearSelfConfigurator;

import java.util.*;

import de.uni_leipzig.simba.data.Tree;

import org.apache.log4j.Logger;


/**
 *
 * @author ngonga
 */
public class WeakLGG implements LGG {
	static Logger logger = Logger.getLogger(WeakLGG.class.getName());

	public double penaltyWeight = 0.5d;

	public static double MAX_FITNESS_THRESHOLD = 1;
	public static long MAX_TREE_SIZE = 200;
	public static int MAX_ITER_NR = 100;
	public static long CHILDREN_PENALTY_WEIGHT = 1;
	public static long COMPLEXITY_PENALTY_WEIGHT = 1;
	public boolean STRICT = true;
	public double MIN_THRESHOLD = 0.4;
	public double learningRate = 0.9;
	public boolean verbose = false;
	Map<String, Double> sourcePropertiesCoverageMap; //coverage map for latter computations
	Map<String, Double> targetPropertiesCoverageMap;//coverage map for latter computations

	double minCoverage;
	Cache source, target;
	Set<String> measures;
	Mapping reference;

	public Tree<RefinementNode> root = null;
	private List<ExtendedClassifier> classifiers = null;
	private int iterationNr = 0;

	private RefinementNode bestSolution;



	public enum Operator {
		AND, DIFF
	};

	/**
	 * ** TODO 
	 * 1- Get relevant source and target resources from sample 
	 * 2- Sample source and target caches 
	 * 3- Run algorithm on samples of source and target 
	 * 4- Get mapping function 
	 * 5- Execute on the whole
	 */
	/**
	 * Constructor
	 *
	 * @param source
	 * @param target
	 * @param examples
	 * @param minCoverage
	 */
	public WeakLGG(Cache source, Cache target, Mapping examples, double minCoverage) {
		sourcePropertiesCoverageMap = LinearSelfConfigurator.getPropertyStats(source, minCoverage);
		targetPropertiesCoverageMap = LinearSelfConfigurator.getPropertyStats(target, minCoverage);
		this.minCoverage = minCoverage;
		this.source = source;
		this.target = target;
		measures = new HashSet<>(Arrays.asList(
				"jaccard",
				"trigrams"
				//"cosine",
				//"hausdorff"
				));
		reference = examples;
	}

	public List<ExtendedClassifier> getAllInitialClassifiers() {
		logger.info("Generating initial classifiers ...");
		long start = System.currentTimeMillis();
		List<ExtendedClassifier> initialClassifiers = new ArrayList<>();
		for (String p : sourcePropertiesCoverageMap.keySet()) {
			for (String q : targetPropertiesCoverageMap.keySet()) {
				for (String m : measures) {
					if(verbose){
						logger.info("Getting classifier for " + p + ", " + q + ", " + m + ".");						
					}
					ExtendedClassifier cp = getInitialClassifier(p, q, m);
					//only add if classifier covers all entries
					initialClassifiers.add(cp);
				}
			}
		}
		logger.info("Done in " + (System.currentTimeMillis() - start) + "ms");
		logger.info("Initial classifiers: " + initialClassifiers);
		return initialClassifiers;
	}
	
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.lgg.LGG#getMapping()
	 */
	public Mapping getMapping() {
		if(bestSolution == null){
			bestSolution =  getBestSolution();
		}
		return bestSolution.map;
	}
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.lgg.LGG#getMetricExpression()
	 */
	@Override
	public String getMetricExpression() {
		if(bestSolution == null){
			bestSolution =  getBestSolution();
		}
		return bestSolution.metricExpression;
	}
	
	

	/**
	 * @return RefinementNode containing the best over all solution
	 * @author sherif
	 */
	public RefinementNode getBestSolution(){
		classifiers = getAllInitialClassifiers();
		createRefinementTreeRoot();
		Tree<RefinementNode> mostPromisingNode = getMostPromisingNode(root, penaltyWeight);
		logger.info("Most promising node: " + mostPromisingNode.getValue());
		iterationNr ++;
		while((mostPromisingNode.getValue().fMeasure) < MAX_FITNESS_THRESHOLD	 
				&& root.size() <= MAX_TREE_SIZE
				&& iterationNr <= MAX_ITER_NR)
		{
			iterationNr++;
			mostPromisingNode = expandNode(mostPromisingNode);
			mostPromisingNode = getMostPromisingNode(root, penaltyWeight);
			if(mostPromisingNode.getValue().recall == -Double.MAX_VALUE){
				break; // no better solution can be found
			}
			logger.info("Most promising node: " + mostPromisingNode.getValue());
		}
		RefinementNode bestSolution = getMostPromisingNode(root, 0).getValue();
		logger.info("Overall Best Solution: " + bestSolution);
		return bestSolution;
	}

	/**
	 * Computes the atomic classifiers by finding the highest possible F-measure
	 * achievable on a given property pair
	 *
	 * @param source Source cache
	 * @param target Target cache
	 * @param sourceProperty Property of source to use
	 * @param targetProperty Property of target to use
	 * @param measure Measure to be used
	 * @param reference Reference mapping
	 * @return Best simple classifier
	 */
	private ExtendedClassifier getInitialClassifier(String sourceProperty, String targetProperty, String measure) {
		double maxOverlap = 0;
		double theta = 1.0;
		Mapping bestMapping = new Mapping();
		PRFCalculator prf = new PRFCalculator();
		for (double threshold = 1d; threshold > MIN_THRESHOLD; threshold = threshold * learningRate) {
			Mapping mapping = execute(sourceProperty, targetProperty, measure, threshold);
			double overlap = prf.fScore(mapping, reference);
			if (maxOverlap < overlap){ //only interested in largest threshold with recall 1
				bestMapping = mapping;
				theta = threshold;
				maxOverlap = overlap;
				bestMapping = mapping;
				//				if(verbose){
				//					logger.info("Works for " + sourceProperty + ", " + targetProperty + ", " + threshold + ", " + overlap);
				//				}
			}
		}
		ExtendedClassifier cp = new ExtendedClassifier(measure, theta);
		cp.fMeasure = maxOverlap;
		cp.sourceProperty = sourceProperty;
		cp.targetProperty = targetProperty;
		cp.mapping = bestMapping;
		return cp;
	}

	//runs an atomic mapper
	public Mapping execute(String sourceProperty, String targetProperty, String measure, double threshold) {
		String measureExpression = measure + "(x." + sourceProperty + ", y." + targetProperty + ")";
		Instruction inst = new Instruction(Instruction.Command.RUN, measureExpression, threshold + "", -1, -1, -1);
		ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
		return ee.executeRun(inst);
	}



	/**
	 * initiate the refinement tree as a root node  
	 * with one child node contains a union of all initial classifiers
	 * @return
	 * @author sherif
	 */
	private void createRefinementTreeRoot(){
		RefinementNode initialNode = new RefinementNode();
		root = new Tree<RefinementNode>(null,initialNode, null);
		Mapping unionMapping = classifiers.get(0).mapping;
		String unionMetricExpr =  classifiers.get(0).getMetricExpression();
		for (int i = 1; i < classifiers.size(); i++) {
			unionMapping = SetOperations.union(unionMapping, classifiers.get(i).mapping);
			unionMetricExpr = "OR(" + unionMetricExpr + "," + classifiers.get(i).getMetricExpression() + ")|0";
//			System.out.println(unionMetricExpr);
//			System.out.println(unionMapping.size());
		}
		PRFCalculator prf = new PRFCalculator();
		double p = prf.precision(unionMapping, reference);
		double r = prf.recall(unionMapping, reference);
//		System.out.println(unionMapping.size());
		RefinementNode n = new RefinementNode(unionMapping, unionMetricExpr,reference);
		root.addChild(new Tree<RefinementNode>(root,n, null));
		if(verbose){
			root.print();
		}
	}

	/**
	 * Expand an input refinement node by applying 
	 * all available operators to the input refinement 
	 * node's mapping with all other classifiers' mappings
	 *   
	 * @param node Refinement node to be expanded
	 * @return The input tree node after expansion
	 * @author sherif
	 */
	private Tree<RefinementNode> expandNode(Tree<RefinementNode> node) {
		Mapping map = new Mapping();
		for(ExtendedClassifier c : classifiers ){
			for(Operator op : Operator.values()){
				if(node.getValue().metricExpression != c.getMetricExpression()){ // do not create the same metricExpression again 
					if(op.equals(Operator.AND)){
						map = SetOperations.intersection(node.getValue().map, c.mapping);
					}else if(op.equals(Operator.DIFF)){
						map = SetOperations.difference(node.getValue().map, c.mapping);
					}
					String metricExpr = op + "(" + node.getValue().metricExpression + "," + c.getMetricExpression() +")|0";
//					System.out.println("node.getValue().map: " + node.getValue().map);
//					System.out.println(metricExpr);
//					System.out.println("map.size(): " + map.size());
					double p = (new PRFCalculator()).precision(map, reference);
					double r = (new PRFCalculator()).recall(map, reference);
					RefinementNode child = new RefinementNode(map, metricExpr,reference);
					node.addChild(new Tree<RefinementNode>(child));
				}
			}
		}
		if(verbose){
			root.print();
		}
		return node;
	}


	/**
	 * Get the most promising node as the node with the best recall then precision
	 *  
	 * @param r The whole refinement tree
	 * @param penaltyWeight 
	 * @return most promising node from the input tree r
	 * @author sherif
	 */
	private Tree<RefinementNode> getMostPromisingNode(Tree<RefinementNode> r, double penaltyWeight){
		// trivial case
		if(r.getchildren() == null || r.getchildren().size() == 0){
			return r;
		}
		// get mostPromesyChild of children
		Tree<RefinementNode> mostPromesyChild = new Tree<RefinementNode>(new RefinementNode());
		for(Tree<RefinementNode> child : r.getchildren()){
			if(child.getValue().recall >= 0){
				Tree<RefinementNode> promesyChild = getMostPromisingNode(child, penaltyWeight);
				double newFitness;
				newFitness = promesyChild.getValue().recall - penaltyWeight * computePenality(promesyChild);
				if( newFitness > mostPromesyChild.getValue().recall  ){
					mostPromesyChild = promesyChild;
				}
			}
		}
		// return the argmax{root, mostPromesyChild}
		if(penaltyWeight > 0){
			return mostPromesyChild;
		}else if(r.getValue().fMeasure >= mostPromesyChild.getValue().fMeasure){
			return r;
		}else{
			return mostPromesyChild;
		}
	}


	/**
	 * @return 
	 * @author sherif
	 */
	private double computePenality(Tree<RefinementNode> promesyChild) {
		long childrenCount = promesyChild.size() - 1;
		double childrenPenalty = (CHILDREN_PENALTY_WEIGHT * childrenCount) / root.size();
		long level = promesyChild.level();
		double complextyPenalty = (COMPLEXITY_PENALTY_WEIGHT * level) / root.depth();
		return  childrenPenalty + complextyPenalty;
	}


	public static void main(String args[]) {
		//DBLP-ACM
		Cache source = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP2.csv");
		Cache target = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-ACM/ACM.csv");
		Mapping reference = Experiment.readReference("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv");
		//		Cache source = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-Scholar/DBLP1.csv");
		//		Cache target = Experiment.readFile("Examples/GeneticEval/Datasets/DBLP-Scholar/Scholar.csv");
		//		Mapping reference = Experiment.readReference("Examples/GeneticEval/Datasets/DBLP-Scholar/DBLP-Scholar_perfectMapping.csv");
		WeakLGG wlgg = new WeakLGG(source, target, reference, 0.6);
		Mapping result = wlgg.getMapping();
		//		System.out.println(result);
//		System.out.println("scource=" + source.size() + " target=" + target.size() + "ref= " + reference.size());
//
//		PRFComputer prf = new PRFComputer();
//		System.out.println("Precision = " + prf.computePrecision(result, reference));
//		System.out.println("Recall = " + prf.computeRecall(result, reference));
//		System.out.println("F-measure = " + prf.computeFScore(result, reference));
	}


}
