/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.query;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.stablematching.HospitalResidents;

/**
 *
 * @author ngonga
 */
public interface PropertyMapper {
    public Mapping getPropertyMapping(String endpoint1, String endpoint2, String classExpression1, String classExpression2);
    public void setSourceModel(Model sourceModel);
    public void setTargetModel(Model targetModel);
}
