/**
 * 
 */
package de.uni_leipzig.simba.measures.pointsets.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.transform.Result;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonReader;
import de.uni_leipzig.simba.measures.pointsets.SetMeasure;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;
import de.uni_leipzig.simba.measures.pointsets.average.NaiveAverage;
import de.uni_leipzig.simba.measures.pointsets.benchmarking.GranularityModifier;
import de.uni_leipzig.simba.measures.pointsets.benchmarking.MeasurementErrorModifier;
import de.uni_leipzig.simba.measures.pointsets.benchmarking.PolygonModifier;
import de.uni_leipzig.simba.measures.pointsets.frechet.NaiveFrechet;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.NaiveHausdorff;
import de.uni_leipzig.simba.measures.pointsets.link.NaiveLink;
import de.uni_leipzig.simba.measures.pointsets.max.NaiveMax;
import de.uni_leipzig.simba.measures.pointsets.min.NaiveMin;
import de.uni_leipzig.simba.measures.pointsets.sumofmin.NaiveSumOfMin;
import de.uni_leipzig.simba.measures.pointsets.surjection.FairSurjection;
import de.uni_leipzig.simba.measures.pointsets.surjection.NaiveSurjection;
import de.uni_leipzig.simba.multilinker.MappingMath;

/**
 * evaluating different polygon distance functions for stability
 * @author sherif
 *
 */
public class GeoDistanceStabilityEvaluation extends GeoDistancesEvaluator {
	private static final Logger logger = Logger.getLogger(GeoDistanceStabilityEvaluation.class.getName());

	private static float granularityThresholdStart 		= 1f;
	private static float granularityThresholdIncrement	= 1f;
	private static float granularityThresholdEnd		= 5f;

	private static float measurErrorThresholdStart 		= 0.02f;
	private static float measurErrorThresholdIncrement 	= 0.02f;
	private static float measurErrorThresholdEnd 		= 0.1f;
	private static float orchidThreshold				= 1; // 1km
	
	private static float borderErrorThresholdStart				= 0.2f;
	private static float borderErrorThresholdEnd				= 0.2f;
	private static float borderErrorThresholdIncrement			= 1.0f;


	private Map<Float, Map<String, FMeasureRecorder>> threshold2distance2mapping;



	/**
	 * 
	 *@author sherif
	 */
	public GeoDistanceStabilityEvaluation() {
		super();
		threshold2distance2mapping = new TreeMap<Float, Map<String,FMeasureRecorder>>();
	}


	/**
	 * modify set of polygons using a given list of modifiers and threshold 
	 * @param polygons
	 * @param modifier
	 * @param threshold
	 * @return
	 * @author sherif
	 */
	private Set<Polygon> modifyPolygons(Set<Polygon> polygons, List<PolygonModifier> modifiers, double threshold) {
		long startTime = System.currentTimeMillis();
		Set<Polygon> result = new HashSet<Polygon>();
		for(Polygon p : polygons){
			Polygon modifiedP = p;
			for(PolygonModifier modifier : modifiers){
				modifiedP = modifier.modify(modifiedP, threshold);
			}
			result.add(modifiedP);
		}
		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Modified polygons generated in " + totalTime + "ms" );
		return result;
	}


	/**
	 * evaluate for a list of modifiers 
	 * @param modifiers
	 * @param file
	 * @param size
	 * @param startValue
	 * @param increment
	 * @param endValue
	 * @return
	 * @author sherif
	 */
	public Map<Float, Map<String, FMeasureRecorder>> evaluate(
			String file, int size, float startValue, float increment, 
			float endValue, List<PolygonModifier> modifiers){

		sourcePolygonSet = PolygonReader.readPolygons(file, size);

		for(float threshold=startValue ; threshold<=endValue ; threshold+=increment){
			logger.info("Apply Modifiers with threshold = " + threshold);
			targetPolygonSet = modifyPolygons(sourcePolygonSet, modifiers, threshold);
			Map<String, FMeasureRecorder> distance2mapping = new TreeMap<String, FMeasureRecorder>(); 

			for(SetMeasure geoDistance : geoDistances){
				logger.info("Evaluate Distance function: " + geoDistance.getName());
				Mapping mapping = new Mapping();

				if(useOrchid){
					GeoHR3 orchid = new GeoHR3(threshold, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(geoDistance));
					mapping = orchid.run(sourcePolygonSet, sourcePolygonSet);
				}else{
					mapping = geoDistance.run(sourcePolygonSet, targetPolygonSet, Float.MAX_VALUE);
				}
				mapping = getBestMappings(mapping);
				int optimalMappingSize = Math.min(sourcePolygonSet.size(), targetPolygonSet.size());
				distance2mapping.put(geoDistance.getName(), new FMeasureRecorder(mapping, optimalMappingSize));

			}
			threshold2distance2mapping.put(threshold, distance2mapping);
		}
		return threshold2distance2mapping;
	}


