package de.uni_leipzig.simba.learning.query;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.SortedMapping;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.query.ModelRegistry;
import de.uni_leipzig.simba.query.QueryModuleFactory;

import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

/**
 * Implements a label-based similarity for classes in two ontologies
 *
 * @author ngonga
 * @author Klaus Lyko
 */
public class LabelBasedClassMapper {

    static Logger logger = Logger.getLogger("LIMES");    
    QGramsDistance metric;
    private Model sourceModel;
    private Model targetModel;
    
    public LabelBasedClassMapper() {
        metric = new QGramsDistance();
    }
    /**
     * Constructor for registering Models.
     * @param sourceModel
     * @param targetModel
     */
    public LabelBasedClassMapper(Model sourceModel, Model targetModel) {
    	this();
    	this.sourceModel = sourceModel;
    	this.targetModel = targetModel;
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
	/**
	 * Method to get simple Name based Class Mappings. If no models were set, we'll assume
	 * endpoints point to a SPARQL endpoint, otherwise we are using the configured QueryModule.
	 * @param endpoint1
	 * @param endpoint2
	 * @return
	 */
	public Mapping getEntityMapping(String endpoint1, String endpoint2) {
        Set<Node> classes1 = getClasses(endpoint1, sourceModel);
        Set<Node> classes2 = getClasses(endpoint2, targetModel);
        String s, t;
        Mapping result = new Mapping();
        for (Node a : classes1) {
            for (Node b : classes2) {
                s = a.getLocalName().toLowerCase();
                t = b.getLocalName().toLowerCase();
                result.add(a.getURI(), b.getURI(), metric.getSimilarity(s, t));
            }
        }
        return result;
    }

    /**
     * Retrieves all nodes from the endpoint that are classes
     *
     * @param endpoint
     * @return Set of all nodes that are classes
     */
    private Set<Node> getClasses(String endpoint, Model model) {
        Set<Node> result = new HashSet<Node>();
        try {
            String query = "SELECT DISTINCT ?y WHERE { ?s a ?y }";
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec;
            if(model == null)
            	qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            else
            	qexec = QueryExecutionFactory.create(sparqlQuery, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                result.add(soln.get("y").asNode());
            }
        } catch (Exception e) {
            logger.warn("Error while processing classes");
        }
        return result;
    }
    
    public static void main(String args[])
    {
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
		
		LabelBasedClassMapper mapper = new LabelBasedClassMapper(sModel, tModel);
		Mapping m = mapper.getEntityMapping(sKB.endpoint, tKB.endpoint);
		System.out.println("Result = "+m);
    }
}
