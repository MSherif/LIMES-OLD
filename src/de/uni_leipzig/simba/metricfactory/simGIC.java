/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.metricfactory;
import java.util.HashMap;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import org.apache.log4j.Logger;
/**
 * Implements the simGIC metric for vector that contain information contents.
 * Assumes that the vectors look as follows:
 * Index<INDEXVALUESEP>Value<ENTRYSEP>(Index<INDEXVALUESEP>Value)*
 * @author ngonga
 */
public class simGIC extends AbstractStringMetric{

        static Logger logger = Logger.getLogger("LIMES");

    String ENTRYSEP = "\t";
    String INDEXVALUESEP = ":";

    /** Constructor. Allows to configure the separators used
     *
     * @param entrySeparator Value of ENTRYSEP
     * @param indexValueSeparator Value of INDEXVALUESEP
     */
    public simGIC(String entrySeparator, String indexValueSeparator)
    {
        ENTRYSEP = entrySeparator;
        INDEXVALUESEP = indexValueSeparator;
    }

    /** Constructor that assumes the default settings
     * 
     */
    public simGIC()
    {

    }

    @Override
    public String getShortDescriptionString() {
        return "simGIC metric";
    }

    @Override
    public String getLongDescriptionString() {
        return "simGIC metric";
    }

    @Override
    public String getSimilarityExplained(String string, String string1) {
        return "simGIC metric";
    }

    @Override
    public float getSimilarityTimingEstimated(String string, String string1) {
        return 0.0f;
    }

    /** Parses a vector entry and transforms it so that it can be used
     *
     * @param vector String representation of the vector
     * @return Vector as a HashMap from index to value
     */
    public HashMap<Integer, Double> getEntries(String vector)
    {
        //logger.info(vector);
        HashMap<Integer, Double> v = new HashMap<Integer, Double>();
        String split[] = vector.split(ENTRYSEP);
        String entryValue[];
        for(int i=1; i<split.length; i++)
        {
            //logger.info(split[i]);
            entryValue = split[i].split(INDEXVALUESEP);
            //logger.info(entryValue[0]+" "+entryValue[1]);
            v.put(new Integer(entryValue[0]), new Double(entryValue[1]));
        }
        return v;
    }

    /** Implements the similarity function per se
     *
     * @param a String representation of the first vector
     * @param b String representation of the second vector
     * @return Similarity of the vectors described by a and b
     */
    @Override
    public float getSimilarity(String a, String b) {        
        HashMap<Integer, Double> v1 = getEntries(a);
        HashMap<Integer, Double> v2 = getEntries(b);

        double intersection = 0;
        double union = 0;

        //compute intersection and union
        for(Integer entry : v1.keySet())
        {
            if(v2.containsKey(entry))
                intersection = intersection + v1.get(entry).doubleValue();
            union = union + v1.get(entry).doubleValue();
        }

        for(Integer entry : v2.keySet())
        {
            union = union + v2.get(entry).doubleValue();
        }

        union = union - intersection;
        if(union == 0 || intersection == 0) return 0.0f;
        return (float)(union/intersection);
    }

    @Override
    public float getUnNormalisedSimilarity(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
