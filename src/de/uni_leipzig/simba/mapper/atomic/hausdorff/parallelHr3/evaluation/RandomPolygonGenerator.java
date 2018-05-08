/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.uni_leipzig.simba.data.Point;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.internal.matchers.Each;

/**
 * @author sherif
 *
 */
public class RandomPolygonGenerator {
	private static final Logger logger = Logger.getLogger(RandomPolygonGenerator.class.getName());

	private static final String POLYGON_PREFIX = "polygon_";
	private static Random r = new Random();
	private static float mean; 
	private static  float variance;
	private static  long polygonCounter;

	/**
	 * initialize a normal Gaussian random generator with 
	 * mean = 0 and variance = 1
	 *@author sherif
	 */
	RandomPolygonGenerator(){
		mean = 0.0f;
		variance = 1.0f;
	}
	
	/**
	 * initialize a normal Gaussian random generator 
	 * with the given men and variance
	 * @param mean
	 * @param variance
	 *@author sherif
	 */
	RandomPolygonGenerator(float mean, float variance){
		RandomPolygonGenerator.mean = mean;
		RandomPolygonGenerator.variance = variance;
	}
	
	private static float getGaussian(){
		return (float) (mean + r.nextGaussian() * variance);
	}

	private static double getRandomLongitude(){
		double l;
		do{
			l = 180.0f + getGaussian() * -360.0f;
		}while(l > 180 || l < -180);
		return l;
	}
	
	private static double getRandomLatitude(){
		double l;
		do{
			l = 90f + getGaussian() * -180.0f;
		}while(l > 90 || l < -90);
		return l;
	}

	private static void log(Object aMsg){
		System.out.println(String.valueOf(aMsg));
	}

	/**
	 * create a polygon containing a set of polygonSize random points
	 * @param polygonSize
	 * @return
	 * @author sherif
	 */
	public Polygon createRandomPolygon(int polygonSize){
		Polygon p = new Polygon(POLYGON_PREFIX + polygonCounter++);
		for(int i=0; i<polygonSize; i++){
			//data is stored as long, lat
			p.add(new Point("", Arrays.asList(getRandomLongitude(), getRandomLatitude())));
		}
		return p;
	}
	
	/**
	 * create a polygon containing a set of polygonSize random points skewed by the skewRatio
	 * @param polygonSize
	 * @param skewRatio
	 * @return
	 * @author sherif
	 */
	public Polygon createRandomPolygon(int polygonSize, float skewRatio){
		Polygon p = new Polygon(POLYGON_PREFIX + polygonCounter++);
		for(int i=0; i<polygonSize; i++){
			//data is stored as long, lat
			p.add(new Point("", Arrays.asList(getRandomLongitude()*skewRatio, getRandomLatitude()*skewRatio)));
		}
		return p;
	}
	
	/**
	 * create a set of polygonNr polygons each contains polygonSize random points
	 * @param polygonNr
	 * @param polygonSize
	 * @return
	 * @author sherif
	 */
	public Set<Polygon> createRandomPolygons(int polygonNr, int polygonSizeStart, int polygonSizeEnd){
		long start = System.currentTimeMillis();
		int polygonSizeRange = polygonSizeEnd - polygonSizeStart;
		Set<Polygon> result = new HashSet<Polygon>();
		for(int i=0; i<polygonNr; i++){
			int polygonSize =(int )((double) (polygonSizeStart) + Math.random() * (double) (polygonSizeRange));
			result.add(createRandomPolygon(polygonSize));
		}
		long time = System.currentTimeMillis() - start;
		logger.info("Generating " + polygonNr + " polygons in done in " + time + "ms");
		return result;
	}
	
	
	/**
	 * create a set of polygonNr polygons each contains polygonSize random points
	 * @param polygonNr
	 * @param polygonSizeStart
	 * @param polygonSizeEnd
	 * @param skewwRatio the percentage of skewed polygons 
	 * @return
	 * @author sherif
	 */
	public Set<Polygon> createSkewedRandomPolygons(int polygonNr, int polygonSizeStart, int polygonSizeEnd, float skewRatio){
		long start = System.currentTimeMillis();
		int polygonSizeRange = polygonSizeEnd - polygonSizeStart;
		Set<Polygon> result = new HashSet<Polygon>();
		for(int i=0; i <= polygonNr*skewRatio ; i++){
			int polygonSize =(int )((double) (polygonSizeStart) + Math.random() * (double) (polygonSizeRange));
			result.add(createRandomPolygon(polygonSize, 1-skewRatio));
		}
		for(int i= (int) (polygonNr*skewRatio +1); i < polygonNr; i++){
			int polygonSize =(int )((double) (polygonSizeStart) + Math.random() * (double) (polygonSizeRange));
			result.add(createRandomPolygon(polygonSize));
		}
		long time = System.currentTimeMillis() - start;
		logger.info("Generating " + polygonNr + " polygons in done in " + time + "ms");
		return result;
	}
	
	public void savePlygonsToFile(Set<Polygon> polygons, String fileName) throws IOException{
		logger.info("Saving " + polygons.size() + "polygons to file: " + fileName + " ...");
		long startTime = System.currentTimeMillis();

		File file = new File(fileName);

		// if file does not exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		
		for (Polygon poly : polygons) {
			bw.write(poly.uri + " ");
			for ( Point  p: poly.points) {
				bw.write(p.coordinates.get(1) + " " + p.coordinates.get(0)); 
			}
			bw.write("\n");
		}
		bw.close();
		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Saving file done in "+ totalTime + "ms.");

	}
	
	public static void main(String... aArgs) throws IOException{
		RandomPolygonGenerator r = new RandomPolygonGenerator();
		Set<Polygon> polygons = r.createRandomPolygons(5, 3, 3);
		r.savePlygonsToFile(polygons, "/media/lod2/geodatasets/datasets/test.csv");
//		for (Polygon poly : polygons) {
//			System.out.print(poly.uri + " " );
//			for ( Point  p: poly.points) {
//				System.out.print(p.coordinates.get(1) + " " + p.coordinates.get(0)); 
//			}
//			System.out.print("\n");
//		}
	}
}
