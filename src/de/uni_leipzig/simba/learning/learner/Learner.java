/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.learning.learner;



/**
 *
 * @author ngonga
 */
public interface Learner {
    public boolean computeNextConfig(int n);
    public Configuration getCurrentConfig();
    public double getPrecision();
    public double getRecall();
}
