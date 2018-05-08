package de.uni_leipzig.simba.genetics.learner;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import cern.jet.math.PlusMult;

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

/**
 * LIMES Link Specification Active Learner based on Genetic Programming using Spreading Activation with weight decay
 * to valdidate most informative Link Candidates.
 * COALA implementation of spreading activation with weight decay method.
 * @author Victor Christen
 * @author Klaus Lyko
 *
 */
public class WeightDecayCorrelation {

	private HashMap<String, StringMeasure> measures;
	private DenseDoubleMatrix2D adjacenceMatrix; // row by row access is quicker
	private DenseDoubleMatrix1D activationVector;
	/**
	 * count of properties for similarity calculation
	 */
	private int maxProperties;
	
	private HashMap<Pair<String>,StringMeasure> propMeasureMap;
	
	private final Logger log = Logger.getLogger(WeightDecayCorrelation.class);
	
	private Cache sourceCache;
	private Cache targetCache;
	
	/**
	 * Basic constructor using 2 as  as default.
	 * @param source
	 * @param target
	 * @param metric
	 */
	public WeightDecayCorrelation(KBInfo source, KBInfo target, String metric){
		this (source,target,metric,2);
	}
	private WeightDecayCorrelation(){}
	public WeightDecayCorrelation(KBInfo source, KBInfo target, String metric, int maxPropertyCount){
		log.setLevel(Level.INFO);
		this.measures = new HashMap<String,StringMeasure>();
		measures.put("cosine",new CosineMeasure());
		measures.put("jaccard",new JaccardMeasure());
		measures.put("levenshtein",new Levenshtein());
		measures.put("overlap",new OverlapMeasure());
		measures.put("qgrams",new QGramSimilarity());
		this.maxProperties = maxPropertyCount;
		propMeasureMap = this.getStringMeasures(metric);
		log.info(metric);
		sourceCache = HybridCache.getData(source);
		targetCache = HybridCache.getData(target);		
	}
	/**
	 * Maps each property pair (metric in constructor) to all String measures
	 * @param metric
	 * @return
	 */
	private HashMap <Pair<String>, StringMeasure> getStringMeasures(String metric){
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
//						log.info("Props: [0]:"+props[0]+" [1]:"+props[1]);
						Pair<String> p = new Pair<String>(props[0].substring(props[0].indexOf(".")+1),props[1].substring(props[1].indexOf(".")+1));
//						log.info("Pair: "+p);
						measureMap.put(p,measures.get(measure));
						copy = copy.substring(0, pos);
						
					}
				}
			}while (pos!= -1);
		}
		int propertyCount =0;
		
		for (Entry<Pair<String>,StringMeasure>e :measureMap.entrySet()){
			trimedMeasureMap.put(e.getKey(), e.getValue());
			propertyCount++;
			if (propertyCount >= this.maxProperties)
				break;
		}
		
		return measureMap;
	}
	
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
			for (Pair<String> pair : this.propMeasureMap.keySet()){
				Double	similarity = propMeasureMap.get(pair).getSimilarity(source,target, (pair.a),
							(pair.b));
				measures.put((pair).toString(), similarity);
			}
		}
		return cache;
	}
	
	private double calculateDistance(Triple t1, Triple t2,
			HashMap<Integer,HashMap<String,Double>> similarityCache){
		
		double simT1 = 0;
		double simT2 =0;
		double squareDiff =0;
		double signT1 = (t1.getSimilarity()<0.5)?-1:1;
		double signT2 = (t2.getSimilarity()<0.5)?-1:1;
		
		for (Pair<String> pair : this.propMeasureMap.keySet()){
			simT1 = similarityCache.get(t1.hashCode()).get((pair).toString());
			simT2 = similarityCache.get(t2.hashCode()).get((pair).toString());
			squareDiff += Math.pow((signT1*(1-simT1))-(signT2*(1-simT2)),2);
		}
		return 1f/(1f+Math.sqrt(squareDiff));
	}
	
	private void initSimilarityGraph(List<Triple> tripleList){
		
		adjacenceMatrix = new DenseDoubleMatrix2D (tripleList.size(),tripleList.size());
		activationVector = new DenseDoubleMatrix1D (tripleList.size());
		HashMap<Integer,HashMap<String,Double>> cache = initSimilarityCache(tripleList); 
		for (int i = 0; i<tripleList.size();i++){
			double distanceToThres = Math.abs(tripleList.get(i).getSimilarity()-0.5);
			activationVector.set(i,1-distanceToThres);
			for (int j = i;j<tripleList.size(); j++){
				double sim;
				if (i!=j)
					sim =  this.calculateDistance(tripleList.get(i), tripleList.get(j),
						cache);
				else 
					sim =0;
				adjacenceMatrix.set(i, j,sim);
				adjacenceMatrix.set(j, i, sim);
			}
		}		
	}
	
	private double getMax (){
		double max = 0;
		for (double value: activationVector.elements){
			if (value >max)
				max =value;
		}
		return max;
	}
	
	/**
	 * calculate the activation vector with weight decay
	 * @param iteration
	 * @param edgeThreshold
	 */
	private void calculation (int iteration, double decreaseExponent){
		log.info("start weight Decay correlation");
		for (int currentIteration = 0 ; currentIteration<iteration; currentIteration++){
			DenseDoubleMatrix1D product=new DenseDoubleMatrix1D(activationVector.size);
			this.adjacenceMatrix.zMult(activationVector, product);
			activationVector = (DenseDoubleMatrix1D) activationVector.assign(product,PlusMult.plusMult(1));
			activationVector.assign(new DoubleFunction(){
				double max = getMax();
//				@Override
				public double apply(double arg0) {
					return arg0/max;
				}
			});	
			adjacenceMatrix.assign(Functions.pow(decreaseExponent));
		}
		log.info(adjacenceMatrix.toString());
		log.info(activationVector.toString());
		log.info("end weight Decay correlation");
	}
	
	/**
	 * Main method: compute most informative links(Triples) using COALA's Spreading Activation with Weight decay
	 * @param tripleList Input: List of Triples of the mappings
	 * @param trainingDataSize: Number of most informative link candidates to return.
	 * @param decreaseExponent: Weight Decay exponent r
	 * @return
	 */
	public List <Triple> getDisimilarMappings(List <Triple> tripleList, int trainingDataSize, double decreaseExponent){
		List <Triple> list = new ArrayList<Triple>();
		if (tripleList.size()<trainingDataSize){
			int toIndex = (tripleList.size());
			return tripleList.subList(0, toIndex);
		}
		
		List <Triple> trimmedInitList = new ArrayList<Triple>();
		Collections.sort(tripleList, new TripleComparator());
		trimmedInitList.addAll(tripleList.subList(0, Math.round((float)tripleList.size()/2f)));
		this.initSimilarityGraph(trimmedInitList);
		this.calculation(10, decreaseExponent);
		log.info(this.activationVector.toString());
		TreeMap<Double,List<Integer>> sortedActivation= new TreeMap<Double,List<Integer>>();
		for (int i = 0;i<activationVector.size;i++){
			List <Integer> activateList = sortedActivation.get(activationVector.get(i));
			if (activateList ==null){
				activateList = new ArrayList<Integer>();
				sortedActivation.put(activationVector.get(i), activateList);
			}
			activateList.add(i);
		}
		int elementCount =0;
		Double maxActivation = sortedActivation.lastKey();
		while (maxActivation!= null && elementCount<trainingDataSize){
			List <Integer> triples = sortedActivation.get(maxActivation);
			for (int triple : triples){
				if (elementCount >= trainingDataSize)
					break;
				list.add(trimmedInitList.get(triple));
				elementCount++;
			}
			maxActivation = sortedActivation.lowerKey(maxActivation);
		}
		
		return list;
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

//		@Implements
		public int compare(Triple o1, Triple o2) {
			Float sim1 = o1.getSimilarity();
			Float sim2 = o2.getSimilarity();
			return ((Double)Math.abs(sim1-0.5)).compareTo(Math.abs(sim2-0.5)); 
			
		}
	}
	
	public static  void main (String[] args){
		WeightDecayCorrelation cor = new WeightDecayCorrelation();
		double [][] values = {{0,0.25,0.5,0},{0.25,0,0.5,0.5},{0.5,0.5,0,0.25},
				{0,0.5,0.25,0}};
		DenseDoubleMatrix2D matrix = new DenseDoubleMatrix2D (values);
		cor.adjacenceMatrix = matrix;
		cor.activationVector = new DenseDoubleMatrix1D(new double[]{0.9,0.8,0.9,0.8});
		cor.calculation(3, 2);
		
	}
}
