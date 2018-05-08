/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.lgg;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

/**
 *
 * @author ngonga
 */
public class ExtendedClassifier extends SimpleClassifier {
    
    public Mapping mapping;
    
    public ExtendedClassifier(String measure, double threshold)
    {
        super(measure, threshold);
    }
    
    public ExtendedClassifier(String measure, double threshold, String sourceProperty, String targetProperty)
    {
        super(measure, threshold, sourceProperty, targetProperty);
    }
    
}
