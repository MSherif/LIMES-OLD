/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.measures;

import java.util.ArrayList;

/**
 *
 * @author ngonga
 */
public interface ComplexMeasure extends Measure{
    public double getSimilarity(ArrayList<Object> a, ArrayList<Object> b);
}
