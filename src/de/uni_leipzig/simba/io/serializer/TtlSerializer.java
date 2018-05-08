/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.io.serializer;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.Serializer;
import de.uni_leipzig.simba.io.SerializerFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class TtlSerializer implements Serializer {

    PrintWriter writer;
    Logger logger = Logger.getLogger("LIMES");
    TreeSet<String> statements; //List of statements to be printed
    Map<String, String> prefixList;
    File folder = new File("");

    /**
     * Constructor
     *
     */
    public TtlSerializer() {
        statements = new TreeSet<String>();
        prefixList = new HashMap<String, String>();
    }

    /**
     * Adds a statement to the list of statements to be printed
     *
     * @param subject Subject of the triple
     * @param predicate Predicate of the triple
     * @param object Object of the triple
     * @param similarity Similarity of subject and object
     */
    public void addStatement(String subject, String predicate, String object, double similarity) {
        statements.add("<" + subject + "> " + predicate + " <" + object + "> .");
    }

    /*
     * Flushes the printer
     *
     */
    public void flush() {
        try {
            for (String s : statements) {
                writer.println(s);
            }
            statements = new TreeSet<String>();
        } catch (Exception e) {
            logger.warn("Error writing");
        }
    }

    /**
     * Write the content of the mapping including the expansion of the prefixes
     * to a file
     *
     * @param prefixes List of prefixes
     * @param m Mapping to be written
     * @param file Output file
     */
    public void writeToFile(Mapping m, String predicate, String file) {
        open(file);
        printPrefixes();
        statements = new TreeSet<String>();
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                writer.println("<" + s + "> " + predicate + " <" + t + "> .");
            }
        }
        close();
    }

    public void printPrefixes() {
        try {
            Iterator<String> iter = prefixList.keySet().iterator();
            String prefix;
            while (iter.hasNext()) {
                prefix = iter.next();
                writer.println("@prefix " + prefix + ": <" + prefixList.get(prefix) + "> .");
            }
        } catch (Exception e) {
            logger.warn("Error writing");
        }
    }

    public void printStatement(String subject, String predicate, String object, double similarity) {
        try {
            writer.println("<" + subject + "> " + predicate + " <" + object + "> .");
        } catch (Exception e) {
        	e.printStackTrace();	
        	System.err.println(e);
            logger.warn("Error writing");
        }
    }

    public boolean close() {
        try {
            if (statements.size() > 0) {
                for (String s : statements) {
                    writer.println(s);
                }
            }
            writer.close();
        } catch (Exception e) {
            logger.warn("Error closing PrintWriter");
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean open(String file) {
        try {
            // if no parent folder is given, then take that of the config that was set by the controller
        	if (!file.contains("/") && !file.contains("\\")) {
                String filePath = folder.getAbsolutePath()+File.separatorChar+file;
                writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
            } else {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            }
        } catch (Exception e) {
            logger.warn("Error creating PrintWriter");
            logger.warn(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getName() {
        return "TtlSerializer";
    }

    public void setPrefixes(Map<String, String> prefixes) {
        prefixList = prefixes;
    }

    public String getFileExtension() {
        return "ttl";
    }

    public static void main(String[] args) {
        Serializer s = SerializerFactory.getSerializer("ttl");
        Map<String, String> prefixes = new HashMap<String, String>();
        prefixes.put("owl", "http://owl/");
        prefixes.put("rdf", "http://rdf/");
        prefixes.put("dbr", "http://dbpedia.org/resource/");
        s.setPrefixes(prefixes);
        Mapping m = new Mapping();
        m.add("dbr:Lagos", "dbr:Lagoos", 1.0);
        m.add("dbr:Lagois", "dbr:Lagoos", 1.0);
        m.add("dbr:Lagosr", "dbr:Lagoos", 1.0);

        s.writeToFile(m, "owl:sameAs", System.getProperty("user.home")+"/"+"nttest.ttl");

        s.close();
    }

    @Override
    public File getFile(String fileName) {
        return new File(folder.getAbsolutePath() + File.separatorChar + fileName);
    }

    @Override
    public void setFolderPath(File f) {
        folder = f;
    }
}