	public Map<Float, Map<String, FMeasureRecorder>> evaluate(){

		sourcePolygonSet = PolygonReader.readPolygons(inputFile, nrOfPolygons);

		for(float granularityThreshold = granularityThresholdStart, measureErrorThreshold = measurErrorThresholdStart, borderErrorThreshold = borderErrorThresholdStart ; 
				(granularityThreshold <= granularityThresholdEnd) && (measureErrorThreshold <= measurErrorThresholdEnd) && (borderErrorThreshold <= borderErrorThresholdEnd); 
				granularityThreshold += granularityThresholdIncrement, measureErrorThreshold += measurErrorThresholdIncrement, borderErrorThreshold += borderErrorThresholdIncrement){
			
			targetPolygonSet = modifyPolygons(sourcePolygonSet, granularityThreshold, measureErrorThreshold, borderErrorThreshold);
			
			Map<String, FMeasureRecorder> distance2mapping = new TreeMap<String, FMeasureRecorder>(); 

			for(SetMeasure geoDistance : geoDistances){
				logger.info("Evaluate Distance function: " + geoDistance.getName());
				Mapping mapping = new Mapping();

				if(useOrchid){
					//TODO find optimal value for orchidThreshold
					GeoHR3 orchid = new GeoHR3(orchidThreshold, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(geoDistance));
					mapping = orchid.run(sourcePolygonSet, sourcePolygonSet);
				}else{
					mapping = geoDistance.run(sourcePolygonSet, targetPolygonSet, Float.MAX_VALUE);
				}
				if(mapping != null){
					mapping = getBestMappings(mapping);
//					mapping = mapping.getBestOneToNMapping();
				}else{
					logger.error("NO Mapping found!");
				}
				int optimalMappingSize = Math.min(sourcePolygonSet.size(), targetPolygonSet.size());
				distance2mapping.put(geoDistance.getName(), new FMeasureRecorder(mapping, optimalMappingSize));

			}
			//TODO solve 2 threshold problem
			threshold2distance2mapping.put(granularityThreshold + measureErrorThreshold + borderErrorThreshold, distance2mapping);
		}
		return threshold2distance2mapping;
	}
	
	
	private Set<Polygon> modifyPolygons(Set<Polygon> polygons, float granularityThreshold, float measureErrorThreshold, float borderErrorThreshold) {
		long startTime 		= System.currentTimeMillis();
		Set<Polygon> result = new HashSet<Polygon>();
		Set<Polygon> tmp 	= new HashSet<Polygon>();
		result = polygons;
		if(granularityThreshold > 0f){
			logger.info("Apply granularity modifier with  Threshold = " + granularityThreshold);
			for(Polygon p : result){
				tmp.add((new GranularityModifier()).modify(p, granularityThreshold));
			}
			result = tmp;
		}
		tmp = new HashSet<Polygon>();
		
		if(measureErrorThreshold > 0f){
			logger.info("Apply measurement error modifier with  Threshold = " + measureErrorThreshold);
			for(Polygon p : result){
				tmp.add((new MeasurementErrorModifier()).modify(p, measureErrorThreshold));
			}
			result = tmp;
		}
		
		if(borderErrorThreshold > 0f){
			logger.info("Apply borderErrorThreshold error modifier with  Threshold = " + borderErrorThreshold);
			for(Polygon p : result){
				tmp.add((new MeasurementErrorModifier()).modify(p, measureErrorThreshold));
			}
			result = tmp;
		}

		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Modified polygons generated in " + totalTime + "ms" );
		return result;
	}
	

