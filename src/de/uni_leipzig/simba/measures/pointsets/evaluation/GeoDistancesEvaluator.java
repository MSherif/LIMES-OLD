/**
 * 
 */
package de.uni_leipzig.simba.measures.pointsets.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.measures.pointsets.SetMeasure;
import de.uni_leipzig.simba.measures.pointsets.average.NaiveAverage;
import de.uni_leipzig.simba.measures.pointsets.frechet.GeOxygeneFrechet;
import de.uni_leipzig.simba.measures.pointsets.frechet.NaiveFrechet;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.NaiveHausdorff;
import de.uni_leipzig.simba.measures.pointsets.link.NaiveLink;
import de.uni_leipzig.simba.measures.pointsets.max.NaiveMax;
import de.uni_leipzig.simba.measures.pointsets.mean.NaiveMean;
import de.uni_leipzig.simba.measures.pointsets.min.NaiveMin;
import de.uni_leipzig.simba.measures.pointsets.sumofmin.NaiveSumOfMin;
import de.uni_leipzig.simba.measures.pointsets.surjection.FairSurjection;
import de.uni_leipzig.simba.measures.pointsets.surjection.NaiveSurjection;

/**
 * Abstract class for evaluating different polygon distance functions
 * @author sherif
 *
 */
public abstract class GeoDistancesEvaluator {
	protected Set<Polygon> 	sourcePolygonSet;
	protected Set<Polygon> 	targetPolygonSet;
	protected List<SetMeasure> geoDistances;
	protected String 			result ;
	protected static boolean useOrchid 		= true;
	protected static String 	helpStr;
	protected static int 		nrOfExperiments	= 5 ;
	protected static String 	inputFile  		= "";
	protected static String 	outputFile 		= "";
	protected static int 		nrOfPolygons 	= -1;
	
	GeoDistancesEvaluator(){
		result 	= "";
		helpStr = "";
		
		sourcePolygonSet = new HashSet<Polygon>();
		targetPolygonSet = new HashSet<Polygon>();
		
		geoDistances = new ArrayList<SetMeasure>();
//		geoDistances.add(new NaiveMin());
//		geoDistances.add(new NaiveMax());
//		geoDistances.add(new NaiveAverage());
//		geoDistances.add(new NaiveSumOfMin());
//		geoDistances.add(new NaiveLink());
//		geoDistances.add(new NaiveSurjection());
//		geoDistances.add(new FairSurjection());
//		geoDistances.add(new NaiveHausdorff());
//		geoDistances.add(new NaiveMean());
		geoDistances.add(new NaiveFrechet());
//		geoDistances.add(new GeOxygeneFrechet());
//		geoDistances.add(new IndexedHausdorff());
//		geoDistances.add(new FastHausdorff());
//		geoDistances.add(new CentroidIndexedHausdorff());
	}
	
	
	public abstract String getResult();
	
	/**
	 * @param fileName
	 * @throws IOException
	 * @author sherif
	 */
	public void saveResultToFile(String fileName) throws IOException{
		File file = new File(fileName);

		if (!file.exists()) {
			file.createNewFile();
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		bw.write(getResult());
		bw.close();
	}
}
