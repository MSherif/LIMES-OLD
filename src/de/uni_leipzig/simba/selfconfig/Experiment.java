/* STILL TODO
 * 1- Fix choice of measures in initial classifiers. Idea is to pick the measure that leads to
 * the best pseudo f-score on each dimension of the problem
 * 2-Show that our pseudu-f-measure correlates well with the real f-measure
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.query.CsvQueryModule;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;



/**
 *
 * @author ngonga
 */
public class Experiment {
    static Logger logger = Logger.getLogger("LIMES");
    static String SEPARATOR = "\t";
    static String CSVSEPARATOR = ",";

    
    /**
     * Read a file into the cache
     *
     * @param file Input fille
     * @return Cache containing the data from the file
     */
    public static Cache readFile(String file) {
        Cache c = new HybridCache();
        String s = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //read properties;
            s = reader.readLine();
      
            String properties[] = s.split(CSVSEPARATOR);
            
            String split[];
            int size = 0;
            s = reader.readLine();
            while (s != null && size < 250000) {
                s = s.toLowerCase();
                s = StringEscapeUtils.unescapeHtml(s);
                
                split = s.split(CSVSEPARATOR);
                
                for (int i = 1; i < properties.length; i++) {
                    try {
                
                    	c.addTriple(split[0], properties[i], split[i]);
                        size++;
                    } catch (Exception e) {
                        e.printStackTrace();
                        
                    }
                }
                s = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
//            logger.info(s);
        }
        return c;
    }

