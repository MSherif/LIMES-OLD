/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.dofin.algorithm;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.dofin.svm.SvmChecker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class Dofin {

    static Logger logger = Logger.getLogger("LIMES");
    /** 
     * Runs the default dofin approach. Does not scale for large numbers of properties
     * @param mappings Maps a property label to the correspoding mapping
     */
    public static Set<Set<String>> run(HashMap<String, Mapping> mappings) {
        // result to be returned
        Set<Set<String>> result = new HashSet<Set<String>>();
        // set of properties
        Set<String> properties = mappings.keySet();
        // powerset over the set of properties
        Set<Set<String>> powerSet = powerSet(properties);
        //mappings used for checking discriminativeness
        Set<Mapping> data;
        for (int i = 1; i <= properties.size(); i++) {
            // get set of subsets of size i
            Set<Set<String>> pI = getSubsets(powerSet, i);
            //for each getSubsets
            for (Set<String> p : pI) {
                // get the corresponding mappings
                data = new HashSet<Mapping>();
                for (String name : p) {
                    data.add(mappings.get(name));
                }
                // if the space is discriminative
                if (discriminative(data)) {
                    result.add(p);
                    //reduce the powerset
                    powerSet = reduce(powerSet, p);
                }
                if (powerSet.isEmpty()) {
                    return result;
                }
            }
        }
        return result;
    }

    /** 
     * Runs the default dofin approach. Does not scale for large numbers of properties
     * @param mappings Maps a property label to the correspoding mapping
     */
    public static Set<Set<String>> runScalable(HashMap<String, Mapping> mappings, ArrayList<String> uris) {
        // result to be returned
        Set<Set<String>> result = new HashSet<Set<String>>();
        // set of properties
        Set<String> properties = mappings.keySet();
        // powerset over the set of properties
        Set<Set<String>> powerSet = generateInitialSetofSets(properties);
        //mappings used for checking discriminativeness
        Set<Mapping> data;
        for (int i = 1; i <= properties.size(); i++) {
            logger.info("Processing level "+i);
            logger.info("Powerset is currently "+powerSet);
            // get set of subsets of size i
            Set<Set<String>> pI = getSubsets(powerSet, i);
            //for each getSubsets
            for (Set<String> propertySet : pI) {
                // get the corresponding mappings
                data = new HashSet<Mapping>();
                for (String property : propertySet) {
                    data.add(mappings.get(property));
                }
                // if the space is discriminative
                logger.info("Running the SVM for "+propertySet);
                if (discriminative(data, uris)) {
                    logger.info(propertySet+" is discriminative ...");
                    result.add(propertySet);
                    //reduce the powerset
                    powerSet = reduce(powerSet, propertySet);
                }
                if (powerSet.isEmpty()) {
                    return result;
                }
                powerSet = generateNextLevel(powerSet, i+1);
            }
        }
        return result;
    }

    /** Checks whether set s1 contains set s2. Tested.
     * 
     * @param s1 Supposedly Larger set
     * @param s2 Smaller set
     * @return true if s2 is a getSubsets of s1
     */
    public static boolean contains(Set<String> s1, Set<String> s2) {
        for (String s : s2) {
            if (!s1.contains(s)) {
                return false;
            }
        }
        return true;
    }

    /** Implements powerset reduction. Tested.
     * 
     * @param set Subset of a powerset
     * @param properties Set of properties 
     * @return Reduced powerset
     */
    public static Set<Set<String>> reduce(Set<Set<String>> set, Set<String> properties) {
        Set<Set<String>> result = new HashSet<Set<String>>();
        for (Set<String> r : set) {
            if (!contains(r, properties)) {
                result.add(r);
            }
        }
        return result;
    }

    /** Checks whether a mapping is discriminative
     * 
     * @param data Set of mappings
     * @return True if set is discriminative, i.e., if there is a classifier 
     * such that all entities can be mapped exclusively to themselves based on
     * this data
     */
    public static boolean discriminative(Set<Mapping> data, ArrayList<String> uris) {        
        return (SvmChecker.getDiscriminativeness(data, uris) >= 1);        
    }
    public static boolean discriminative(Set<Mapping> data) {        
        return (SvmChecker.getDiscriminativeness(data) >= 1);        
    }

    /** Computes the set of subsets of powerSet that are of size size
     * 
     * @param <T> Type 
     * @param powerSet Current powerset
     * @param size Size of entries of result
     * @return Subsets of size size
     */
    public static <T> Set<Set<T>> getSubsets(Set<Set<T>> powerSet, int size) {
        Set<Set<T>> result = new HashSet<Set<T>>();
        for (Set<T> set : powerSet) {
            if (set.size() == size) {
                result.add(set);
            }
        }
        return result;
    }
    
    /* Compute the powerset of a given set
     * Tested.
     */
    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        logger.info("Starting with "+originalSet.size()+" unique properties ...");
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    public static void main(String args[]) {
        HashSet<String> s1 = new HashSet<String>();
        s1.add("test1");
        s1.add("test2");
        
        HashSet<String> s2 = new HashSet<String>();
        s2.add("test1");
        s2.add("test2");
        s2.add("test3");
        s2.add("test4");

        Set<Set<String>> p1 = generateInitialSetofSets(s2);
        p1 = generateNextLevel(p1, 2);
            System.out.println(p1);
        p1 = generateNextLevel(p1, 3);
            System.out.println(p1);
        p1 = generateNextLevel(p1, 4);
            System.out.println(p1);
        Set<Set<String>> p = powerSet(s2);
        System.out.println("P = "+p);
        System.out.println("Subsets of size 2 = "+getSubsets(p, 2));
        
        p = reduce(p, s1);
        System.out.println("Reduced P = "+p);
    }

    private static Set<Set<String>> generateNextLevel(Set<Set<String>> powerSet, int level)
    {
        Set<Set<String>> result = new HashSet<Set<String>>();
        for(Set<String> s1:powerSet)
        {
            for(Set<String> s2:powerSet)
            {
                if(!s1.equals(s2))
                {
                    Set<String> union = new HashSet<String>(s1);
                    union.addAll(s2);
                    if(union.size()==level)
                    result.add(union);
                }
            }
            result.add(s1);
        }                
        return result;        
    }
    private static Set<Set<String>> generateInitialSetofSets(Set<String> properties) {                
        Set<Set<String>> result = new HashSet<Set<String>>();
        for(String p: properties)
        {
            Set s = new HashSet<String>();
            s.add(p);
            result.add(s);
        }
        return result;
    }
}
