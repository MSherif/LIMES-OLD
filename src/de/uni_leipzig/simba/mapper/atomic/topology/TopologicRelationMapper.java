/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.mapper.atomic.topology;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import java.util.Set;

/**
 * Interface for mappers that check for RCC-8 topological relations
 * @author ngonga
 */
public interface TopologicRelationMapper {
    public Mapping getMapping(Set<Polygon> source, Set<Polygon> target);
}
