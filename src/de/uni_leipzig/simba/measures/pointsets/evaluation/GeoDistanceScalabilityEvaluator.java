/**
 * 
 */
package de.uni_leipzig.simba.measures.pointsets.evaluation;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonReader;
import de.uni_leipzig.simba.measures.pointsets.SetMeasure;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;

/**
 * evaluating different polygon distance functions for scalability
 * @author sherif
 *
 */
public class GeoDistanceScalabilityEvaluator extends GeoDistancesEvaluator {
	private static final Logger logger = 	Logger.getLogger(GeoDistanceScalabilityEvaluator.class.getName());

	private int 							chunkSize	= 200;
	private Map<Float, Map<String, Long>> 	chunkSize2distanceName2time;

	private static int initChunkSize 				= -1; // the whole file by default
	private static int incrementChunkSize;
	private static int maxChunkSize;

	private static float orchidThresholdStart 	= 1f; // 1KM
	private static float orchidThresholdIncrement	= 1f;  // 1km 
	private static float orchidThresholdEnd 		= 3f;  // 3km


	/**
	 * constructor to initialize geoDistances to all available geoDistances so far
	 *@author sherif
	 */
	public GeoDistanceScalabilityEvaluator(){
		super();
		chunkSize2distanceName2time = new TreeMap<Float, Map<String,Long>>();
	}

	/**
	 * @param file
	 * @param size
	 *@author sherif
	 */
	public GeoDistanceScalabilityEvaluator(String file, int size){
		this();
		chunkSize 	= size;
		sourcePolygonSet 	= PolygonReader.readPolygons(file, chunkSize);
	}

	/**
	 * @param geoDistance
	 * @return
	 * @author sherif
	 */
	public long computeDistanceTime(SetMeasure geoDistance, float orchidThreshold){
		long startTime = System.currentTimeMillis();
		logger.info("Evaluationg distance: " + geoDistance.getName() + " for " + sourcePolygonSet.size() 
				+ "*" + sourcePolygonSet.size() + " polygons");
		if(useOrchid){
			GeoHR3 orchid = new GeoHR3(orchidThreshold, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(geoDistance));
			orchid.run(sourcePolygonSet, sourcePolygonSet);
		}
		else{
			for(Polygon X : sourcePolygonSet){
				for(Polygon Y : sourcePolygonSet){
					double d = geoDistance.computeDistance(X, Y, Double.MAX_VALUE);
				}
			}
		}
		return System.currentTimeMillis() - startTime;
	}


	/**
	 * Runs the scalability evaluations against the polygons in the <strong>file</strong>  
	 * in incremental way; starting by <strong>initChunkSize</strong> using 
	 * step = <strong>incrementChunkSize</strong> ending by <strong>maxChunkSize</strong>
	 * @param file
	 * @param initChunkSize
	 * @param incrementChunkSize
	 * @param maxChunkSize
	 * @return
	 * @author sherif
	 */
	public Map<Float, Map<String, Long>> run(){
		int j = 1;
		for(int i=initChunkSize ; i<maxChunkSize ; i+=incrementChunkSize){
			logger.info("Compute distances for the " + j++ + ". chunk of size: " + i);
			this.chunkSize 	= incrementChunkSize;
			sourcePolygonSet 	= PolygonReader.readPolygons(inputFile, i);

			Map<String, Long> distance2time = new TreeMap<String, Long>();
			for(SetMeasure geoDistance : geoDistances){
				distance2time.put(geoDistance.getName(), computeDistanceTime(geoDistance, orchidThresholdStart));
			}
			chunkSize2distanceName2time.put((float) i, distance2time);
		}
		return chunkSize2distanceName2time;
	}

