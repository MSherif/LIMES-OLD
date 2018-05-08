/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.cache;
import de.uni_leipzig.simba.data.Instance;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Interface for data storage, i.e., caches.
 *
 * @author ngonga
 */
public interface Cache {

    public Instance getNextInstance();

    public ArrayList<Instance> getAllInstances();

    public ArrayList<String> getAllUris();

    public void addInstance(Instance i);

    public void addTriple(String s, String p, String o);

    public boolean containsInstance(Instance i);

    public boolean containsUri(String uri);

    public Instance getInstance(String uri);

    public void resetIterator();

    public int size();

    public Cache getSample(int size);

//    public Cache processData(String processingChain);
    /**
     * Method to processData according to specific preprocessing steps.
     * @param propertyProcess Map maps propertyNames to preprocessing functions.
     * @return
     */
    public Cache processData(Map<String,String> propertyProcess);
    
    /**
     * Method to process data of a property into a new property with specific preprocessing.
     * @param sourcePropertyName Name of the property to process.
     * @param targetPropertyName Name of the new property to process data into.
     * @param processingChain Preprocessing Expression.
     * @return
     */
    public Cache addProperty(String sourcePropertyName, String targetPropertyName, String processingChain);
    public void replaceInstance(String uri, Instance a);
    public Set<String> getAllProperties();
    
    
   /**
    * Basic method to create a JENA Model out of a cache.
    * Restriction 1: Assumes all objects are literal values. Thus, resource URIs are represented as Strings.
    * Restriction 2: Adds a rdf:Type statement for all instances.
    * @param baseURI Base URI of properties, could be empty.
    * @param IDbaseURI Base URI for id of resources: URI(instance) := IDbaseURI+instance.getID(). Could be empty.
    * @param rdfType rdf:Type of the instances.
    * @return JENA RDF Model
    */
    public Model parseCSVtoRDFModel(String baseURI, String IDbaseURI, String rdfType);
}
