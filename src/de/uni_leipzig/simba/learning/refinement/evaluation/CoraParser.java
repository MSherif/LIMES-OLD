package de.uni_leipzig.simba.learning.refinement.evaluation;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.jgap.InvalidConfigurationException;


import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.PseudoEvaluation;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.refinement.RefinementBasedLearningAlgorithm;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;

public class CoraParser {
	
	HashMap<String, List<Integer>> same = new HashMap<String, List<Integer>>();
	HashMap<String, List<String>> entries = new HashMap<String, List<String>>();
	
	List<CoraReference> refList = new LinkedList<CoraReference>();
	Mapping reference = new Mapping();
	Cache sC = new MemoryCache();
	Cache tC = new MemoryCache();
	
	  public static void main( String[] args ) throws Exception {
		  CoraParser cora = new CoraParser();
		  File file = new File("resources/cora-ref/fahl-labeled");
//		  cora.testEagle(file, "fahl");
		  cora.testRefiner(file, "fahl");
//		  EvaluationData data = cora.loadCoraData(file, "fahl");
//		  System.out.println("sC:"+data.getSourceCache().size());
//		  System.out.println("tC:"+data.getTargetCache().size());
//		  System.out.println("ref:"+data.getReferenceMapping().size());
	  }
	  
	  
	  public void readFile(File f) throws IOException {
		  BufferedReader in = new BufferedReader(new FileReader(f));
		  String line = null;//in.readLine();
		  int refNr = -1;
		  while((line = in.readLine()) != null) {
			  
			  if(line.trim().length()!=0) {
//				  System.out.println(line);
				  line = line.trim();
				  if(line.startsWith("<NEWREFERENCE>")) {
					  String refNrString = line.substring(14);
					  if(refNrString.length()>0) //fahl-labeled doesn't contain refNr
						  refNr = Integer.parseInt(refNrString);
					  else
						  refNr ++;
				  } else {
					  parseRef(line, refNr);
				  }
			  }
//			  line = in.readLine();
		  }//finished reading
		  
		  
//		  for(Entry<String, List<String>> e : entries.entrySet()) {
//			  System.out.println("\n"+e.getKey()+": ");
//			  for(String l : e.getValue()) {
//				  System.out.println("\t"+l);
//			  }
//		  }
		  in.close();
	  }
	  
	  public void parseRef(String line, int nr) {
		  line = line.trim();
		  //reference key to build gold standard
//		  System.out.println(nr+"Parsing: "+line);
		  String index = line.substring(0, Math.min(line.indexOf("<", 0), line.indexOf(" ")));
		  if(same.containsKey(index)) { // already parsed it
			 same.get(index).add(nr); 
			 entries.get(index).add(line);
		  } else {
			  List<Integer> ids = new LinkedList<Integer>();
			  List<String> lines = new LinkedList<String>();
			  ids.add(nr);
			  lines.add(line);
			  same.put(index, ids);
			  entries.put(index, lines);
		  }
		  
		  line = line.substring(line.indexOf("<"));
		  // create Instance
		  CoraReference coraRef = new CoraReference();
		  coraRef.key = nr;
		  coraRef.refKey = index;
		  int start = 0;
		  int end = line.length()-1;
		  
		  /**parsing author*/
		  if(line.contains("<author>")) {
//			  System.out.println(nr+"("+index+"): Parsing author: \n\t"+line);
			  start = line.indexOf("<author>")+8;
			  end = line.indexOf("</", start);
//			  System.out.println("["+nr+"]author: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.author = line.substring(start, end).trim();
		  }
		  
		  /**parsing title*/
		  if(line.contains("<title>")) {
//			  System.out.println(nr+"("+index+"): Parsing title: \n\t"+line);
			  start = line.indexOf("<title>")+7;
			  end = line.indexOf("</", start);
//			  System.out.println("["+nr+"]title: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.title = line.substring(start, end).trim();
		  }
	
		  /**parsing year*/
		  if(line.contains("<year>")) {
//			  System.out.println(nr+"("+index+"): Parsing year: \n\t"+line);
			  start = line.indexOf("<year>")+6;
			  end = line.indexOf("</", start);
//			  System.out.println("["+nr+"]year: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.year = line.substring(start, end).trim();
		  }
		  
		  /**parsing pages*/
		  if(line.contains("<pages>")) {
//			  System.out.println(nr+"("+index+"): Parsing pages: \n\t"+line);
			  start = line.indexOf("<pages>")+7;
			  end = line.indexOf("</", start);
//			  System.out.println("["+nr+"]pages: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.pages = line.substring(start, end).trim();
		  }
		  
		  
		  /**********************************************
		   * FIXME book vs. journal paper ***************
		   * 		have to also parse journal volume
		   */
		  
		  /*Parsing journal*/
		  if(line.contains("<journal>")) {
//			  System.out.println(nr+"("+index+"): Parsing journal: \n\t"+line);
			  start = line.indexOf("<journal>")+9;
			  end = line.indexOf("</", start);
//			  System.out.println("["+index+"]journal: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.journal = line.substring(start, end).trim();
		  }
		  /*Parsing volume*/
		  if(line.contains("<volume>")) {
//			  System.out.println(nr+"("+index+"): Parsing volume: \n\t"+line);
			  start = line.indexOf("<volume>")+8;
			  end = line.indexOf("</", start);
//			  System.out.println("["+index+"]volume: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.volume = line.substring(start, end).trim();
		  }
		   
		  /*parsing booktitles*/
		  if(line.contains("<booktitle>")) {
//			  System.out.println(nr+"("+index+"): Parsing book: \n\t"+line);
			  start = line.indexOf("<booktitle>")+11;
			  end = line.indexOf("</", start);
//			  System.out.println("booktitle: ("+start+","+end+"): "+ line.substring(start, end).trim());
			  coraRef.booktitle = line.substring(start, end).trim();
		  }
		  
		  refList.add(coraRef);
	  }
	  
