/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.space;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.measures.date.DayMeasure;
import de.uni_leipzig.simba.measures.date.SimpleDateMeasure;
import de.uni_leipzig.simba.measures.date.YearMeasure;

/**
 *
 * @author ngonga
 */
public class SpaceMeasureFactory {

    static Logger logger = Logger.getLogger("LIMES");

    public static SpaceMeasure getMeasure(String name, int dimension) {
//    	System.out.println("SpaceMesure.getMeasure("+name+")");
    	if (name.toLowerCase().startsWith("geo")) {
            if (dimension != 2) {
                logger.warn("Erroneous dimension settings for GeoDistance (" + dimension + ").");
            }
            return new GeoDistance();
        } else if (name.toLowerCase().startsWith("datesim")) {
        	return new SimpleDateMeasure();
        }
        else if (name.toLowerCase().startsWith("daysim")) {
        	return new DayMeasure();  
        } 
        else if (name.toLowerCase().startsWith("yearsim")) {
        	return new  YearMeasure();
        }
        else {
            EuclideanMetric measure = new EuclideanMetric();
            measure.setDimension(dimension);
            return measure;
        }
    }
}
