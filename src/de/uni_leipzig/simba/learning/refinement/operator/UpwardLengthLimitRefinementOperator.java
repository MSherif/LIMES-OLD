package de.uni_leipzig.simba.learning.refinement.operator;


import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.measures.string.CosineMeasure;
import de.uni_leipzig.simba.measures.string.JaccardMeasure;
import de.uni_leipzig.simba.measures.string.Levenshtein;
import de.uni_leipzig.simba.measures.string.StringMeasure;
import de.uni_leipzig.simba.measures.string.TrigramMeasure;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;
import de.uni_leipzig.simba.util.SetUtilities;
/**
 * Default implementation of the length limited refinement operator
 * @author Klaus Lyko
 */
public class UpwardLengthLimitRefinementOperator 
//		extends UpwardRefinementOperator 
		implements LengthLimitedRefinementOperator{
	 static Logger logger = Logger.getLogger("LIMES");
	 
	 EvaluationData evalData;
	 
	ThresholdDecreaser thresDec = new ThresholdDecrement();
	
	static Set<StringMeasure> stringMeasures = new HashSet<StringMeasure>();

	Double[] intialThresholds = {1.0d};
	
	public UpwardLengthLimitRefinementOperator(){
			stringMeasures = new HashSet<StringMeasure>();
			stringMeasures.add(new TrigramMeasure());
			stringMeasures.add(new Levenshtein());	
			stringMeasures.add(new CosineMeasure());
//			stringMeasures.add(new OverlapMeasure());
			stringMeasures.add(new JaccardMeasure());
	}
	
	@Override
	public Set<LinkSpec> refine(LinkSpec spec, int maxLength) {
		Set<LinkSpec> linkSpecs = new HashSet<LinkSpec>();
		
		/*root/bottom case: all atomics with 1d threshold
		*/
		if((spec == null || spec.isEmpty() ) && maxLength<=1) {
			for(LinkSpec ls: getAllAtomicMeasures(intialThresholds)) {
				linkSpecs.add(ls);
			}
			logger.error("Refined root to the first time. Created "+linkSpecs.size()+" children (atomic specs).");
		} 
		/*atomic at maxLength1: Threshold decrease or OR
		 */
		if( !spec.isEmpty() && spec.isAtomic() ) {
//			logger.info("Have to refine atomic LS "+spec);
			
			if(maxLength<=1) {
				Set<Double> thresholds = thresDec.decrease(spec);
				logger.info("Refining atomicspec into "+thresholds.size()+" new specs: "+spec.getShortendFilterExpression()+"|"+spec.threshold+" with threshold decrease. MaxLength= "+maxLength);
				for(Double thres : thresholds) {
					LinkSpec child = spec.clone();
					child.threshold = thres;
					linkSpecs.add(child);
				}
			} else {
//				logger.error("Creating disjuntions of size "+maxLength);
				List<LinkSpec> atoms = getAllAtomicsExcept(spec);
				if(maxLength<3) {// only create disjunctions at length 2
					for(LinkSpec child : atoms) {
	
						LinkSpec disjunction = new LinkSpec();
						disjunction.operator = Operator.OR;
						LinkSpec oldChild = spec.clone();
						oldChild.parent = disjunction;
						disjunction.addChild(oldChild);
						disjunction.threshold = oldChild.threshold;
						if(disjunction.threshold>child.threshold) {
							disjunction.threshold = child.threshold;
						}
						child.parent = disjunction;
						disjunction.addChild(child);
						if(disjunction.size()>=2)
							linkSpecs.add(disjunction);
					}
				} else { // create disjunctions of size >= 3
					Set<LinkSpec> conjunctions =  generateAllConjunctions(maxLength, Operator.OR);
					logger.error("Expanding root with maxlength="+maxLength+" into "+conjunctions.size()+" OR");
					for(LinkSpec conjunction : conjunctions)
						linkSpecs.add(conjunction);
					return linkSpecs;
				}
				logger.error("Creating disjuntions of size "+maxLength+" result: "+linkSpecs.size()+" specs");
			}
			return linkSpecs;
		} //end atomics
		/***/
	
		else
		/* bottom again with expansion >= 2
		 * Create all possible conjunctions (of size maxlength)  of atomic measure*/
		if((spec== null || spec.isEmpty()) && maxLength >= 2) {
			Set<LinkSpec> conjunctions =  generateAllConjunctions(maxLength, Operator.AND);
			logger.error("Expanding root with maxlength="+maxLength+" into "+conjunctions.size()+" ANDs");
			for(LinkSpec conjunction : conjunctions)
				linkSpecs.add(conjunction);
			return linkSpecs;
		}
		
		/*recursive AND case: spec!=atomic, maxLength <= 2
		 *  new Root: same operator as spec AND
		 * 		refine each child: recursivRefined, should decrease thresholds
		 */
		if(!spec.isAtomic() && spec.operator==Operator.AND) {
			logger.error("Refining complex AND LS with conjunction: "+spec+"");
			for(int i = 0; i<spec.children.size(); i++) {
//				System.out.println("Generating "+i+"th new Root for "+spec.children.get(i));
			
				Set<LinkSpec> recursivRefined = refine(spec.children.get(i), maxLength-1);
				for(LinkSpec recLS : recursivRefined) {
					System.out.println(i+"\tCreating LS for recursive "+recLS);
					LinkSpec newRoot = new LinkSpec();
					newRoot.operator = spec.operator;
					newRoot.threshold = spec.threshold;
					for(int j = 0; j<spec.children.size(); j++) {
						if(i!=j) {
							LinkSpec cloneJ = spec.children.get(j).clone();
							cloneJ.parent = newRoot;
							newRoot.addChild(cloneJ);
							if(newRoot.threshold > cloneJ.threshold)
								newRoot.threshold = cloneJ.threshold;
						}
					}
					recLS.parent = newRoot;
					newRoot.addChild(recLS);
					if(newRoot.threshold > recLS.threshold)
						newRoot.threshold = recLS.threshold;
					
					boolean added = linkSpecs.add(newRoot);
//					System.out.println(added+" added "+newRoot);
				}// for all refinements
			}// for all children of AND
			return linkSpecs;
		}

		/*recursive OR case: spec!=atomic, maxLength >= 2
		 *  new Root: same operator as spec
		 * 		refine each child: recursivRefined
		 */
		if(spec.operator == Operator.OR) {
			logger.debug("Attempting to expand OR");
			if(spec.children==null || spec.children.size()==0)
				return linkSpecs;
			if(maxLength <= 2) {
				logger.debug("Refining complex OR LS with length "+spec.size()+" and maxLength "+maxLength+" by refining a child.");
				for(int i = 0; i<spec.children.size(); i++) {// forall children
					LinkSpec orgChild = spec.children.get(i);
					Set<LinkSpec> recursivRefined = refine(orgChild, maxLength-1);
					for(LinkSpec recLS : recursivRefined) {// forall refinements of child
						// add all other children
						LinkSpec newRoot = new LinkSpec(); // copy OR
						newRoot.operator = spec.operator;
						newRoot.threshold = spec.threshold;
						for(int j = 0; j<spec.children.size(); j++)
							if(j!=i) {
								LinkSpec cloneJ = spec.children.get(j).clone();
								cloneJ.parent = newRoot;
								newRoot.addChild(cloneJ);
							}
						recLS.parent = newRoot;
						newRoot.addChild(recLS);
						if(newRoot.threshold<recLS.threshold) 
							newRoot.threshold = recLS.threshold;
						linkSpecs.add(newRoot);
					}// for all refinements of this node
				} // for all children of OR
			}// maxlength<=2
			else {
				logger.debug("Refining complex OR LS with length "+spec.size()+" and maxLength "+maxLength+ " by adding new atomic.");
				List<LinkSpec> children = spec.getAllLeaves();
				List<LinkSpec> allOthers;
				if(children.size()>=1)
					 allOthers = getAllAtomicsExcept(children.get(0));
				else 
					allOthers = getAllAtomicMeasures(intialThresholds);
				for(LinkSpec newChild : allOthers) {
					boolean valid = true;
					for(int i = 1; i<children.size(); i++) {
						LinkSpec child = children.get(i);
						if(child.isAtomic()) {
							if(//child.getAtomicMeasure().equalsIgnoreCase(newChild.getAtomicMeasure()) &&
								 child.prop1.equalsIgnoreCase(newChild.prop1) &&
									child.prop2.equalsIgnoreCase(newChild.prop2)) {
								valid = false;
							}
						}
					}
					if(valid) {
						LinkSpec newDisjunction = spec.clone();
						for(LinkSpec child:children) {
							LinkSpec copy = child.clone();
							copy.parent = newDisjunction;
							newDisjunction.addChild(copy);
						}
							
						newChild.parent = newDisjunction;
						newDisjunction.addChild(newChild);
						if(newDisjunction.size()>=2)
							linkSpecs.add(newDisjunction);
					}
				}
			}
			logger.debug("Refined Or into "+linkSpecs.size()+" specs");
			return linkSpecs;			
		}		
		return linkSpecs;
	}
	private List<LinkSpec> getAllAtomicMeasures(Double[] thresholds) {
		List<LinkSpec> linkSpecs = new LinkedList<LinkSpec>();
		/*get all mapping properties*/
		PropertyMapping propMapper = evalData.getPropertyMapping();
		Mapping propMap = propMapper.getCompletePropMapping();
	
		String sourceVar = evalData.getConfigReader().sourceInfo.var;
		if(sourceVar.startsWith("?")&& sourceVar.length()>=2)
			sourceVar = sourceVar.substring(1);
		String targetVar = evalData.getConfigReader().targetInfo.var;
		if(targetVar.startsWith("?")&& targetVar.length()>=2)
			targetVar = targetVar.substring(1);
		for(Double threshold : thresholds)
		for(String prop1 : propMap.map.keySet()) {
			for(String prop2 : propMap.map.get(prop1).keySet()) {
				for(Measure m : stringMeasures) {
						//for each (Propertypair x Measure) tupel: create an atomic LinkSpec
						LinkSpec child = new LinkSpec();
//						child.operator = Operator.
						child.setAtomicFilterExpression(m.getName(), sourceVar+"."+prop1, targetVar+"."+prop2);
//												child.filterExpression=m.getName()+"("+sourceVar+"."+prop1+","+targetVar+"."+prop2+")";
						child.threshold = threshold;
						linkSpecs.add(child);
				}//end for measure					
			}
		}//end each source Prop
		logger.info("Created "+linkSpecs.size()+" atomic measures");
		return linkSpecs;
	}

	/**
	 * Creates all other atomic measures for current EvaluationData except those over the same properties as the atomic LinkSpec ls0
	 * @param ls0
	 * @return
	 */
	private List<LinkSpec> getAllAtomicsExcept(LinkSpec ls0 ) {
		List<LinkSpec> linkSpecs = new LinkedList<LinkSpec>();
		/*get all mapping properties*/
		PropertyMapping propMapper = evalData.getPropertyMapping();
		Mapping propMap = propMapper.getCompletePropMapping();
	
		String sourceVar = evalData.getConfigReader().sourceInfo.var;
		if(sourceVar.startsWith("?")&& sourceVar.length()>=2)
			sourceVar = sourceVar.substring(1);
		String targetVar = evalData.getConfigReader().targetInfo.var;
		if(targetVar.startsWith("?")&& targetVar.length()>=2)
			targetVar = targetVar.substring(1);
		for(Double threshold : new Double[]{1d, 0.5d})
		for(String prop1 : propMap.map.keySet()) {
			for(String prop2 : propMap.map.get(prop1).keySet()) {
				for(Measure m : stringMeasures) {
					
						//for each (Propertypair x Measure) tupel: create an atomic LinkSpec
						if(!ls0.getAtomicMeasure().trim().equalsIgnoreCase(m.getName().trim()) && (
								!ls0.prop1.equalsIgnoreCase(sourceVar+"."+prop1) ||
								!ls0.prop2.equalsIgnoreCase(targetVar+"."+prop2)))
						{
							LinkSpec child = new LinkSpec();
	//						child.operator = Operator.
							child.setAtomicFilterExpression(m.getName(), sourceVar+"."+prop1, targetVar+"."+prop2);
							//						child.filterExpression=m.getName()+"("+sourceVar+"."+prop1+","+targetVar+"."+prop2+")";
							child.threshold = threshold;
//							System.out.println("Get all Atoms expect "+ls0+"\n\t" +
//									"returning "+m.getName()+"("+sourceVar+"."+prop1+","+targetVar+"."+prop2+")");
							linkSpecs.add(child);
						}else{
//							System.out.println("Get all Atoms expect "+ls0+"\n\t" +
//									"not returning "+m.getName()+"("+sourceVar+"."+prop1+","+targetVar+"."+prop2+")");
						}
					
				}//end for measure					
			}
		}//end each source Prop
//		logger.info("Created "+linkSpecs.size()+" atomic measures");
		return linkSpecs;
	}
	
	@Override
	public void setEvalData(EvaluationData evalData) {
		this.evalData = evalData;
	}

	/**
	 * Method to generate all LinkSpecs of size >=2 using conjunctions.
	 * @param size
	 * @return
	 */
	private Set<LinkSpec> generateAllConjunctions(int size, Operator op) {
		Double[] atomsThres = { 1d };
		List<LinkSpec> atomics = getAllAtomicMeasures(atomsThres);
		Set<LinkSpec> allAtomics = new HashSet<LinkSpec>();
		allAtomics.addAll(atomics);
		if(atomics.size() != allAtomics.size())
			logger.warn("Casting list of all atomic "+atomics.size()+" specs into set resulted only in"+allAtomics.size());
		Set<LinkSpec> specs = new HashSet<LinkSpec>();
		logger.info("create all conjunctions of size " + Math.min(atomics.size(), size));
		for(Set<LinkSpec> set : SetUtilities.sizeRestrictPowerSet(allAtomics, Math.min(5, size))) {
			LinkSpec conjunction = new LinkSpec();
			conjunction.operator = op;
			double threshold = 0d;
			if(set.size()<=1)
				continue;
			for(LinkSpec child : set) {
//				child.threshold = 0.9; //FIXME Why that????
				child.parent = conjunction;
				conjunction.addChild(child);
				threshold += child.threshold;
			}
			if(conjunction.containsRedundantProperties()) // avoid different children over same properties
				continue;
			conjunction.threshold = threshold / set.size();
			boolean success = specs.add(conjunction);
			if(!success)
				logger.warn("Could'nt add conjunction as it already exists.");
		}	
		logger.info("Created "+specs.size()+" LinkSpec Conjunction of size "+size);
		return specs;
	}
	
	public static void main(String args[]) {
		UpwardLengthLimitRefinementOperator op = new UpwardLengthLimitRefinementOperator();
		op.setEvalData(DataSetChooser.getData(DataSets.DBLPACM));
		logger.setLevel(Level.INFO);
		LinkSpec spec = new LinkSpec();
		op.testRefineMentAnd();
	}
	
	/**
	 * Method generates the number of thresholds in [low,high]
	 * @param low lowest threshold
	 * @param high highest threshold
	 * @param number specifies the number of different thresholds.
	 * @return List of size number of doubled valued thresholds in [low,high]
	 */
	public static List<Double> allOptimizedThresholds(double low, double high, int number) {
		double range = high - low;
		
		double steps = range / number;
		double t = low;
		List<Double> thresh = new LinkedList<Double>();
		if(range < 0.01) {
					thresh.add(high);
					return thresh;
		}
		for(int i = 1; i<= number; i++) {
			t = t + steps;
			thresh.add(t);
		}
		return thresh;
	}
//	@Test
//	public void testRefineMent2() {
//		UpwardLengthLimitRefinementOperator op = new UpwardLengthLimitRefinementOperator();
//		
//		LinkSpec ls = new LinkSpec();
//		ls.operator = Operator.OR;
//		ls.threshold = 1;
//		LinkSpec ch1 = new LinkSpec();
//		ch1.threshold = 1;
//		ch1.setAtomicFilterExpression("trigrams", "x.prop1", "y.prop1");
//		LinkSpec ch2 = new LinkSpec();
//		ch2.threshold = 0.8;
//		ch2.setAtomicFilterExpression("trigrams", "x.prop2", "y.prop2");
//		ls.addChild(ch1);
//		ls.addChild(ch2);
//		Set<LinkSpec> list = op.refine(ls, 2);
//		System.out.println(list.size() == 1);
//		LinkSpec ref = list.iterator().next();
//		System.out.println(ref);
//		assertTrue(ref.children.size() == 2);
//	}
	@Test
	public void testRefineMentAnd() {
		UpwardLengthLimitRefinementOperator op = new UpwardLengthLimitRefinementOperator();

		EvaluationData data = DataSetChooser.getData(DataSets.DBLPACM);
		op.setEvalData(data);
		
		
		LinkSpec ls = new LinkSpec();
		ls.operator = Operator.AND;
		ls.threshold = 1;
		LinkSpec ch1 = new LinkSpec();
		ch1.threshold = 1;
		ch1.setAtomicFilterExpression("trigrams", "x.title", "y.title");
		LinkSpec ch2 = new LinkSpec();
		ch2.threshold = 0.8;
		ch2.setAtomicFilterExpression("cosine", "x.authors", "y.authors");
		ls.addChild(ch1);
		ls.addChild(ch2);
		Set<LinkSpec> list = op.refine(ls, 4);
		System.out.println("list.size()="+list.size());
		LinkSpec ref = list.iterator().next();
		System.out.println(ref);
		assertTrue(ref.children.size() == 2);
	}
	
}
