/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.data.Mapping;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class ComplexClassifier {
    public List<SimpleClassifier> classifiers;
    public double fMeasure;
    public Mapping mapping;
    
    public ComplexClassifier(List<SimpleClassifier> classifiers, double fMeasure)
    {
        this.classifiers = classifiers;
        this.fMeasure = fMeasure;
    }
    
    public ComplexClassifier clone()
    {
        ComplexClassifier copy = new ComplexClassifier(null, 0);
        copy.fMeasure = fMeasure;
        copy.classifiers = new ArrayList<SimpleClassifier>();
        for(int i=0; i<classifiers.size(); i++)
            copy.classifiers.add(classifiers.get(i).clone());
        return copy;
    }
    
    public String toString() {
    	String s = "";
    	for(SimpleClassifier sc : classifiers) {
    		s+= "CC{ ("+sc.toString2()+") }";
    	}
    	return s;
    }
}
