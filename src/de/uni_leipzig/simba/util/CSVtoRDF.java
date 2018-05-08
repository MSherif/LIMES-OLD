package de.uni_leipzig.simba.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.jena.riot.RiotException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.keydiscovery.IKeyDiscovery;
import de.uni_leipzig.simba.keydiscovery.RockerKeyDiscovery;
import de.uni_leipzig.simba.keydiscovery.model.CandidateNode;

public class CSVtoRDF {
	
	public static void main(String margs[]) {		
		EvaluationData data = DataSetChooser.getData(DataSets.RESTAURANTS);
		
		File f = CacheToJenaModelNTSerialization(data.getTargetCache(), "", "", 
				"http://www.okkam.org/ontology_restaurant2.owl#Restaurant", "Restaurant2.nt");
	
		testRocker(f, "http://www.okkam.org/ontology_restaurant2.owl#Restaurant");
	}
	
	public static File CacheToJenaModelNTSerialization(Cache data, String baseURI, String IDbaseURI, String rdfType, String outputFile) {
		Cache cache = data;
		Model m = cache.parseCSVtoRDFModel(baseURI, IDbaseURI, rdfType);
		
//		System.out.println("Model size: "+m.size());
//		StmtIterator stmtIt = m.listStatements();
//		int count = 0;
//		while(stmtIt.hasNext()) {
//			Statement stmt = stmtIt.next();
//			System.out.println(stmt);
//			count++;
//		}
//		System.out.println("#Stmt"+count);
		
		// write n-triples
		File out = new File(outputFile);
		OutputStream fos;
		try {
			fos = new BufferedOutputStream(new FileOutputStream(out, false));

			m.write(fos, "N-TRIPLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	public static void testRocker(File f, String rdfType) {
//		Model m = ModelFactory.createDefaultModel();
//		int errors = 0;
//		try {
//			m.read("file:///"+f.getAbsolutePath(), null, "N-TRIPLES");
//		} catch (RiotException e) {
//			errors++;
//			
//		}
//		System.out.println("#RIOT-Errors: "+errors);
		
		IKeyDiscovery rocker = new RockerKeyDiscovery();
		rocker.init("acm", "file:///"+f.getAbsolutePath(), rdfType, false, 0.5, false);
		rocker.run();
		Set<CandidateNode> nodes = rocker.getResults();
		for(CandidateNode node : nodes) {
			System.out.println("node "+node.getProperties()+" score:="+node.getScore());
		}
	}
	

//	public static void testJENA_CSV() {
//		String dblpFile = "C:\\Users\\Lyko\\workspace2\\LIMES\\Examples\\GeneticEval\\Datasets\\DBLP-ACM\\DBLP3.csv";
//		LangCSV.register();
//		Model model = ModelFactory.createModelForGraph(new GraphCSV(dblpFile));
//		
//		
//		String query = "SELECT ?s ?p ?o {" +
//				"?s ?p ?o." +
//				"}";
//		QueryExecution qExec = QueryExecutionFactory.create(query, model) ;
//		ResultSet rs = qExec.execSelect(); int count = 0;
//		while(rs.hasNext()) {
//			QuerySolution qs = rs.next();
//			System.out.println(qs.toString());
//			count++;
//			if(count>10)
//				break;
//		}
//	}
	
	
//	public static void testMultiValueMap() {
//		Cache sc = new MemoryCache();
//		Cache tc = new MemoryCache();
//		
//		Instance i1 = new Instance("i1");
//		i1.addProperty("author", "Axel-Cyrille Ngonga Ngomo");
//		i1.addProperty("author", "Klaus Lyko");
//		
//		Instance i2 = new Instance("i2");
//		
//		i2.addProperty("author", "Jens Lehmann");
//		i2.addProperty("author", "Axel-Cyrille Ngonga Ngomo");
//		
//		Instance i3 = new Instance("i3");
//		i3.addProperty("author", "MAx Mustermann");
//		
//		Instance i4 = new Instance("i4");
//		i4.addProperty("author", "Axel-Cyrille Ngonga Ngomo");
//		i4.addProperty("author", "Klaus Lyko");	
//		
//		sc.addInstance(i1);
//		sc.addInstance(i3);
//		
//		tc.addInstance(i2);
//		tc.addInstance(i4);
//		
//		ExecutionPlanner planner = new CanonicalPlanner();
//		ExecutionEngine engine;
//		engine = new ExecutionEngine(sc, tc, "?x", "?y");
//		LinkSpec spec = new LinkSpec();
//		spec.setAtomicFilterExpression("cosine", "x.author", "y.author");
//		spec.threshold = 0.8;
//		Mapping mapping = engine.runNestedPlan(planner.plan(spec));
//		
//		System.out.println(mapping);
//	}
	
}
