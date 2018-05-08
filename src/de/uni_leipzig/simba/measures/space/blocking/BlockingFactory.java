/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.space.blocking;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.mapper.atomic.PPJoinPlusPlus;
import de.uni_leipzig.simba.mapper.atomic.TotalOrderBlockingMapper;

/**
 *
 * @author ngonga
 */
public class BlockingFactory {

    public static BlockingModule getBlockingModule(String props, String measureName, double threshold, int granularity) {
        if (measureName.toLowerCase().startsWith("euclidean")) {
            if (granularity > 1) {
        //        System.out.println("Granularity is " + granularity);
                //return new VariableGranularityBlocker(props, measureName, threshold, granularity);
                return new HR3Blocker(props, measureName, threshold, granularity);
            } else {
                return new EuclideanBlockingModule(props, measureName, threshold);
            }
        }
//     else if (measureName.toLowerCase().startsWith("datesim")) {
//    	 return new VariableGranularityBlocker(props, measureName, threshold, granularity);
//    } else if (measureName.toLowerCase().startsWith("daysim")) {
//    	return new VariableGranularityBlocker(props, measureName, threshold, granularity);
//    } else if (measureName.toLowerCase().startsWith("yearsim")) {
//    	return new VariableGranularityBlocker(props, measureName, threshold, granularity);
//    }
        return new EuclideanBlockingModule(props, measureName, threshold);
    }
}
