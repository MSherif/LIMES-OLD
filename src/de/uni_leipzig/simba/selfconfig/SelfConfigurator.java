/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public interface SelfConfigurator {
    public void computeMeasure(Cache source, Cache target, String parameters[]);
    public String getMeasure();
    public String getThreshold();
    public Mapping getResults();    
    public void setMeasure(Measure measure);

}
