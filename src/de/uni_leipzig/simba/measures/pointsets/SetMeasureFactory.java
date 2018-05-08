/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.measures.pointsets;

import org.apache.commons.collections.functors.InstanceofPredicate;

import de.uni_leipzig.simba.measures.pointsets.average.NaiveAverage;
import de.uni_leipzig.simba.measures.pointsets.frechet.NaiveFrechet;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.CentroidIndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.FastHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.IndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.NaiveHausdorff;
import de.uni_leipzig.simba.measures.pointsets.hausdorff.ScanIndexedHausdorff;
import de.uni_leipzig.simba.measures.pointsets.link.NaiveLink;
import de.uni_leipzig.simba.measures.pointsets.max.NaiveMax;
import de.uni_leipzig.simba.measures.pointsets.mean.NaiveMean;
import de.uni_leipzig.simba.measures.pointsets.min.NaiveMin;
import de.uni_leipzig.simba.measures.pointsets.sumofmin.NaiveSumOfMin;
import de.uni_leipzig.simba.measures.pointsets.surjection.FairSurjection;
import de.uni_leipzig.simba.measures.pointsets.surjection.NaiveSurjection;

/**
 * Generates a SetMeasure implementation
 *
 * @author ngonga
 */
public class SetMeasureFactory {

    public enum Type {
        NAIVEHAUSDORFF, INDEXEDHAUSDORFF, FASTHAUSDORFF, CENTROIDHAUSDORFF, SCANHAUSDORFF, GEOMIN, 
        GEOMAX, GEOAVG, GEOSUMMIN, GEOLINK, GEOQUINLAN, FRECHET, SURJECTION, FAIRSURJECTION, MEAN   
    };

    public static SetMeasure getMeasure(Type type) {
        SetMeasure measure;
        if (type == Type.NAIVEHAUSDORFF) {
            measure = new NaiveHausdorff();
        } else if (type == Type.FASTHAUSDORFF) {
            measure = new FastHausdorff();
        } else if (type == Type.INDEXEDHAUSDORFF) {
            measure = new IndexedHausdorff();
        } else if (type == Type.SCANHAUSDORFF) {
            measure = new ScanIndexedHausdorff();
        } else if (type == Type.GEOMIN) {
            measure = new NaiveMin();
        } else if (type == Type.GEOMAX) {
            measure = new NaiveMax();
        } else if (type == Type.GEOAVG) {
            measure = new NaiveAverage();
        } else if (type == Type.GEOSUMMIN) {
            measure = new NaiveSumOfMin();
        } else if (type == Type.GEOLINK) {
            measure = new NaiveLink();
        } else if (type == Type.FRECHET) {
            measure = new NaiveFrechet();
        } else if (type == Type.SURJECTION) {
            measure = new NaiveSurjection();
        } else if (type == Type.FAIRSURJECTION) {
            measure = new FairSurjection();
        } else if (type == Type.MEAN) {
            measure = new NaiveMean();
        } 
        else {
            measure = new CentroidIndexedHausdorff();
        }
        return measure;
    }
    
    public static Type getType(SetMeasure measure) {
        if (measure instanceof NaiveHausdorff) {
			return Type.NAIVEHAUSDORFF;
		}  
        if (measure instanceof FastHausdorff) {
			return Type.FASTHAUSDORFF;
		}
        if (measure instanceof IndexedHausdorff) {
			return Type.INDEXEDHAUSDORFF;
		}
        if (measure instanceof ScanIndexedHausdorff) {
			return Type.SCANHAUSDORFF;
		}
        if (measure instanceof NaiveMin) {
			return Type.GEOMIN;
		}
        if (measure instanceof NaiveMax) {
			return Type.GEOMAX;
		}
        if (measure instanceof NaiveAverage) {
			return Type.GEOAVG;
		}
        if (measure instanceof NaiveSumOfMin) {
			return Type.GEOSUMMIN;
		}
        if (measure instanceof NaiveLink) {
			return Type.GEOLINK;
		}
        if (measure instanceof NaiveFrechet) {
			return Type.FRECHET;
		}
        if (measure instanceof NaiveSurjection) {
			return Type.SURJECTION;
		}
        if (measure instanceof FairSurjection) {
			return Type.FAIRSURJECTION;
		}
        if (measure instanceof CentroidIndexedHausdorff) {
			return Type.CENTROIDHAUSDORFF;
		}
        if (measure instanceof NaiveMean) {
			return Type.MEAN;
		}
        return null;
    }
}
