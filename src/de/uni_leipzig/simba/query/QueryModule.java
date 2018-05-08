/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.query;
import de.uni_leipzig.simba.cache.Cache;
/**
 * Interface for query modules. SPARQL query module implemented so far.
 * Query from file and databases will be implemented soon.
 * @author ngonga
 */
public interface QueryModule {
    public void fillCache(Cache c);
}
