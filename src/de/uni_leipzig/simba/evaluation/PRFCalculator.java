/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.evaluation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;

/**
 * Computes the precision, recall and F-score of mappings as computed by LIMES
 * @author ngonga
 */
public class PRFCalculator {

    /** Computes the intersection between two mappings, used for computing true 
     * positives
     * @param m Input mapping
     * @param reference Reference mapping
     * @return The size of the intersection of the two mappings
     */ 
    public static double getOverlap(final Mapping m, final Mapping reference)
    {
        double counter = 0;
        
        for(String key: m.map.keySet())
        {
            for(String value : m.map.get(key).keySet())
            {
                if(reference.map.containsKey(key))
                {
                    if(reference.map.get(key).containsKey(value))
                    {
                        counter++;
                    }
                }
            }
        }
        return counter;
    }

    public static double computeFalsePositives(Mapping m, Mapping reference) {
        double counter = 0;
        for(String key: m.map.keySet())
        {
            for(String value : m.map.get(key).keySet())
            {
            	if(!reference.contains(key, value))
            		counter++;
            }
        }
        return counter;
    }
    
    
    /**
     * Computes the precision of the mapping m with respect to the reference mapping
     * @param m
     * @param reference
     * @return Precision
     */
    public static double precision(Mapping m, Mapping reference)
    {
    	if(m.size()<=0)
    		return 0d;
        return getOverlap(m, reference)/(double)m.size();
    }

    /**
     * Computes the recall of the mapping m with respect to the reference mapping
     * @param m
     * @param reference
     * @return Recall
     */
    public static double recall(Mapping m, Mapping reference)
    {
        return getOverlap(m, reference)/(double)reference.size();
    }

    /**
     * Computes the F1-score of the mapping m with respect to the reference mapping
     * @param m
     * @param reference
     * @return F1-score
     */
    public static double fScore(Mapping m, Mapping reference)
    {
    	if(m.size() == 0) {
        	return 0d;
        }
        double overlap = getOverlap(m, reference);
        return 2*(overlap/(double)m.size())*(overlap/(double)reference.size())/(overlap/(double)m.size()+(overlap/(double)reference.size()));
    }
    @Test
    public void testComputeFScore() {
    	Mapping m1 = new Mapping();
    	Mapping m2 = new Mapping();
    	Mapping ref = new Mapping();
    	m1.add("a", "b", 1d);
    	ref.add("a", "b", 1d);
    	m1.add("a", "c", 2d);
    	PRFCalculator prf = new PRFCalculator();
    	double val = prf.fScore(m1, ref);
    	boolean b = (val-((double)2/3)<=0.00001d);
    	assertTrue(b&!Double.isNaN(prf.fScore(m2, ref)));
    }
    
    /**
     * Counts the differences of the 2 Mappings as  |m1 \ m2|. That is, the
     * number of pairs in m1, which are not in m2.
     * @return Number of instances in m1 which are not in m2.
     */
    public double computeDifference(Mapping m1, Mapping m2) {
    	double counter = 0;
    	for(String key : m1.map.keySet()) {
    		for(String value : m1.map.get(key).keySet()) {
    			if(!m2.contains(key, value) && !m2.contains(value, key)) {
    				counter++;
    			}
    		}
    	}
    	return counter;
    }
    
    /**
     * Computes the Matthews correlation coefficient MCC of the two mappings.
     * MCC returns a value of [-1, 1].
     * 1: perfect prediction
     * O: means no better than random
     * -1: disagreement 
     * @param m Mapping 
     * @param reference Reference Mapping.
     * @param crossProduct Cross Product of source and target. Is needed to compute the true negatives.
     * @return MCC
     */
    public double computeMatthewsCorrelation(Mapping m, Mapping reference, double crossProduct) {
    	double tp = getOverlap(m, reference); // true positives
   
    	double fn = computeDifference(reference, m); // false negatives
    	double fp = computeDifference(m, reference); // false positives
    	double tn = crossProduct-tp-fn-fp;
//    	System.out.println("tp="+tp+" tn="+tn+" fp="+fp+" fn="+fn);
    	double help = (tp+fp)*(tp+fn)*(tn+fp)*(tn+fn);
    	double denom = 1;
    	if(help>0)
    		denom = Math.sqrt(help);
//    	System.out.println("help="+help+" denom="+denom);
//    	System.out.println("res=="+((tp*tn - fp*fn)/denom));
    	return ((tp*tn - fp*fn)/denom);    	
    }
    @Test
    public void testcomputeMatthewsCorrelation() {
    	Mapping m = new Mapping();
    	Mapping reference = new Mapping();
    	m.add("a", "c", 1);
    	reference.add("a", "c", 1);
    	double val; double eps = 0.01d; double ref;
    	val = computeMatthewsCorrelation(m, reference, 4);
    	ref= 1.0;
    	System.out.println("Matthew="+ val + " FScore=" + fScore(m, reference));
    	assertTrue(Math.abs(val - ref)<eps);
    	reference.add("b", "d", 1);
    	val = computeMatthewsCorrelation(m, reference, 4);
    	ref = 2/Math.sqrt(12);
    	System.out.println("Matthew="+ val + " FScore=" + fScore(m, reference));
    	assertTrue(Math.abs(val - ref)<eps);
    	m = new Mapping();
    	reference = new Mapping();
    	m.add("a", "c", 1);
    	reference.add("b", "d", 1);
    	val = computeMatthewsCorrelation(m, reference, 4);
    	ref = -1d/3d;
    	System.out.println("Matthew="+ val + " FScore=" + fScore(m, reference));
    	assertTrue(Math.abs(val - ref)<eps);
    }
    
    @Test
    public void testComputeFalsePositives() {
    	Mapping m = new Mapping();
    	Mapping ref = new Mapping();
    	m.add("a", "b", 1d);
    	m.add("a", "b2", 1d);
    	m.add("b", "c", 1d);
    	m.add("b", "d", 1d);
    	ref.add("a", "b", 1d);
    	ref.add("b", "c", 1d);
    	double fp = computeFalsePositives(m, ref);
    	assertTrue((fp - 2.0 ) < 0.0002);
    }
    
    
//    public static void main(String ...args){
//     	Mapping m = new Mapping();
//    	Mapping ref = new Mapping();
//    	m.add("a", "b", 1d);
//    	m.add("a", "b2", 1d);
//    	m.add("b", "c", 1d);
//    	m.add("b", "d", 1d);
//    	ref.add("a", "b", 1d);
//    	ref.add("b", "c", 1d);
//    	System.out.println((new PRFComputer()).computeFScore(m, ref));
//    }
}
