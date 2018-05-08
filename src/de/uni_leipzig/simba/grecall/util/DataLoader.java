package de.uni_leipzig.simba.grecall.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheBackend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontendImpl;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDao;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDaoPostgres;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDataSource;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jena_sparql_api.retry.core.QueryExecutionFactoryRetry;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import cern.colt.matrix.linalg.Property;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Point;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.lgg.evaluation.LGGEvaluator4RealDatarets;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.specification.LinkSpec;

public class DataLoader {
	private static final Logger logger = Logger.getLogger(LGGEvaluator4RealDatarets.class.getName());
	
	static String[] replaceTokens = {"?city ",
								"?city ",
								"?city ",
								"?city ",
								"?film "};
	static String[] selectInput = {"resources/DB-LINKGEODATA_CITIES/citiesDB_select_output.txt",
			"resources/DB-LINKGEODATA_CITIES/citiesLINKGEODATA_select_output.txt",
		"resources/DB-LINKGEODATA_VILLAGES/villagesDB_select_output.txt",
		"resources/DB-LINKGEODATA_VILLAGES/villagesLINKGEODATA_select_output.txt",
		"resources/DB-LINKEDMDB_MOVIES/moviesDB_select_output.txt",
		//"resources/DB-LINKEDMDB_MOVIES/moviesLINKEDMDB_select.sparql"
		};
	
	static String[] selectQueries = {"resources/DB-LINKGEODATA_CITIES/citiesDB_select.sparql",
				"resources/DB-LINKGEODATA_CITIES/citiesLINKGEODATA_select.sparql",
			"resources/DB-LINKGEODATA_VILLAGES/villagesDB_select.sparql",
			"resources/DB-LINKGEODATA_VILLAGES/villagesLINKGEODATA_select.sparql",
			"resources/DB-LINKEDMDB_MOVIES/moviesDB_select.sparql",
			//"resources/DB-LINKEDMDB_MOVIES/moviesLINKEDMDB_select.sparql"
			};
	
	static String[] EndPoints = {
								"http://linkedgeodata.org/sparql",
            					
								"http://linkedgeodata.org/sparql",
      							
      							//"http://linkedmdb.org/sparql"
      							};

	static String[] Graphs = {
								"http://linkedgeodata.org",
							  
								"http://linkedgeodata.org",
    							
    							//"http://linkedmdb.org"
    							};

	static String[] sparqlQueries = {
								"resources/DB-LINKGEODATA_CITIES/citiesLINKGEODATA.sparql",
								
								"resources/DB-LINKGEODATA_VILLAGES/villagesLINKGEODATA.sparql",
								
								//"resources/DB-LINKEDMDB_MOVIES/moviesLINKEDMDB.sparql"
								};

	
	static String[] outputFiles = {
									"resources/DB-LINKGEODATA_CITIES/citiesLINKGEODATA.ttl",
            						 
									 "resources/DB-LINKGEODATA_VILLAGES/villagesLINKGEODATA.ttl",
									 
									 //"resources/DB-LINKEDMDB_MOVIES/moviesLINKEDMDB.ttl"
									 };

