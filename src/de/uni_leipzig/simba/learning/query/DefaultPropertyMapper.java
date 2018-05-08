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
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.stablematching.HospitalResidents;
import de.uni_leipzig.simba.query.ModelRegistry;
import de.uni_leipzig.simba.query.QueryModuleFactory;
import java.util.TreeSet;

/**
 *
 * @author ngonga
 * @author Klaus Lyko
 */
public class DefaultPropertyMapper implements PropertyMapper{

    public int LIMIT = 500;
    static Logger logger = Logger.getLogger("LIMES");
    public int MINSIM = 1;
    Model sourceModel, targetModel;

    /** Applies stable matching to determine the best possible mapping of 
     * properties from two endpoints
     * @param endpoint1 Source endpoint
     * @param endpoint2 Target endpoint
     * @param classExpression1 Source class expression
     * @param classExpression2 Target class expression
     * @return 
     */
    public Mapping getPropertyMapping(String endpoint1,
            String endpoint2, String classExpression1, String classExpression2) {
        Mapping m = getMappingProperties(endpoint1, endpoint2, classExpression1, classExpression2);
        HospitalResidents hr = new HospitalResidents();
        m = hr.getMatching(m);
        Mapping copy = new Mapping();
        //clean from nonsense, i.e., maps of weight 0
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                if (m.getSimilarity(s, t) >= MINSIM) {
                    copy.add(s, t, m.getSimilarity(s, t));
                }
            }
        }
        return copy;
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

	/** Computes the mapping of property from enpoint1 to endpoint2 by using the
     * restriction classExpression 1 and classExpression2
     *
     */
    public Mapping getMappingProperties(String endpoint1,
            String endpoint2, String classExpression1, String classExpression2) {

        logger.info("Getting mapping from " + classExpression1 + " to " + classExpression2);
        Mapping m2 = getMonoDirectionalMap(endpoint1, endpoint2, classExpression1, classExpression2);
        logger.info("m2="+m2.size());
        logger.info("Getting mapping from " + classExpression2 + " to " + classExpression1);
        Mapping m1 = getMonoDirectionalMap(endpoint2, endpoint1, classExpression2, classExpression1);
        logger.info("m1="+m1.size());
        logger.info("Merging the mappings...");
        double sim1, sim2;
        for (String key : m1.map.keySet()) {
            for (String value : m1.map.get(key).keySet()) {
                sim2 = m2.getSimilarity(value, key);
                sim1 = m1.getSimilarity(key, value);
                m2.add(value, key, sim1 + sim2);
            }
        }
        logger.info("Property mapping is \n" + m2);
        return m2;
    }

    public Mapping getMonoDirectionalMap(String endpoint1,
            String endpoint2, String classExpression1, String classExpression2) {


        HashMap<String, TreeSet<String>> propertyValueMap =
                new HashMap<String, TreeSet<String>>();
        Mapping propertyToProperty = new Mapping();

        //get property values from first knowledge base
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT ?s ?p ?o \n"
                + "WHERE { ?s rdf:type <" + classExpression1 + "> . \n"
                + "?s ?p ?o . "
                //+ "FILTER(lang(?o) = \"en\"). "
                + //"FILTER REGEX (str(?o), \"^^xsd:string\" " +
                "\n}";
        if (LIMIT > 0) {
            query = query + " LIMIT " + LIMIT;
        }

        logger.info("Query:\n" + query);
        Query sparqlQuery = QueryFactory.create(query);
        QueryExecution qexec;
        if(sourceModel == null)
        	qexec = QueryExecutionFactory.sparqlService(endpoint1, sparqlQuery);
        else
        	qexec = QueryExecutionFactory.create(sparqlQuery, sourceModel);
        ResultSet results = qexec.execSelect();
        // first get LIMIT instances from
        String s, p, o;
        int count = 0;
        while (results.hasNext()) {
        	count++;
            QuerySolution soln = results.nextSolution();
            {
                try {
                    s = soln.get("s").toString();
                    p = soln.get("p").toString();
                    o = soln.get("o").toString();
                    //gets rid of all numeric properties
                    if (!isNumeric(o)) {
                        if (!propertyValueMap.containsKey(p)) {
                            propertyValueMap.put(p, new TreeSet<String>());
                        }
                        propertyValueMap.get(p).add(o);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        logger.info("Had "+count+ "results");
//        logger.info("Got " + instanceToClassMap.size() + " classes");
//        logger.info(instanceToClassMap);
//        logger.info(instanceToInstanceMap);
        double sim;
        for (String property : propertyValueMap.keySet()) {

            for (String object : propertyValueMap.get(property)) {
                object = object.split("@")[0];
                if (!object.contains("\\") && !object.contains("\n") && !object.contains("\"")) {
                    //System.out.println(object);
                	String objectString;
                	if(!object.startsWith("http"))
                		objectString = "\"" + object.replaceAll(" ", "_") + "\"";
                	else
                		objectString = "<"+object.replaceAll(" ", "_") +">";
                    query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
                            "SELECT ?p " +
                            "WHERE { ?s rdf:type <" + classExpression2 + "> . "+
                            "?s ?p "+objectString+"}" +
                            "LIMIT 50";
                    logger.info(query);
                    sparqlQuery = QueryFactory.create(query);
                    if(targetModel == null)
                    	qexec = QueryExecutionFactory.sparqlService(endpoint2, sparqlQuery);
                    else
                    	qexec = QueryExecutionFactory.create(sparqlQuery, targetModel);
                    results = qexec.execSelect();
                    int count2 = 0;
                    QuerySolution soln;
                    while (results.hasNext()) {
                        soln = results.nextSolution();
                        {
                        	count2++;
                            try {
                                p = soln.get("p").toString();
                                sim = propertyToProperty.getSimilarity(property, p);
                                if (sim > 0) {
                                    propertyToProperty.map.get(property).put(p, sim + 1);
                                } else {
                                    propertyToProperty.add(property, p, 1.0);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    logger.info("lead to "+count2+" results");
                }
            }
        }

        //logger.info(classToClassMapping.map);
        return propertyToProperty;
    }

    public static void main(String args[]) {
//        DefaultPropertyMapper pm = new DefaultPropertyMapper();
//        String ep1, ep2, class1, class2;
//        ep1 = ep2 = "http://live.dbpedia.org/sparql";
//        class1 = class2 = "http://dbpedia.org/ontology/Holiday";
//        Mapping m;
//        m =   pm.getMonoDirectionalMap(ep1, ep2, class1, class2);//pm.getPropertyMapping(ep1, ep2, class1, class2);
//        System.out.println("Mapping =" + m);
    	String base = "C:/Users/Lyko/workspace/LIMES/resources/";
    	
    	KBInfo sKB = new KBInfo();
    	sKB.endpoint=base+"Persons1/person11.nt";
    	sKB.graph=null;
    	sKB.pageSize=1000;
    	sKB.id="person11";
		KBInfo tKB = new KBInfo();
		tKB.endpoint=base+"Persons1/person12.nt";
		tKB.graph=null;
		tKB.pageSize=1000;
		tKB.id="person12";
    	QueryModuleFactory.getQueryModule("nt", sKB);
    	QueryModuleFactory.getQueryModule("nt", tKB);
		Model sModel = ModelRegistry.getInstance().getMap().get(sKB.endpoint);
		Model tModel = ModelRegistry.getInstance().getMap().get(tKB.endpoint);
	
		DefaultPropertyMapper mapper = new DefaultPropertyMapper();
		mapper.setSourceModel(sModel); mapper.setTargetModel(tModel);
		
		Mapping m = mapper.getPropertyMapping(sKB.endpoint, tKB.endpoint, "http://www.okkam.org/ontology_person1.owl#Person", "http://www.okkam.org/ontology_person2.owl#Person");
		System.out.println("Result = "+m);
      
    }

    /** Test whether the input string is numeric
     *
     * @param input String to test
     * @return True is numeric, else false
     */
    public static boolean isNumeric(String input) {
        if(input.contains("^^"))
            input = input.split("^^")[0];
        if(input.contains("%"))
            input = input.split("\\%")[0];
        try {
            Double.parseDouble(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
