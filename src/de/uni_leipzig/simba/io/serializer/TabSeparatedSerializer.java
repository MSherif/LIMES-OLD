/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.io.serializer;

import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public class TabSeparatedSerializer extends NtSerializer{
    public String SEPARATOR = "\t";

    @Override
    public void addStatement(String subject, String predicate, String object, double similarity)
    {
        statements.add(subject + SEPARATOR + object + SEPARATOR + similarity);
    }

    @Override
   public void printStatement(String subject, String predicate, String object, double similarity) {
        try {
            writer.println(subject + SEPARATOR + object + SEPARATOR + similarity);
        } catch (Exception e) {
            logger.warn("Error writing");
        }
    }

    public String getName()
    {
        return "TabSeparatedSerializer";
    }
    /**
     * Gets a mapping and serializes it to a file in the N3 format. The method
     * assume that the class already knows all the prefixes used in the uris and
     * expands those.
     *
     * @param m Mapping to serialize
     * @param predicate Predicate to use while serializing
     * @param file File in which the mapping is to be serialized
     */
    public void writeToFile(Mapping m, String predicate, String file) {
        open(file);
     
        if (m.size() > 0) {
            //first get the prefix used in the subjects
            String source = m.map.keySet().iterator().next();
            String target = m.map.get(source).keySet().iterator().next();
            for (String s : m.map.keySet()) {
                for (String t : m.map.get(s).keySet()) {
                    writer.println("<" + s + ">\t<" + t + ">\t"+m.getSimilarity(s, t));
                }
            }
        }
        close();
    }
    
    public String getFileExtension() {
    	return "tsv";
    }
}