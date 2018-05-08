/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.data;
import java.util.TreeSet;
/**
 * Contain contraints on the input data. Especially used for generating SPARQL
 * queries and restrict the data in source and target.
 * @author ngonga
 */
public class Constraints {
    TreeSet<String> constraints;
    public Constraints(String restriction)
    {
        constraints = new TreeSet<String>();
        constraints.add(restriction);
    }

    public void addConstraint(String restriction)
    {
        constraints.add(restriction);
    }    
}
