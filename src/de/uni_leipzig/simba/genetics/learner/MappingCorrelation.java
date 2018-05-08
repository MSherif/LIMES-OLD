package de.uni_leipzig.simba.genetics.learner;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.uni_leipzig.gk.cluster.BorderFlowHard;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.measures.string.CosineMeasure;
import de.uni_leipzig.simba.measures.string.JaccardMeasure;
import de.uni_leipzig.simba.measures.string.Levenshtein;
import de.uni_leipzig.simba.measures.string.OverlapMeasure;
import de.uni_leipzig.simba.measures.string.QGramSimilarity;
import de.uni_leipzig.simba.measures.string.StringMeasure;
import de.uni_leipzig.simba.measures.string.TrigramMeasure;

/**
 * 
 * @author Klaus Lyko
 *
 */
public class MappingCorrelation {
	public static final Logger log = Logger.getLogger(MappingCorrelation.class);
	
	public static final int DEFAULT_MAX_PROP = 2;
	private BorderFlowHard clustering;
	
	private String metric; 
	
	private KBInfo source;
	
	private KBInfo target;
	
	private double similarThreshold;
	
	private HashMap<String, StringMeasure> measures;
	
	private Cache sourceCache;
	
	private Cache targetCache;
	
	/**
	 * count of properties for similarity calculation
	 */
	private int maxProperties;
	
	/**
	 * cache for the similarity of properties of a triple key:=propSource-propTarget value:=StringMeasure
	 */
	private	 HashMap<Pair<String>,StringMeasure> propMeasureMap ;
	private static final String CLUSTER_FILE = "cluster.txt";
	public MappingCorrelation(KBInfo source, KBInfo target, String metric){
		this(source,target,metric,DEFAULT_MAX_PROP);
	}
	