	/**
	 * Runs the scalability evaluations against all polygons in the file
	 * @param file
	 * @return
	 * @author sherif
	 */
	public Map<Float, Map<String, Long>> runWholeDatasetWithOrchid(){
		int j = 1;
		for(float orchidThreshold = orchidThresholdStart ; orchidThreshold <= orchidThresholdEnd ; orchidThreshold += orchidThresholdIncrement){
			sourcePolygonSet = PolygonReader.readPolygons(inputFile);
			logger.info("Running Orchid with threshold = " + orchidThreshold);
			Map<String, Long> distance2time = new TreeMap<String, Long>();
			for(SetMeasure geoDistance : geoDistances){
				logger.info("Test for distance function: " + geoDistance.getName());
				distance2time.put(geoDistance.getName(), computeDistanceTime(geoDistance, orchidThreshold));
                                logger.info(distance2time);
			}
			chunkSize2distanceName2time.put(orchidThreshold, distance2time);
		}
		return chunkSize2distanceName2time;
	}


	/**
	 * @return
	 * @author sherif
	 */
	public String getResult(){
		//if already computed do not compute it again
		if(result.length() != 0){
			return result;
		}

		//table header
		result = "Threshould\t";
		double firstChunk = chunkSize2distanceName2time.keySet().iterator().next();
		Map<String, Long> distance2time = chunkSize2distanceName2time.get(firstChunk);
		for(String geoDistanceName : distance2time.keySet()){
			result += geoDistanceName + "\t"; 
		}
		result += "\n";

		//table content
		for(double chunk : chunkSize2distanceName2time.keySet()){
			result += chunk + "\t";
			distance2time = chunkSize2distanceName2time.get(chunk);
			for(String distance : distance2time.keySet()){
				result += distance2time.get(distance) + "\t";
			}
			result += "\n";
		}
		return result;
	}


	private static void getParametersFromComandLine(String[] args) {
		helpStr = "Parameters:\n" +
				"\targs[0]: Number of experiments\n" +
				"\targs[1]: Input dataset file\n" +
				"\targs[2]: Output file Name\n" +
				"Optional parameters:\n" +
				"\targs[3]: [true_S_I_E/false] Use Orchid, S, I and E for orchid threshod start, increment and end values\n" +
				"\targs[4]: Data chunk initial size (the whole dataset at once by default) \n" +
				"\targs[5]: Data chunk increment size\n" +
				"\targs[6]: Data chunk Maximum size\n" ;

		if(args[0].equals("-?")){
			logger.info(helpStr);
			System.exit(0);
		}
		if(args.length < 3){
			logger.error("Wrong number of parameters.\n" + helpStr);
			System.exit(1);
		}
		nrOfExperiments 				= Integer.parseInt(args[0]);
		inputFile 						= args[1];
		outputFile 						= args[2];
		if(args.length > 3){
			if(args[3].contains("_")){
				String str[] 			= args[3].split("_");
				useOrchid 				= Boolean.parseBoolean(str[0]);
				orchidThresholdStart 	= Float.parseFloat(str[1]);
				orchidThresholdIncrement= Float.parseFloat(str[2]);
				orchidThresholdEnd		= Float.parseFloat(str[3]);
			}else{
				useOrchid 				= Boolean.parseBoolean(args[3]);
			}
		}
		if(args.length > 4){
			initChunkSize				= Integer.parseInt(args[4]);
			incrementChunkSize			= Integer.parseInt(args[5]);
			maxChunkSize 				= Integer.parseInt(args[6]);
		}
	}
	
	
	/**
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		getParametersFromComandLine(args);

		for(int i=1 ; i <= nrOfExperiments ; i++){
			logger.info("Running experiment number " + i);
			GeoDistanceScalabilityEvaluator evaluator = new GeoDistanceScalabilityEvaluator();
			if(initChunkSize == -1){
				evaluator.runWholeDatasetWithOrchid();
			}else{
				evaluator.run();
			}
			logger.info("Result of experiment number " + i + "\n" + evaluator.getResult());
			evaluator.saveResultToFile(outputFile + "_" + i + ".tsv");
		}

	}

}
