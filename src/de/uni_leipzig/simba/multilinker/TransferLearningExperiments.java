/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.multilinker;

import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public class TransferLearningExperiments {
    public static void main(String args[])
    {
        Mapping m = MultiInterlinker.getDeterministicUnsupervisedMappings("E:/Work/Java/TransferLearning/finalSpecs/dbpedia-linkedgeodata-university/spec_learned.xml", "x/dbpedia.x", "x/openei.x");
        System.out.println(m);
    }
}
