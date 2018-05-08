/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.query;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.query.ModelRegistry;
import de.uni_leipzig.simba.query.QueryModuleFactory;

import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

/**
 *
 * @author ngonga
 * @author Klaus Lyko
 */
public class LabelBasedPropertyMapper implements PropertyMapper {

    static Logger logger = Logger.getLogger("LIMES");
    QGramsDistance metric;
    Model sourceModel, targetModel;
    
    public LabelBasedPropertyMapper() {
        metric = new QGramsDistance();
    }
    
    /**
     * Constructor to use Model for query execution, thereby making it possible to use 
     * registered local dumps insted of regular SPARQL endpoints.
     * @param sourceModel
     * @param targetModel
     */
    public LabelBasedPropertyMapper(Model sourceModel, Model targetModel) {
    	this();
    	this.sourceModel = sourceModel;
    	this.targetModel = targetModel;
    }

    public Mapping getPropertyMapping(String endpoint1, String endpoint2, String classExpression1, String classExpression2) {
        Set<Node> properties1 = getProperties(endpoint1, classExpression1, sourceModel);
        Set<Node> properties2 = getProperties(endpoint2, classExpression2, targetModel);
        String s, t;
        Mapping result = new Mapping();
        for (Node a : properties1) {
            for (Node b : properties2) {                
                s = a.getLocalName().toLowerCase();
                t = b.getLocalName().toLowerCase();
                result.add(a.getURI(), b.getURI(), metric.getSimilarity(s, t));
            }
        }
        return result;
    }

    public Model getTargetModel() {
		return targetModel;
	}

	public void setTargetModel(Model targetModel) {
		this.targetModel = targetModel;
	}
	 public Model getSourceModel() {
			return sourceModel;
	}

	public void setSourceModel(Model sourceModel) {
		this.sourceModel = sourceModel;
	}
	/**
	 * Retrieves all nodes from the endpoint that are classes.
     * @param endpoint
	 * @param classExpression
	 * @param model
	 * @return Set of all nodes that are classes
     */
    private Set<Node> getProperties(String endpoint, String classExpression, Model model) {
        Set<Node> result = new HashSet<Node>();
        try {
            String query = "SELECT DISTINCT ?p WHERE { ?s ?p ?y. ?s a <" + classExpression + "> }";
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;
            if(model == null)
            	qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            else
            	qexec = QueryExecutionFactory.create(sparqlQuery, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                result.add(soln.get("p").asNode());
            }
        } catch (Exception e) {
            logger.warn("Error while processing classes");
        }
        return result;
    }
    
    public static void main(String args[])
    {
        //System.out.println("Result = "+new LabelBasedPropertyMapper().getPropertyMapping("http://www4.wiwiss.fu-berlin.de/sider/sparql", "http://www4.wiwiss.fu-berlin.de/drugbank/sparql", "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs", "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs"));
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
	
		LabelBasedPropertyMapper mapper = new LabelBasedPropertyMapper(sModel, tModel);
		Mapping m = mapper.getPropertyMapping(sKB.endpoint, tKB.endpoint, "http://www.okkam.org/ontology_person1.owl#Person", "http://www.okkam.org/ontology_person2.owl#Person");
		System.out.println("Result = "+m);
    }
}
