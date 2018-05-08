/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.pointsets;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.measures.Measure;
import java.util.Set;

/**
 *
 * @author ngonga
 */
public interface SetMeasure extends Measure {
	
	public static boolean USE_GREAT_ELLIPTIC_DISTANCE = true; //if false, orthodomic distance will be used
	
    public double computeDistance(Polygon X, Polygon Y, double threshold);
    public Mapping run(Set<Polygon> source, Set<Polygon> target, double threshold);
    public int getComputations();
    public String getName();
}