	/**
	 * @param map
	 * @return
	 * @author sherif
	 */
	private Mapping getBestMappings(Mapping mapping) {
		Mapping result = new Mapping();
		for (String s : mapping.map.keySet()) {
			double minSim = Double.MAX_VALUE;
			Set<String> target = new HashSet<String>();;
			for (String t : mapping.map.get(s).keySet()) {
				double sim = mapping.getSimilarity(s, t);
				if (sim == minSim) {
					target.add(t);
				}
				if (sim < minSim) {
					minSim = sim;
					target = new HashSet<String>();
					target.add(t);
				}
			}
			for (String t : target) {
				result.add(s, t, minSim);
			}
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.measures.pointsets.evaluation.GeoDistancesEvaluator#getResult()
	 */
	public String getResult(){
		//if already computed do not compute it again
		if(result.length() != 0){
			return result;
		}

		//table header
		result = "Threshold\t";
		float firstThreshold = threshold2distance2mapping.keySet().iterator().next();
		Map<String, FMeasureRecorder> distance2mapping = threshold2distance2mapping.get(firstThreshold);
		for(String geoDistanceName : distance2mapping.keySet()){
			result += geoDistanceName +"_P"+ "\t"; 
			result += geoDistanceName +"_R"+ "\t"; 
			result += geoDistanceName +"_F"+ "\t"; 
		}
		result += "\n";

		//table content
		for(float threshold : threshold2distance2mapping.keySet()){
			result += threshold + "\t";
			distance2mapping = threshold2distance2mapping.get(threshold);
			for(String distance : distance2mapping.keySet()){
				FMeasureRecorder fMeasureRecorder = distance2mapping.get(distance);
				result += fMeasureRecorder.precision 	+ "\t";
				result += fMeasureRecorder.recall 		+ "\t";
				result += fMeasureRecorder.fMeasure 	+ "\t";
			}
			result += "\n";
		}
		return result;
	}


	/**
	 * @param args
	 * @author sherif
	 */
	private static void getParametersFromComandLine(String[] args) {
		helpStr = "Parameters:\n" +
				"\targs[0]: Number of experiments\n" +
				"\targs[1]: Input file name\n" +
				"\targs[2]: Number of polygons (-1 for all polygons in the input file)\n" +
				"\targs[3]: Output file name\n" +
				"Optional parameters:\n" +
				"\targs[4]: [true_<orchidThreshold>/false] Use Orchid, true by default with orchidThreshold = 1km\n" +
				"\targs[5]: Granularity modifier Threshold start value, default value = 1f\n" +
				"\targs[6]: Granularity modifier Threshold increment value, default value = 1f\n" +
				"\targs[7]: Granularity modifier Threshold end value, default value = 5f\n" +
				"\targs[8]: Measurement Error modifier Threshold start value, default value = 0.02f\n" +
				"\targs[9]: Measurement Error modifier Threshold increment value, default value = 0.02f\n" +
				"\targs[10]: Measurement Error modifier Threshold end value, default value = 0.1f\n" +
				"\targs[11]: Border modifier Threshold start value, default value = 0.2f\n" +
				"\targs12]: Border modifier Threshold increment value, default value = 0.2f\n" +
				"\targs[13]: Border modifier Threshold end value, default value = 1.0f\n" ;

		if(args[0].equals("-?")){
			logger.info(helpStr);
			System.exit(0);
		}
		if(args.length < 4){
			logger.error("Wrong number of parameters.\n" + helpStr);
			System.exit(1);
		}

		nrOfExperiments 					= Integer.parseInt(args[0]);
		inputFile 							= args[1];
		nrOfPolygons 						= Integer.parseInt(args[2]);
		outputFile 							= args[3];
		if(args.length > 4){
			if(args[4].contains("_")){
				String str[] 				= args[4].split("_");
				useOrchid 					= Boolean.parseBoolean(str[0]);
				orchidThreshold 			= Float.parseFloat(str[1]);
			}else{
				useOrchid 					= Boolean.parseBoolean(args[4]);
			}
		}

		if(args.length > 5){
			granularityThresholdStart		= Float.parseFloat(args[5]);
			granularityThresholdIncrement 	= Float.parseFloat(args[6]);
			granularityThresholdEnd			= Float.parseFloat(args[7]);

			measurErrorThresholdStart 		= Float.parseFloat(args[8]);
			measurErrorThresholdIncrement 	= Float.parseFloat(args[9]);
			measurErrorThresholdEnd 		= Float.parseFloat(args[10]);
			
			borderErrorThresholdStart 		= Float.parseFloat(args[11]);
			borderErrorThresholdIncrement 	= Float.parseFloat(args[12]);
			borderErrorThresholdEnd 		= Float.parseFloat(args[13]);
		}
	}
	
	
	/**
	 * @param args
	 * @throws IOException
	 * @author sherif
	 */
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		getParametersFromComandLine(args);

		for(int i=1 ; i <= nrOfExperiments ; i++){
			logger.info("---------- Running " + i + ". Experiment ----------");
			GeoDistanceStabilityEvaluation evaluator = new GeoDistanceStabilityEvaluation();
			evaluator.evaluate();
			logger.info("\n---------- Results ----------\n" + evaluator.getResult());
			evaluator.saveResultToFile(outputFile + "_" + i + ".tsv");
		}
		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("All experiments done in " + totalTime + "ms" );
	}
}