	  public EvaluationData loadCoraData(File f, String name) throws IOException {
		  EvaluationData data = new EvaluationData();
		  data.setBaseFolder("resources/");
		  data.setEvauationResultFolder("resources/results/");
		  data.setEvaluationResultFileName("cora_"+name+".csv");
		  data.setMaxRuns(5);
		  data.setName("cora_"+name);
		  data.setConfigFileName("cora_"+name);
		  // read data
		  readFile(f);
		  for(CoraReference cr : refList) {
			  sC.addInstance(cr.toCacheInstance());
			  tC.addInstance(cr.toCacheInstance());
		  }
		  
		  for(Entry<String, List<Integer>> e : same.entrySet()) {
//		  System.out.println("\n"+e.getKey()+": ");
		  for(Integer key1 : e.getValue()) {
			  for(Integer key2 : e.getValue())
				  reference.add(""+key1, ""+key2, 1d);
		  }
	  }
		  data.setReferenceMapping(reference);
		  data.setSourceCache(sC);
		  data.setTargetCache(tC);
		  
		  KBInfo source = new KBInfo();
		  KBInfo target = new KBInfo();
		  source.id = name+"_source";
		  source.var = "?x";
		  source.endpoint = "CORA_"+name+"_source";
		  source.properties = CoraReference.getProperties();
		  source.type = "CORA";
		  
		  
		  target.id = name+"_target";
		  target.var = "?y";
		  target.endpoint = "CORA_"+name+"_targt";
		  target.properties = CoraReference.getProperties();
		  target.type = "CORA";
		  
		  ConfigReader cR = new ConfigReader();
		  cR.metricExpression = "trigrams(x.title, y.title)";
		  cR.acceptanceThreshold = 0.8d;
		  cR.verificationThreshold = 0.7d;
		  cR.granularity = 2;
		  cR.sourceInfo = source;
		  cR.targetInfo = target;
		  
		  data.setConfigReader(cR);
		  PropertyMapping propMap = new PropertyMapping();
		  for(String prop : CoraReference.getProperties())
			  propMap.addStringPropertyMatch(prop, prop);
		  data.setPropertyMapping(propMap);
		  
		  return data;
	  }
	  
	  
	  public void testRefiner(File f, String name) throws IOException {
		  EvaluationData data = loadCoraData(f, "fahl");
		  System.out.println("sC:"+data.getSourceCache().size());
		  System.out.println("tC:"+data.getTargetCache().size());
		  System.out.println("ref:"+data.getReferenceMapping().size());
		  RefinementBasedLearningAlgorithm.hardRootExpansion = true;
		  RefinementBasedLearningAlgorithm algo = new RefinementBasedLearningAlgorithm(data);
		  algo.init(data, 0.1d, 0.5d, 1.2d, 60);
		  algo.start();
	  }
	  
	  public void testEagle(File f, String name) throws IOException, InvalidConfigurationException {
		  EvaluationData data = loadCoraData(f, "fahl");
		  System.out.println("sC:"+data.getSourceCache().size());
		  System.out.println("tC:"+data.getTargetCache().size());
		  System.out.println("ref:"+data.getReferenceMapping().size());
		  data.setMaxRuns(2);
		  PseudoMeasures measure = new PseudoMeasures();
		  PseudoEvaluation eval = new PseudoEvaluation();
		  eval.generations = 10;
		  eval.population = 10;
		  eval.maxDuration = 60;
		  for(int run = 1; run <= data.getMaxRuns(); run++)
			  eval.run(data, run, measure, "EAGLE_PFM_");
	  }
	  
	  
}

