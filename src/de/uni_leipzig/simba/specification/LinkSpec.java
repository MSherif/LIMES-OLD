/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.specification;

import static org.junit.Assert.*;
import de.uni_leipzig.simba.controller.Parser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 *
 * @author ngonga
 * @author Klaus Lyko
 */
public class LinkSpec implements Comparable {

    /**
     * just a quick hack to have lower borders for advanced threshold searches
     */
    public double lowThreshold = 0d;
    public double quality = 0d;
    static Logger logger = Logger.getLogger("LIMES");
    // children must be a list because not all operators are commutative
    public List<LinkSpec> children;
    public Operator operator;
    /*
     * If the LinkSpecification is atomic the measure and properties are this.
     * filterexpression: e.g. trigrams(s.label,t.label).
     */
    private String filterExpression;
    public double threshold;
    public LinkSpec parent;
    // dependencies are the list of specs whose result set is included in the
    // result
    // set of this node
    public List<LinkSpec> dependencies;

    private String atomicMeasure = ""; // eg. trigrams...
    public String prop1 = "";
    public String prop2 = "";

    public String treePath = "";
    
    public void setAtomicFilterExpression(String atomicMeasure, String prop1, String prop2) {
	this.setAtomicMeasure(atomicMeasure);
	this.prop1 = prop1;
	this.prop2 = prop2;
	this.filterExpression = atomicMeasure + "(" + prop1 + "," + prop2 + ")";
    }

    public LinkSpec() {
	operator = null;
	children = null;
	threshold = -1;
	parent = null;
	dependencies = null;
    }

    /**
     * Creates a spec with a measure read inside
     * 
     * @param measure
     *            String representation of the spec
     */
    public LinkSpec(String measure, double threshold) {
	operator = null;
	children = null;
	parent = null;
	dependencies = null;
	readSpec(measure, threshold);
    }
    /**
     * Creates a spec with a measure read inside
     * 
     * @param measure
     *            String representation of the spec
     */
    public LinkSpec(String measure, double threshold, boolean flag) {
	operator = null;
	children = null;
	parent = null;
	dependencies = null;
	readSpecXOR(measure, threshold);
	
    }
    /**
     * Adds a child to the current node of the spec
     * 
     * @param spec
     */
    public void addChild(LinkSpec spec) {
	if (children == null)
	    children = new ArrayList<LinkSpec>();
	children.add(spec);
    }

    /**
     * Adds a child to the current node of the spec
     * 
     * @param spec
     */
    public void addDependency(LinkSpec spec) {
	if (dependencies == null)
	    dependencies = new ArrayList<LinkSpec>();
	dependencies.add(spec);
    }

    /**
     * Removes a dependency from the list of dependencies
     * 
     * @param spec
     *            Input spec
     */
    public void removeDependency(LinkSpec spec) {
	if (dependencies.contains(spec)) {
	    dependencies.remove(spec);
	}
	if (dependencies.isEmpty())
	    dependencies = null;
    }

    /**
     * Checks whether a spec has dependencies
     * 
     */
    public boolean hasDependencies() {
	if (dependencies == null)
	    return false;
	return (!dependencies.isEmpty());
    }

    /**
     *
     * @return True if the spec is empty, all false
     */
    public boolean isEmpty() {
	if (threshold <= 0)
	    return (threshold <= 0);
	if (filterExpression == null && (children == null || children.isEmpty()))
	    return true;
	return false;
    }

    /**
     *
     * @return True if the spec is a leaf (has no children), else false
     */
    public boolean isAtomic() {
	if (children == null)
	    return true;
	return children.isEmpty();
    }
    /**
     * Returns all leaves of the link spec
     * 
     * @return List of atomic spec, i.e., all leaves of the link spec
     */
    public void getAllChildren() {
	for (LinkSpec child : children) {
	    if(this.operator == Operator.OR){
		logger.info(this.parent);
		this.parent = child;
		logger.info(this.parent);
	    }
	    if(!child.isAtomic())
		child.getAllChildren();
	}
	
    }
    
