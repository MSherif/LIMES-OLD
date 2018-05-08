/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.measures;

import de.uni_leipzig.simba.data.Instance;

/**
 *
 * @author ngonga
 */
public interface Measure {
    public double getSimilarity(Object a, Object b);    
    public String getType();
    public double getSimilarity(Instance a, Instance b, String property1, String property2);
    public String getName();
    public double getRuntimeApproximation(double mappingSize);
    
}
