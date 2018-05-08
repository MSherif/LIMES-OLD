/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.PolygonReader;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.ParallelGeoHr3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.GreedyGeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.NaiveGeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.PSOGeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.PairBasedGeoLoadBalancer;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.parallelGeoLoadBalancer.dpso.DPSOGeoLoadBalancer;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.NaiveHausdorff;

/**
 * @author sherif
 *
 */
public class LB4LDEvaluation {
	private static final Logger logger = Logger.getLogger(LB4LDEvaluation.class.getName());

	private static final double TIME_DEV = 60000f; // to return time in min.

	protected static int experimentRepeats = 5;
	protected static int MaxThreadCount = 4;
	protected static float distanceThreshold = 2;
	protected static int PSOIterationCount;
	protected static int DPSOIterationCount;
	protected static String result = new String();
	protected static String inputFile;
	protected static String outputFile;
	private static int polygonSizeStart = 1;
	private static int polygonSizeEnd = 10;
	private static float mean = 0f;
	private static float variance = 1f;

	private static boolean doGeoHR3 = false;

	/**
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length < 9 || args[0].equals("-?")){
			logger.info("Wrong parameters number\nParameters:\n\t" +
					"args[0] = Experiment type [PSO2GHR3, ParallelLB, DPSO, allrandom]\n\t" +
					"args[1] = Count of experiment repeats\n\t" +
					"args[2] = Max number of threads\n\t" +
					"args[3] = Distance threshold\n\t" +
					"args[4] = Polygon number in form of start_increment_end\n\t" +
					"args[5] = Input File (- for generating random polygons)\n\t" +
					"args[6] = Output file \n\t" +
					"args[7] = PSO iteration count\n\t" +
					"args[8] = DPSO iteration count\n\t" +
					"args[9] = Random polygon start size\n\t" +
					"args[10]= Random polygon end size\n\t" +
					"args[11]= Mean\n\t" +
					"args[12]= Variance\n\t" +
					"args[13]= add GeoHR3 time to result (false by default)\n\t") ;
			System.exit(1);
		}

		String experimentType = args[0];
		experimentRepeats 	= Integer.parseInt(args[1]);
		MaxThreadCount 		= Integer.parseInt(args[2]);
		distanceThreshold 	= Float.parseFloat(args[3]);
		String polyNr 		= args[4];
		int polyNrStart 	= Integer.parseInt(polyNr.split("_")[0]);
		int polyNrIncr 		= Integer.parseInt(polyNr.split("_")[1]);
		int polyNrEnd 		= Integer.parseInt(polyNr.split("_")[2]);
		inputFile			= args[5];
		outputFile			= args[6];
		PSOIterationCount	= Integer.parseInt(args[7]);
		DPSOIterationCount	= Integer.parseInt(args[8]);
		if(args.length > 9){
			polygonSizeStart 	= Integer.parseInt(args[9]);
			polygonSizeEnd 	= Integer.parseInt(args[10]);
			mean 			= Float.parseFloat(args[11]);
			variance 		= Float.parseFloat(args[12]);
		}
		doGeoHR3 		= Boolean.parseBoolean(args[13]);
		if(experimentType.toLowerCase().equals("pso2ghr3")){
			evaluatePSO2GHR3(polyNrStart, polyNrIncr, polyNrEnd);
		}else if(experimentType.toLowerCase().equals("parallellb")){
			evaluateAllLB(polyNrStart, polyNrIncr, polyNrEnd);
		}else if(experimentType.toLowerCase().equals("dpso")){
			evaluateDPSO(polyNrStart, polyNrIncr, polyNrEnd, DPSOIterationCount);
		}else if(experimentType.toLowerCase().equals("allrandom")){
			evaluateAllRandomPolygons(polyNrStart, polyNrIncr, polyNrEnd);
		}

		saveResultsToFile(outputFile);
	}

	public static void oldMain(String[] args) throws IOException {
		if(args.length < 6 || args[0].equals("-?")){
			logger.info("Wrong parameters number\nParameters:\n\t" +
					"args[0] = Count of experiment repeats\n\t" +
					"args[1] = Distance threshold\n\t" +
					"args[2] = Polygon number in form of start_increment_end\n\t" +
					"args[3] = Input File\n\t" +
					"args[4] = Output file\n\t" +
					"args[5] = NewLB iteration countform of \n\t") ;
			System.exit(1);
		}

		experimentRepeats 	= Integer.parseInt(args[0]);
		distanceThreshold 	= Float.parseFloat(args[1]);
		String polyNr 		= args[2];
		int polyNrStart 	= Integer.parseInt(polyNr.split("_")[0]);
		int polyNrIncr 		= Integer.parseInt(polyNr.split("_")[1]);
		int polyNrEnd 		= Integer.parseInt(polyNr.split("_")[2]);
		inputFile			= args[3];
		outputFile			= args[4];
		int lbItrMaxNr		= Integer.parseInt(args[5]);
		evaluateDPSO(polyNrStart, polyNrIncr, polyNrEnd, lbItrMaxNr);
		saveResultsToFile(outputFile);
	}
	/**
	 * @param inputFile
	 * @author sherif
	 */
	private static void evaluatePSO2GHR3(int polyStart, int PolyInc, int PolyEnd) {
		result = "\nRepeat\tPolyNr\tGeoHR3Time\tGeoHR3Indexing\t" ;
		for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
			result += threadNr + "Threads\tindexingTime\ttasksCreationTime\tloadBalancerTime\t" ;
		}
		result += "\n";
		//		System.out.println("-------------------- GeoHR3 --------------------");
		for(int polyNr = polyStart ; polyNr <= PolyEnd ; polyNr += PolyInc ){ //731922
			for(int repeat = 1 ; repeat <= experimentRepeats ; repeat++){
				logger.info("REPEAT (" + repeat + "):");
				logger.info("Reading " + polyNr + " polygons ...");
				Set<Polygon> sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);
				long startTime = System.currentTimeMillis();
				GeoHR3 geoHr3 = new GeoHR3(0.5f, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(new NaiveHausdorff()));
				Mapping m1 = geoHr3.run(sourcePolygonSet, sourcePolygonSet);
				//				System.out.println("Mapping:"+ m1);
				long hr3Time = System.currentTimeMillis() - startTime;
				result += repeat + "\t" + polyNr + "\t" + hr3Time + "\t" + geoHr3.indexingTime + "\t" ;

				//			System.out.println("-------------------- Parallel GeoHR3 --------------------");
				for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
					logger.info("Evaluation using " + threadNr + "threads ...");
					startTime = System.currentTimeMillis();
					ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(1f, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), new DPSOGeoLoadBalancer(threadNr, PSOIterationCount), threadNr);

