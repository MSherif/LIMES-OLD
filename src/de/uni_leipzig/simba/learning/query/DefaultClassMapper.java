/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.query;

import java.util.HashMap;
import org.apache.log4j.Logger;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.stablematching.HospitalResidents;
import de.uni_leipzig.simba.util.Clock;
import java.util.TreeSet;

/**
 * Tries to map the classes out of two SPARQL endpoints via stable marriages
 * @author ngonga
 * @author Klaus Lyko
 */
public class DefaultClassMapper implements OntologyClassMapper {

    //number of instances to use for sampling
    public int LIMIT = 300;
    static Logger logger = Logger.getLogger("LIMES");
    String relation = "http://www.w3.org/2002/07/owl#sameAs";
    
    Model sourceModel, targetModel;
    public DefaultClassMapper() {}
    public DefaultClassMapper(int limit) {
    	LIMIT = limit;
    }
    
    public Model getSourceModel() {
		return sourceModel;
	}
	public void setSourceModel(Model sourceModel) {
		this.sourceModel = sourceModel;
	}
	public Model getTargetModel() {
		return targetModel;
	}
	public void setTargetModel(Model targetModel) {
		this.targetModel = targetModel;
	}
	
	public Mapping getEntityMapping(String endpoint1,
            String endpoint2, String namespace1, String namespace2) {
        Mapping m = getMappingClasses(endpoint1, endpoint2, namespace1, namespace2);
        logger.info("Got class mapping " + m);
        HospitalResidents hr = new HospitalResidents();
        m = hr.getMatching(m);
        logger.info("Final class mapping is " + m);
        return m;
    }

    /** Computes mapping between classes in two given endpoints by using the 
     * owl:sameAs links. If no such links exists between the two endpoints, the 
     * fallback solution is used
     * 
     * @param endpoint1 Source endpoint for the class mapping
     * @param endpoint2 Target endpoint for the class mapping
     * @param namespace1 Namespace for the source endpoint
     * @param namespace2 Namespace for the target endpoint
     * @return Mapping of classes from source to target weighted with number of 
     * owl:sameAs links or fallback weight based on matching properties
     */
    public Mapping getMappingClasses(String endpoint1,
            String endpoint2, String namespace1, String namespace2) {
        Clock clock = new Clock();
        logger.info("Getting mapping from " + namespace1 + " to " + namespace2);
        Mapping m1 = getMonoDirectionalMap(endpoint1, endpoint2, namespace1, namespace2);
        logger.info("Took " + clock.durationSinceClick() + " ms");        
        logger.info("Getting mapping from " + namespace2 + " to " + namespace1);
        Mapping m2 = getMonoDirectionalMap(endpoint2, endpoint1, namespace2, namespace1);
        logger.info("Took " + clock.durationSinceClick() + " ms");
        //logger.info(m2);
        logger.info("Merging the mappings...");
        double sim1, sim2;
        for (String value : m2.map.keySet()) {
            for (String key : m2.map.get(value).keySet()) {
                sim2 = m2.getSimilarity(value, key);
                sim1 = m1.getSimilarity(key, value);
                m1.add(key, value, sim1 + sim2);
            }
        }
        //logger.info("Class mapping is \n"+m2);
        if(m1.size() == 0)
        {
            logger.info("No mapping found. Using fallback solution.");
            m1 = getMappingClassesFallback(endpoint1, endpoint2, namespace1, namespace2);
        }
        return m1;
    }

    
        public Mapping getMappingClassesFallback(String endpoint1,
            String endpoint2, String namespace1, String namespace2) {
        Clock clock = new Clock();
        logger.info("Getting mapping from " + namespace1 + " to " + namespace2);
        Mapping m1 = getMonoDirectionalMapFallback(endpoint1, endpoint2, namespace1, namespace2);
        logger.info("Took " + clock.durationSinceClick() + " ms");        
        logger.info("Getting mapping from " + namespace2 + " to " + namespace1);
        Mapping m2 = getMonoDirectionalMapFallback(endpoint2, endpoint1, namespace2, namespace1);
        logger.info("Took " + clock.durationSinceClick() + " ms");
        //logger.info(m2);
        logger.info("Merging the mappings...");
        double sim1, sim2;
        for (String value : m2.map.keySet()) {
            for (String key : m2.map.get(value).keySet()) {
                sim2 = m2.getSimilarity(value, key);
                sim1 = m1.getSimilarity(key, value);
                m1.add(key, value, sim1 + sim2);
            }
        }
        //logger.info("Class mapping is \n"+m2);
        return m1;
    }
        
