/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.metricfactory;

import de.uni_leipzig.simba.data.Instance;

/**
 *
 * @author ngonga
 */
public class ComplexMetric {
    String metricExpression;
    public ComplexMetric(String expression)
    {
        metricExpression = expression;
    }

    /** Uses the expression given in to compute the similarity of two objects.
     * Uses the parser to decompose the metric expression into single blocks.
     * Then runs the similarity computation recursively and returns the results.
     * @param source
     * @param target
     * @param uri1
     * @param uri2
     * @return
     */
    public float getSimilarity(Instance a, Instance b)
    {
        return 1.0f;
    }
}
