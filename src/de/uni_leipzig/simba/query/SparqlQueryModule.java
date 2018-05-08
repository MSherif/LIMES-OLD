/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.query;

import com.hp.hpl.jena.query.*;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.preprocessing.Preprocessor;
import java.util.TreeSet;

/**
 *
 * @author ngonga
 */
public class SparqlQueryModule implements QueryModule {

    KBInfo kb;

    public SparqlQueryModule(KBInfo kbinfo) {
        kb = kbinfo;
    }

    /**
     * Reads from a SPARQL endpoint and writes the results in a cache
     *
     * @param cache The cache in which the content on the SPARQL endpoint is to
     * be written
     */
    public void fillCache(Cache cache) {
        fillCache(cache, true);
    }

    /**
     * Reads from a SPARQL endpoint or a file and writes the results in a cache
     *
     * @param cache The cache in which the content on the SPARQL endpoint is to
     * be written
     * @param sparql True if the endpoint is a remote SPARQL endpoint, else
     * assume that is is a jena model
     */
    public void fillCache(Cache cache, boolean sparql) {
        Logger logger = Logger.getLogger("LIMES");
        long startTime = System.currentTimeMillis();
        //write prefixes
        Iterator<String> iter = kb.prefixes.keySet().iterator();
        String key, query = "";
        while (iter.hasNext()) {
            key = iter.next();
            query = query + "PREFIX " + key + ": <" + kb.prefixes.get(key) + ">\n";
        }

        // fill in variable for the different properties to be retrieved
        query = query + "SELECT DISTINCT " + kb.var;
        for (int i = 0; i < kb.properties.size(); i++) {
            query = query + " ?v" + i;
        }
        query = query + "\n";
        // graph
        if (kb.graph != null) {
            if (!kb.graph.equals(" ") && kb.graph.length() > 3) {
                logger.info("Query Graph: " + kb.graph);
                query = query + "FROM <" + kb.graph + ">\n";
            } else {
                kb.graph = null;
            }
        }
        //restriction
        if (kb.restrictions.size() > 0) {
            String where;
            iter = kb.restrictions.iterator();
            query = query + "WHERE {\n";
            for (int i = 0; i < kb.restrictions.size(); i++) {
                where = kb.restrictions.get(i).trim();
                if (where.length() > 3) {
                    query = query + where + " .\n";
                }
            }
        }
        //properties
        String optional;
        if (kb.properties.size() > 0) {
            logger.info("Properties are " + kb.properties);
            //optional = "OPTIONAL {\n";
            optional = "";
            //iter = kb.properties.iterator();
            for (int i = 0; i < kb.properties.size(); i++) {
                //optional = optional + kb.var + " " + kb.properties.get(i) + " ?v" + i + " .\n";
                optional = optional + kb.var + " " + kb.properties.get(i) + " ?v" + i + " .\n";
            }
            //some endpoints and parsers do not support property paths. We replace
            //them here with variables

            int varCount = 1;
            while (optional.contains("/")) {
                optional = optional.replaceFirst("/", " ?w" + varCount + " .\n?w" + varCount + " ");
                varCount++;
            }

            //close optional
            //query = query + optional + "}\n";
            query = query + optional;
        }

        //finally replace variables in inverse properties
        String q[] = query.split("\n");
        query = "";
        for (int ql = 0; ql < q.length; ql++) {
        	if(q[ql].contains("regex"))
        		query = query + q[ql]+ "\n";
        	else
            if (q[ql].contains("^")) {
            	System.out.println(q[ql]);
                String[] sp = q[ql].replaceAll("\\^", "").split(" ");
                query = query + sp[2] + " " + sp[1] + " " + sp[0] + " " + sp[3] + "\n";
            } else {
                query = query + q[ql] + "\n";
            }
        }

        // close where
        if (kb.restrictions.size() > 0) {
            query = query + "}";
        }
        //query = query + " LIMIT 500";


        logger.info("Query issued is \n" + query);

        logger.info("Querying the endpoint.");
        //run query

        int offset = 0;
        boolean moreResults = false;
        int counter = 0, counter2 = 0;
        String basicQuery = query;
        do {
            logger.info("Getting statements " + offset + " to " + (offset + kb.pageSize));
            if (kb.pageSize > 0) {
                query = basicQuery + " LIMIT " + kb.pageSize + " OFFSET " + offset;
            }else{
            	query = basicQuery;
            }

            //logger.info(query);
            Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);
            QueryExecution qexec;

            // take care of graph issues. Only takes one graph. Seems like some sparql endpoint do
            // not like the FROM option.

            if (!sparql) {
                Model model = ModelRegistry.getInstance().getMap().get(kb.endpoint);
                if (model == null) {
                    throw new RuntimeException("No model with id '" + kb.endpoint + "' registered");
                }
                qexec = QueryExecutionFactory.create(sparqlQuery, model);
            } else {
                if (kb.graph != null) {
                    qexec = QueryExecutionFactory.sparqlService(kb.endpoint, sparqlQuery, kb.graph);
                } //
                else {
                    qexec = QueryExecutionFactory.sparqlService(kb.endpoint, sparqlQuery);
                }
            }
            ResultSet results = qexec.execSelect();


            //write            
            String uri, propertyLabel, rawValue, value;
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
                                propertyLabel = kb.properties.get(i);
                                if (soln.contains("v" + i)) {
                                    rawValue = soln.get("v" + i).toString();
                                    //remove localization information, e.g. @en
                                    for (String propertyDub : kb.functions.get(propertyLabel).keySet()) {
//                                        if (kb.functions.get(propertyDub).equals("POINT")) {
//                                            rawValue = soln.get("v"+i).asNode().getLiteralLexicalForm();
//                                            List<Double> coordinates = Preprocessor.getPoints(rawValue);
//                                            for(int c=0; c<coordinates.size(); c++)
//                                            {
//                                                cache.addTriple(uri, "c"+c, coordinates.get(c)+"");
//                                            }
//                                        } else {
                                        value = Preprocessor.process(rawValue, kb.functions.get(propertyLabel).get(propertyDub));
                                        cache.addTriple(uri, propertyDub, value);
                                        
//                                        }
                                    }
                                    //logger.info("Adding (" + uri + ", " + property + ", " + value + ")");
                                }
                                //else logger.warn(soln.toString()+" does not contain "+property);
                            }

                            //else
                            //    cache.addTriple(uri, property, "");

                        } catch (Exception e) {
                            logger.warn("Error while processing: " + soln.toString());
                            logger.warn("Following exception occured: " + e.getMessage());
                            e.printStackTrace();
                            System.exit(1);
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
                logger.warn("XML = \n" + ResultSetFormatter.asXMLString(results));
            } finally {
                qexec.close();
            }
            offset = offset + kb.pageSize;
        } while (moreResults && kb.pageSize > 0);
        logger.info("Retrieved " + counter + " triples and " + cache.size() + " entities.");
        logger.info("Retrieving statements took " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");
    }
}
