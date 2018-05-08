/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.query;

import de.uni_leipzig.simba.io.KBInfo;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class QueryModuleFactory {

    static Logger logger = Logger.getLogger("LIMES");

    public static QueryModule getQueryModule(String name, KBInfo kbinfo) {
        logger.info("Generating <" + name + "> reader");
        if (name.toLowerCase().startsWith("csv")) {
            return new CsvQueryModule(kbinfo);
        } 
        //processes N3 files
        else if (name.toLowerCase().startsWith("n3")||name.toLowerCase().startsWith("nt")) {
            kbinfo.type = "N3";
            return new FileQueryModule(kbinfo);
        } 
        //processes N-TRIPLE files
        else if (name.toLowerCase().startsWith("n-triple")) {
            kbinfo.type = "N-TRIPLE";
            return new FileQueryModule(kbinfo);
        }
                //process turtle files
        else if (name.toLowerCase().startsWith("turtle") || name.toLowerCase().startsWith("ttl")) {
            kbinfo.type = "TURTLE";
            return new FileQueryModule(kbinfo);
        } 
        //process rdf/xml files        
        else if (name.toLowerCase().startsWith("rdf") || name.toLowerCase().startsWith("xml")) {
            kbinfo.type = "RDF/XML";
            return new FileQueryModule(kbinfo);
        }
        
        else if (name.toLowerCase().startsWith("vector")) {
            return new VectorQueryModule(kbinfo);
        }
        //default
        return new SparqlQueryModule(kbinfo);
    }
}
