/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.data.Mapping;
import java.util.List;

/**
 * An implementation of the psuedo f measure presented by: 
 * 		Andriy Nikolov, Mathieu D'Aquin, and Enrico Motta. Unsupervised learning of data linking configuration. 
 * 			In	Proceedings of ESWC, 2012
 * @author ngonga
 */
public class ReferencePseudoMeasures extends PseudoMeasures {
    
    /** Computes Pseudo-f-measure for different beta values
     * 
     * @param sourceUris Source Uris
     * @param targetUris Target Uris
     * @param result Mapping resulting from classifiers
     * @param beta Beta for F-beta
     * @return Pseudo measure
     */
    public double getPseudoFMeasure(List<String> sourceUris, List<String> targetUris,
            Mapping result, double beta) {
        double p = getPseudoPrecision(result);
        double r = getPseudoRecall(sourceUris, targetUris, result);        
        if(p==0 && r==0) return 0.0;
        double f = (1 + beta * beta) * p * r / (beta * beta * p + r);
//        System.out.println("P:"+p+"; R:"+r+"; F="+f);
        return f;
    }
//        
    /** Computes the pseudo-precision, which is basically how well the mapping 
     * maps one single s to one single t
     * @param sourceUris List of source uris
     * @param targetUris List of target uris
     * @param result Mapping of source to targer uris
     * @return Pseudo precision score
     */
    public double getPseudoPrecision(Mapping result) {
    	Mapping res = result;
    	if(use1To1Mapping)
    		res = Mapping.getBestOneToOneMappings(result);
        double p = res.map.keySet().size();
        double q = 0;
        for (String s : res.map.keySet()) {
            q = q + res.map.get(s).size();
        }
        if(p==0 || q==0) return 0;
        return p / q;
    }
     
    /** The assumption here is a follows. We compute how many of the s and t
     * were mapped. 
     * @param sourceUris Uris in source cache
     * @param targetUris Uris in target cache
     * @param result Mapping computed by our learner
     * @param Run mapping minimally and apply filtering. Compare the runtime of both approaches
     * @return Pseudo recall 
     */
    public double getPseudoRecall(List<String> sourceUris, List<String> targetUris,
            Mapping result) {
    	Mapping res = result;
    	if(use1To1Mapping)
    		res = Mapping.getBestOneToOneMappings(result);
        double size = 0;
        for (String s : res.map.keySet()) {
            
            size = size + res.map.get(s).size();
        }
        return size/ Math.min(sourceUris.size(), targetUris.size());
    }
    
//    public static double getPseudoRecall(double sourceSize, double targetSize,
//            double mappingSize) {
//        double min = Math.min(sourceSize, targetSize);
//        return mappingSize/min;
//    }
//    
//    public static double getSize(Mapping m)
//    {
//        double q = 0;
//        for (String s : m.map.keySet()) {
//            q = q + m.map.get(s).size();
//        }
//        return q;
//    }
    @Override
	public String getName() {
		return "Reference Pseudo F-Measure";
	}

	@Override
	public double getPseudoPrecision(List<String> sourceUris,
			List<String> targetUris, Mapping result) {
		return getPseudoPrecision(result);
	}
}
