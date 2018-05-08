/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.pointsets.benchmarking;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author ngonga
 */
public abstract class AbstractPolygonModifier implements PolygonModifier {    
    public Set<Polygon> modifySet(Set<Polygon> dataset, double threshold) {
        Set<Polygon> polygons = new HashSet<Polygon>();
        for (Polygon p : dataset) {
            polygons.add(modify(p, threshold));
        }
        return polygons;
    }    
}