	/////////////////////////////////////////////////////////////////////
	//////////////////DBPEDIA///////////////////
	static String[] dbpediaInputSources = {"resources/DB-LINKGEODATA_CITIES/citiesDB.sparql",
			//"resources/DB-LINKGEODATA_VILLAGES/villagesDB.sparql",
			//"resources/DB-LINKEDMDB_MOVIES/moviesDB.sparql",
			//"resources/DB-MUSICBRAINZ_ARTISTS/musicDB.sparql"
			};
	static String[] dbpediaOutputSources = {"resources/DB-LINKGEODATA_CITIES/citiesDB.ttl",
			// "resources/DB-LINKGEODATA_VILLAGES/villagesDB.ttl",
			// "resources/DB-LINKEDMDB_MOVIES/moviesDB.ttl",
			 //"resources/DB-MUSICBRAINZ_ARTISTS/musicDB.ttl"
			 };
	/////////////////////////////////////////////////
	//works FOR DBPEDIA ONLY if loaded locally
	private static void loadDBFromFile(String datasetName) throws IOException{
		String[] inputSources = null;
		String[] outputSources = null;
		if(datasetName.equals("DBPEDIA")){
			inputSources=dbpediaInputSources;
			outputSources=dbpediaOutputSources;
		}
		Model model = ModelFactory.createDefaultModel();
		
	    
		for(int i = 0 ; i<inputSources.length ; i++){
			//read query from input file
			int len;
		    char[] chr = new char[4096];
		    final StringBuffer bufferSource = new StringBuffer();
		    String constructQryStrSource = null;
		    FileReader readerSource = new FileReader(inputSources[i]);
		    try {
		    	 while ((len = readerSource.read(chr)) > 0) {
		    		 bufferSource.append(chr, 0, len);
		          }
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        if(readerSource !=null){readerSource.close();}
		    }
		    constructQryStrSource = bufferSource.toString();
		    logger.info("Running construct query:\n" + constructQryStrSource);
		    
		    org.aksw.jena_sparql_api.core.QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://akswnc8.informatik.uni-leipzig.de:8890/sparql","http://dbpedia.org");
		    logger.info("here");
			long timeToLive = 24l * 60l*  60l * 1000l; 
			//				qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
			qef = new QueryExecutionFactoryRetry(qef, 5, 10000);
			logger.info("QueryExecutionFactoryPaginated");
			qef = new QueryExecutionFactoryPaginated(qef, 100000);
			QueryExecution qe = qef.createQueryExecution(constructQryStrSource);
			model = qe.execConstruct();
			
			logger.info("Saving dataset to " + outputSources[i] + "...");
			FileWriter fileWriter = new FileWriter(outputSources[i]);
			model.write(fileWriter, "TTL");
			logger.info("Source model size: "+model.size());
	}
		
		
	}
	
	public static void reReadFromFile() throws IOException{
		String input = "resources/DB-LINKGEODATA_CITIES/temp.ttl";
		String output = "resources/DB-LINKGEODATA_CITIES/output.ttl";
		
		java.io.InputStream in = FileManager.get().open( input );
		Model model=ModelFactory.createDefaultModel();
		Model newModel = ModelFactory.createDefaultModel();
		
		model.read(in, null, "TTL");
		
		StmtIterator triples = model.listStatements();
		Cache c = new HybridCache();
		while (triples.hasNext()) {
			Statement triple = triples.next();
			String s = triple.getSubject().toString();
            String p = triple.getPredicate().toString();
            String o = triple.getObject().toString();
            
            if(p.contains("geometry")){
            	
            	String t = o.replace(",", "");
            	o = t;
      
            }
           
            c.addTriple(triple.getSubject().toString(), triple.getPredicate().toString(), o);
            newModel.add(triple.getSubject(), triple.getPredicate(), o);
           
		}
		FileWriter fileWriter = new FileWriter(output,true);

		Model m = ModelFactory.createDefaultModel();
		for (Instance instance : c.getAllInstances()) {
			for(String property: instance.getAllProperties()){
				Resource resource = newModel.getResource(instance.getUri());
				com.hp.hpl.jena.rdf.model.Property pr = newModel.getProperty(property);

				if(property.equals("http://www.w3.org/2003/01/geo/wgs84_pos#geometry")){
					TreeSet<String> values = instance.getProperty(property);
					if(values.size() > 1){
						String t = "LINESTRING(";
						for(String value:values){
							value = value.substring(value.indexOf("("), value.lastIndexOf(")")+1);
							//logger.info(value);
							t += value+" ";
						}
						t += ")";
						
						m.add(resource,pr,t);
					}else{
						m.add(resource,pr,values.first());
					}
					
				}else if(property.equals("http://www.w3.org/2000/01/rdf-schema#label")
						||property.equals("http://dbpedia.org/ontology/country")){
					TreeSet<String> values = instance.getProperty(property);
					for(String value:values){
							String[] split = value.split("@");
							Literal l = m.createLiteral(split[0], split[1]);
							m.add(resource,pr,l);
					}
				}else if(property.equals("http://dbpedia.org/ontology/populationTotal")){
					TreeSet<String> values = instance.getProperty(property);
					for(String value:values){
						value = value.replace("^^",",");
						String[] split = value.split(",");
						
						Literal l = m.createTypedLiteral(new Integer(split[0]));
						m.add(resource,pr,l);
					}
				}else {
					TreeSet<String> values = instance.getProperty(property);
					for(String value:values){
							Resource object = newModel.getResource(value);
							m.add(resource,pr,object);
					}
				
			    }
				
			
			}
			m.write(fileWriter, "TTL");
		
		}
	}
	