    /**
    *
    * Create the path of operators for each leaf spec
    */
    public void pathOfAtomic() {
	if (this.isAtomic())
	    treePath += "";
	else {
	    if (children != null) {
		for (LinkSpec child : children) {
		    String parentPath = this.treePath;
		    if (child == children.get(0)) {
			child.treePath = parentPath + ": " + operator + "->left";
		    } else {
			child.treePath = parentPath + ": " + operator + "->right";
		    }
		    child.pathOfAtomic();
		}
	    }
	}

    }
    /**
     * Reads a spec expression into its canonical form Don't forget to optimize
     * the filters by checking (if threshold_left and threshold_right >= theta,
     * then theta = 0)
     *
     * @param spec
     *            Spec expression to read
     * @param theta
     *            Global threshold
     */
    public void readSpecXOR(String spec, double theta) {
	
	Parser p = new Parser(spec, threshold);
	if (p.isAtomic()) {
	    filterExpression = spec;
	    threshold = theta;
	} else {
	    LinkSpec leftSpec = new LinkSpec();
	    LinkSpec rightSpec = new LinkSpec();
	    leftSpec.parent = this;
	    rightSpec.parent = this;
	    children = new ArrayList<LinkSpec>();
	    children.add(leftSpec);
	    children.add(rightSpec);

	    if (p.getOperation().equalsIgnoreCase("AND")) {
		operator = Operator.AND;
		leftSpec.readSpecXOR(p.term1, p.threshold1);
		rightSpec.readSpecXOR(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MIN")) {
		operator = Operator.AND;
		leftSpec.readSpecXOR(p.term1, theta);
		rightSpec.readSpecXOR(p.term2, theta);
		filterExpression = null;
		threshold = theta;

	    } else if (p.getOperation().equalsIgnoreCase("OR")) {
		operator = Operator.OR;
		leftSpec.readSpecXOR(p.term1, p.threshold1);
		rightSpec.readSpecXOR(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MAX")) {
		operator = Operator.OR;
		leftSpec.readSpecXOR(p.term1, theta);
		rightSpec.readSpecXOR(p.term2, theta);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("XOR")) {
		operator = Operator.MINUS;
		leftSpec.readSpecXOR("OR("+p.term1+"|"+p.threshold1+","+p.term2+"|"+p.threshold2+")", theta);
		rightSpec.readSpecXOR("AND("+p.term1+"|"+p.threshold1+","+p.term2+"|"+p.threshold2+")", theta);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MINUS")) {
		operator = Operator.MINUS;
		leftSpec.readSpecXOR(p.term1, p.threshold1);
		rightSpec.readSpecXOR(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("ADD")) {
		operator = Operator.AND;
		leftSpec.readSpecXOR(p.term1, (theta - p.coef2) / p.coef1);
		rightSpec.readSpecXOR(p.term2, (theta - p.coef1) / p.coef2);
		filterExpression = spec;
		threshold = theta;
	    }
	}
    }
    /**
     * Reads a spec expression into its canonical form Don't forget to optimize
     * the filters by checking (if threshold_left and threshold_right >= theta,
     * then theta = 0)
     *
     * @param spec
     *            Spec expression to read
     * @param theta
     *            Global threshold
     */
    public void readSpec(String spec, double theta) {

	Parser p = new Parser(spec, threshold);
	if (p.isAtomic()) {
	    filterExpression = spec;
	    threshold = theta;
	} else {
	    LinkSpec leftSpec = new LinkSpec();
	    LinkSpec rightSpec = new LinkSpec();
	    leftSpec.parent = this;
	    rightSpec.parent = this;
	    children = new ArrayList<LinkSpec>();
	    children.add(leftSpec);
	    children.add(rightSpec);

	    if (p.getOperation().equalsIgnoreCase("AND")) {
		operator = Operator.AND;
		leftSpec.readSpec(p.term1, p.threshold1);
		rightSpec.readSpec(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MIN")) {
		operator = Operator.AND;
		leftSpec.readSpec(p.term1, theta);
		rightSpec.readSpec(p.term2, theta);
		filterExpression = null;
		threshold = theta;

	    } else if (p.getOperation().equalsIgnoreCase("OR")) {
		operator = Operator.OR;
		leftSpec.readSpec(p.term1, p.threshold1);
		rightSpec.readSpec(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MAX")) {
		operator = Operator.OR;
		leftSpec.readSpec(p.term1, theta);
		rightSpec.readSpec(p.term2, theta);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("XOR")) {
		operator = Operator.XOR;
		leftSpec.readSpec(p.term1, p.threshold1);
		rightSpec.readSpec(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("MINUS")) {
		operator = Operator.MINUS;
		leftSpec.readSpec(p.term1, p.threshold1);
		rightSpec.readSpec(p.term2, p.threshold2);
		filterExpression = null;
		threshold = theta;
	    } else if (p.getOperation().equalsIgnoreCase("ADD")) {
		operator = Operator.AND;
		leftSpec.readSpec(p.term1, (theta - p.coef2) / p.coef1);
		rightSpec.readSpec(p.term2, (theta - p.coef1) / p.coef2);
		filterExpression = spec;
		threshold = theta;
	    }
	}
    }
    
    /**
     * Returns all leaves of the link spec
     * 
     * @return List of atomic spec, i.e., all leaves of the link spec
     */
    public List<LinkSpec> getAllLeaves() {
	List<LinkSpec> allLeaves = new ArrayList<LinkSpec>();
	if (isAtomic()) {
	    allLeaves.add(this);
	} else {
	    for (LinkSpec child : children) {
		allLeaves.addAll(child.getAllLeaves());
	    }
	}
	return allLeaves;
    }

    /**
     * Returns size of the spec, i.e., 1 for atomic spec, 0 for empty spec and
     * else 1 + sum of size of all children
     * 
     * @return Size of the current spec
     */
    public int size() {
	int size = 1;
	if (isEmpty()) {
	    return 0;
	}
	if (isAtomic()) {
	    return 1;
	} else {
	    for (LinkSpec c : children) {
		size = size + c.size();
	    }
	}
	return size;
    }

    /**
     * Computes a hashCode for the current spec
     * 
     * @return Hash code
     */
    public int hashCode() {
	int res = new Random().nextInt();
	// if(this.isEmpty())
	// return 0;
	// if(this.isAtomic())
	// res =
	// filterExpression.hashCode()+Long.valueOf(Double.doubleToLongBits(threshold)).hashCode();
	return res;
	//
	//
	//
	// long bits = doubleToLongBits(thu);
	// return (int)(bits ^ (bits >>> 32));
	// return toString().hashCode();
	// return (int) System.currentTimeMillis();
    }

    /**
     * Generates a clone of the current spec
     * 
     * @return Clone of current spec
     */
    public LinkSpec clone() {
	LinkSpec clone = new LinkSpec();
	clone.threshold = threshold;
	clone.lowThreshold = lowThreshold;
	clone.operator = operator;
	clone.filterExpression = filterExpression;
	clone.prop1 = prop1;
	clone.prop2 = prop2;
	clone.atomicMeasure = atomicMeasure;
	List<LinkSpec> l = new ArrayList<LinkSpec>();
	LinkSpec childCopy;
	if (children != null)
	    for (LinkSpec c : children) {
		childCopy = c.clone();
		clone.addChild(childCopy);
		childCopy.parent = clone;
		l.add(childCopy);
	    }

	return clone;
    }
   

    /**
     *
     * @return A string representation of the spec
     */
    @Override
    public String toString() {
	// if (parent != null) {
	// if(children != null) {
	// String str = "(" + filterExpression + ", " + threshold + ", " +
	// operator + ", "+ parent.hashCode()+")";
	// for(LinkSpec child:children)
	// str +="\n ->"+child;
	// return str;
	// }
	//
	// else
	// return "(" + filterExpression + ", " + threshold + ", " + operator +
	// ", "+ parent.hashCode()+")";
	//// return "(" + filterExpression + ", " + threshold + ", " + operator
	// + ", " + parent.hashCode() +") -> " + children;
	// } else {
	if (children != null) {
	    String str = "(" + filterExpression + ", " + threshold + ", " + operator + ", null,)";
	    for (LinkSpec child : children) {

		str += "\n  ->" + child;
	    }
	    return str;
	}

	else
	    return "(" + filterExpression + ", " + threshold + ", " + operator + ", null)";
	// }
    }

    /**
     *
     * @return A string representation of the spec in a single line
     */
    public String toStringOneLine() {
	if (children != null) {
	    String str = "(" + getShortendFilterExpression() + ", " + threshold + ", " + operator + ", null,)";
	    str += "{";
	    for (LinkSpec child : children)
		str += child.toStringOneLine() + ",";
	    str += "}";
	    return str;
	}

	else
	    return "(" + getShortendFilterExpression() + ", " + threshold + ", " + operator + ", null)";
	// }
    }

    /**
     * Checks whether the current node is the root of the whole spec
     * 
     * @return True if root, else false
     */
    public boolean isRoot() {
	return (parent == null);
    }

    /**
     * Returns the filter expression implemented in the spec
     * 
     */
    public String getMeasure() {
	if (isAtomic())
	    return filterExpression;
	else {
	    return operator + "(" + ")";
	}
    }

    public static void main(String args[]) {
	LinkSpec spec = new LinkSpec();
	String l = "XOR(cosine(x.A,y.A)|0.7881,OR(trigrams(x.B,y.B)|0.5003,"
		+ "XOR(MINUS(overlap(x.C,y.C)|0.48,OR(overlap(x.D,y.D)|0.4733,"
		+ "cosine(x.E,y.E)|0.7063)|0.3552)|0.8884,cosine(x.F,y.F)|0.3)|0.3552)|0.6307)";
	// spec.readSpec("OR(trigrams(x.rdfs:label,y.name)|0.3,
	// jaro(x.rdfs:label, y.name)|0.5)", 0.8);
	spec.readSpec(l,0.4678);
	logger.info(spec.toString());
	spec.readSpecXOR(l,0.4678);
	logger.info(spec + " " + spec.size());
	// System.out.println(spec.clone());
	
	spec.pathOfAtomic();
	/*for(LinkSpec leaf:spec.getAllLeaves()){
	   logger.info(leaf);
	   logger.info(leaf.treePath);
	}*/
	spec.getAllChildren();
	
	List<LinkSpec> leaves = spec.getAllLeaves();
	int i = 0;
	for (LinkSpec sp : leaves) {
	    
	    System.out.println("Leave " + (++i) + ": " + sp);
	    Parser p = new Parser(sp.filterExpression, sp.threshold);
	    System.out.println("\tp.term1=" + p.term1 + " p.term2=" + p.term2 + "");
	    if (sp.parent.parent != null)
		System.out.println((sp.parent).parent.operator);
	    
	    String sourceProp = p.term1.substring(p.term1.indexOf(".") + 1);
	    String targetProp = p.term2.substring(p.term2.indexOf(".") + 1);

	    System.out.println("\tsourceProp=" + sourceProp + ", targetProp=" + targetProp+"\n\n");
	}
    }

    @Override
    public boolean equals(Object other) {
	LinkSpec o = (LinkSpec) other;

	if (this.isAtomic() && o.isAtomic()) {
	    if (this.filterExpression == null && o.filterExpression == null)
		return true;
	    if (this.filterExpression != null && o.filterExpression == null)
		return false;
	    if (this.filterExpression == null && o.filterExpression != null)
		return false;
	    if (this.filterExpression.equalsIgnoreCase(o.filterExpression))
		return Math.abs(this.threshold - o.threshold) < 0.001d;
	} else {
	    if (this.operator == null && o.operator == null)
		return true;
	    if (this.operator.equals(o.operator)) {
		// if(this.children.size()==o.children.size()) {
		HashSet<LinkSpec> hs = new HashSet<LinkSpec>();
		if (this.children != null)
		    hs.addAll(children);
		// System.out.println(hs);
		// boolean b = hs.addAll(o.children);
		// System.out.println(hs+ " " + b);
		if (o.children == null)
		    return true;
		return (!hs.addAll(o.children));
		// System.out.println(hs);
		//// boolean containsAll=true;
		// for(LinkSpec oChild:o.children) {
		//
		// if(!hs.contains(oChild)) {
		// System.out.println("Doesnt contain child"+oChild);
		// return false;
		// }else {
		// System.out.println("Does contain child"+oChild);
		// }
		// }
		// return true;
		// }
		// return false;
	    }
	    return false;

	}
	return false;

    }

    @Override
    public int compareTo(Object o) {

	LinkSpec other = (LinkSpec) o;

	// logger.info("LinkSpec.compareTo: this="+this+" -other="+other);
	if (other.size() > size())
	    return -1;
	if (other.size() < size())
	    return 1;
	if (this.isEmpty() && other.isEmpty())
	    return 0;
	// size is equal
	// if(!this.isAtomic() && !other.isAtomic()) {
	// return 0;
	// }
	if (this.isAtomic() && other.isAtomic()) {
	    if (this.threshold > other.threshold)
		return 1;
	    if (this.threshold < other.threshold)
		return -1;
	    if (this.filterExpression == null && other.filterExpression != null)
		return -1;
	    if (this.filterExpression != null && other.filterExpression == null)
		return 1;
	    if (this.filterExpression == null && other.filterExpression == null)
		return 0;
	    return this.filterExpression.compareToIgnoreCase(other.filterExpression);
	} else { // even size, non atomic
	    // System.out.println("Comparing operators returned
	    // "+(this.operator==other.operator));
	    if (this.operator == other.operator) {

		// same operators

		if (getAllLeaves().size() == other.getAllLeaves().size()) {
		    List<LinkSpec> leaves = getAllLeaves();
		    List<LinkSpec> otherLeaves = other.getAllLeaves();
		    for (int i = 0; i < leaves.size(); i++) {
			if (leaves.get(i).compareTo(otherLeaves.get(i)) != 0)
			    return leaves.get(i).compareTo(otherLeaves.get(i));
		    }
		    return 0;
		} else
		    return getAllLeaves().size() - other.getAllLeaves().size();
	    } else {
		// non-atomic, different operators
		// logger.info("compare"+this+" \n with \n"+other);
		return this.operator.compareTo(other.operator);
	    }
	}
	// logger.info("LinkSpec.compareTo returns 0");
	// return 0;
    }

    public String getShortendFilterExpression() {
	if (filterExpression == null)
	    return null;
	if (filterExpression.length() <= 0)
	    return "";
	if (!isAtomic())
	    return filterExpression;
	// "trigrams(x.prop1,y.prop2)" expect something like this...
	int beginProp1 = filterExpression.indexOf("(");
	int brakeProp = filterExpression.indexOf(",");
	int endProp2 = filterExpression.indexOf(")");
	String measure = filterExpression.substring(0, beginProp1);
	String prop1 = filterExpression.substring(beginProp1 + 1, brakeProp);
	String prop2 = filterExpression.substring(brakeProp + 1, endProp2);
	if (prop1.lastIndexOf("#") != -1)
	    prop1 = prop1.substring(prop1.lastIndexOf("#") + 1);
	else if (prop1.lastIndexOf("/") != -1)
	    prop1 = prop1.substring(prop1.lastIndexOf("/") + 1);
	if (prop2.lastIndexOf("#") != -1)
	    prop2 = prop2.substring(prop2.lastIndexOf("#") + 1);
	else if (prop2.lastIndexOf("/") != -1)
	    prop2 = prop2.substring(prop2.lastIndexOf("/") + 1);
	DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ENGLISH);
	df.applyPattern("#,###,######0.00");
	return measure + "(" + prop1 + "," + prop2 + ")|" + df.format(threshold);
    }

    public String getFilterExpression() {
	return filterExpression;
    }

    // @Test
    // public void testEquals() {
    // LinkSpec ls1 = new LinkSpec();
    // LinkSpec ls2 = new LinkSpec();
    // ls1.filterExpression="trigrams(x.prop1,y.prop2)";
    // ls1.threshold = 0.9d;
    // ls2.filterExpression="trigrams(x.prop1,y.prop2)";
    // ls2.threshold = 0.7d;
    //// assertFalse(ls1.equals(ls2));
    //
    // LinkSpec c1 = new LinkSpec();
    // c1.operator=Operator.AND;
    // c1.addChild(ls1);
    // c1.addChild(ls2);
    // c1.threshold=0.9d;
    // c1.addChild(ls2);
    //
    // LinkSpec c2 = new LinkSpec();
    // c2.operator=Operator.AND;
    // c2.addChild(ls2);
    // c2.addChild(ls1);
    // c2.threshold=0.9d;
    // c2.addChild(ls2);
    //
    //// assertTrue(c2.equals(c1));
    //
    // TreeSet<LinkSpec> treeset = new TreeSet<LinkSpec>();
    // treeset.add(ls1);
    // assertFalse(treeset.add(ls2));
    //// c2.operator=Opera
    //
    // }

    @Test
    public void testEqualsComplex() {
	LinkSpec p1 = new LinkSpec();
	p1.operator = Operator.AND;
	LinkSpec p2 = new LinkSpec();
	p2.operator = Operator.AND;
	LinkSpec c11 = new LinkSpec();
	c11.setAtomicFilterExpression("trigrams", "p1", "p2");
	c11.threshold = 1.0;
	LinkSpec c12 = new LinkSpec();
	c12.setAtomicFilterExpression("cosine", "p1", "p2");
	c12.threshold = 0.8;

	LinkSpec c21 = new LinkSpec();
	c21.setAtomicFilterExpression("trigrams", "p1", "p2");
	c21.threshold = 1.0;
	LinkSpec c22 = new LinkSpec();
	c22.setAtomicFilterExpression("cosine", "p1", "p2");
	c22.threshold = 0.8;

	p1.addChild(c11);
	p1.addChild(c12);
	p2.addChild(c21);
	p1.addChild(c22);

	assertFalse(p1.equals(p2));
	assertFalse(p2.equals(p1));

	Set<LinkSpec> set = new HashSet<LinkSpec>();
	assertTrue(set.add(p1));
	assertTrue(set.add(p2));
	// false added (null, 0.7400000000000001, AND, null,)
	// ->(trigrams(x.prop1,y.prop1), 1.0, null, null)
	// ->(cosine(x.prop2,y.prop2), 0.7400000000000001, null, null)
	// list.size()=1
	// (null, 0.8, AND, null,)
	// ->(cosine(x.prop2,y.prop2), 0.8, null, null)
	// ->(trigrams(x.prop1,y.prop1), 0.925, null, null)
    }

    /**
     * @return the atomicMeasure
     */
    public String getAtomicMeasure() {
	if (isAtomic()) {
	    if (atomicMeasure.length() > 0)
		return atomicMeasure;
	    else
		return filterExpression.substring(0, filterExpression.indexOf("("));
	}

	else
	    return null;
    }

    /**
     * @param atomicMeasure
     *            the atomicMeasure to set
     */
    public void setAtomicMeasure(String atomicMeasure) {
	this.atomicMeasure = atomicMeasure;
    }

    /**
     * Checks of at least two leaves compare the same properties, possibly with
     * different measures though.
     * 
     * @return true if two leaves compare the same properties, possibly with
     *         different measures
     */
    public boolean containsRedundantProperties() {
	List<LinkSpec> leaves = getAllLeaves();
	HashSet<String> props = new HashSet<String>();
	for (LinkSpec leave : leaves) {
	    String propStr = leave.prop1 + "_" + leave.prop2;
	    if (!props.add(propStr))
		return true;
	}
	return false;
    }
}
