/**
 * 
 */
package de.uni_leipzig.simba.io.rdfconfig;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public interface RDFSpecs {
	Model xmlConfigToRDFConfigExtended(String filePath) throws Exception;
}
