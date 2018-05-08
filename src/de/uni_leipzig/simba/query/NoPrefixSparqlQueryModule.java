/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.query;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.io.KBInfo;
import java.util.Iterator;
import com.hp.hpl.jena.query.*;
import org.apache.log4j.*;
/**
 *
 * @author ngonga
 */
public class NoPrefixSparqlQueryModule implements QueryModule{

   KBInfo kb;

    public NoPrefixSparqlQueryModule(KBInfo kbinfo) {
        kb = kbinfo;
    }

    /** Reads from a SPARQL endpoint and writes the results in a cache
     *
     * @param cache The cache in which the content on the SPARQL endpoint is
     * to be written
     */
    public void fillCache(Cache cache) {

        Logger logger = Logger.getLogger("LIMES");
        long startTime = System.currentTimeMillis();
        String query = "";
        // fill in variable for the different properties to be retrieved
        query = query + "SELECT DISTINCT " + kb.var;
        for (int i = 0; i < kb.properties.size(); i++) {
            query = query + " ?v" + i;
        }
        query = query + "\n";

        //restrictions
        Iterator iter;
        if (kb.restrictions.size() > 0) {
            String where;
            iter = kb.restrictions.iterator();
            query = query + "WHERE {\n";
            for (int i = 0; i < kb.restrictions.size(); i++) {
                where = kb.restrictions.get(i);
                query = query +where + " .\n";
            }
        }

        //properties
        String optional;
        query = query + "OPTIONAL {";
        if (kb.properties.size() > 0) {
            logger.info("Properties are " + kb.properties);
            //optional = "OPTIONAL {\n";
            optional = "";
            //iter = kb.properties.iterator();
            for (int i = 0; i < kb.properties.size(); i++) {
                //optional = optional + kb.var + " " + kb.properties.get(i) + " ?v" + i + " .\n";
                optional = optional + kb.var + " <" + kb.properties.get(i) + "> ?v" + i + " .\n";
            }
            query = query + optional;
        }
        query = query + "}";


        // close where
        if (kb.restrictions.size() > 0) {
            query = query + "}\n";
        }

        logger.info("Query issued is \n" + query);
        //query = query + " LIMIT 1000";

        logger.info("Querying the endpoint.");
        //run query

        int offset = 0;
        boolean moreResults = false;
        int counter=0, counter2 = 0;
        String basicQuery = query;
        do {
            logger.info("Getting statements " + offset + " to " + (offset + kb.pageSize));
            if (kb.pageSize > 0) {
                query = basicQuery + " LIMIT " + kb.pageSize + " OFFSET " + offset;
            }

            //logger.info(query);
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;

            // take care of graph issues. Only takes one graph. Seems like some sparql endpoint do
            // not like the FROM option.

//            if (kb.graph != null) {
//                qexec = QueryExecutionFactory.sparqlService(kb.endpoint, sparqlQuery, kb.graph);
//                logger.info("Querying default graph "+kb.graph);
//            } //
//            else {
                qexec = QueryExecutionFactory.sparqlService(kb.endpoint, sparqlQuery);
                logger.info("No default graph "+kb.graph);
//            }
            ResultSet results = qexec.execSelect();


            //write
            String uri, property, value;
            try {
                if (results.hasNext()) {
                    moreResults = true;
                } else {
                    moreResults = false;
                    break;
                }

                while (results.hasNext()) {

                    QuerySolution soln = results.nextSolution();
                    // process query here
                    {
                        //logger.info(soln.toString());
                        try {
                            //first get uri
                            uri = soln.get(kb.var.substring(1)).toString();

                            //now get (p,o) pairs for this s
                            String split[];
                            for (int i = 0; i < kb.properties.size(); i++) {
                                property = kb.properties.get(i);
                                if (soln.contains("v" + i)) {
                                    value = soln.get("v" + i).toString();
                                    //remove localization information, e.g. @en
                                    if (value.contains("@")) {
                                        value = value.substring(0, value.indexOf("@"));
                                    }
                                    //int test = 0;
                                    if (value.contains("^^")) {
//                                        if(test%100 == 0)
//                                        {
//                                        System.out.println(value);
//                                        }
//                                        test++;
                                        if(value.contains(":date"))
                                        {
                                        value = value.substring(0, value.indexOf("^^"));

                                        if(value.contains(" ")) value = value.substring(0, value.indexOf(" "));
                                        split = value.split("-");
                                        value = Integer.parseInt(split[0])*365
                                                + Integer.parseInt(split[1])*12
                                                + Integer.parseInt(split[2])+"";
                                        }
                                        else
                                            value = value.substring(0, value.indexOf("^^"));
                                    }
                                    cache.addTriple(uri, property, value);
                                    //logger.info("Adding (" + uri + ", " + property + ", " + value + ")");
                                }
                                else
                                    cache.addTriple(uri, property, "");
                                //else logger.warn(soln.toString()+" does not contain "+property);
                            }
                            //else
                            //    cache.addTriple(uri, property, "");

                        } catch (Exception e) {
                            logger.warn("Error while processing: " + soln.toString());
                            logger.warn("Following exception occured: " + e.getMessage());
                            logger.info("Processing further ...");
                        }
                        counter2++;
                    }
                    counter++;
                    //logger.info(soln.get("v0").toString());       // Get a result variable by name.
                }

            } catch (Exception e) {
                logger.warn("Exception while handling query");
                logger.warn(e.toString());
                logger.warn("XML = \n"+ResultSetFormatter.asXMLString(results));
            } finally {
                qexec.close();
            }
            offset = offset + kb.pageSize;
        } while (moreResults && kb.pageSize > 0);
        logger.info("Retrieved " + counter + " triples and " + cache.size() + " entities.");
        logger.info("Retrieving statements took " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");
    }

}
