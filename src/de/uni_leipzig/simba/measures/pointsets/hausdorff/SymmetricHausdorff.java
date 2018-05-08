/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.pointsets.hausdorff;

import de.uni_leipzig.simba.data.Point;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;

/**
 *
 * @author ngonga
 */
public class SymmetricHausdorff extends NaiveHausdorff{

    @Override
    public double computeDistance(Polygon X, Polygon Y, double threshold) {
        NaiveHausdorff nh = new NaiveHausdorff();
        return Math.max(nh.computeDistance(X, Y, threshold), nh.computeDistance(Y, X, threshold));
    }
    
    @Override
    public String getName() {
        return "symmetricHausdorff";
    }
}
