/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.mapper;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public interface AtomicMapper {
    public enum Language {EN, FR, DE, NULL};
    public Mapping getMapping(Cache source, Cache target, String sourceVar, String targetVar, String expression, double threshold);
    public String getName();
    public double getRuntimeApproximation(int sourceSize, int targetSize, double theta, Language language);
    public double getMappingSizeApproximation(int sourceSize, int targetSize, double theta, Language language);
    //for strings, average size is the average length of the strings to compare.
    //for numbers, it is the number of dimensions involved
    //public double getFilterRuntime(double mappingSize, Language language);
}