    public Mapping getMonoDirectionalMap(String endpoint1,
            String endpoint2, String namespace1, String namespace2) {


        HashMap<String, TreeSet<String>> instanceToClassMap =
                new HashMap<String, TreeSet<String>>();
        HashMap<String, TreeSet<String>> instanceToInstanceMap =
                new HashMap<String, TreeSet<String>>();
        Mapping classToClassMapping = new Mapping();

        String query = "SELECT ?x ?a ?b "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?x. "
                + "?a <" + relation + "> ?b. "
                + "FILTER REGEX(str(?b), \"" + namespace2 + "\") "
                + //"FILTER (1 > <bif:rnd>(0.1, ?x, ?a, ?b)) " +
                "} ";
        if (LIMIT > 0) {
            query = query + "LIMIT " + LIMIT;
        }

        logger.info("Query from " + namespace1 + " to " + namespace2 + ":\n" + query);

        Query sparqlQuery = QueryFactory.create(query);
        
        QueryExecution qexec;
//        if (!namespace1.contains("dbpedia") ) {
//            logger.info("Querying with default graph "
//                    + //"http://spatial-data.org/un-fao/");
//                    "http://www.instancematching.org/oaei/di/" + namespace1 + "/");
//            qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery,
//                    "http://www.instancematching.org/oaei/di/" + namespace1 + "/");
//            //"http://spatial-data.org/un-fao/");
//        } else {
//            qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery);
//        }
        if(sourceModel == null)
        	qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery);
        else
        	qexec = QueryExecutionFactory.create(sparqlQuery, sourceModel);
        ResultSet results = qexec.execSelect();

