/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.learner;

import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.learning.oracle.oracle.OracleFactory;
import de.uni_leipzig.simba.learning.query.DefaultClassMapper;
import de.uni_leipzig.simba.learning.query.DefaultPropertyMapper;
import de.uni_leipzig.simba.learning.stablematching.HospitalResidents;
import de.uni_leipzig.simba.util.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author ngonga
 */
public class LearningController {

    static int CLASS_LIMIT = 100;
    static int PROP_LIMIT = 500;
    static int NUMBER_INQUIRIES = 4;
    static int NUMBER_ITERATIONS = 30;
    static double LR = 0.01;
    String sourceEndpoint;
    String targetEndpoint;
    String sourceNamespace;
    String targetNamespace;
    SimpleOracle oracle;
    static Logger logger = Logger.getLogger("LIMES");
    Clock clock;

    /** Constructor
     *
     * @param endpoint1 Endpoint for source
     * @param endpoint2 Endpoint for target
     * @param ns1 Namespace (later id) for source
     * @param ns2 Namespace (later id) for target
     */
    public LearningController(String endpoint1, String endpoint2,
            String ns1, String ns2, String oracleData, String oracleDataType) {
        sourceEndpoint = endpoint1;
        targetEndpoint = endpoint2;
        sourceNamespace = ns1;
        targetNamespace = ns2;
        oracle = (SimpleOracle) OracleFactory.getOracle(oracleData, oracleDataType, "simple");
        String configFile = oracleData + ".log";
        clock = new Clock();
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            FileAppender fileAppender = new FileAppender(layout, configFile.replaceAll(".xml", "") + ".log", false);
            fileAppender.setLayout(layout);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }
    }

    /** Extracts pair with highest match from Mapping m
     *
     * @param m Mapping from which the matches are to be extracted
     * @return Best matching pair
     */
    public String[] getBestMatch(Mapping m) {
        double maxSim = 0, sim = 0;
        String s = null, t = null;
        for (String a : m.map.keySet()) {
            for (String b : m.map.get(a).keySet()) {
                if (!a.equals("http://www.w3.org/2002/07/owl#Thing")
                        && !b.equals("http://www.w3.org/2002/07/owl#Thing")) {
                    sim = m.getSimilarity(a, b);
                    if (sim > maxSim) {
                        maxSim = sim;
                        s = a;
                        t = b;
                    }
                }
            }
        }

        String[] match;
        if (s == null) {
            return null;
        } else {
            match = new String[2];
            match[0] = s;
            match[1] = t;
        }
        return match;
    }

    /** Basically runs the controller
     *
     */
    public void run() {
        //Mappers for classes and properties
        Clock c = new Clock();
        logger.info(c.durationSinceClick());
        DefaultClassMapper cm = new DefaultClassMapper();
        cm.LIMIT = CLASS_LIMIT;
        DefaultPropertyMapper pm = new DefaultPropertyMapper();
        pm.LIMIT = PROP_LIMIT;
        logger.info("Generating initial configuration");

        Mapping properties = new Mapping(), classes = new Mapping();
        Mapping mappingClasses = cm.getMappingClasses(sourceEndpoint, targetEndpoint, sourceNamespace, targetNamespace);
        HospitalResidents hr = new HospitalResidents();
        String[] match;

        int counter=0;
        logger.info("Classes:\n" + classes);
        do {
            counter++;
            logger.info("\n******\nAttempt "+counter+" ... "); 
            //1. First get classes
            if (mappingClasses.size() == 0) {
                logger.info("No mapping classes found. Exiting.");
                System.exit(1);
            }
            classes = hr.getMatching(mappingClasses);

            //2. Get best class. This will be decided by the user live
            logger.info("Classes:\n" + classes);
            match = getBestMatch(classes);
            //3. Get properties
            //////////////
            // FOR TESTS
            //        match[0] = "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/targets";
            // match[1] = "http://dbpedia.org/ontology/Drug";
            /////////////
            if (match != null) {
                logger.info("Best match : " + match[0] + " -> " + match[1]);
                // change this to getMappingProperties to learn using all mapping properties
                properties = pm.getPropertyMapping(sourceEndpoint, targetEndpoint,
                        match[0], match[1]);
               // System.out.println(">>"+properties);
                if (properties.size() == 0) {
                    logger.info(match[0] + " to " + match[1] + " did not have matching properties.");
                    logger.info("Trying the next option.");
                    //System.exit(1);
                    mappingClasses.map.get(match[0]).remove(match[1]);
                }
            }
        } while (properties.size() == 0);

        logger.info(
                "Properties:\n" + properties);
        //3.1. Generate initial configuration
        LinearConfiguration lc = new LinearConfiguration(properties,
                LinearConfiguration.generateStringTyping(properties));
        logger.info(
                "Initial configuration: " + lc.getExpression() + " >= " + lc.threshold);

        //3.2 Now create source config
        KBInfo sourceInfo = new KBInfo();
        sourceInfo.endpoint = sourceEndpoint;
        sourceInfo.id = sourceNamespace;


        if (!sourceNamespace.equals("dbpedia") && 
                sourceEndpoint.contains("http://lgd.aksw.org:5678")) {
            sourceInfo.graph = "http://www.instancematching.org/oaei/di/" + sourceNamespace + "/";
        }
        sourceInfo.pageSize = 100;
        sourceInfo.prefixes = new HashMap<String, String>();
        ArrayList<String> sourceProperties = new ArrayList<String>();
        ArrayList<String> sourceRestrictions = new ArrayList<String>();
        for (String prop : properties.map.keySet()) {
            sourceProperties.add(prop);
        }
        sourceRestrictions.add(
                "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"
                + match[0] + ">");

        sourceInfo.properties = sourceProperties;
        sourceInfo.restrictions = sourceRestrictions;
        sourceInfo.type = "sparql";
        sourceInfo.var = "?x";
        //3.3 Now create target config
        KBInfo targetInfo = new KBInfo();
        targetInfo.endpoint = targetEndpoint;
        targetInfo.id = targetNamespace;
        if (!targetNamespace.equals("dbpedia") && 
                targetEndpoint.contains("http://lgd.aksw.org:5678")) {
            targetInfo.graph = "http://www.instancematching.org/oaei/di/" + targetNamespace + "/";
        }
        targetInfo.pageSize = 100;
        targetInfo.prefixes = new HashMap<String, String>();
        ArrayList<String> targetProperties = new ArrayList<String>();
        ArrayList<String> targetRestrictions = new ArrayList<String>();
        for (String prop : sourceProperties) {
            for (String targetProp : properties.map.get(prop).keySet()) {
                if (!targetProperties.contains(targetProp)) {
                    targetProperties.add(targetProp);
                }
            }
        }

        targetRestrictions.add(
                "?y <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"
                + match[1] + ">");
        targetInfo.properties = targetProperties;
        targetInfo.restrictions = targetRestrictions;
        targetInfo.type = "sparql";
        targetInfo.var = "?y";

        logger.info("Source Info = " + sourceInfo);
        logger.info("Target Info = " + targetInfo);
        //3.4 get caches
        HybridCache sourceCache = HybridCache.getNoPrefixData(sourceInfo);
        HybridCache targetCache = HybridCache.getNoPrefixData(targetInfo);

        //3.5 Create learner. Important: we might need to experiment with
        // different values of a and b
        clock.durationSinceClick();

        
        PerceptronLearner learner = new PerceptronLearner(sourceInfo,
        targetInfo, sourceCache, targetCache, oracle, properties,
        LinearConfiguration.generateStringTyping(properties), LR, LR); 

        
        
  /*
        BooleanClassifierLearner learner = new BooleanClassifierLearner(sourceInfo,
                targetInfo, sourceCache, targetCache, oracle, properties,
                LinearConfiguration.generateStringTyping(properties), LR, LR); */
        logger.info("Preparation time is "+c.durationSinceClick()+" ms"); 
//        logger.info("\n\nInitial config = "
//                + learner.config.getExpression() + " >= "
//                + learner.config.threshold);

        logger.info("\n\nInitial config = "
                + learner.config.getExpression());

        boolean more = true;
        String results = "Iteration\tInquiries\tPrecision\tRecall\tF-Score\tRuntime\n";
        results = results +"0\t0\t";
        HashMap<String, Double> prf;
        prf = learner.getPRF();

        results = results + prf.get("precision") + "\t";
        results = results + prf.get("recall") + "\t";
        results = results + prf.get("fscore") + "\t";
        results = results + clock.durationSinceClick() + "\n";
        for (int i = 0; i < NUMBER_ITERATIONS; i++) {
            results = results + (i + 1) + "\t" + ((i+1)*NUMBER_INQUIRIES) + "\t";
            clock.durationSinceClick();
            logger.info("\n\n========= Iteration Nr. " + (i + 1));
            more = learner.computeNextConfig(NUMBER_INQUIRIES);

//            logger.info("Iteration " + i + " led to " + learner.config.getExpression() + " >= "
//                    + learner.config.threshold);

            logger.info("Iteration " + i + " led to " + learner.config.getExpression());

            prf = learner.getPRF();
            results = results + prf.get("precision") + "\t";
            results = results + prf.get("recall") + "\t";
            results = results + prf.get("fscore") + "\t";
            results = results + clock.durationSinceClick() + "\n";
            logger.info("Computation lasted " + clock.durationSinceClick() + " ms.");


            //logger.info("Precision = "+learner.getPrecision());

//            if (!more) {
//                logger.info("Done in " + (i + 1) + " iterations.");
//                break;
//            }
        }
        logger.info(learner.returnFinalResults());
        logger.info("Results: \n\n" + results);

    }

    public static void main(String args[]) {
        String namespace = "diseasome";
// DISEASOME-SIDER     
        LearningController c = new LearningController("http://lgd.aksw.org:5678/sparql",
                "http://lgd.aksw.org:5678/sparql",
                //"http://139.18.2.96:8910/sparql",
                "diseasome", "sider", "D:/Work/Data/Linking/Final Reference Data/diseasome_sider_reference.nt",
                "CSV");
//        
// DRUGBANK-SIDER
//                LearningController c = new LearningController(//"http://lgd.aksw.org:5678/sparql",
//                "http://lgd.aksw.org:5678/sparql",
//                "http://lgd.aksw.org:5678/sparql",
//                "drugbank", "sider", "D:/Work/Data/Linking/Final Reference Data/drugbank_sider_accepted.nt",
//                "CSV");

//        LearningController c = new LearningController(//"http://lgd.aksw.org:5678/sparql",
//                "http://139.18.2.96:8910/sparql",
//                "http://lgd.aksw.org:5678/sparql",
//                "dbpedia", "diseasome", "D:/Work/Data/Linking/Final Reference Data/diseasome_dbpedia_reference.nt",
//                "CSV");

        
//                LearningController c = new LearningController("http://www4.wiwiss.fu-berlin.de/diseasome/sparql",
//                "http://www4.wiwiss.fu-berlin.de/diseasome/sparql",
//                "drugbank", "sider", "D:/Work/Data/Linking/Final Reference Data/diseasome_sider_reference.nt",
//                "CSV");
        c.run();
    }
}