    public static Mapping readOAEIMapping(String file) {
        Mapping m = new Mapping();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //read properties;
            String s = reader.readLine();
            String e1 = "", e2;
            while (s != null) {
                String[] split = s.split(" ");
                {
                    if (s.contains("entity1")) {
                        e1 = s.substring(s.indexOf("=") + 2, s.lastIndexOf(">") - 2);
                    } else if (s.contains("entity2")) {
                        e2 = s.substring(s.indexOf("=") + 2, s.lastIndexOf(">") - 2);
                        m.add(e1, e2, 1.0);
                    }
                    s = reader.readLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }
   
    
    public static Cache readRDFData(String filePath, ConfigReader cR) {
        

        Map<String, String> prefixes = new HashMap<String, String>();
        for(Entry<String, String> entry : cR.prefixes.entrySet()){
                prefixes.put(entry.getValue(), entry.getKey());
        }
        System.out.println(prefixes);
        Cache c = new MemoryCache();
        File file = new File(filePath);
        Model model=ModelFactory.createDefaultModel();
        System.out.println("Reading file..."+file+" exists?"+file.exists());
        java.io.InputStream in = FileManager.get().open( file.getAbsolutePath() );
        
        System.out.println("reading..."+in+" ... ");
        
        if(filePath.contains(".ttl")){
        	System.out.println(file);
            logger.info("Opening TURTLE file");
            model.read(in, "","TURTLE");
        }
        
        StmtIterator triples = model.listStatements();
        while(triples.hasNext()){
	        Statement triple = triples.next();
	        String subject = triple.getSubject().toString();
	
	        String predicate = null;
	        String p = triple.getPredicate().toString();
	        if(p.contains("#")){
	                String temp = prefixes.get(p.split("#")[0]+"#");
	                predicate = temp+":"+p.split("#")[1];
	
	        }else if(p.contains("/")){
	                String t = p.substring(p.lastIndexOf("/")+1);
	                String temp = prefixes.get(p.replace(t,""));
	                predicate = temp+":"+t;
	
	        }
	
	        //System.out.println(triple.getPredicate().toString());
	        String object = triple.getObject().toString();
	
	        c.addTriple(subject,predicate,object);
	
        }
        return c;
    }
    
    public static Cache readOAEIFile(String file, String token) {
        Cache c = new MemoryCache();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //read properties;
            String s = reader.readLine();
            while (s != null) {
                String[] split = s.split(" ");
                String value = split[2];
                if (split.length > 3) {
                    for (int i = 3; i < split.length; i++) {
                        value = value + " " + split[i];
                    }
                }
                if (split[0].contains(token) && !split[1].contains("#type")) {
                    c.addTriple(split[0].substring(1, split[0].length() - 1),
                            split[1].substring(1, split[1].length() - 1),
                            value.substring(1, value.length() - 3).toLowerCase());
                }
                s = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        logger.info(c);
        c.resetIterator();
        return c;
    }

    public static Mapping readReference(String file) {
        Mapping m = new Mapping();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //read properties;
            String s = reader.readLine();
            String properties[] = s.split(CSVSEPARATOR);
            String split[];
            s = reader.readLine();
            while (s != null) {
                s = s.toLowerCase();
                s = StringEscapeUtils.unescapeHtml(s);
                split = s.split(CSVSEPARATOR);
                m.add(split[0], split[1], 1.0);
                s = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }

    // computes precision, recall and f-measure in that order
    public ArrayList<Double> runExperiment(String source, String target, String reference,
            double coverage, double beta, boolean cleanUp) {
        Cache s = readFile(source);
        Cache t = readFile(target);
        Mapping r = readReference(reference);
        BooleanSelfConfigurator bsc = new BooleanSelfConfigurator(s, t, coverage, beta);
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
        cp = bsc.learnClassifer(cp);
        Mapping m = bsc.getMapping(cp);
        PRFCalculator prf = new PRFCalculator();
        if (cleanUp) {
            m = bsc.getBestOneToOneMapping(m);
        }
        ArrayList<Double> result = new ArrayList<Double>();
        result.add(prf.precision(m, r));
        result.add(prf.recall(m, r));
        result.add(prf.fScore(m, r));
        return result;
    }

    public static void test2() {
        Cache source = readOAEIFile("E:/Work/Data/OAEI2010/restaurant1.nt", "-Restaurant");
        toCsvFile(source, "E:/Work/Data/OAEI2010/restaurant1.csv");
        Cache target = readOAEIFile("E:/Work/Data/OAEI2010/restaurant2.nt", "-Restaurant");
        toCsvFile(target, "E:/Work/Data/OAEI2010/restaurant2.csv");

        Mapping reference = readOAEIMapping("E:/Work/Data/OAEI2010/restaurant1_restaurant2_goldstandard.rdf");
        toCsvFile(reference, "E:/Work/Data/OAEI2010/restaurant-reference.csv");

    }

    public static void test3() {
        KBInfo S = new KBInfo();
        S.endpoint = "E:/Work/Data/EAGLE/dbpedia-linkedmdb/source.csv";
        KBInfo T = new KBInfo();
        T.endpoint = "E:/Work/Data/EAGLE/dbpedia-linkedmdb/target.csv";
        CsvQueryModule qm = new CsvQueryModule(S);
        qm.setSeparation("\t");
        Cache source = new HybridCache();
        qm.fillAllInCache(source);

        CsvQueryModule qm2 = new CsvQueryModule(T);
        Cache target = new HybridCache();
        qm2.setSeparation("\t");
        qm2.fillAllInCache(target);

        Mapping reference = Mapping.readFromCsvFile("E:/Work/Data/EAGLE/dbpedia-linkedmdb/reference.csv");
        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, 0.6, 1.0);
//        DisjunctiveMeshSelfConfigurator bsc = new DisjunctiveMeshSelfConfigurator(source, target, 0.6, 1.0);
        bsc.setMeasure("reference");

//        bsc.MIN_THRESHOLD = 0.2;
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
        ComplexClassifier cc = bsc.getZoomedHillTop(5, 10, cp);
        Mapping m2 = cc.mapping;
        //m2 = bsc.getMapping(cc.classifiers);
        Mapping m3 = bsc.getBestOneToOneMapping(m2);
        PRFCalculator prf = new PRFCalculator();        
        double p = prf.precision(m3, reference);
        double r = prf.recall(m3, reference);
        String output = p + "\t" + r + "\t" + (2 * p * r / (p + r)) + "\n";
        System.out.println(output);
    }

    public static void test() {
        long begin = System.currentTimeMillis();
//
        //OEAI 2010 person 1
//        Cache source = readOAEIFile("E:/Work/Data/OAEI2010/person11.nt", "-Person");
//        Cache target = readOAEIFile("E:/Work/Data/OAEI2010/person12.nt", "-Person");
//        Mapping reference = readOAEIMapping("E:/Work/Data/OAEI2010/dataset11_dataset12_goldstandard_person.xml");


//
//        List<SimpleClassifier> cp = new ArrayList<SimpleClassifier>();
//        cp.add(new SimpleClassifier("jaccard", 0.5, "http://www.okkam.org/ontology_person1.owl#surname",
//                "http://www.okkam.org/ontology_person2.owl#surname"));
//        cp.add(new SimpleClassifier("levenshtein", 0.5, "http://www.okkam.org/ontology_person1.owl#soc_sec_id",
//                "http://www.okkam.org/ontology_person2.owl#soc_sec_id"));
//        cp.add(new SimpleClassifier("levenshtein", 0.5, "http://www.okkam.org/ontology_person1.owl#phone_numer",
//                "http://www.okkam.org/ontology_person2.owl#phone_numer"));
////                cp.add(new SimpleClassifier("levenshtein", 0.5, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
////                        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")); 
//        cp.add(new SimpleClassifier("jaccard", 0.5, "http://www.okkam.org/ontology_person1.owl#has_address",
//                "http://www.okkam.org/ontology_person2.owl#has_address"));
//        cp.add(new SimpleClassifier("levenshtein", 0.5, "http://www.okkam.org/ontology_person1.owl#date_of_birth",
//                "http://www.okkam.org/ontology_person2.owl#date_of_birth"));
//        cp.add(new SimpleClassifier("jaccard", 0.5, "http://www.okkam.org/ontology_person1.owl#given_name",
//                "http://www.okkam.org/ontology_person2.owl#given_name"));
//        cp.add(new SimpleClassifier("levenshtein", 0.5, "http://www.okkam.org/ontology_person1.owl#age",
//                "http://www.okkam.org/ontology_person2.owl#age"));
//        //OEAI 2010 person 2
//        Cache source = readOAEIFile("E:/Work/Data/OAEI2010/person21.nt", "-Person");
//        Cache target = readOAEIFile("E:/Work/Data/OAEI2010/person22.nt", "-Person");
//        Mapping reference = readOAEIMapping("E:/Work/Data/OAEI2010/dataset21_dataset22_goldstandard_person.xml");

//        //OEAI 2010 restaurant
//        Cache source = readOAEIFile("E:/Work/Data/OAEI2010/restaurant1.nt", "-Restaurant");
//        Cache target = readOAEIFile("E:/Work/Data/OAEI2010/restaurant2.nt", "-Restaurant");
//        Mapping reference = readOAEIMapping("E:/Work/Data/OAEI2010/restaurant1_restaurant2_goldstandard_correct.rdf");
        //DBLP-Scholar
//        Cache source = readFile("E:/Work/Data/Datenbanken/DBLP-Scholar/DBLP.csv");
//        Cache target = readFile("E:/Work/Data/Datenbanken/DBLP-Scholar/Scholar.csv");
//        Mapping reference = readReference("E:/Work/Data/Datenbanken/DBLP-Scholar/DBLP-Scholar-matches.csv");

//        List<SimpleClassifier> cp = new ArrayList<SimpleClassifier>();
//        SimpleClassifier authors = new SimpleClassifier("jaccard", 0.5, "authors", "authors");
//        SimpleClassifier title = new SimpleClassifier("jaccard", 0.5, "title", "title");
//        SimpleClassifier venue = new SimpleClassifier("jaccard", 0.5, "venue", "venue");
//        //SimpleClassifier year = new SimpleClassifier("jaccard", 0.5, "year", "year");
//        cp.add(authors);
//        cp.add(title);
//        cp.add(venue);
        //cp.add(year);

////      //DBLP-ACM
        Cache source = readFile("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP2.csv");
        Cache target = readFile("Examples/GeneticEval/Datasets/DBLP-ACM/ACM.csv");
        Mapping reference = readReference("Examples/GeneticEval/Datasets/DBLP-ACM/DBLP-ACM_perfectMapping.csv");
System.out.println("scource="+source.size()+" target="+target.size()+ "ref= "+reference.size());
        LinearMeshSelfConfigurator bsc = new LinearMeshSelfConfigurator(source, target, 0.6, 1.0);        
//        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, 0.6, 1.0);
//        DisjunctiveMeshSelfConfigurator bsc = new DisjunctiveMeshSelfConfigurator(source, target, 0.6, 1.0);
        bsc.setMeasure("reference");

//        bsc.MIN_THRESHOLD = 0.2;
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();

        logger.info(cp);
        //System.exit(1);
        Mapping m = bsc.getMapping(cp);
        PRFCalculator prf = new PRFCalculator();
        Mapping m1 = bsc.getBestOneToOneMapping(m);
        long middle = System.currentTimeMillis();// 5 3
        String output = "Iteration\tPrecision\tRecall\tF-Measure\n";
        Mapping m2 = new Mapping();
        Mapping m3 = new Mapping();
        double p, r;

        for (int i = 1; i < 2; i++) {
            ComplexClassifier cc = bsc.getZoomedHillTop(5, 10, cp);
            m2 = cc.mapping;
            //m2 = bsc.getMapping(cc.classifiers);
            m3 = bsc.getBestOneToOneMapping(m2);
            p = prf.precision(m2, reference);
            r = prf.recall(m2, reference);
            output = output + i + "\t" + p + "\t" + r + "\t" + (2 * p * r / (p + r)) + "\n";
        }


        System.out.println(m.size() + " " + prf.fScore(m, reference));
        System.out.println(m1.size() + " " + prf.fScore(m1, reference));
        System.out.println(m2.size() + " " + prf.fScore(m2, reference));
        System.out.println(m3.size() + " " + prf.fScore(m3, reference));
        long end = System.currentTimeMillis();
        System.out.println("Experiment carried out in " + ((end - begin) / 1000.0) + " seconds");
        System.out.println("Property mapping took " + ((middle - begin) / 1000.0) + " seconds");
        System.out.println("Finding mapping took " + ((end - middle) / 1000.0) + " seconds");
    }

    /**
     * Converts cache into CSV file
     *
     * @param c Cache
     * @param properties List of properties to be read
     * @param outputFile Output file
     */
    public static void toCsvFile(Cache c, String outputFile) {
        Set<String> pSet = c.getAllInstances().get(0).getAllProperties();
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            writer.print("ID");
            for (String p : pSet) {
                writer.print(CSVSEPARATOR + p);
            }
            writer.print("\n");
            ArrayList<Instance> instances = c.getAllInstances();
            for (Instance instance : instances) {
                writer.print(instance.getUri());
                for (String p : pSet) {
                    writer.print(CSVSEPARATOR + instance.getProperty(p).first());
                }
                writer.print("\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void toCsvFile(Mapping m, String outputFile) {
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            writer.print("ID");
            for (String m1 : m.map.keySet()) {
                for (String m2 : m.map.get(m1).keySet()) {
                    writer.println(m1 + CSVSEPARATOR + m2);
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            FileAppender fileAppender = new FileAppender(layout, "E:/tmp/experiment.log", false);
            fileAppender.setLayout(layout);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }
//        Mapping m = readOAEIMapping("E:/Work/Data/OAEI2010/dataset11_dataset12_goldstandard_person.xml");
//        System.out.println(m);
        //System.out.println(c);
        test();
    }
}
