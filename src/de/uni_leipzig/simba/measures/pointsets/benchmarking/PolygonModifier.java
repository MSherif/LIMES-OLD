/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.pointsets.benchmarking;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import java.util.Set;

/**
 *
 * @author ngonga
 */
public interface PolygonModifier {
    Set<Polygon> modifySet(Set<Polygon> dataset, double threshold);
    Polygon modify(Polygon p, double threshold);
	/**
	 * @return
	 * @author sherif
	 */
	String getName();
}
