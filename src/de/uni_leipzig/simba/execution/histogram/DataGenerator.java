/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.histogram;

import de.uni_leipzig.simba.cache.Cache;

/**
 *
 * @author ngonga
 */
public interface DataGenerator {
    public static String LABEL = "label";
    public Cache generateData(int size);
    public String getName();
    //return average string length or value generated
    public double getMean();
    public double getStandardDeviation();
}
