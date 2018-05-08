package de.uni_leipzig.simba.learning.learner;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.learner.EuclidMain;

/**
 * Example code on how to run EUCLID upon Ninas files.
 * @author Klaus Lyko
 *
 */
public class LRMain {
	
	/**Separation for the serialization of the Mapping*/
	public static final String SEP = " ";
	
	/**
	 * Basic method to read NT Files out of URL to local file and building Cache for it.
	 * @param address URL of the NT file
	 * @param dest File for the local copy
	 * @return Parsed cache.
	 * @throws IOException
	 */
	public static File NTFileFromURL(String address, String dest) throws IOException {
		URL url = new URL(address);
		File f = new File(dest);
		FileUtils.copyURLToFile(url, f);
		return f;
	}
	
	/**
	 * Parses N-Triples file to a cache representation. Basiclally reads the N-Triples with Jena,
	 * iterates over all statements and adds them to a Cache.
	 * Be aware this is a quick hack, maybe noisy and buggy.
	 * @param file The N-Triples file.
	 * @return Cache representation hopefully holding all triples of the underlying N-Triples.
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public static Cache NTFileToCache(File file) throws UnsupportedEncodingException, FileNotFoundException {
		Cache cache = new MemoryCache();
		
		InputStream in = new FileInputStream(file);
		InputStreamReader reader = new InputStreamReader(in, "UTF8");
		
		
		Model model = ModelFactory.createDefaultModel();
		RDFReader r = model.getReader("N-TRIPLE");
		r.read(model, reader, null);
		
		System.out.println("model "+model.size());


		StmtIterator it =  model.listStatements();
		while (it.hasNext()) {
		     Statement stmt = it.next();
		     // do your stuff with the Statement (which is a triple)
		     System.out.println(stmt.getSubject().toString()+" - "+stmt.getPredicate().toString()+" - "+stmt.getObject().toString());
		     String s = stmt.getSubject().toString();
		     String p = stmt.getPredicate().toString();
		     String o = stmt.getObject().toString();
		     
		     cache.addTriple(s,p,o);
		}
		
		System.out.println("Cache of size "+cache.size()+" buildt");
		return cache;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String url1 = "http://users.ics.forth.gr/~jsaveta/ISWC2015_experiments/10K/value/source.nt";
		String url2 = "http://users.ics.forth.gr/~jsaveta/ISWC2015_experiments/10K/value/target.nt";
		
		File sourceFile = NTFileFromURL(url1, "source.nt");
		File targetFile = NTFileFromURL(url2, "target.nt");
		// for debug
		LRMain lrm = new LRMain();
//		lrm.setOutStreams("euclid_nina");
//		
		
		String types[] = {"linear", "disjunctive", "conjunctive"};
		String typ = "disjunctive"; //disjunctive //conjunctive
		/**This parameter defines the coverage of the properties shared ba all instances to be regarded by EUCLID*/
		double coverage = 0.9; // 0.6 only rdf:type
		int iterations = 3;
		String outputFile = "results_euclid_"+typ+".txt";
		