					Mapping m2 = parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
					//					System.out.println("Mapping:"+ m2);
					result += (System.currentTimeMillis() - startTime) + "\t" + 
							parallelGeoHr3.indexingTime + "\t" + parallelGeoHr3.tasksCreationTime + "\t" +
							parallelGeoHr3.loadBalancerTime + "\t";
					System.out.println("result so far:\n" + result);
				}
				result += "\n";
			}
			System.out.println("result so far:\n" + result);
		}
		System.out.println("Final Results:\n" + result);
	}

	/**
	 * @param polyStart
	 * @param polyInc
	 * @param polyEnd
	 * @author sherif
	 */
	private static void evaluateAllLB(int polyStart, int polyInc, int polyEnd) {
		result = "\nRepeat\tPolyNr\tThreadNr\t" +
				"NaiveTime\tNaiveMSE\t" +
				"GreedyTime\tGreedyMSE\t" +
				"PairBasedTime\tPairBasedMSE\n" +
				"PSOTime\tPSOMSE\t" +
				"DPSOTime\tDPSOMSE\n" ;


		//		System.out.println("-------------------- GeoHR3 --------------------");
		for(int polyNr = polyStart ; polyNr <= polyEnd ; polyNr += polyInc ){ //731922
			for(int repeat = 1 ; repeat <= experimentRepeats ; repeat++){
				logger.info("REPEAT (" + repeat + "):");
				logger.info("Reading " + polyNr + " polygons ...");
				Set<Polygon> sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);
				//				long startTime = System.currentTimeMillis();
				//				GeoHR3 geoHr3 = new GeoHR3(0.5f, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(new NaiveHausdorff()));
				//				geoHr3.run(sourcePolygonSet, sourcePolygonSet);
				//				long hr3Time = System.currentTimeMillis() - startTime;

				//			System.out.println("-------------------- Parallel GeoHR3 --------------------");
				for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
					logger.info("Evaluation using " + threadNr + " threads ...");

					// NaiveGeoLoadBalancer
					long startTime = System.currentTimeMillis();
					NaiveGeoLoadBalancer naiveLB = new NaiveGeoLoadBalancer(threadNr);
					ParallelGeoHr3 naive = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), naiveLB, threadNr);
					naive.run(sourcePolygonSet, sourcePolygonSet);
					long naiveTime = System.currentTimeMillis() - startTime;

					// GreedyGeoLoadBalancer
					startTime = System.currentTimeMillis();
					GreedyGeoLoadBalancer greedyLB = new GreedyGeoLoadBalancer(threadNr);
					ParallelGeoHr3 greedy = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), greedyLB, threadNr);
					greedy.run(sourcePolygonSet, sourcePolygonSet);
					long greedyTime = System.currentTimeMillis() - startTime;

					// PairBasedGeoLoadBalancer
					startTime = System.currentTimeMillis();
					PairBasedGeoLoadBalancer pairBasedLB = new PairBasedGeoLoadBalancer(threadNr);
					ParallelGeoHr3 pairBased = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), pairBasedLB, threadNr);
					pairBased.run(sourcePolygonSet, sourcePolygonSet);
					long pairBasedTime = System.currentTimeMillis() - startTime;

					// DPSOGeoLoadBalancer
					startTime = System.currentTimeMillis();
					DPSOGeoLoadBalancer dpsoLB = new DPSOGeoLoadBalancer(threadNr, DPSOIterationCount);
					ParallelGeoHr3 newLB = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), dpsoLB, threadNr);
					newLB.run(sourcePolygonSet, sourcePolygonSet);
					long dpsoTime = System.currentTimeMillis() - startTime;

					// PSOGeoLoadBalancer
					startTime = System.currentTimeMillis();
					PSOGeoLoadBalancer psoGeoLB = new PSOGeoLoadBalancer(threadNr, PSOIterationCount);
					ParallelGeoHr3 pso = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), psoGeoLB, threadNr);
					pso.run(sourcePolygonSet, sourcePolygonSet);
					long psoTime = System.currentTimeMillis() - startTime;

					result += repeat + "\t" + polyNr + "\t" + threadNr + "\t" + 
							naiveTime     + "\t" + naiveLB.getMeanSquaredError()     + "\t" + 
							greedyTime    + "\t" + greedyLB.getMeanSquaredError()    + "\t" +
							pairBasedTime + "\t" + pairBasedLB.getMeanSquaredError() + "\t" +
							psoTime       + "\t" + psoGeoLB.getMeanSquaredError()       + "\t" +
							dpsoTime 	  + "\t" + dpsoLB.getMeanSquaredError() 	  + "\n" ;

				}
				//				System.out.println(result);
			}
			System.out.println(result);
		}
		System.out.println("Final Results:\n" + result);
	}


	private static void evaluateDPSO(int polyStart, int polyInc, int polyEnd, int itr) {
		result = "\nRepeat\tPolyNr\tThreadNr\t" +
				"DPSOTimr\tDPSOMSE\n" ;

		for(int polyNr = polyStart ; polyNr <= polyEnd ; polyNr += polyInc ){ //731922
			for(int repeat = 1 ; repeat <= experimentRepeats ; repeat++){
				logger.info("REPEAT (" + repeat + "):");
				logger.info("Reading " + polyNr + " polygons ...");
				Set<Polygon> sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);

				for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
					logger.info("Evaluation using " + threadNr + " threads ...");

					// NewGeoLoadBalancer
					long startTime = System.currentTimeMillis();
					DPSOGeoLoadBalancer dpso = new DPSOGeoLoadBalancer(threadNr, itr);
					ParallelGeoHr3 dbso = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), dpso, threadNr);
					dbso.run(sourcePolygonSet, sourcePolygonSet);
					long newLBTime = System.currentTimeMillis() - startTime;

					result += repeat + "\t" + polyNr + "\t" + threadNr + "\t" + 
							newLBTime 	  + "\t" + dpso.getMeanSquaredError() 	  + "\n" ;
				}
				//				System.out.println(result);
			}
			System.out.println(result);
		}
		System.out.println("Final Results:\n" + result);
	}

	private static void trainDPSOParameters(int polyStart, int polyInc, int PolyEnd, int lbItrMaxNr) {
		result = "\nRepeat\tPolyNr\tnewLBItrCount\tThreadNr\t" +
				"NewTimr\tNewMSE\n" ;
		//		System.out.println("-------------------- GeoHR3 --------------------");
		for(int polyNr = polyStart ; polyNr <= PolyEnd ; polyNr += polyInc ){ //731922
			for(int repeat = 1 ; repeat <= experimentRepeats ; repeat++){
				logger.info("REPEAT (" + repeat + "):");
				logger.info("Reading " + polyNr + " polygons ...");
				Set<Polygon> sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);

				for(int lbItr = 1 ; lbItr <= lbItrMaxNr ; lbItr++){
					logger.info("Evaluating NewGeoLoadBalancer with " + lbItr + " iterations ...");
					int threadNr = 4;
					logger.info("Evaluation using " + threadNr + " threads ...");
					// NewGeoLoadBalancer
					long startTime = System.currentTimeMillis();
					DPSOGeoLoadBalancer newGeoLoadBalancer = new DPSOGeoLoadBalancer(threadNr, lbItr);
					ParallelGeoHr3 newLB = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), newGeoLoadBalancer, threadNr);
					newLB.run(sourcePolygonSet, sourcePolygonSet);
					long newLBTime = System.currentTimeMillis() - startTime;

					result += repeat + "\t" + polyNr + "\t"  + lbItr + "\t" + threadNr + "\t" + 
							newLBTime 	  + "\t" + newGeoLoadBalancer.getMeanSquaredError() 	  + "\n" ;
					//				System.out.println(result);
				}
				System.out.println(result);
			}
		}
		System.out.println("Final Results:\n" + result);
	}

	
	/**
	 * @param polyStart
	 * @param polyInc
	 * @param polyEnd
	 * @author sherif
	 * @throws IOException 
	 */
	private static void _evaluateAllRandomPolygons(int polyStart, int polyInc, int polyEnd) throws IOException {
		RandomPolygonGenerator r = new RandomPolygonGenerator(mean, variance);

		result = "\nPolyNr\tThreadNr\t" +
				"NaiveTime\t" +
				"GreedyTime\t" +
				"PairBasedTime\t" +
				"PSOTime\t" +
				"DPSOTime\t" +
				"NaiveMSE\t" +
				"GreedyMSE\t" +
				"PairBasedMSE\t" +
				"PSOMSE\t" +
				"DPSOMSE\t" +
				"PSOTimeSD\t" +
				"PSOMSESD\t" +
				((doGeoHR3) ? "GeoHR3Time" : "") +
				"\n" ;
		//		System.out.println("-------------------- GeoHR3 --------------------");
		for(int polyNr = polyStart ; polyNr <= polyEnd ; polyNr += polyInc ){ //731922
			Set<Polygon> sourcePolygonSet = new HashSet<Polygon>();
			if(inputFile.equals("-")){
				logger.info("Generating " + polyNr + " random polygons ...");
				sourcePolygonSet = r.createRandomPolygons(polyNr, polygonSizeStart, polygonSizeEnd);
				r.savePlygonsToFile(sourcePolygonSet, "dataset_" + sourcePolygonSet.size() + ".csv");
			}else{
				sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);
			}
			
			double hr3Time = -1;
			if(doGeoHR3){
				logger.info("Evaluation GeoHR3 ...");
				long startTime = System.currentTimeMillis();
				GeoHR3 geoHr3 = new GeoHR3(0.5f, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(new NaiveHausdorff()));
				geoHr3.run(sourcePolygonSet, sourcePolygonSet);
				hr3Time = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;
			}

			//			System.out.println("-------------------- Parallel GeoHR3 --------------------");
			for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
				logger.info("Evaluation using " + threadNr + " threads ...");

				// NaiveGeoLoadBalancer
				long startTime = System.currentTimeMillis();
				NaiveGeoLoadBalancer naiveLB = new NaiveGeoLoadBalancer(threadNr);
				ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), naiveLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double naiveTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// GreedyGeoLoadBalancer
				startTime = System.currentTimeMillis();
				GreedyGeoLoadBalancer greedyLB = new GreedyGeoLoadBalancer(threadNr);
				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), greedyLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double greedyTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// PairBasedGeoLoadBalancer
				startTime = System.currentTimeMillis();
				PairBasedGeoLoadBalancer pairBasedLB = new PairBasedGeoLoadBalancer(threadNr);
				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), pairBasedLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double pairBasedTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// PSOGeoLoadBalancer
				double[] psoRunTimes = new double[experimentRepeats];
				double[] psoMSEs = new double[experimentRepeats];
				for(int repeat = 0 ; repeat < experimentRepeats ; repeat++){
					startTime = System.currentTimeMillis();
					PSOGeoLoadBalancer psoGeoLB = new PSOGeoLoadBalancer(threadNr, PSOIterationCount);
					parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
							SetMeasureFactory.getType(new NaiveHausdorff()), psoGeoLB, threadNr);
					parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
					psoRunTimes[repeat] = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;
					psoMSEs[repeat] = psoGeoLB.getMeanSquaredError();
				}
				Statistics psoTime = new Statistics(psoRunTimes);
				Statistics psoMSE = new Statistics(psoMSEs);

				// DPSOGeoLoadBalancer
				startTime = System.currentTimeMillis();
				DPSOGeoLoadBalancer dpsoLB = new DPSOGeoLoadBalancer(threadNr, DPSOIterationCount);
				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), dpsoLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double dpsoTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				result += polyNr + "\t" + threadNr + "\t" + 
						naiveTime     + "\t" +
						greedyTime    + "\t" +
						pairBasedTime + "\t" +
						psoTime.getMean()       + "\t" +
						dpsoTime 	  + "\t" +
						naiveLB.getMeanSquaredError()     + "\t" + 
						greedyLB.getMeanSquaredError()    + "\t" +
						pairBasedLB.getMeanSquaredError() + "\t" +
						psoMSE.getMean()   						  + "\t" +
						dpsoLB.getMeanSquaredError() 	  + "\t" +
						psoTime.getStdDev()	+ "\t" +
						psoMSE.getStdDev()	  + "\t" +
						((doGeoHR3)? hr3Time : "")      + "\n" ;
				System.out.println("result so far:\n" + result);
			}
			System.out.println(result);
			saveResultsToFile(outputFile);
		}
		System.out.println("Final Results:\n" + result);
	}

	/**
	 * @param polyStart
	 * @param polyInc
	 * @param polyEnd
	 * @author sherif
	 * @throws IOException 
	 */
	private static void evaluateAllRandomPolygons(int polyStart, int polyInc, int polyEnd) throws IOException {
		RandomPolygonGenerator r = new RandomPolygonGenerator(mean, variance);

		result = "\nPolyNr\tThreadNr\t" +
				"NaiveTime\t" +
//				"GreedyTime\t" +
//				"PairBasedTime\t" +
//				"PSOTime\t" +
				"DPSOTime\t" +
				"NaiveMSE\t" +
//				"GreedyMSE\t" +
//				"PairBasedMSE\t" +
//				"PSOMSE\t" +
				"DPSOMSE\t" +
//				"PSOTimeSD\t" +
//				"PSOMSESD\t" +
				((doGeoHR3) ? "GeoHR3Time" : "") +
				"\n" ;
		//		System.out.println("-------------------- GeoHR3 --------------------");
		for(int polyNr = polyStart ; polyNr <= polyEnd ; polyNr += polyInc ){ //731922
			Set<Polygon> sourcePolygonSet = new HashSet<Polygon>();
			if(inputFile.equals("-")){
				logger.info("Generating " + polyNr + " random polygons ...");
//				sourcePolygonSet = r.createRandomPolygons(polyNr, polygonSizeStart, polygonSizeEnd);
				sourcePolygonSet = r.createSkewedRandomPolygons(polyNr, polygonSizeStart, polygonSizeEnd, 0.9f);
				r.savePlygonsToFile(sourcePolygonSet, "dataset_" + sourcePolygonSet.size() + ".csv");
			}else{
				sourcePolygonSet = PolygonReader.readPolygons(inputFile, polyNr);
			}
			
			double hr3Time = -1;
			if(doGeoHR3){
				logger.info("Evaluation GeoHR3 ...");
				long startTime = System.currentTimeMillis();
				GeoHR3 geoHr3 = new GeoHR3(0.5f, GeoHR3.DEFAULT_GRANULARITY, SetMeasureFactory.getType(new NaiveHausdorff()));
				geoHr3.run(sourcePolygonSet, sourcePolygonSet);
				hr3Time = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;
			}

			//			System.out.println("-------------------- Parallel GeoHR3 --------------------");
			for(int threadNr = 2; threadNr <= MaxThreadCount ; threadNr *= 2){
				logger.info("Evaluation using " + threadNr + " threads ...");

				// NaiveGeoLoadBalancer
				long startTime = System.currentTimeMillis();
				NaiveGeoLoadBalancer naiveLB = new NaiveGeoLoadBalancer(threadNr);
				ParallelGeoHr3 parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), naiveLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double naiveTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// GreedyGeoLoadBalancer
//				startTime = System.currentTimeMillis();
//				GreedyGeoLoadBalancer greedyLB = new GreedyGeoLoadBalancer(threadNr);
//				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
//						SetMeasureFactory.getType(new NaiveHausdorff()), greedyLB, threadNr);
//				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
//				double greedyTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// PairBasedGeoLoadBalancer
//				startTime = System.currentTimeMillis();
//				PairBasedGeoLoadBalancer pairBasedLB = new PairBasedGeoLoadBalancer(threadNr);
//				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
//						SetMeasureFactory.getType(new NaiveHausdorff()), pairBasedLB, threadNr);
//				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
//				double pairBasedTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				// PSOGeoLoadBalancer
//				double[] psoRunTimes = new double[experimentRepeats];
//				double[] psoMSEs = new double[experimentRepeats];
//				for(int repeat = 0 ; repeat < experimentRepeats ; repeat++){
//					startTime = System.currentTimeMillis();
//					PSOGeoLoadBalancer psoGeoLB = new PSOGeoLoadBalancer(threadNr, PSOIterationCount);
//					parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
//							SetMeasureFactory.getType(new NaiveHausdorff()), psoGeoLB, threadNr);
//					parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
//					psoRunTimes[repeat] = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;
//					psoMSEs[repeat] = psoGeoLB.getMeanSquaredError();
//				}
//				Statistics psoTime = new Statistics(psoRunTimes);
//				Statistics psoMSE = new Statistics(psoMSEs);

				// DPSOGeoLoadBalancer
				startTime = System.currentTimeMillis();
				DPSOGeoLoadBalancer dpsoLB = new DPSOGeoLoadBalancer(threadNr, DPSOIterationCount);
				parallelGeoHr3 = new ParallelGeoHr3(distanceThreshold, GeoHR3.DEFAULT_GRANULARITY, 
						SetMeasureFactory.getType(new NaiveHausdorff()), dpsoLB, threadNr);
				parallelGeoHr3.run(sourcePolygonSet, sourcePolygonSet);
				double dpsoTime = (double)(System.currentTimeMillis() - startTime)/ TIME_DEV;

				result += polyNr + "\t" + threadNr + "\t" + 
						naiveTime     + "\t" +
//						greedyTime    + "\t" +
//						pairBasedTime + "\t" +
//						psoTime.getMean()       + "\t" +
						dpsoTime 	  + "\t" +
						naiveLB.getMeanSquaredError()     + "\t" + 
//						greedyLB.getMeanSquaredError()    + "\t" +
//						pairBasedLB.getMeanSquaredError() + "\t" +
//						psoMSE.getMean()   						  + "\t" +
						dpsoLB.getMeanSquaredError() 	  + "\t" +
//						psoTime.getStdDev()	+ "\t" +
//						psoMSE.getStdDev()	  + "\t" +
						((doGeoHR3)? hr3Time : "")      + "\n" ;
				System.out.println("result so far:\n" + result);
			}
			System.out.println(result);
			saveResultsToFile(outputFile);
		}
		System.out.println("Final Results:\n" + result);
	}


	public static void saveResultsToFile(String fileName) throws IOException{
		logger.info("Saving results to file: " + fileName + " ...");
		long startTime = System.currentTimeMillis();

		File file = new File(fileName);

		// if file does not exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		bw.write(result);
		bw.close();

		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Saving file done in "+ totalTime + "ms.");

	}
}
