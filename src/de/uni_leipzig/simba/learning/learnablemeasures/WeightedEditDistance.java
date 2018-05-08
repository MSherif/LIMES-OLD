/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.learnablemeasures;

import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.measures.Measure;
import java.util.HashMap;

/**
 *
 * @author ngonga
 */
public class WeightedEditDistance implements Measure {

    public HashMap<String, HashMap<String, Integer>> weights;

    public double getSimilarity(Object a, Object b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getType() {
        return "string";
    }

    public double getSimilarity(Instance a, Instance b, String property1, String property2) {
        double max = 0, sim;
        for (String s : a.getProperty(property1)) {
            for (String t : b.getProperty(property2)) {
                sim = getSimilarity(s, t);
                if(sim > max)
                    max = sim;
            }
        }
        return max;
    }

    public String getName() {
        return "WeightedTrigrams";
    }

    public double getRuntimeApproximation(double mappingSize) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
