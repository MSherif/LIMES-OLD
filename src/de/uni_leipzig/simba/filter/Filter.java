/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.filter;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public interface Filter {
    public Mapping filter(Mapping map, String condition, double threshold, Cache source, Cache target, String sourceVar, String targetVar);
    public Mapping filter(Mapping map, double threshold);
    /** Filter for linear combinations when operation is set to "add",
     * given the expression a*sim1 + b*sim2 >= t or multiplication given the
     * expression (a*sim1)*(b*sim2) >= t, which is not likely to be used
     * 
     * @param map1 Map bearing the results of sim1 >= (t-b)/a for add, sim1 >= t/(a*b) for mult
     * @param map2 Map bearing the results of sim2 >= (t-a)/b for add, sim2 >= t/(a*b) for mult
     * @param coef1 Value of a
     * @param coef2 Value of b
     * @param threshold Value of t
     * @return Mapping that satisfies a*sim1 + b*sim2 >= t for add, (a*sim1)*(b*sim2) >= t for mult
     */
    public Mapping filter(Mapping map1, Mapping map2, double coef1, double coef2, 
            double threshold, String operation);
}
