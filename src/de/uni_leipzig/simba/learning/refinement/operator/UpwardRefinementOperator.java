package de.uni_leipzig.simba.learning.refinement.operator;

import java.util.HashSet;
import java.util.Set;

import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.measures.string.CosineMeasure;
import de.uni_leipzig.simba.measures.string.Levenshtein;
import de.uni_leipzig.simba.measures.string.StringMeasure;
import de.uni_leipzig.simba.measures.string.TrigramMeasure;
import de.uni_leipzig.simba.specification.LinkSpec;

/**
 * Default implementation of a RefinementOperator. Starting from an empty LinkSpecification
 * it generates LinkSpecification oh height 1, e.g. atomic Operators between 2 Properties.
 * @author Klaus Lyko
 *
 */
@Deprecated
public class UpwardRefinementOperator implements RefinementOperator {

	ThresholdDecreaser thresDec = new ThresholdDecrement();
	
	static Set<StringMeasure> stringMeasures = new HashSet<StringMeasure>();
	{
		stringMeasures.add(new CosineMeasure());
		stringMeasures.add(new TrigramMeasure());
		stringMeasures.add(new Levenshtein());
	}
	
	PropertyMapping propMap;
	
	public UpwardRefinementOperator(PropertyMapping propMap) {
		this.propMap = propMap;
	}
	
	@Override//need expansion size paramter
	public Set<LinkSpec> refine(LinkSpec spec) {
		Set<LinkSpec> LinkSpecs = new HashSet<LinkSpec>();
		
//		if(spec == null || spec.size()<=0) { 
//			//produces AND(atomic1, atomic2, atomic3,...,atomicn)
//			LinkSpec root = null;
//			root = new LinkSpec();
//			root.threshold = 1d;
//			root.operator = Operator.AND;///FIXME create 
//			/*get all mapping properties*/
//			for(String prop1 : propMap.getStringPropMapping().map.keySet()) {
//				for(String prop2 : propMap.getStringPropMapping().map.get(prop1).keySet()) {
//					for(Measure m : stringMeasures) {
//							//for each (Propertypair x Measure) tupel: create an atomic LinkSpec
//							LinkSpec child = new LinkSpec();
//							child.filterExpression=m.getName()+"("+prop1+","+prop2+")";
//							child.threshold = 1d;
//							child.parent=root;
//							root.addChild(child);					
//					}//end for measure					
//				}
//			}//end each source Prop
//			
//			//root has now probably n>2 atomic children!
//			LinkSpecs.add(root);
//			return LinkSpecs;			
//		}
//		else if(spec.isAtomic()) {// atomics
//			spec.threshold = thresDec.decrease(spec).;
//			LinkSpecs.add(spec);
//		}
//		else if(spec.operator.name().equalsIgnoreCase(Operator.OR.name())) {// Disjunction
//			Random rand = new Random();
//			int i = rand.nextInt(spec.children.size());
//			LinkSpec org = spec.children.get(i);
//			LinkSpecs = refine(org);
//			return LinkSpecs;
//		}
//		else if(spec.operator.name().equalsIgnoreCase(Operator.AND.name())) {// Conjunction
//			// pick randomly LSi
//			Random rand = new Random();
//			int i = rand.nextInt(spec.children.size());
//			LinkSpec org = spec.children.get(i);
//			Set<LinkSpec> ref = refine(org);
//			// create disjunction LinkSpec
//			LinkSpec disjunction = new LinkSpec();
//			disjunction.operator=Operator.OR;
//			// randomly create a new Spec with a new Measure, same properties
//			LinkedList<Measure> measureList = new LinkedList<Measure>();
//			measureList.addAll(stringMeasures);
//			Measure newMeasure = measureList.get(rand.nextInt(measureList.size()));
//			String expr = org.filterExpression.substring(org.filterExpression.indexOf("("));
//			LinkSpec randomLinkSpec = new LinkSpec();
//			randomLinkSpec.filterExpression=newMeasure+expr;
//			randomLinkSpec.threshold=1d;
//			
//			disjunction.addChild(randomLinkSpec);
//			disjunction.addChild(org);			
//			disjunction.children.addAll(ref);
//			
//			LinkSpecs.add(disjunction);
//			return LinkSpecs;
//		}
		// failure/default emptyList
		return LinkSpecs;
	}

}