	public static void main(String[] args) throws Exception {
		//constructDATA();
		reReadFromFile();
		//select();
		//construct();
		//loadDBFromFile("LINKEDMDB");
		
		/*DataSets d = DataSets.CITIES;
		EvaluationData data = DataSetChooser.getData(d);
		HybridCache source = HybridCache.getData(data.getConfigReader().sourceInfo);
		HybridCache target = HybridCache.getData(data.getConfigReader().targetInfo);
		
		ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
		ExecutionPlanner p;
        Mapping cMapping = null;
       
        p = new CanonicalPlanner();
        
        
        //NestedPlan np = p.plan(new LinkSpec("orthodromic(x.http://www.w3.org/2003/01/geo/wgs84_pos#geometry, y.http://geovocab.org/geometry#geometry)",0.0001));
        NestedPlan np = p.plan(new LinkSpec("hausdorff(x.geo:geometry,y.geom:geometry)",0.2241));
        //NestedPlan np = p.plan(new LinkSpec("qgrams(x.http://www.w3.org/2000/01/rdf-schema#label, y.http://www.w3.org/2000/01/rdf-schema#label)",0.0001));
        //NestedPlan np = p.plan(new LinkSpec("euclidean(x.http://dbpedia.org/ontology/populationTotal, y.http://linkedgeodata.org/ontology/population)",0.0001));
       
        cMapping = ee.runNestedPlan(np);

        Iterator it = cMapping.map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            logger.info(pair.getKey() + " = " + pair.getValue());
        }
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(data.getSourceFile()));
			
			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				//logger.info(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}*/
		
		
	}
	
