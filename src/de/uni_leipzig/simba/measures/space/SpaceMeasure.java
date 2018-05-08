/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.measures.space;

import de.uni_leipzig.simba.measures.Measure;

/**
 *
 * @author ngonga
 */
public interface SpaceMeasure extends Measure{
    public void setDimension(int n);
    public double getThreshold(int dimension, double simThreshold);
}
