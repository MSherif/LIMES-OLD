package de.uni_leipzig.simba.genetics.evaluation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.execution.Instruction.Command;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.genetics.core.LinkSpecGeneticLearnerConfig;
import de.uni_leipzig.simba.genetics.core.PseudoFMeasureFitnessFunction;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.statistics.Statistics;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.oracle.mappingreader.CSVMappingReader;
import de.uni_leipzig.simba.learning.oracle.mappingreader.XMLMappingReader;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.query.ModelRegistry;
import de.uni_leipzig.simba.query.QueryModule;
import de.uni_leipzig.simba.query.QueryModuleFactory;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.selfconfig.SizeAwarePseudoFMeasure;
import de.uni_leipzig.simba.selfconfig.SizeUnawarePseudoFMeasure;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;
/**
 * Another testing set, to run certain LinkSpecs
 * @author Klaus Lyko
 *
 */
public class Tester {
	static Logger log = Logger.getLogger("LIMES");


	
	public static void testLinkSpec() {
		EvaluationData data = DataSetChooser.getData(DataSets.PERSON1_CSV);
		System.out.println(data);
		List<Pair<String>> props=data.getPropertyMapping().stringPropPairs;
		List<LinkSpec> children = new LinkedList<LinkSpec>();
		LinkSpec parent = new LinkSpec();
		parent.operator = Operator.AND;
		parent.threshold = 0.5d;
//		parent.
		for(Pair<String> prop : props) {
			LinkSpec spec = new LinkSpec();
			spec.setAtomicFilterExpression("trigrams",prop.a, prop.b);
			spec.threshold = 0.7d;
			spec.parent = parent;
			System.out.println(spec.isAtomic());
			parent.addChild(spec);
		}
		AlgebraicRewriter ar = new AlgebraicRewriter();
        parent = ar.rewrite(parent);
		System.out.println(parent +"\n parent.isAtomic?"+parent.isAtomic());
		
		ExecutionEngine eng = new ExecutionEngine(data.getSourceCache(), 
				data.getTargetCache(), data.getConfigReader().sourceInfo.var, 
				data.getConfigReader().targetInfo.var);

		ExecutionPlanner planner = new CanonicalPlanner();
		Instruction inst = new Instruction(Command.RUN, "AND", "0.5", 1, 1, 1);
		NestedPlan nested = planner.plan(parent);
//		nested.addInstruction()
		System.out.println(nested.operator);
		System.out.println(nested.getInstructionList());
		for(NestedPlan sub : nested.subPlans) {
			System.out.println("Subplan="+sub);
		}
//		nested.addInstruction(inst);
//		System.out.println(nested.subPlans)
//		nested.addInstruction()
		eng.runNestedPlan(nested);
	}
	
	
	public static void main(String args[]) {
		testLinkSpec();
	}
	
	public static void urlDecode() throws MalformedURLException, UnsupportedEncodingException {
		String s = "lgdp:official_name%3Ael";
//		String res = s;

			System.out.println( URLDecoder.decode(s, "UTF-8"));
	}
	
	public static void loadingdata() {
		ConfigReader cf = new ConfigReader();
		cf.validateAndRead("C:\\Users\\Lyko\\Downloads\\linkspec_countries.xml");
		HybridCache.getData(cf.getTargetInfo());
	}
	
	
	
	public static void testModelRegistry() throws FileNotFoundException, UnsupportedEncodingException {
		KBInfo kb = new KBInfo();
		kb.id = "ACM";
		kb.endpoint = "C:\\Users\\Lyko\\workspace\\LIMES\\Examples\\GeneticEval\\Datasets\\DBLP-ACM\\ACM.csv";
		kb.type = "CSV";
		QueryModule qm = QueryModuleFactory.getQueryModule("csv", kb);
//		qm.fillCache(c)
		System.out.println(qm);


         Model model = ModelFactory.createDefaultModel();
         RDFReader r = model.getReader(kb.type);
         InputStream in = new FileInputStream(kb.endpoint);
         InputStreamReader reader = new InputStreamReader(in, "UTF8");
         r.read(model, reader, null);
         System.out.println("RDF model read from "+kb.endpoint+" is of size "+model.size());
         ModelRegistry.register(kb.endpoint, model);
		
//		ModelRegistry.register(info.endpoint, qm);
		Model model2 = ModelRegistry.getInstance().getMap().get(kb.endpoint);
		
		System.out.println(model);
		
		System.out.println(model2);
	}
	
	public static void readConfigProperties() {
		String configFile = "PublicationData.xml";
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile);
		
