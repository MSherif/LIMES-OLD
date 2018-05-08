/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.mapper;

import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public interface SetConstraintsMapper {
    Mapping getLinks(String expression, double threshold);
    Mapping getLinksFromAtomic(String expression, double threshold);
}
