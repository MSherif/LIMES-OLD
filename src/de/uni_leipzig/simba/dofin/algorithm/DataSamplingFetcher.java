/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.dofin.algorithm;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.AtomicMapper;
import de.uni_leipzig.simba.mapper.atomic.EDJoin;
import de.uni_leipzig.simba.mapper.atomic.PPJoinPlusPlus;
import de.uni_leipzig.simba.mapper.atomic.TotalOrderBlockingMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author ngonga
 */
public class DataSamplingFetcher extends DataFetcher{

    public int sampleSize = 10;
    static Logger logger = Logger.getLogger("LIMES");
    HashMap<String, String> propertyIndex;
    HashMap<String, Set<String>> classToUriSample;

    public DataSamplingFetcher() {
        propertyIndex = new HashMap<String, String>();
        classToUriSample = new HashMap<String, Set<String>>();
    }

    public void getSample(String className, String endpoint, String graph) {
        Set<String> result = new HashSet<String>();
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT DISTINCT ?s\n"
                + "WHERE { ?s rdf:type <" + className + "> } LIMIT " + sampleSize;

        //logger.info("Query:\n" + query);
        try {
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;
            if (graph == null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, graph);
            }
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                result.add(soln.get("s").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        classToUriSample.put(className, result);
    }

    /** Fetches the classes in a given graph of an endpoint
     * Tested
     * @param endpoint SPARQL Endpoint
     * @param graph Graph from which the data is to be fetched. If not needed, set to null
     * @return  Set of classes in the graph/endpoint
     */
    public Set<String> getClasses(String endpoint, String graph) {
        Set<String> result = new HashSet<String>();
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT DISTINCT ?x\n"
                + "WHERE { ?s rdf:type ?x }";

        //logger.info("Query:\n" + query);
        try {
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;
            if (graph == null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, graph);
            }
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                result.add(soln.get("x").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Fetches the properties to a given class
     * Tested 
     * @param endpoint SPARQL Endpoint
     * @param graph Graph from which the data is to be fetched. If not needed, set to null
     * @param className Name of the class for which the properties are to be fetched
     * @return Set of property labels
     */
    public Set<String> getProperties(String endpoint, String graph, String className) {
        Set<String> result = new HashSet<String>();
        //reinit index
        propertyIndex = new HashMap<String, String>();
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT DISTINCT ?x\n"
                + "WHERE { ?s rdf:type <" + className + ">. \n"
                + "?s ?x ?o }";

        //logger.info("Query:\n" + query);

        String p;
        try {
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;
            if (graph == null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, graph);
            }
            ResultSet results = qexec.execSelect();
            int counter = 0;
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                p = soln.get("x").toString();
                result.add(p);
                propertyIndex.put(p, "p" + counter);
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //remove unneeded properties
        result.remove("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        result.remove("http://www.w3.org/2002/07/owl#sameAs");
        return result;
    }

    public Cache fillCache(String endpoint, String graph, String className, String property, Cache c) {
        if (propertyIndex.containsKey(property)) {
            Set<String> uris;
            if (!classToUriSample.containsKey(className)) {
                getSample(className, endpoint, graph);
            }
            uris = classToUriSample.get(className);

            for (String uri : uris) {
                String s, o;
                String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                        + "SELECT DISTINCT ?o\n"
                        + "WHERE { <"+uri+"> <" + property + "> ?o }";
                //logger.log(Level.INFO, "Query:\n{0}", query);
                try {
                    Query sparqlQuery = QueryFactory.create(query);
                    QueryExecution qexec;
                    if (graph == null) {
                        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
                    } else {
                        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, graph);
                    }
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution soln = results.nextSolution();
                        o = soln.get("o").toString();
                        c.addTriple(uri, propertyIndex.get(property), o);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return c;
    }

    public Mapping getMapping(Cache source, String measure, String property, double threshold) {
        if (threshold >= 1) {
            threshold = 1 / (1 + threshold);
        }
        String expression = measure + "(x." + propertyIndex.get(property) + ", y." + propertyIndex.get(property) + ")";
        //System.out.println(expression);
        AtomicMapper mapper;
        if (measure.startsWith("leven")) {
            mapper = new EDJoin();
        } else if (measure.startsWith("euclid")) {
            mapper = new TotalOrderBlockingMapper();
        } else {
            mapper = new PPJoinPlusPlus();
        }

        return mapper.getMapping(source, source, "?x", "?y", expression, threshold);
    }

    public static void main(String args[]) {
        String endpoint = "http://www4.wiwiss.fu-berlin.de/drugbank/sparql";
        String graph = null;
        DataSamplingFetcher df = new DataSamplingFetcher();
        Set<String> classes = df.getClasses(endpoint, graph);
        logger.info(classes.size() + " classes: "+classes);
        for (String c : classes) {
            //System.out.println(c);
            Set<String> properties = df.getProperties(endpoint, graph, c);
            //create new mapping
            HashMap<String, Mapping> mappings = new HashMap<String, Mapping>();
            System.out.println(df.propertyIndex);
            logger.info(properties.size() + " properties for class " + c);


            int counter = 0;
            // first create cache    
            Cache cache = new MemoryCache();
            for (String p : properties) {
                // get values for class c and property p
                logger.info("Getting property " + p + " for class " + c + "...");
                // then fill it with the data from the endpoint. This method can use
                // sampling to accelerate the data filling. In general, this sould not
                // be necessary unless we have plenty of data in one class, for example
                // a lot of animal names
                cache = df.fillCache(endpoint, graph, c, p, cache);
                //System.out.println(cache);
                //now use a measure and a distance or similarity threshold to generate the 
                // mapping for this given property
                logger.info("Computing the corresponding mapping ...");
                Mapping m = df.getMapping(cache, "trigrams", p, 1);
                //System.out.println(m);
                if (m.size > 0 && m != null) {
                    mappings.put(p, m);
                }
                counter++;
                if (counter > 9) {
                    break;
                }
            }
            ArrayList<String> uris = cache.getAllUris();
            //run the dofin algorithm on the mappings
            logger.info("Running the Dofin algorithm");
            Set<Set<String>> result = Dofin.runScalable(mappings, uris);
            logger.info("Dofin returned the following set of properties: " + result);
            for(Set<String> p: result)
            {
                logger.info("Coverage of set "+p+" is "+df.getCoverage(p, cache));
            }
            
            break;
        }
    }

    public double getCoverage(Set<String> properties, Cache cache) {
        ArrayList<String> uris = cache.getAllUris();
        double count = 0;
        for (String uri : uris) {
            for (String property : properties) {
                String p = propertyIndex.get(property);
                if (!cache.getInstance(uri).getProperty(p).isEmpty()) {
                    count++;
                    break;
                }
            }
        }
        return count / (double) (uris.size());
    }
}