		Map<String, Cache> sourceMap = inspectModel(sourceFile);
		System.out.println("taregt...");
		Map<String, Cache> targetMap = inspectModel(targetFile);
		boolean append = false;
		for(String p1 : sourceMap.keySet()) {
			if(targetMap.containsKey(p1)) {
				System.out.println("Euclid for rdf:type"+p1+" sC.size="+sourceMap.size()+" - tC.size="+targetMap.get(p1).size());
				EuclidMain em = new EuclidMain();
				Mapping mapP1 = em.runEuclid(sourceMap.get(p1), targetMap.get(p1), coverage, typ, iterations);
				serializeMapping(mapP1, new File(outputFile), append);
				append = true;
			} else {
				System.err.println("Not found source type "+p1+" for target");
			}
			
		}
		
//		// params
//		String url1 = "http://users.ics.forth.gr/~jsaveta/ISWC2015_experiments/10K/value/source.nt";
//		String url2 = "http://users.ics.forth.gr/~jsaveta/ISWC2015_experiments/10K/value/target.nt";
//		
//		String types[] = {"linear", "disjunctive", "conjunctive"};
//		String type = "linear"; //disjunctive //conjunctive
//		/**This parameter defines the coverage of the properties shared ba all instances to be regarded by EUCLID*/
//		double coverage = 0.4; // 0.6 only rdf:type
//		int iterations = 10;
//
		
		
//
//		try {
//			//1st parse Caches
//			//
//			// This avoids handling them via build-in LIMES SPARQL Query modules.
//			// Thus throwing all data about all instances together, without any restrictions
//			// due to rdf:type and such. This means EUCLID won't be as precise if we would parse Caches
//			// using LIMES build-in XML definition to restrict to certain types
//			Cache sC = NTFileFromURLToCache(url1, "source.nt");
//			Cache tC = NTFileFromURLToCache(url2, "target.nt");
//		
//			// running EUCLID
//			for(String typ : types) {
//			
//				Mapping map = em.runEuclid(sC, tC, coverage, typ, iterations);
//				String outputFile = "results_euclid_"+typ+".txt";
//				
//				// write results in your output format to the specified file
//				serializeMapping(map, new File(outputFile));
//			}
//			
//		
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
			
	
	}
	
	/**
	 * Writes Mapping according to Ninas format to file.
	 * @param map Mapping to serialize
	 * @param mapFile File.
	 * @throws IOException
	 */
	public static void serializeMapping(Mapping map, File mapFile, boolean append) throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(mapFile, append)));
		
		// iterating mapping
		for(String uri1 : map.map.keySet()) {
			for(String uri2 : map.map.get(uri1).keySet()) {
				writer.println(expandURL(uri1)+SEP+expandURL(uri2));
			}
				
		}
		
		writer.flush();
		writer.close();
	}
	
	public static String expandURL(String url) {
		String result = url;
		if(url.startsWith("http://")) {
			result ="<"+url+">";
		}
		return result;
	}
	
	/**
	 * Example dummy method on how to manually fill caches based upon triples.
	 * @return Cache instance
	 */
	public static Cache buildDummyCache() {
		//init a cache
		Cache cache = new MemoryCache();
		String props[] = {"ex:name", "ex:someProperty"};
		Random rand = new Random();
		
		for(int i = 0; i<10; i++) {
			
			for(String prop : props) {
				//add triples manually
				cache.addTriple("ex"+i, prop, "text"+rand.nextInt(100));
			}
		}
		return cache;
	}
	
	public void setOutStreams(String name) {
		try {
			File stdFile = new File(name+"_stdOut.txt");
			PrintStream stdOut;
			stdOut = new PrintStream(new FileOutputStream(stdFile, false));
			File errFile = new File(name+"_errOut.txt");
			PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
			System.setErr(errOut);
			System.setOut(stdOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Map<String, Cache> inspectModel(File file) throws UnsupportedEncodingException, FileNotFoundException {
		HashMap<String, Cache> map = new HashMap<String, Cache>();
		InputStream in = new FileInputStream(file);
		InputStreamReader reader = new InputStreamReader(in, "UTF8");
		
		
		Model model = ModelFactory.createDefaultModel();
		RDFReader r = model.getReader("N-TRIPLE");
		r.read(model, reader, null);
		
		System.out.println("model "+model.size());

		
		Property pType = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		NodeIterator nodeIt = model.listObjectsOfProperty(pType);
		int iType = 0;
		while(nodeIt.hasNext()) {
			RDFNode node = nodeIt.next();
			
			System.out.println(iType+++". type: "+node.toString());
			ResIterator resIt = model.listSubjectsWithProperty(pType, node);
			Cache cache = new MemoryCache();
			while(resIt.hasNext()) {
				Resource s = resIt.next();

				Selector selector = new SimpleSelector(s, null,(RDFNode) null);
				
				StmtIterator stmtIt = model.listStatements(selector);
				while(stmtIt.hasNext()) {
					Statement stmt = stmtIt.next();
					if(stmt.getPredicate() != pType) {
						 String sh = stmt.getSubject().toString();
					     String p = stmt.getPredicate().toString();
					     String o = stmt.getObject().toString();					     
					     cache.addTriple(sh,p,o);
					}
				}
			}
			map.put(node.toString(), cache);
		}// for each rdf:type
		return map;
	}
}