	private static void constructDATA() throws IOException{
		for(int i = 0 ; i < sparqlQueries.length ; i++){
			if(i != 0)
				continue;
			
			String input = sparqlQueries[i];
			//use this later
			String output = outputFiles[i];
			//String constructQryStr = null;
			String constructQryStr = null;
			FileReader reader = null;
			
			int len;
		    char[] chr = new char[4096];
		    final StringBuffer bufferSource = new StringBuffer();
		    reader = new FileReader(input);
		    try {
		    	 while ((len = reader.read(chr)) > 0) {
		    		 bufferSource.append(chr, 0, len);
		          }
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        if(reader !=null){reader.close();}
		    }
		    
		    constructQryStr  = bufferSource.toString();
		    logger.info("Using "+ EndPoints[i] + " to run construct query:\n" + constructQryStr);
			
		    org.aksw.jena_sparql_api.core.QueryExecutionFactory qef = FluentQueryExecutionFactory.http(EndPoints[i], Graphs[i]).config()
					.withPagination(1000).end().create();

			long timeToLive = 24l * 60l*  60l * 1000l;
			// Add delay in order to be nice to the remote server (delay in milli seconds)
			
			QueryExecutionFactoryHttp foo = qef.unwrap(QueryExecutionFactoryHttp.class);
			logger.info(foo);
			// Create a QueryExecution object from a query string ...
			logger.info("createQueryExecution");
			QueryExecution qe = qef.createQueryExecution(constructQryStr);
			// and run it.
			logger.info("execSelect");
			Model m = qe.execConstruct();
			FileWriter fileWriter = new FileWriter(output);
			m.write(fileWriter, "TTL");
		}
	
	}
	//perform construct queries if select queries are done before
	private static void construct() throws IOException, SQLException{

		for(int i = 0 ; i < sparqlQueries.length ; i++){
			
			if(i != 0)
				continue;
			
			String Entities = selectInput[i];
			String inputConstruct = sparqlQueries[i];
			String output = outputFiles[i];
			String constructQryStr = null;
			
			FileReader reader = null;
			
			int len;
		    char[] chr = new char[4096];
		    final StringBuffer bufferSource = new StringBuffer();
		    reader = new FileReader(inputConstruct);
		    try {
		    	 while ((len = reader.read(chr)) > 0) {
		    		 bufferSource.append(chr, 0, len);
		          }
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        if(reader !=null){reader.close();}
		    }
		    
		    constructQryStr  = bufferSource.toString();
		    String replaceToken = replaceTokens[i];
		    
		    
		    //read entities from selectInput file
		    List<String> entities = new ArrayList<String>();
		    String line;
		    try (
		        InputStream fis = new FileInputStream(Entities);
		        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		        BufferedReader br = new BufferedReader(isr);
		    ) {
		        while ((line = br.readLine()) != null) {
		            if(line.contains("<") && line.contains(">")){
		            	String entity =  line.substring(line.indexOf("<"), line.indexOf(">")+1);
		            	logger.info(entity);
		            	entities.add(entity);
		            }
		        }
		    }
		    Model model = ModelFactory.createDefaultModel();
		    FileWriter fileWriter = new FileWriter(output,true);
		    for(String entity: entities){
		    	String tempString = constructQryStr.replace(replaceToken,entity+" ");
		    	
		    	logger.info("Using "+ EndPoints[i] + " to run construct query:\n" + tempString);
				org.aksw.jena_sparql_api.core.QueryExecutionFactory qef = new QueryExecutionFactoryHttp(EndPoints[i], Graphs[i]);
				
				long timeToLive = 24l * 60l*  60l * 1000l;
				logger.info("QueryExecutionFactoryRetry");
				qef = new QueryExecutionFactoryRetry(qef, 5, 10000);
				logger.info("QueryExecutionFactoryDelay");
				// Add delay in order to be nice to the remote server (delay in milli seconds)
				qef = new QueryExecutionFactoryDelay(qef, 5000);
				QueryExecutionFactoryHttp foo = qef.unwrap(QueryExecutionFactoryHttp.class);
				
				// Add pagination
				logger.info("QueryExecutionFactoryPaginated");
				qef = new QueryExecutionFactoryPaginated(qef, 10);
				// Create a QueryExecution object from a query string ...
				logger.info("createQueryExecution");
				QueryExecution qe = qef.createQueryExecution(tempString);
				
				model = qe.execConstruct();
				
				model.write(fileWriter, "TTL");
		    }
		    
		   
		}
	
	}
	//perform select queries
	private static void select() throws IOException, SQLException{
		for(int i = 0 ; i < sparqlQueries.length ; i++){
			
			if(i != 0)
				continue;
			String inputSelect = selectQueries[i];
			//use this later
			String input = sparqlQueries[i];
			//use this later
			String output = outputFiles[i];
			//String constructQryStr = null;
			String selectQryStr = null;
			FileReader reader = null;
			
			int len;
		    char[] chr = new char[4096];
		    final StringBuffer bufferSource = new StringBuffer();
		    reader = new FileReader(inputSelect);
		    try {
		    	 while ((len = reader.read(chr)) > 0) {
		    		 bufferSource.append(chr, 0, len);
		          }
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        if(reader !=null){reader.close();}
		    }
		    
		    selectQryStr  = bufferSource.toString();
		    logger.info("Using "+ EndPoints[i] + " to run construct query:\n" + selectQryStr);
			org.aksw.jena_sparql_api.core.QueryExecutionFactory qef = new QueryExecutionFactoryHttp(EndPoints[i], Graphs[i]);
			
			long timeToLive = 24l * 60l*  60l * 1000l;
			logger.info("QueryExecutionFactoryRetry");
			qef = new QueryExecutionFactoryRetry(qef, 5, 10000);
			logger.info("QueryExecutionFactoryDelay");
			// Add delay in order to be nice to the remote server (delay in milli seconds)
			qef = new QueryExecutionFactoryDelay(qef, 5000);
			QueryExecutionFactoryHttp foo = qef.unwrap(QueryExecutionFactoryHttp.class);
			logger.info(foo);
			// Add pagination
			logger.info("QueryExecutionFactoryPaginated");
			qef = new QueryExecutionFactoryPaginated(qef, 10);
			// Create a QueryExecution object from a query string ...
			logger.info("createQueryExecution");
			QueryExecution qe = qef.createQueryExecution(selectQryStr);
			// and run it.
			logger.info("execSelect");
			ResultSet rs = qe.execSelect();
			System.out.println(ResultSetFormatter.asText(rs));
		}
	}
}	
