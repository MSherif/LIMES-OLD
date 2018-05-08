/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.histogram;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.vocabulary.RDFS;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.util.Utils;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class DumpBasedGenerator implements DataGenerator {

    List<String> data;
    Logger logger = Logger.getLogger("LIMES");
    double mean = 0d;
    double stdDev = 0d;
    /**
     * Generator based on data dump from the cloud
     *
     * @param dump
     */
    public DumpBasedGenerator(String dump) {
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFReader r = model.getReader("N3");
            InputStream in = new FileInputStream(dump);
            r.read(model, in, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = readLabels(model);
    }

    /**
     * Fetches all labels from a dump
     *
     * @param model Jena model that contains the labels
     * @return List of labels (note, not a set to keep duplicates and allow for
     * the probability of finding duplicates being higher)
     */
    public List<String> readLabels(Model model) {
        List<String> labels = new ArrayList<String>();
        try {
            String query = "SELECT ?s WHERE { ?x <" + RDFS.label.getURI() + "> ?s }";
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                //System.out.println(soln.get("s"));
                labels.add(soln.get("s").asNode().getLiteral().getLexicalForm());
            }
        } catch (Exception e) {
            logger.warn("Error while processing classes");
        }
      return labels;
    }

    /** Generates a cache that contains all the data required for runtime estimation
     * 
     * @param size Size of the cache to generate
     * @return A memory cache that contains all the data required
     */
    public Cache generateData(int size) {
        Cache c = new MemoryCache();
        List<Double> lengths = new ArrayList<Double>();
        if (!data.isEmpty()) {
            while (c.size() < size) {
                int index = (int) Math.floor(Math.random() * (data.size() - 1));
                lengths.add(new Double(data.get(index).length()));
                c.addTriple(data.get(index), DataGenerator.LABEL, data.get(index));
            }
        }
        stdDev = Utils.getStandardDeviation(lengths);
        mean = Utils.getMean(lengths);
        
        return c;
    }

    public String getName()
    {
        return "dumpBased";
    }
    public static void main(String args[]) {
        DumpBasedGenerator dbg = new DumpBasedGenerator("E:/Work/Papers/Eigene/2012/ISWC_HELIOS/Data/labels_en_uris_fr.nt");
        long memory = Runtime.getRuntime().totalMemory();        
        memory = Runtime.getRuntime().totalMemory() - memory;
        System.out.println(dbg.generateData(10));
        System.out.println(memory/(1024*1024));
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return stdDev;
    }
}
