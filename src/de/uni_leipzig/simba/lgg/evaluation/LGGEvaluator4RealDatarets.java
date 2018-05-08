/**
 * 
 */
package de.uni_leipzig.simba.lgg.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jena_sparql_api.retry.core.QueryExecutionFactoryRetry;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.io.rdfconfig.LIMES;
import de.uni_leipzig.simba.io.rdfconfig.RDFConfigReader;
import de.uni_leipzig.simba.lgg.SimpleLGG;
import de.uni_leipzig.simba.lgg.refinementOperator.RefinementNode;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

/**
 * @author sherif
 *
 */
public class LGGEvaluator4RealDatarets {
	private static final Logger logger = Logger.getLogger(LGGEvaluator4RealDatarets.class.getName());
	static Cache source 			= new HybridCache();
	static Cache sourceSample		= new HybridCache();;
	static Cache target 			= new HybridCache();
	static Cache targetSample 		= new HybridCache();
	public static Mapping reference = new Mapping();
	public static String resultStr 	= new String();


	public static void readExternalCaches(String sourceEndPoint, String sourceClassName,
			String targetEndPoint, String targetClassName){
		Model configModel = createConfigFile(sourceEndPoint, sourceClassName, targetEndPoint, targetClassName);
		RDFConfigReader cr = new RDFConfigReader();
		cr.validateAndRead(configModel);
		logger.info("Loading source data ...");
		source = HybridCache.getData(cr.getSourceInfo()); 
		logger.info("Loading target data ..."); 
		target = HybridCache.getData(cr.getTargetInfo()); 
	}



