/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.evaluation;

import java.io.*;
import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.query.*;
import de.uni_leipzig.simba.io.*;
import de.uni_leipzig.simba.io.serializer.TtlSerializer;
import de.uni_leipzig.simba.metricfactory.*;
import de.uni_leipzig.simba.organizer.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.*;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Helps when running evaluations. Allows to configure runs with several
 * similarity thresholds.
 * @author ngonga
 */
public class Eval {

    public static void runTest(String configFile, String[] organizerList, double[] thresholdList, String output) {
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output)));
            writer.println("Organizer\tComparisons\tRuntime\tThreshold");

            long startTime = System.currentTimeMillis();
            //0. configure logger
        Logger logger = Logger.getLogger("LIMES");
        PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %c: %m%n");
        try {
            FileAppender fileAppender = new FileAppender(layout, configFile.replaceAll(".xml", "")+"."+System.currentTimeMillis()+".log", false);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }
        logger.setLevel(Level.DEBUG);

            //1. Read configFile
            ConfigReader cr = new ConfigReader();
            cr.validateAndRead(configFile);

            //2. Fill caches using the query module
            //2.1 First sourceInfo
            SparqlQueryModule sourceQm = new SparqlQueryModule(cr.getSourceInfo());
            MemoryCache source = new MemoryCache();
            sourceQm.fillCache(source);

            //2.2 Then targetInfo
            SparqlQueryModule targetQm = new SparqlQueryModule(cr.getTargetInfo());
            MemoryCache target = new MemoryCache();
            targetQm.fillCache(target);

            //3. Read metrics
            SimpleMetricFactory mf = new SimpleMetricFactory(cr.sourceInfo.var, cr.targetInfo.var);
            mf.setExpression(cr.metricExpression);
            logger.info("Comparisons will be carried out by using " + cr.metricExpression);

            SimpleMetricFactory organizerMf = new SimpleMetricFactory(cr.sourceInfo.var, cr.targetInfo.var);
            String var1 = cr.sourceInfo.var.replaceAll("\\?", "");
            String var2 = cr.targetInfo.var.replaceAll("\\?", "");
            organizerMf.setExpression(mf.foldExpression(cr.metricExpression, var2, var1));
            logger.info("Organizing will be carried out by using " + mf.foldExpression(cr.metricExpression, var2, var1));

            for (int ol = 0; ol < organizerList.length; ol++) {
                for (int tl = 0; tl < thresholdList.length; tl++) {
                //4. Get right organizer
                Organizer organizer;
                if (organizerList[ol].toLowerCase().startsWith("density")) {
                    organizer = new DensityBasedOrganizer();
                } else if (organizerList[ol].toLowerCase().startsWith("brute")) {
                    organizer = new BruteForceOrganizer();
                } else {
                    organizer = new LimesOrganizer();
                }
                
                    // set threshold
                    cr.verificationThreshold = thresholdList[tl];
                    cr.acceptanceThreshold = 1;

                    if (cr.exemplars < 2) {
                        organizer.computeExemplars(target, organizerMf);
                    } else {
                        organizer.computeExemplars(target, organizerMf, cr.exemplars);
                    }

                    ArrayList<String> uris = source.getAllUris();

                    //5. Compare
                    //5.1 Get Writer ready
                    TtlSerializer accepted = new TtlSerializer();
                    TtlSerializer toReview = new TtlSerializer();

                    accepted.open(organizer.getName() + "_" + thresholdList[tl] + "_" + cr.acceptanceFile);
                    accepted.setPrefixes(cr.prefixes);
                    accepted.printPrefixes();
                    toReview.open(organizer.getName() + "_" + thresholdList[tl] + "_" + cr.verificationFile);
                    toReview.setPrefixes(cr.prefixes);
                    toReview.printPrefixes();
                    
                    //5.2 Now write results
                    HashMap<String, Double> results;
                    Iterator<String> resultIterator;
                    String s;
                    for (int i = 0; i < uris.size(); i++) {
                        results = organizer.getSimilarInstances(source.getInstance(uris.get(i)), cr.verificationThreshold, mf);
                        //logger.info("Getting results for "+sourceInfo.getInstance(uris.get(i)));
                        resultIterator = results.keySet().iterator();
                        while (resultIterator.hasNext()) {
                            s = resultIterator.next();
                            if (results.get(s) >= cr.acceptanceThreshold) {
                                accepted.printStatement(uris.get(i), cr.acceptanceRelation, s, results.get(s));
                            } else if (results.get(s) >= cr.verificationThreshold) {
                                toReview.printStatement(uris.get(i), cr.acceptanceRelation, s, results.get(s));
                            }
                        }
                    }



                    //5.3 Close writers
                    accepted.close();
                    toReview.close();

                    //6. Write some stats
                    logger.info("Required " + organizer.getComparisons() + " comparisons overall.\n");
                    logger.info("Comparisons were carried out in " + organizer.getComparisonTime() + " seconds overall.\n");
                    logger.info("Required " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds overall.");

                    writer.println(organizer.getName()+"\t"+organizer.getComparisons()
                            +"\t"+organizer.getComparisonTime()+"\t"+thresholdList[tl]);
                    //7. DONE
                    logger.info("Done.");
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {   
        double[] thresholdList = new double[5];
        //thresholds
        thresholdList[0] = 0.75;
        thresholdList[1] = 0.80;
        thresholdList[2] = 0.85;
        thresholdList[3] = 0.90;
        thresholdList[4] = 0.95;

        String[] organizerList = new String[2];
        organizerList[0] = "limes";
        organizerList[1] = "densitybased";


        if(new File(args[0]).exists())
            runTest(args[0], organizerList, thresholdList, args[1]);
        else System.exit(1);
    }
}
