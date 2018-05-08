package de.uni_leipzig.simba.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;

import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.query.ModelRegistry;

public class ModelTester {

	 KBInfo kb;
	 Model model;
	 static Logger logger = Logger.getLogger("LIMES");
	    
	public void testModel1(KBInfo kbinfo) {
	
		try {
            kb = kbinfo;
            model = ModelFactory.createDefaultModel();
            RDFReader r = model.getReader(kb.type);
            InputStream in = new FileInputStream(kb.endpoint);
            InputStreamReader reader = new InputStreamReader(in, "UTF8");
            r.read(model, reader, null);
            logger.info("RDF model read from "+kb.endpoint+" is of size "+model.size());
            ModelRegistry.register(kb.endpoint, model);
        } catch (Exception e) {
            logger.fatal("Error while reading input stream " + kb.endpoint);
            e.printStackTrace();
        }	
	}
	
	public void query() {
		String basicQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  \n";
		String query = "SELECT ?o \n" +
				" WHERE { ?s rdf:type ?o } LIMIT 10";
		Query sparqlQuery = QueryFactory.create(basicQuery+query, Syntax.syntaxARQ);
        QueryExecution qexec;
        Model model = ModelRegistry.getInstance().getMap().get(kb.endpoint);
        if (model == null) {
            throw new RuntimeException("No model with id '" + kb.endpoint + "' registered");
        }
        qexec = QueryExecutionFactory.create(sparqlQuery, model);
//        qexec = QueryExecutionFactory.sparqlService(kb.endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();
        System.out.println(results);
        while(results.hasNext()) {
        	QuerySolution sol = results.next();
        	System.out.println(sol);
        }
	}
	
	
	public static void main(String args[]) {
		ModelTester test = new ModelTester();
		String ex = "C:\\Users\\Lyko\\Desktop\\dailymed_dump.nt";
		ex = "resources/OAEI2014/im_oaei2014_datasets/im-identity/oaei2014_identity_a.owl";
		String type = "N-TRIPLE";
		type = "RDF/XML";
//		String 
		KBInfo info = new KBInfo();
		info.endpoint = ex;
		info.type = type;
		info.id = "dailymed";
		info.var = "?x";
		
		test.testModel1(info);
		test.query();
//		  Properties systemproperties = System.getProperties();
//		  for (Enumeration e = systemproperties.propertyNames(); e.hasMoreElements(); ) {
//		    String prop = (String) e.nextElement();
//		    System.out.println("Property: " + prop + " , Wert: " + systemproperties.getProperty(prop));
//		  }
		 
	}
}
