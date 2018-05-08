/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.query;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.io.KBInfo;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * @author ngonga
 * Can load from a resource as well.
 */
public class FileQueryModule implements QueryModule {

	KBInfo kb;
	Model model;
	static Logger logger = Logger.getLogger("LIMES");

	/** Constructor
	 * 
	 * @param kbinfo
	 * Loads the endpoint as a file and if that fails as a resource. 
	 */
	public FileQueryModule(KBInfo kbinfo) {
		try
		{
			InputStream in;
			kb = kbinfo;
			model = ModelFactory.createDefaultModel();
			System.out.println("Trying to get reader "+kb.type);
			RDFReader r = model.getReader(kb.type);
			
			try
			{
				in = new FileInputStream(kb.endpoint);
			} catch(FileNotFoundException e)
			{
				in = getClass().getClassLoader().getResourceAsStream(kb.endpoint);
				if(in==null)
				{
					logger.fatal("endpoint could not be loaded as a file or resource");
					return;
				}
			}       
			InputStreamReader reader = new InputStreamReader(in, "UTF8");
			r.read(model, reader, null);
			logger.info("RDF model read from "+kb.endpoint+" is of size "+model.size());
			ModelRegistry.register(kb.endpoint, model);
			reader.close();
			in.close();
		} catch(Exception e)
		{
			logger.fatal("Error loading endpoint",e);
		} 		
	}

	/** Reads data from a model in the model registry
	 * 
	 * @param c Cache to be filled
	 */
	public void fillCache(Cache c) {
		SparqlQueryModule sqm = new SparqlQueryModule(kb);
		sqm.fillCache(c, false);
		
	}

	public static void main(String args[])
	{


		String ex = "C:\\Users\\Lyko\\Desktop\\dailymed_dump.nt";
		//		String ex2 = "C:\\Users\\Lyko\\workspace\\Limes\\Examples\\LATC\\finals\\dailymed-drugbank-ingredients.ttl";
		KBInfo kb = new KBInfo();
		kb.endpoint = ex;//"C:/Users/Lyko/workspace/Limes/Examples/GeneticEval/Datasets/dbpedia-linkedmdb.csv";
		kb.type = "N3";
		FileQueryModule fqm = new FileQueryModule(kb);
		HybridCache hc = new HybridCache();
		fqm.fillCache(hc);
		System.out.println("CACHE: "+hc.size());
	}
}
