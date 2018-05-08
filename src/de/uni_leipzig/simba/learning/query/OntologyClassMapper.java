/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.learning.query;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.stablematching.HospitalResidents;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public interface OntologyClassMapper {
      public Mapping getEntityMapping(String endpoint1,
            String endpoint2, String namespace1, String namespace2);

}