	public static Set<String> getMappingProperities(String endPoint, String classUri){
		Set<String> result = new HashSet<>();
		//1. get 1000 random instances
		String sparqlQueryString = "SELECT DISTINCT ?p {?s a <" + classUri + "> . "
				+ "?s ?p ?o . FILTER(isLiteral(?o) && (langMatches(lang(?o), \"en\") || langMatches(lang(?o), \"\")) )}";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endPoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			Resource p = qs.getResource("?p");
			result.add(p.toString());
			System.out.println("add Properity: " + p);
		}
		qexec.close() ;
		System.out.println(result);
		System.out.println(result.size());
		return result;
	}

	public static Set<String> getDBpediaClassMappingProperities(String endPoint, String classUri){
		Set<String> result = new HashSet<>();
		//1. get 1000 random instances
		String sparqlQueryString = "SELECT DISTINCT ?p {?s a <" + classUri + "> . "
				+ "?s ?p ?o . "
				+ "FILTER(regex(?p, \"^http://dbpedia.org/ontology/\") && "
				+ "isLiteral(?o) && "
				+ "(langMatches(lang(?o), \"EN\") || langMatches(lang(?o), \"\")) )}";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endPoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			Resource p = qs.getResource("?p");
			result.add(p.toString());
			System.out.println("add Properity: " + p);
		}
		qexec.close() ;
		System.out.println(result);
		System.out.println(result.size());
		return result;
	}

	public static Set<String> _getMappingProperities(String endPoint, String classUri){
		Set<String> result = new HashSet<>();
		//1. get 1000 random instances
		String sparqlQueryString = "SELECT DISTINCT ?s {?s a <" + classUri + ">} LIMIT 1000";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endPoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			Resource s = qs.getResource("?s");
			String qString = "SELECT DISTINCT ?p {<" + s.toString() + "> ?p ?o . "
					+ "FILTER(isLiteral(?o) && langMatches(lang(?o), \"en\"))}";
			QueryExecution qExe = QueryExecutionFactory.sparqlService(endPoint, qString);
			ResultSet qResults = qExe.execSelect();
			while(qResults.hasNext()){
				QuerySolution qSol = qResults.nextSolution();
				Resource p = qSol.getResource("?p");
				result.add(p.toString());
				System.out.println("add Properity: " + p);
			}
			qExe.close();
		}
		qexec.close() ;
		return result;
	}


	public static Model createConfigFile(String sourceEndPoint, String sourceClassName,
			String targetEndPoint, String targetClassName) {
		int i = 0;
		Model specsModel = ModelFactory.createDefaultModel();

		//prefixes
		specsModel.setNsPrefix("rdf", RDF.getURI());
		specsModel.setNsPrefix("rdfs", RDFS.getURI());
		specsModel.setNsPrefix("owl", OWL.getURI());
		specsModel.setNsPrefix("dcterms", DCTerms.getURI());


		Resource baseResource   = ResourceFactory.createResource(LIMES.uri + "config");
		Resource sourceResource = ResourceFactory.createResource(LIMES.uri + "source");
		Resource targetResource = ResourceFactory.createResource(LIMES.uri + "target");
		Resource metricResource = ResourceFactory.createResource(LIMES.uri + "metric");
		Resource acceptResource = ResourceFactory.createResource(LIMES.uri + "accept");
		Resource reviewResource = ResourceFactory.createResource(LIMES.uri + "review");

		//Base resource
		specsModel.add(baseResource, RDF.type, LIMES.LimesSpecs);
		specsModel.add(baseResource, LIMES.hasSource, sourceResource);
		specsModel.add(baseResource, LIMES.hasTarget, targetResource);
		specsModel.add(baseResource, LIMES.hasMetric, metricResource);
		specsModel.add(baseResource, LIMES.hasAcceptance, acceptResource);
		specsModel.add(baseResource, LIMES.hasReview, reviewResource);

		//Source
		specsModel.add(sourceResource, RDF.type, LIMES.SourceDataset);
		specsModel.add(sourceResource, RDFS.label, sourceEndPoint);
		specsModel.add(sourceResource, LIMES.endPoint, sourceEndPoint);
		specsModel.add(sourceResource, LIMES.variable, "?x");
		specsModel.add(sourceResource, LIMES.pageSize, "1000");
		String classPrefix = "prf"+ i++;
		String classPrefixUri = getPrefix(sourceClassName);
		specsModel.setNsPrefix(classPrefix, classPrefixUri);
		String sourceClassNamePrefixed = classPrefix + ":" + sourceClassName.replace(classPrefixUri, "");
		specsModel.add(sourceResource, LIMES.restriction, "?x rdf:type " + sourceClassNamePrefixed);
		Set<String> sourceProperities = null;
		if(sourceClassName.startsWith("http://dbpedia.org/")){
			sourceProperities = getDBpediaClassMappingProperities(sourceEndPoint, sourceClassName);
		}else{
			sourceProperities = getMappingProperities(sourceEndPoint, sourceClassName);	
		}
		for(String p : sourceProperities){
			String prefixUri = getPrefix(p);
			String nsURIPrefix = specsModel.getNsURIPrefix(prefixUri);
			String prefix;
			if(nsURIPrefix == null){
				prefix = "prf" + i++ ;
				specsModel.setNsPrefix(prefix, prefixUri);
			}else{
				prefix = nsURIPrefix;
			}
			String prefixedProperty = prefix + ":" + p.replace(prefixUri, "");
			specsModel.add(sourceResource, LIMES.property, prefixedProperty);
		}

		//Target
		specsModel.add(targetResource, RDF.type, LIMES.TargetDataset);
		specsModel.add(targetResource, RDFS.label, targetEndPoint);
		specsModel.add(targetResource, LIMES.endPoint, targetEndPoint);
		specsModel.add(targetResource, LIMES.variable, "?y");
		specsModel.add(targetResource, LIMES.pageSize, "1000");
		classPrefix = "prf"+ i++;
		classPrefixUri = getPrefix(targetClassName);
		specsModel.setNsPrefix(classPrefix, classPrefixUri);
		String targetClassNamePrefixed = classPrefix + ":" + targetClassName.replace(classPrefixUri, "");
		specsModel.add(targetResource, LIMES.restriction,  "?y rdf:type " + targetClassNamePrefixed);
		Set<String> targetProperities = getMappingProperities(targetEndPoint, targetClassName);
		for(String p : targetProperities){
			String prefixUri = getPrefix(p);
			String nsURIPrefix = specsModel.getNsURIPrefix(prefixUri);
			String prefix;
			if(nsURIPrefix == null){
				prefix = "prf" + i++ ;
				specsModel.setNsPrefix(prefix, prefixUri);
			}else{
				prefix = nsURIPrefix;
			}
			String prefixedProperty = prefix + ":" + p.replace(prefixUri, "");
			specsModel.add(targetResource, LIMES.property, prefixedProperty);
		}

		//Metric
		specsModel.add(metricResource, RDF.type, LIMES.Metric);
		specsModel.add(metricResource, LIMES.expression, "");

		//Acceptance
		specsModel.add(acceptResource, RDF.type, LIMES.Acceptance);
		specsModel.add(acceptResource, LIMES.threshold, "0");
		specsModel.add(acceptResource, LIMES.file, "");
		specsModel.add(acceptResource, LIMES.relation, "owl:sameAs");

		//Review
		specsModel.add(reviewResource, RDF.type, LIMES.Review);
		specsModel.add(reviewResource, LIMES.threshold, "1");
		specsModel.add(reviewResource, LIMES.file, "");
		specsModel.add(reviewResource, LIMES.relation, "owl:sameAs");
		return specsModel;
	}


	public static Model createManualConfigFile(String sourceEndPoint, String sourceClassName,
			String targetEndPoint, String targetClassName) {
		int i = 0;
		Model specsModel = ModelFactory.createDefaultModel();

		//prefixes
		specsModel.setNsPrefix("rdf", RDF.getURI());
		specsModel.setNsPrefix("rdfs", RDFS.getURI());
		specsModel.setNsPrefix("owl", OWL.getURI());
		specsModel.setNsPrefix("dcterms", DCTerms.getURI());


		Resource baseResource   = ResourceFactory.createResource(LIMES.uri + "config");
		Resource sourceResource = ResourceFactory.createResource(LIMES.uri + "source");
		Resource targetResource = ResourceFactory.createResource(LIMES.uri + "target");
		Resource metricResource = ResourceFactory.createResource(LIMES.uri + "metric");
		Resource acceptResource = ResourceFactory.createResource(LIMES.uri + "accept");
		Resource reviewResource = ResourceFactory.createResource(LIMES.uri + "review");

		//Base resource
		specsModel.add(baseResource, RDF.type, LIMES.LimesSpecs);
		specsModel.add(baseResource, LIMES.hasSource, sourceResource);
		specsModel.add(baseResource, LIMES.hasTarget, targetResource);
		specsModel.add(baseResource, LIMES.hasMetric, metricResource);
		specsModel.add(baseResource, LIMES.hasAcceptance, acceptResource);
		specsModel.add(baseResource, LIMES.hasReview, reviewResource);

		//Source
		specsModel.add(sourceResource, RDF.type, LIMES.SourceDataset);
		specsModel.add(sourceResource, RDFS.label, sourceEndPoint);
		specsModel.add(sourceResource, LIMES.endPoint, sourceEndPoint);
		specsModel.add(sourceResource, LIMES.variable, "?x");
		specsModel.add(sourceResource, LIMES.pageSize, "1000");
		specsModel.add(sourceResource, LIMES.restriction, "?x rdf:type " + sourceClassName);
		Set<String> sourceProperities = getMappingProperities(sourceEndPoint, sourceClassName);

		for(String p : sourceProperities){
			String prefixUri = getPrefix(p);
			String nsURIPrefix = specsModel.getNsURIPrefix(prefixUri);
			String prefix;
			if(nsURIPrefix == null){
				prefix = "prf" + i++ ;
				specsModel.setNsPrefix(prefix, prefixUri);
			}else{
				prefix = nsURIPrefix;
			}
			String prefixedProperty = prefix + ":" + p.replace(prefixUri, "");
			specsModel.add(sourceResource, LIMES.property, prefixedProperty);
		}

		//Target
		specsModel.add(targetResource, RDF.type, LIMES.TargetDataset);
		specsModel.add(targetResource, RDFS.label, targetEndPoint);
		specsModel.add(targetResource, LIMES.endPoint, targetEndPoint);
		specsModel.add(targetResource, LIMES.variable, "?y");
		specsModel.add(targetResource, LIMES.pageSize, "1000");
		specsModel.add(targetResource, LIMES.restriction,  "?y rdf:type " + targetClassName);
		Set<String> targetProperities = getMappingProperities(targetEndPoint, targetClassName);
		for(String p : targetProperities){
			String prefixUri = getPrefix(p);
			String nsURIPrefix = specsModel.getNsURIPrefix(prefixUri);
			String prefix;
			if(nsURIPrefix == null){
				prefix = "prf" + i++ ;
				specsModel.setNsPrefix(prefix, prefixUri);
			}else{
				prefix = nsURIPrefix;
			}
			String prefixedProperty = prefix + ":" + p.replace(prefixUri, "");
			specsModel.add(targetResource, LIMES.property, prefixedProperty);
		}

		//Metric
		specsModel.add(metricResource, RDF.type, LIMES.Metric);
		specsModel.add(metricResource, LIMES.expression, "");

		//Acceptance
		specsModel.add(acceptResource, RDF.type, LIMES.Acceptance);
		specsModel.add(acceptResource, LIMES.threshold, "0");
		specsModel.add(acceptResource, LIMES.file, "");
		specsModel.add(acceptResource, LIMES.relation, "owl:sameAs");

		//Review
		specsModel.add(reviewResource, RDF.type, LIMES.Review);
		specsModel.add(reviewResource, LIMES.threshold, "1");
		specsModel.add(reviewResource, LIMES.file, "");
		specsModel.add(reviewResource, LIMES.relation, "owl:sameAs");
		return specsModel;
	}

	/**
	 * @param p
	 * @return
	 * @author sherif
	 */
	private static String getPrefix(String p) {
		String prefix = null;
		if(p.contains("#")){
			prefix = p.substring(0, p.lastIndexOf("#")+1);
		}else if(p.contains("/")){
			prefix = p.substring(0, p.lastIndexOf("/")+1);
		}
		return prefix;
	}

	/**
	 * Computes a sample of the reference dataset for experiments
	 */
	public static Mapping sampleReference(Mapping reference, double fraction) {
		if(fraction == 1){
			return reference;
		}
		int mapSize = reference.map.keySet().size();
		if (fraction > 1) {
			fraction = 1 / fraction;
		}
		int size = (int) (mapSize * fraction);
		Set<Integer> index = new HashSet<>();
		//get random indexes
		for (int i = 0; i < size; i++) {
			int number;
			do {
				number = (int) (mapSize * Math.random()) - 1;
			} while (index.contains(number));
			index.add(number);
		}

		//get data
		Mapping sample = new Mapping();
		int count = 0;
		for (String key : reference.map.keySet()) {
			if (index.contains(count)) {
				sample.map.put(key, reference.map.get(key));

			}
			count++;
		}

		// compute sample size
		for (String key : sample.map.keySet()) {
			for (String value : sample.map.get(key).keySet()) {
				sample.size++;
			}
		}
		return sample;
	}

	/**
	 * Extract the source and target caches triples based on the input sample
	 * a fallback is implemented where to try to get the data through content negotiation
	 * @param sample
	 * @return
	 * @author sherif
	 */
	private static void fillSampleSourceTargetCaches(Mapping sample) {

		for (String s : sample.map.keySet()) {
			if(source.containsUri(s)){
				sourceSample.addInstance(source.getInstance(s));
				for (String t : sample.map.get(s).keySet()) {
					if(target.containsUri(t)){
						targetSample.addInstance(target.getInstance(t));
					}else{
						logger.warn("Instance " + t + " not exist in the target dataset");
						//						logger.info("Fallback: trying to get " + t + " through content negotiation...");
						//						targetSample.addInstance(fallback(t));
					}
				}
			}else{
				logger.warn("Instance " + s + " not exist in the source dataset");
				//				logger.info("Fallback: trying to get " + s + " through content negotiation...");
				//				sourceSample.addInstance(fallback(s));
			}
		}

	}




	private static Instance fallback(String t) {
		Model m = readModel(t);
		Instance instance = new Instance(t);
		StmtIterator triples = m.listStatements();
		while(triples.hasNext()){
			Statement triple = triples.next();
			instance.addProperty(triple.getSubject().toString(), triple.getObject().toString());
		}
		return instance;
	}


	public static Model saveExternalDataToFile(String endPoint,String graph, String constructQryStr, String fileName) throws Exception{
		logger.info("Using "+ endPoint + " to run constrauct query:\n" + constructQryStr);
		org.aksw.jena_sparql_api.core.QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endPoint, graph);
		qef = new QueryExecutionFactoryRetry(qef, 5, 10000);
		qef = new QueryExecutionFactoryDelay(qef, 5000);
		long timeToLive = 24l * 60l * 60l * 1000l; 
		QueryExecutionFactoryHttp foo = qef.unwrap(QueryExecutionFactoryHttp.class);
		System.out.println(foo);
		qef = new QueryExecutionFactoryPaginated(qef, 900);
		QueryExecution qe = qef.createQueryExecution(constructQryStr);
		Model m = qe.execConstruct();
		saveModel(m, "N-TRIPLE", fileName);
		return m;
	}

	private static void saveModel(Model model, String format, String outputFile) throws IOException
	{
		logger.info("Saving dataset to " + outputFile + "...");
		long starTime = System.currentTimeMillis();
		FileWriter fileWriter = new FileWriter(outputFile);
		model.write(fileWriter, format);
		logger.info("Saving file done in " + (System.currentTimeMillis() - starTime) +"ms.");
	}

	/**
	 * @param args
	 * @author sherif
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String prefix = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX lgdo: <http://linkedgeodata.org/ontology/>\n"
				+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
				+ "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
				+ "PREFIX agc: <http://www.opengis.net/ont/geosparql#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX geom: <http://geovocab.org/geometry#>\n";
		String sourceConstructStr = prefix +
				"CONSTRUCT {?s rdfs:label ?l . ?s geom:geometry ?g . ?s owl:sameAs ?o . } "
				+ "WHERE { "
				+ "?s rdf:type dbpedia-owl:City . "
				+ "?s owl:sameAs ?o . FILTER(regex(?o, \"^http://linkedgeodata.org\")) "
				+ "?s geo:geometry ?g . "
				+ "?s rdfs:label ?l  FILTER ( lang(?l) = \"en\") . "
				+ "}";
		String targetConstructStr = prefix +
				"CONSTRUCT {?s rdfs:label ?l . ?s geom:geometry ?g . } "
				+ "WHERE { "
				+ "?s rdf:type lgdo:Town . "
				+ "?s geom:geometry/agc:asWKT ?g . "
				+ "?s rdfs:label ?l  FILTER ( lang(?l) = \"en\" || lang(?l) = \"\") . "
				+ "}";
		String dbpedia39EndPoint = "http://139.18.2.164:3033/ds/sparql "; 
		String dbpedia2014EndPoint = "http://139.18.2.164:3033/ds/sparq";
		String sourceGraph = "http://dbpedia.org";
		String lgdEndPoint = "http://linkedgeodata.org/sparql";
		String targetGraph = "http://linkedgeodata.org";
		String dbpedia39CitiesFile = "src/de/uni_leipzig/simba/lgg/evaluation/dbpedia39Cities.nt";
		String dbpedia2014CitiesFile = "src/de/uni_leipzig/simba/lgg/evaluation/dbpedia2014Cities.nt";
		String lgd39TownsFile = "src/de/uni_leipzig/simba/lgg/evaluation/lgd39Towns.nt";
		String lgd2014TownsFile = "src/de/uni_leipzig/simba/lgg/evaluation/lgd2014Towns.nt";;
		String refMappingFile39Cities = "src/de/uni_leipzig/simba/lgg/evaluation/refMap.nt";
		if(!(new File(dbpedia39CitiesFile)).exists()) { 
			saveExternalDataToFile(dbpedia39EndPoint, sourceGraph, sourceConstructStr, dbpedia39CitiesFile);
		}
		if(!(new File(lgd39TownsFile)).exists()) { 
			saveExternalDataToFile(lgdEndPoint, targetGraph, targetConstructStr, lgd39TownsFile);
		}
		if(!(new File(dbpedia2014CitiesFile)).exists()) { 
			saveExternalDataToFile(dbpedia2014EndPoint, sourceGraph, sourceConstructStr, dbpedia2014CitiesFile);
		}
		if(!(new File(lgd2014TownsFile)).exists()) { 
			saveExternalDataToFile(lgdEndPoint, targetGraph, targetConstructStr, lgd2014TownsFile);
		}
		evaluate(dbpedia39CitiesFile, lgd39TownsFile, refMappingFile39Cities,dbpedia2014CitiesFile, lgd2014TownsFile);
	}

	private static String evaluate(String sourceOldFile, String targetOldFile, 
			String refMappingFile, String sourceNewFile, String targetNewFile) throws ClassNotFoundException {
		resultStr = "lP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";

		logger.info("Loading Old Source Data ...");
		Model sourceModel = readModel(sourceOldFile);
		StmtIterator triples = sourceModel.listStatements();
		while (triples.hasNext()) {
			Statement triple = triples.next();
			source.addTriple(triple.getSubject().toString(), triple.getPredicate().toString(), triple.getObject().toString());
		}
		logger.info("Loading Old Target Data ...");
		Model targetModel = readModel(targetOldFile);
		triples = targetModel.listStatements();
		while (triples.hasNext()) {
			Statement triple = triples.next();
			target.addTriple(triple.getSubject().toString(), triple.getPredicate().toString(), triple.getObject().toString());
		}
		logger.info("Loading Old Source-Target Reference Mapping ...");
		//		model = readModel(refMappingFile);
		triples = sourceModel.listStatements(null, OWL.sameAs, (Resource) null);
		while (triples.hasNext()) {
			Statement triple = triples.next();
			if(targetModel.contains(triple.getObject().asResource(), null,(RDFNode) null)){
				reference.add(triple.getSubject().toString(), triple.getObject().toString(), 1d);
			}
		}
		System.out.println(reference);
		String metricExpr = learnSpecsFromOldVersion();
		logger.info("Learned Specs = " + metricExpr);
		System.out.println("Larning Results:\n" + resultStr);
		return evaluateNewVersion(metricExpr, sourceNewFile, targetNewFile);
	}


	private static String learnSpecsFromOldVersion() {
		logger.info("Learning from DBpedia 3.9 ...");
		fillSampleSourceTargetCaches(reference);
		long start = System.currentTimeMillis();
		SimpleLGG clgg = new SimpleLGG(sourceSample, targetSample, reference, 0.6);
		RefinementNode bestSolusion = clgg.getBestSolution();
		Mapping mapSample = bestSolusion.map;

		PRFCalculator prf = new PRFCalculator();
		String metricExpr = bestSolusion.metricExpression;
		resultStr +=  
				prf.precision(mapSample, reference)+ "\t" + 
				prf.recall(mapSample, reference) 	+ "\t" + 
				prf.fScore(mapSample, reference) 	+ "\t" +
				(System.currentTimeMillis() - start) 			+ "\t" +
				metricExpr 					+ "\t" ;
		return metricExpr;
	}
	
	private static String evaluateNewVersion(String metricExpr, String sourceNewFile, String targetNewFile) {
		logger.info("Loading New Source Data ...");
		source = new HybridCache(); 
		Model sourceModel = readModel(sourceNewFile);
		StmtIterator triples = sourceModel.listStatements();
		while (triples.hasNext()) {
			Statement triple = triples.next();
			source.addTriple(triple.getSubject().toString(), triple.getPredicate().toString(), triple.getObject().toString());
		}
		logger.info("Loading New Target Data ...");
		target = new HybridCache(); 
		Model targetModel = readModel(targetNewFile);
		triples = targetModel.listStatements();
		while (triples.hasNext()) {
			Statement triple = triples.next();
			target.addTriple(triple.getSubject().toString(), triple.getPredicate().toString(), triple.getObject().toString());
		}
		logger.info("Loading Old Source-Target Reference Mapping ...");
		reference = new Mapping();
		triples = sourceModel.listStatements(null, OWL.sameAs, (Resource) null);
		while (triples.hasNext()) {
			Statement triple = triples.next();
			if(targetModel.contains(triple.getObject().asResource(), null,(RDFNode) null)){
				reference.add(triple.getSubject().toString(), triple.getObject().toString(), 1d);
			}
		}
		SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
				new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
		long start = System.currentTimeMillis();
		String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
		Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
		Mapping kbMap = mapper.getLinks(expression, threshold);
		resultStr += PRFCalculator.precision(kbMap, reference)	+ "\t" + 
				PRFCalculator.recall(kbMap, reference) 	 	+ "\t" + 
				PRFCalculator.fScore(kbMap, reference) 	 	+ "\t" +
				(System.currentTimeMillis() - start) 		+ "\n" ;
		System.out.println("Final rasults:\n" + resultStr);
		return resultStr;
	}


	private static String runEvaluations() {
		for(int s = 10 ; s <= 10 ; s +=1){
			logger.info("Running experiment with positive example size = " +  s*10 + "%");
			Mapping referenceSample = sampleReference(reference, s/10f);
			fillSampleSourceTargetCaches(referenceSample);

			// 1. Learning phase
			long start = System.currentTimeMillis();
			SimpleLGG clgg = new SimpleLGG(sourceSample, targetSample, referenceSample, 0.6);
			RefinementNode bestSolusion = clgg.getBestSolution();
			Mapping mapSample = bestSolusion.map;

			PRFCalculator prf = new PRFCalculator();
			String metricExpr = bestSolusion.metricExpression;
			resultStr +=  (int) s*10 + "%"							+ "\t" + 
					prf.precision(mapSample, referenceSample)+ "\t" + 
					prf.recall(mapSample, referenceSample) 	+ "\t" + 
					prf.fScore(mapSample, referenceSample) 	+ "\t" +
					(System.currentTimeMillis() - start) 			+ "\t" +
					metricExpr 					+ "\t" ;

			// 2. Apply for the whole KB
			SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
					new KBInfo("?x"), new KBInfo("?y"), source, target, new LinearFilter(), 2);
			start = System.currentTimeMillis();
			String expression = metricExpr.substring(0, metricExpr.lastIndexOf("|"));
			Double threshold = Double.parseDouble(metricExpr.substring(metricExpr.lastIndexOf("|")+1, metricExpr.length()));
			Mapping kbMap = mapper.getLinks(expression, threshold);

			resultStr += prf.precision(kbMap, reference)	+ "\t" + 
					prf.recall(kbMap, reference) 	 	+ "\t" + 
					prf.fScore(kbMap, reference) 	 	+ "\t" +
					(System.currentTimeMillis() - start) 		+ "\n" ;
			System.out.println("Results so far:\n" + resultStr);
		}
		System.out.println("Final rasults:\n" + resultStr);

		return resultStr;
	}




	private static String evaluate(String configFile, String endPoint, String classUri, String linkingProperty) throws ClassNotFoundException {
		resultStr = "Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";

		Model configModel = readModel(configFile);
		RDFConfigReader cr = new RDFConfigReader();
		cr.validateAndRead(configModel);
		logger.info("Loading Source Data ...");
		source = HybridCache.getData(cr.getSourceInfo()); System.out.println(source.size());
		logger.info("Loading Target Data ...");
		target = HybridCache.getData(cr.getTargetInfo()); System.out.println(target.size());
		logger.info("Loading Source-Target Mapping ...");
		reference = readReferenceMapping(endPoint, classUri, linkingProperty); System.out.println(reference.size());

		return runEvaluations();
	}


	public static Mapping readReferenceMapping(String endPoint, String classUri, String linkingProperty) throws ClassNotFoundException{
		Mapping result = new Mapping();
		String refMapSerFile = "/home/sherif/JavaProjects/LIMES/cache/refMap.ser";
		try{
			logger.info("Looking for cached reference mapping");
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(refMapSerFile));
			result = (Mapping) in.readObject();
			in.close();
			logger.info("Cached reference mapping found and loaded");
		}catch(IOException i){
			logger.info("Cached reference mapping not found, read reference mapping from end point: " + endPoint);
			String sparqlQueryString = "SELECT DISTINCT ?s ?o {"
					+ "?s a <" + classUri + "> . "
					+ "?s <" + linkingProperty + "> ?o . "
					+ "FILTER(regex(?o, \"^http://linkedgeodata.org\"))}";

			QueryExecution qexec = QueryExecutionFactory.sparqlService(endPoint, sparqlQueryString);
			ResultSet queryResults = qexec.execSelect();
			while(queryResults.hasNext()){
				QuerySolution qs = queryResults.nextSolution();
				Resource s = qs.getResource("?s");
				Resource o = qs.getResource("?o");
				result.add(s.toString(), o.toString() , 1d);
				System.out.println("Found mapping entry: " + s + " <---> " + o);
			}
			qexec.close() ;
			System.out.println(result);
			System.out.println(result.size());
			try{
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(refMapSerFile));
				out.writeObject(result);
				out.close();
				logger.info("Serialized reference mapping is saved in " + refMapSerFile);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return result;
	}

	public static Model readModel(String fileNameOrUri)
	{
		long startTime = System.currentTimeMillis();
		Model model=ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(fileNameOrUri + " not found");
		}
		if(fileNameOrUri.contains(".ttl") || fileNameOrUri.contains(".n3")){
			logger.info("Opening Turtle file");
			model.read(in, null, "TTL");
		}else if(fileNameOrUri.contains(".rdf")){
			logger.info("Opening RDFXML file");
			model.read(in, null);
		}else if(fileNameOrUri.contains(".nt")){
			logger.info("Opening N-Triples file");
			model.read(in, null, "N-TRIPLE");
		}else{
			logger.info("Content negotiation to get RDFXML from " + fileNameOrUri);
			model.read(fileNameOrUri);
		}
		logger.info("Loading " + fileNameOrUri + "(" + model.size()+" statements) is done in " + 
				(System.currentTimeMillis()-startTime) + "ms.");
		return model;
	}


}