	public MappingCorrelation (KBInfo source, KBInfo target, String metric, int maxProperties){
		this.source = source;
		this.target = target;
		this.metric = metric;
		this.measures = new HashMap<String,StringMeasure>();
		measures.put("cosine",new CosineMeasure());
		measures.put("jaccard",new JaccardMeasure());
		measures.put("levenshtein",new Levenshtein());
		measures.put("overlap",new OverlapMeasure());
		measures.put("qgrams",new QGramSimilarity());
		measures.put("trigrams", new TrigramMeasure());
		this.maxProperties = maxProperties;
		propMeasureMap = this.getStringMeasures(metric);
		log.info(propMeasureMap.toString());
		sourceCache = HybridCache.getData(source);
		targetCache = HybridCache.getData(target);
	}
	
	
	/**
	 * Calculate dissimilar and most informative mappings for the oracle. <br>
	 * Assuming that similar mappings are not useful for the next learning step, the mappings will
	 * clustered in respect to their property similarity. A triple t1 is similar to t2, if the 
	 * euclid distance between similarity of the source and target properties of the triples is equal
	 * At first the method build two adjacent matrices with t1 and t2 as edge and the similarity 
	 * value as weight for each triple pair, which is informative enough. 
	 * The two matrices will used for clustering. The result of the clustering are a positive cluster
	 * set, which includes positive informative triples and a negative cluster set. A cluster include
	 * all triples, which are similar.
	 * A mapping will chosen in respect to his informative value and if no other mapping was 
	 *  chosen in the same cluster of this mapping
	 * @param tripleList Input List of triples, which are basically entries of a mapping.
	 * @param trainingDataSize Number of returned links to be evaluated
	 * @param edgeCountPerNode Best edges Borderflow will hold on to.
	 * @return
	 */
	public List <Triple> getDisimilarMappings(List <Triple> tripleList, int trainingDataSize, int edgeCountPerNode){
		List<Triple> returnList = null;
		log.info("start similarity  calculation for "+ tripleList.size()+ " triples");
		if (tripleList.size()/2<trainingDataSize){
			log.info("triple list too small for clustering");
			int toIndex = (tripleList.size()<trainingDataSize?tripleList.size():trainingDataSize);
			this.propMeasureMap.clear();
			return tripleList.subList(0,toIndex);
		}
		/*
		 * map of triples with informative value as key
		 */
		TreeMap<Float,List<Triple>> tripleMap = this.initInformativeTripleMap(tripleList);
		/*
		 * number of triples, which will used for clustering
		 * assuming the other triples are not informative 
		 */
		int totalElements = (int)Math.round((float)tripleList.size()*0.15f);
		
		log.info("number of triples for clustering "+ totalElements);
		//triples with informative value >=0.5
		List <Triple> positiveTriples = this.initListForClustering(false, tripleMap, totalElements);
		List <Triple> negativeTriples = this.initListForClustering(true, tripleMap, totalElements);
		try {
			/* positive cluster map with cluster id and triple list in the cluster*/
			HashMap<Integer,List<String>>clusterPositiveMap = new HashMap<Integer,List<String>>();
			HashMap<String,Integer> reversePositiveMap = new HashMap<String,Integer> ();
			
			HashMap<Integer,List<String>>clusterNegativeMap = new HashMap<Integer,List<String>>();
			HashMap<String,Integer> reverseNegativeMap = new HashMap<String,Integer> ();
			
			Map<Set<String>,Set<String>> clusterPos = this.clustering(positiveTriples,edgeCountPerNode);
			Map<Set<String>,Set<String>> clusterNeg = this.clustering(negativeTriples,edgeCountPerNode);
			//build clusterMap and reverse cluster map
				
			int id= 0;
			for (Set<String> cluster:clusterPos.keySet()){
				
				List<String> nodes = new ArrayList<String>();
				nodes.addAll(cluster);
				clusterPositiveMap.put(id,nodes);
				for (String node:nodes){
					reversePositiveMap.put(node, id);
				}
				id++;
			}
			for (Set <String> cluster :clusterNeg.keySet()){
				List<String> nodes = new ArrayList<String>();
				nodes.addAll(cluster);
				clusterNegativeMap.put(id,nodes);
				for (String node:nodes){
					reverseNegativeMap.put(node, id);
				}
				id++;
			}
			ArrayList<Triple> initList = new ArrayList<Triple>();
			initList.addAll(tripleList);
			
			if (clusterPos.size()==0&&clusterNeg.size()==0){
				int toIndex = (initList.size()<trainingDataSize)? initList.size():trainingDataSize;
				log.info("no Cluster:"+initList.subList(0, toIndex).toString());
				return initList.subList(0, toIndex);
			}
			returnList = this.initOracleList(initList, trainingDataSize, reversePositiveMap, reverseNegativeMap, clusterPositiveMap, clusterNegativeMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("count of returned elements after clustering:"+returnList.size());
		log.info("end similarity calculation...");
		return returnList;
	}
	
	/**
	 * generate a map of informative value list of triples pairs<br>
	 * this list will used to use only triples for clustering, which are close to the threshold
	 * @param tripleList
	 * @return
	 */
	private TreeMap<Float,List<Triple>> initInformativeTripleMap(List <Triple> tripleList){
		TreeMap<Float,List<Triple>> tripleMap = new TreeMap<Float,List<Triple>>();
		for (Triple t : tripleList){
			List<Triple> triples = tripleMap.get(t.getSimilarity());
			if (triples==null){
				triples = new ArrayList<Triple>();
				tripleMap.put(t.getSimilarity(), triples);
			}
			triples.add(t);
		}
		return tripleMap;
		
	}
	/**
	 * @param isNegative true generate list for negative Triples, which are smaller than threshold else equal/greater
	 * @param tripleMap informative value - list triple Pair
	 * @param totalElements number of elements for clustering
	 * @return most informative Triples for clustering
	 */
	private List <Triple> initListForClustering (boolean isNegative,
			TreeMap<Float, List <Triple>> tripleMap, int totalElements){
		Float currentKey = (isNegative)?tripleMap.lowerKey(0.5f):tripleMap.ceilingKey(0.5f);
		int elementCount =0;
		List <Triple> listForClustering = new ArrayList<Triple>();
		do{
			if (currentKey != null){
				List <Triple>list = tripleMap.get(currentKey);
				elementCount += list.size(); 
				listForClustering.addAll(list);
				if (isNegative){
					currentKey = tripleMap.lowerKey(currentKey);
				}else  {
					currentKey = tripleMap.higherKey(currentKey);
				}
			}
		}while (elementCount<totalElements && currentKey != null);
		return listForClustering;
	}
	
	private double calculateDistance(Triple t1, Triple t2,
			HashMap<Integer,HashMap<String,Double>> similarityCache){
		
		double simT1 = 0;
		double simT2 = 0;
		double squareDiff =0;
		
		
		for (Pair<String> pair : this.propMeasureMap.keySet()){
			simT1 = similarityCache.get(t1.hashCode()).get((pair).toString());
			simT2 = similarityCache.get(t2.hashCode()).get((pair).toString());
			squareDiff += Math.pow((1-simT1)-(1-simT2),2);
		}
		return 1f/(1f+Math.sqrt(squareDiff));
	}
	
	/**
	 * This cluster a set of triples in respect to their similarity
	 * @param tripleList triples for clustering
	 * @param edgesPerNode number of edges per node
	 * @return map of clusters with their triples 
	 * @throws IOException
	 */
	private Map<Set<String>, Set<String>> clustering(List<Triple> tripleList, int edgesPerNode) throws IOException{
		log.info("init Cluster File"+ tripleList.size());
		// cache for a triple and his similarity values for each property
		HashMap<Integer,HashMap<String,Double>> similarityCache  = this.initSimilarityCache(tripleList);
		//map of triples key as hashcode  value map of edges with similarity as key and list of triples as value 
		HashMap<Integer, TreeMap<Double,List<Integer>>> nodes = new HashMap<Integer, TreeMap<Double,List<Integer>>>();
		for (int i= 0; i<tripleList.size();i++){
			for (int j = i;j<tripleList.size();j++){
				if (i!=j){
					double sim = this.calculateDistance(tripleList.get(i),tripleList.get(j),similarityCache);
					Integer t1 =tripleList.get(i).hashCode();
					Integer t2 = tripleList.get(j).hashCode();
					
					TreeMap<Double,List<Integer>> edges= nodes.get(t1);
					if (edges ==null){ //no edges 
						edges = new TreeMap<Double,List<Integer>>();
						nodes.put(t1, edges);
					}
					List <Integer> adjNodes = edges.get(sim);
					if (adjNodes == null){// no nodes for this similarity
						adjNodes = new ArrayList<Integer>();
						edges.put(sim, adjNodes);
					}
					adjNodes.add(t2); // add triple to edges for the current node with the calculated similarity
					
				}
			}
		}
		log.info("similarity calculation ready");
		int edgelog= 0;
		//init cluster file 
		//chose for every node only the most similarity triples
		FileWriter fw = new FileWriter(CLUSTER_FILE);
		for (Integer node : nodes.keySet()){
			TreeMap<Double,List<Integer>>  edges= nodes.get(node);
			if (edges != null){
				Double currentKey = edges.lastKey();
				int edgeCount = 0;
				if (currentKey != null)
				do{
					List <Integer> adjNodes = edges.get(currentKey);
					for (Integer adjNode: adjNodes){
						if (edgeCount == edgesPerNode)
							break;
						fw.append(node +"\t"+adjNode+"\t"+currentKey+System.getProperty("line.separator"));
						edgelog++;
						edgeCount++;
					}
					currentKey = edges.lowerKey(currentKey);
				}while (edgeCount<edgesPerNode && currentKey != null);
			}
		}
		log.info("edges in graph"+ edgelog);
		nodes.clear();
		fw.close();
		clustering = new BorderFlowHard (CLUSTER_FILE);
		log.info ("start clustering mappings");
		clustering.hardPartitioning = true;
		
		Map<Set<String>,Set<String>> clusters = clustering.cluster(1, true, true, false);
		return clusters;
	}
	/**
	 * calculate similarity for each triple in the list
	 * @param triples
	 * @return key:= tripleHashcode value:= hashmap {key :=property Pair value:=similarity}
	 */
	private HashMap<Integer,HashMap<String,Double>> initSimilarityCache (List <Triple> triples) {
		HashMap<Integer,HashMap<String,Double>> cache = new HashMap<Integer,HashMap<String,Double>>();
		for (Triple t : triples){
			HashMap<String,Double> measures  = cache.get(t.hashCode());
			if (measures == null){
				measures = new HashMap<String,Double>();
				cache.put(t.hashCode(), measures);
			}
			Instance source = sourceCache.getInstance(t.getSourceUri());
			Instance target = targetCache.getInstance(t.getTargetUri());
			if(source == null) {
				log.error("no instance found in source cache for URI: "+t.getSourceUri());
			}
			if(target == null) {
				log.error("no instance found in target cache for URI: "+t.getTargetUri());
			}
			for (Pair<String> pair : this.propMeasureMap.keySet()){
				Double	similarity = propMeasureMap.get(pair).getSimilarity(source,target, (pair.a),
							(pair.b));
				measures.put((pair).toString(), similarity);
			}
		}
		return cache;
	}
	private List <Triple> initOracleList(List<Triple> initList, int trainingDataSize, HashMap<String, Integer> reverseMap,
			HashMap<String, Integer> reverseNegativeMap, HashMap<Integer, List<String>> clusterMap,
			HashMap<Integer, List<String>> clusterNegativeMap){
			//sorting by (informative value-0.5) ascendent
			Collections.sort(initList, new TripleComparator());
			List <Triple> tripleList =new ArrayList<Triple>();
			//set for visited positive clusters
			HashSet<Integer> visitPosCluster = new HashSet<Integer>();
			//set for visited negative clusters
			HashSet<Integer> visitNegCluster = new HashSet<Integer>();
			
			int counter= 0;
			for (int tripleId =0;tripleId<initList.size();tripleId++){ //
				if (counter == trainingDataSize)
					break;
				Triple t = initList.get(tripleId);
				if (!visitPosCluster.contains(reverseMap.get(t.hashCode()+""))&&
						!visitNegCluster.contains(reverseNegativeMap.get(t.hashCode()+""))){
					// only chosen if a other triple was not chosen in the same cluster
					tripleList.add(initList.get(tripleId));
					//get clusterid of current triple 
					Integer clusterId = (reverseMap.containsKey(t.hashCode()+""))?
							reverseMap.get(t.hashCode()+""):reverseNegativeMap.get(t.hashCode()+"");
					counter++;
					if (reverseMap.containsKey(t.hashCode()+"")){
						visitPosCluster.add(clusterId);						
					}else if (reverseNegativeMap.containsKey(t.hashCode()+"")){
						visitNegCluster.add(clusterId);
					}
				}
			}
			return tripleList;
	}
	
	/**
	 * initialize the property-measure map 
	 * @param metric used metric
	 * @return
	 */
	private HashMap <Pair<String>,StringMeasure> getStringMeasures(String metric){
		String copy;
		HashMap<Pair<String>,StringMeasure> measureMap= new HashMap<Pair<String>,StringMeasure>();
		HashMap<Pair<String>,StringMeasure> trimedMeasureMap = new HashMap<Pair<String>,StringMeasure>();
		int pos;
//		int max =-1;
		Pattern propP = Pattern.compile("\\((.){3,}?,(.){3,}?\\)");
		for (String measure : measures.keySet()){
			copy = metric.toLowerCase();
			do {
			pos = copy.lastIndexOf(measure);
				if (pos!=-1){
					
					Matcher m = propP.matcher(copy.substring(pos+measure.length()));
					if (m.find()){
						String simPart =m.group();
						simPart = simPart.replaceAll("\\(|\\)", "");
						String[] props = simPart.split(",");
						
						Pair<String> p = new Pair<String>(props[0].substring(props[0].indexOf(".")+1), props[1].substring(props[1].indexOf(".")+1));
						log.info("identified Properties: "+props[0].substring(props[0].indexOf(".")+1)+"  AND   "+ props[1].substring(props[1].indexOf(".")+1));
						measureMap.put(p,measures.get(measure));
						copy = copy.substring(0, pos);
						
					}
				}
			}while (pos!= -1);
		}
		//trim to max property count
		int propertyCount =0;
		for (Entry<Pair<String>,StringMeasure>e :measureMap.entrySet()){
			trimedMeasureMap.put(e.getKey(), e.getValue());
			propertyCount++;
			if (propertyCount >= this.maxProperties)
				break;
		}
		return trimedMeasureMap;
		
	}
	
	
	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public KBInfo getSource() {
		return source;
	}

	public void setSource(KBInfo source) {
		this.source = source;
	}

	public KBInfo getTarget() {
		return target;
	}

	public void setTarget(KBInfo target) {
		this.target = target;
	}

	/**
	 * @param similarThreshold the similarThreshold to set
	 */
	public void setSimilarThreshold(double similarThreshold) {
		this.similarThreshold = similarThreshold;
	}

	/**
	 * @return the similarThreshold
	 */
	public double getSimilarThreshold() {
		return similarThreshold;
	}

	
	
	/**
	 * @param maxProperties the maxProperties to set
	 */
	public void setMaxProperties(int maxProperties) {
		this.maxProperties = maxProperties;
	}

	/**
	 * @return the maxProperties
	 */
	public int getMaxProperties() {
		return maxProperties;
	}

	private class TripleComparator implements Comparator <de.uni_leipzig.simba.data.Triple> {

//		@Override
		public int compare(Triple o1, Triple o2) {
			Float sim1 = o1.getSimilarity();
			Float sim2 = o2.getSimilarity();
			return ((Double)Math.abs(sim1-0.5)).compareTo(Math.abs(sim2-0.5)); 
			
		}
		
	}
}