		KBInfo source = cR.getSourceInfo();
		String prefix = source.var;
		if(prefix.startsWith("?") && prefix.length()>=2)
			prefix=prefix.substring(1);
		
		
		for(String prop : source.properties) {
			System.out.println(prefix+"."+prop);
		}
		
		
	}
	
	
	public static void testMeasures() {
		Mapping a = new Mapping();
		a.add("a", "a", 1.0);
		a.add("a", "aa", 0.45);
		a.add("b", "b", 1.0);
		List<String> sourceUris = new LinkedList<String>();
		List<String> targetUris = new LinkedList<String>();
		sourceUris.addAll(a.map.keySet());
		for(HashMap<String, Double> e : a.map.values())
			targetUris.addAll(e.keySet());
		Mapping aref = new Mapping();
		aref.add("a", "a", 1.0);
//		a.add("a", "aa", 0.45);
		aref.add("b", "b", 1.0);
		System.out.println("Mapping a - aref\n"+a+"\n"+aref);
		PRFCalculator prf = new PRFCalculator();
		System.out.println("f="+prf.fScore(Mapping.getBestOneToOneMappings(a), aref));
		System.out.println("p="+prf.precision(Mapping.getBestOneToOneMappings(a), aref));
		System.out.println("r="+prf.recall(Mapping.getBestOneToOneMappings(a), aref));
		System.out.println("sourceUris="+sourceUris);
		System.out.println("targetUris="+targetUris);
		Measure[] ms = {new SizeAwarePseudoFMeasure(), new SizeUnawarePseudoFMeasure()};
		for(Measure m : ms) {
			System.out.println(m.getName());
			Double pfm = m.getPseudoFMeasure(sourceUris, targetUris, a, 1.0);
			Double ppm = m.getPseudoPrecision(sourceUris, targetUris, a);
			Double prm = m.getPseudoRecall(sourceUris, targetUris, a);
			System.out.println("pfm = "+pfm);
			System.out.println("ppm = "+ppm);
			System.out.println("prm = "+prm);
		}
		
	}
	
	public static void testOracleReader() {
		String folder = "C:\\Users\\Lyko\\workspace\\LIMES\\resources\\Persons1\\";
		String xml = "dataset11_dataset12_goldstandard_person.xml";	String csv = "dataset11_dataset12_goldstandard_person.xml.csv";
		 Mapping m = new XMLMappingReader().getMapping(folder+xml);
		 Mapping m2 = new CSVMappingReader().getMapping(folder+csv);
	        System.out.println(m.size()+ " vs. " + m2.size());
	}
	
	public static void testFormatting() {
		Statistics stati = new Statistics();
		stati.add(0.5d);
//		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
//		df.applyPattern( "#,###,######0.00000" );
		System.out.println(stati);
		System.out.println(stati.standardDeviation);
//		System.out.println(df.format(stati.standardDeviation));
	}
	
	public static void run(EvaluationData params, String expression, double threshold) throws InvalidConfigurationException {
//		ConfigReader cR = new ConfigReader();
//		cR.validateAndRead((String)params.get(MapKey.BASE_FOLDER)+params.get("config"));
//		
		ConfigReader cR = params.getConfigReader();
		Cache sC = params.getSourceCache();
		Cache tC = params.getTargetCache();
		Mapping reference = params.getReferenceMapping();

		PropertyMapping pM = params.getPropertyMapping();
	
		LinkSpecGeneticLearnerConfig config = new LinkSpecGeneticLearnerConfig(cR.sourceInfo, cR.targetInfo, pM);
		config.setCrossoverProb(0.4f);
		config.setMutationProb(0.4f);
		config.setReproductionProb(0.4f);
		config.setPopulationSize(100);
		config.setPreservFittestIndividual(true);
		PseudoFMeasureFitnessFunction fitness = PseudoFMeasureFitnessFunction.getInstance(config, new PseudoMeasures(), sC, tC);
		
		Mapping bestMapping = fitness.getMapping(expression, threshold);
//		System.out.println(bestMapping);
		System.out.println(bestMapping.size());
		double prec, recall, fMeasure;
		PRFCalculator prf = new PRFCalculator();
		prec = prf.precision(bestMapping, reference);
		recall = prf.recall(bestMapping, reference);
		fMeasure = prf.fScore(bestMapping, reference);
		
		System.out.println("prec="+prec+" recall="+ recall+" fscore="+ fMeasure);
		System.out.println("best Mapping size:"+bestMapping.size());
//		System.out.println("reference size: "+reference.size());
	}
	
	public static void run(ConfigReader cR) {
		
		
		//try reading into oracle
	//	Oracle o = OracleFactory.getOracle(file, "csv", "simple");
	//	Mapping optimalMapping = o.getMapping();
		//o.loadData(optimalMapping);
	
		//optimalMapping = ExampleOracleTrimmer.trimExamples(o, 500);
		
		// create Caches
		
		HybridCache sC = HybridCache.getData(cR.getSourceInfo());	
		HybridCache tC = HybridCache.getData(cR.getTargetInfo());
	
	//	HybridCache[] trimedCaches = ExampleOracleTrimmer.processData(sC, tC, optimalMapping);
	//	sC = trimedCaches[0];
	//	tC = trimedCaches[1];
//		Filter f = new LinearFilter();
		// call Mapper	
		
//		SetConstraintsMapper sCM = SetConstraintsMapperFactory.getMapper("simple", 
//				cR.sourceInfo, cR.targetInfo, 
//				sC, tC, f, cR.granularity);
		SetConstraintsMapper sCM = SetConstraintsMapperFactory.getMapper(cR.executionPlan,
                cR.sourceInfo, cR.targetInfo, sC, tC, new LinearFilter(), cR.granularity);
//		Mapping actualMapping = f.filter(sCM.getLinks(cR.metricExpression, cR.acceptanceThreshold), cR.acceptanceThreshold);
//		Mapping actualMapping = sCM.getLinks(split[0], Double.parseDouble(split[1]));
		Mapping actualMapping = sCM.getLinks(cR.metricExpression, cR.acceptanceThreshold);
		
		log.info("Found "+actualMapping.size()+" links.");
//		
//		PRFComputer prfC = new PRFComputer();
//		double fScore = prfC.computeFScore(actualMapping, optimalMapping);
//		log.info("Precision="+prfC.computePrecision(actualMapping, optimalMapping));
//		log.info("Recall="+prfC.computeRecall(actualMapping, optimalMapping));
//		log.info("F-Score = "+fScore);
		
//		for(String a:sC.getAllUris()) {
//			System.out.println(a);
//		}
//		System.out.println(actualMapping);
//		for(int i =0; i<10; i++) {
//			Instance inst = sC.getAllInstances().get(i);
//			System.out.println(inst);
//		}
	}
	
}
