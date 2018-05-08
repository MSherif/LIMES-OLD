/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.histogram;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.util.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class RandomNumberGenerator implements DataGenerator {

    int minimum, maximum;
    double mean = 0d;
    double stdDev = 0d;

    public RandomNumberGenerator(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Generates strings that are in (a-zA-Z)*
     *
     * @param size Size of the corpus that is to be generated
     * @return Corpus
     */
    public Cache generateData(int size) {
        Cache c = new MemoryCache();
        double number;
        List<Double> values = new ArrayList<Double>();
        while (c.size() < size) {
            number = minimum + Math.random() * (maximum - minimum);
            values.add(number);
            c.addTriple(number + "", DataGenerator.LABEL, number + "");
        }
        stdDev = Utils.getStandardDeviation(values);
        mean = Utils.getMean(values);

        return c;
    }

    public String getName() {
        return "randomNumber";
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return stdDev;
    }
}