        // first get LIMIT instances from
        String x, a, b;
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            {
                try {
                    x = soln.get("x").toString();
                    a = soln.get("a").toString();
                    b = soln.get("b").toString();

                    if (!x.equalsIgnoreCase("http://www.w3.org/2002/07/owl#Thing")) {
                        if (!instanceToClassMap.containsKey(a)) {
                            instanceToClassMap.put(a, new TreeSet<String>());
                        }
                        instanceToClassMap.get(a).add(x);

                        if (!instanceToInstanceMap.containsKey(b)) {
                            instanceToInstanceMap.put(b, new TreeSet<String>());
                        }
                        instanceToInstanceMap.get(b).add(a);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        logger.info("Got " + instanceToClassMap.size() + " classes");
        //logger.info(instanceToClassMap);
        //logger.info(instanceToInstanceMap);

        for (String bValue : instanceToInstanceMap.keySet()) {
            bValue = bValue.replaceAll(" ", "_");
            query = "SELECT distinct ?x "
                    + "WHERE { <" + bValue + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?x }";
            //logger.info(query);
            sparqlQuery = QueryFactory.create(query);
            if(targetModel == null)
            	qexec = QueryExecutionFactory.sparqlService(endpoint2, sparqlQuery);
            else
            	qexec = QueryExecutionFactory.create(sparqlQuery, targetModel);
            results = qexec.execSelect();
            QuerySolution soln;
            while (results.hasNext()) {

                soln = results.nextSolution();
                {
                    try {
                        x = soln.get("x").toString();
                        //get instances a that maps with b, i.e., key
                        //logger.info(bValue + "->" + x);
                        //logger.info(bValue +"->"+instanceToInstanceMap.get(bValue));
                        for (String instance : instanceToInstanceMap.get(bValue)) {
                            TreeSet<String> set = instanceToClassMap.get(instance);
                            for (String clas : set) {
                                if (classToClassMapping.map.containsKey(x)) {
                                    if (classToClassMapping.map.get(x).containsKey(clas)) {
                                        classToClassMapping.map.get(x).put(clas,
                                                classToClassMapping.map.get(x).get(clas) + 1);
                                    } else {
                                        classToClassMapping.map.get(x).put(clas, 1.0);
                                    }
                                } else {
                                    classToClassMapping.add(x, clas, 1);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //logger.info(classToClassMapping.map);
        return classToClassMapping;
    }

    public Mapping getMonoDirectionalMapFallback(String endpoint1,
            String endpoint2, String namespace1, String namespace2) {


        HashMap<String, TreeSet<String>> classValueMap =
                new HashMap<String, TreeSet<String>>();
        Mapping classToClass = new Mapping();

        //get property values from first knowledge base
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT ?s ?p ?x ?o \n"
                + "WHERE { ?s rdf:type ?x . \n"
                + "?s ?p ?o ."
                //+ " FILTER(lang(?o) = \"en\"). "
                + //"FILTER REGEX (str(?o), \"^^xsd:string\" " +
                "\n}";
        if (LIMIT > 0) {
            query = query + " LIMIT " + LIMIT	;
        }

        logger.info("Query:\n" + query);
        Query sparqlQuery = QueryFactory.create(query);
        QueryExecution qexec;
//        if (!namespace1.contains("dbpedia") ) {
//            logger.info("Querying with default graph "
//                    + //"http://spatial-data.org/un-fao/");
//                    "http://www.instancematching.org/oaei/di/" + namespace1 + "/");
//            qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery,
//                    "http://www.instancematching.org/oaei/di/" + namespace1 + "/");
//            //"http://spatial-data.org/un-fao/");
//        } else {
//            qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery);
//        }
        if(sourceModel == null)
        	qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery);
        else
        	qexec = QueryExecutionFactory.create(sparqlQuery, sourceModel);
        ResultSet results = qexec.execSelect();

        // first get LIMIT instances from
        String s, p, o, x, y;
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            {
                try {
                    s = soln.get("s").toString();
                    p = soln.get("p").toString();
                    o = soln.get("o").toString();
                    x = soln.get("x").toString();
                    //gets rid of all numeric properties
                    if (!classValueMap.containsKey(x)) {
                        classValueMap.put(x, new TreeSet<String>());
                    }
                    classValueMap.get(x).add(o);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //logger.info("Got " + instanceToClassMap.size() + " classes");
        //logger.info(instanceToClassMap);
        //logger.info(instanceToInstanceMap);
        double sim;
        //logger.info("Resulting mapping \n"+classValueMap);
        for (String className : classValueMap.keySet()) {

            for (String object : classValueMap.get(className)) {
                object = object.split("@")[0];
                if (!object.contains("\\") && !object.contains("\n") && !object.contains("\"")) {
                    //System.out.println(object);
                    query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                            + "SELECT ?y "
                            + "WHERE { ?s rdf:type ?y . "
                            + "?s ?p \"" + object +"\"}";
                    //logger.info(query);
                    sparqlQuery = QueryFactory.create(query);
//                    if (!namespace2.contains("dbpedia") ) {
//                        //logger.info("Querying with default graph "
//                        //        + //"http://spatial-data.org/un-fao/");
//                        //        "http://www.instancematching.org/oaei/di/" + namespace2 + "/");
//                        qexec = QueryExecutionFactory.sparqlService(endpoint2, sparqlQuery,
//                                "http://www.instancematching.org/oaei/di/" + namespace2 + "/");
//                        //"http://spatial-data.org/un-fao/");
//                    } else {
//                        qexec = QueryExecutionFactory.sparqlService(endpoint2, sparqlQuery);
//                    }
                    if(targetModel == null)
                    	qexec = QueryExecutionFactory.sparqlService(endpoint2, sparqlQuery);
                    else
                    	qexec = QueryExecutionFactory.create(sparqlQuery, targetModel);
                    results = qexec.execSelect();
                    QuerySolution soln;
                    while (results.hasNext()) {
                        soln = results.nextSolution();
                        {
                            try {
                                y = soln.get("y").toString();
                                //logger.info(y);
                                sim = classToClass.getSimilarity(className, y);
                                if (sim > 0) {
                                    classToClass.map.get(className).put(y, sim + 1);
                                } else {
                                    classToClass.add(className, y, 1.0);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        //logger.info(classToClass);
        return classToClass;
    }

    public static void main(String args[]) {
        DefaultClassMapper cm = new DefaultClassMapper(100);
//        Mapping m = cm.getEntityMapping("http://live.dbpedia.org/sparql", "http://lgd.aksw.org:5678/sparql", "dbpedia", "drugbank");
//        for (String s : m.map.keySet()) {
//            System.out.println(s + " " + m.map.get(s));
//        }
        String ep1,ep2,nsp1,nsp2;
//        "http://lgd.aksw.org:5678/sparql",
//        "http://lgd.aksw.org:5678/sparql", "diseasome", "sider"
//        ep1 = "http://www4.wiwiss.fu-berlin.de/drugbank/sparql";
        ep1 = "http://live.dbpedia.org/sparql";
        ep2 ="http://www4.wiwiss.fu-berlin.de/sider/sparql";
        nsp1 = "dbpedia";
        nsp2 = "sider";

        Mapping m = cm.getMappingClasses(ep1, ep2, nsp1, nsp2).reverseSourceTarget();
//        Mapping m = cm.getMappingClasses("http://dbpedia.org/sparql", "http://lgd.aksw.org:5678/sparql", "dbpedia", "diseasome");
        logger.info(m);
    }

    public static boolean isNumeric(String input) {
        if (input.contains("^^")) {
            input = input.split("^^")[0];
        }
        if (input.contains("%")) {
            input = input.split("\\%")[0];
        }
        try {
            Double.parseDouble(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
}
