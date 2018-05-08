/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.controller;

import de.uni_leipzig.simba.genetics.evaluation.Tester;
import de.uni_leipzig.simba.io.SerializerFactory;
import java.io.File;
import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.query.*;
import de.uni_leipzig.simba.io.*;
import de.uni_leipzig.simba.metricfactory.*;
import de.uni_leipzig.simba.organizer.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.*;

/**
 * Runs the total LIMES pipeline
 * @author ngonga
 */
public class Controller {

    static Logger logger = Logger.getLogger("LIMES");

    public static void main(String args[]) {
    	if(args.length == 0) {
    		String configFile = "Examples/GeneticEval/dbpedia-linkedmdb.xml";
    		run(configFile);
    		ConfigReader cR = new ConfigReader();
    		cR.validateAndRead(configFile);
    		
    		Tester.run(cR);
    	} else
        if (new File(args[0]).exists()) {
            run(args[0]);
        } else {
            System.exit(1);
        }
    	
    }

    public static void run(String configFile) {
    	   int size = 0;
        long startTime = System.currentTimeMillis();
        //0. configure logger
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            FileAppender fileAppender = new FileAppender(layout, configFile.replaceAll(".xml", "") + ".log", false);
            fileAppender.setLayout(layout);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }

        logger.setLevel(Level.DEBUG);

        //1. Read configFile
        ConfigReader cr = new ConfigReader();
        cr.validateAndRead(configFile);

        logger.info(cr.getSourceInfo());
                logger.info(cr.getTargetInfo());
                //System.exit(1);
                
        //2. Fill caches using the query module
        //2.1 First sourceInfo
        logger.info("Loading source data ...");
        HybridCache source;
        source = getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        HybridCache target;
        target = getData(cr.getTargetInfo());

                
        //2.3 Swap targetInfo and sourceInfo if targetInfo size is larger than sourceInfo size
        HybridCache help;
        KBInfo swap;
        String var;

        if(source.size() == 0) {
        	logger.info("Source is empty - nothing to do");
        	return;
        }

        if(target.size() == 0) {
        	logger.info("Target is empty - nothing to do");
        	return;
        }

        
        if (target.size() > source.size()) {
            logger.info("Swapping data sources as |T| > |S|");
            //swap data sources
            help = target;
            target = source;
            source = help;
            //swap configs
            swap = cr.sourceInfo;
            cr.sourceInfo = cr.targetInfo;
            cr.targetInfo = swap;
        }
        
        //3. Read metrics
        SimpleMetricFactory mf = new SimpleMetricFactory(cr.sourceInfo.var, cr.targetInfo.var);
        mf.setExpression(cr.metricExpression);
        logger.info("Comparisons will be carried out by using " + cr.metricExpression);

        SimpleMetricFactory organizerMf = new SimpleMetricFactory(cr.sourceInfo.var, cr.targetInfo.var);
        String var1 = cr.sourceInfo.var.replaceAll("\\?", "");
        String var2 = cr.targetInfo.var.replaceAll("\\?", "");
        organizerMf.setExpression(mf.foldExpression(cr.metricExpression, var2, var1));
        logger.info("Organizing will be carried out by using " + mf.foldExpression(cr.metricExpression, var2, var1));

        //4. Reorganize targetInfo
        Organizer organizer = new DensityBasedOrganizer();
        //Organizer organizer = new LimesOrganizer();
        //Organizer organizer = new BruteForceOrganizer();
        if (cr.exemplars < 2) {
        	logger.info("Exemplars < 2: Automatically choosing size");
            organizer.computeExemplars(target, organizerMf);
        } else {
            organizer.computeExemplars(target, organizerMf, cr.exemplars);
        }

        ArrayList<String> uris = source.getAllUris();

        logger.info("Ready to compare.");
        //5. Compare
        //5.1 Get Writer ready
        Mapping acceptedLinks = new Mapping();
        Mapping linksToReview = new Mapping();
        Serializer serializer = SerializerFactory.getSerializer(cr.outputFormat);
        
        //5.2 Now write results
        HashMap<String, Double> results;
        Iterator<String> resultIterator;
        String s;
        for (int i = 0; i < uris.size(); i++) {

            if ((i + 1) % 1000 == 0) {
                logger.info(((i * 100) / uris.size()) + "% of links computed ...");
            }
            results = organizer.getSimilarInstances(source.getInstance(uris.get(i)), cr.verificationThreshold, mf);
            //logger.info("Getting results for "+sourceInfo.getInstance(uris.get(i)));
            resultIterator = results.keySet().iterator();
         
            while (resultIterator.hasNext()) {
                s = resultIterator.next();
                if (results.get(s) >= cr.acceptanceThreshold) {
                    acceptedLinks.add(uris.get(i), s, results.get(s));
                    size++;
                } else if (results.get(s) >= cr.verificationThreshold) {
                    linksToReview.add(uris.get(i), s, results.get(s));
                }
            }
        }

        //5.3 Close writers
        serializer.writeToFile(acceptedLinks, cr.acceptanceRelation, cr.acceptanceFile);
        serializer.writeToFile(linksToReview, cr.verificationRelation, cr.verificationFile);
        
        //6. Write some stats
        logger.info("Required " + organizer.getComparisons() + " comparisons overall.\n");
        logger.info("Comparisons were carried out in " + organizer.getComparisonTime() + " seconds overall.\n");
        logger.info("Required " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds overall.");
        System.out.println("Mapping size = "+size);
        //7. DONE
        logger.info("Done.");
    }

    public static HybridCache getData(KBInfo kb) {
        HybridCache cache = new HybridCache();
        //1. Try to get content from a serialization
        File cacheFile = new File("cache/" + kb.hashCode() + ".ser");
        try {
            if (cacheFile.exists()) {
                logger.info("Found cached data. Loading data from file " + cacheFile.getAbsolutePath());
                cache = HybridCache.loadFromFile(cacheFile);
            }
            if (cache.size() == 0) {
                throw new Exception();
            } else {
                logger.info("Cached data loaded successfully from file " + cacheFile.getAbsolutePath());
                logger.info("Size = " + cache.size());
            }
        } //2. If it does not work, then get it from data sourceInfo as specified
        catch (Exception e) {
            // need to add a QueryModuleFactory
            logger.info("No cached data found for " + kb.id);
            QueryModule module = QueryModuleFactory.getQueryModule(kb.type, kb);
            module.fillCache(cache);

            if (!new File("cache").exists() || !new File("cache").isDirectory()) {
                new File("cache").mkdir();
            }
            cache.saveToFile(new File("cache/" + kb.hashCode() + ".ser"));
        }

        return cache;
    }
}
