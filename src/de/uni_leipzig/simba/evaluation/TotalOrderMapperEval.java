/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.evaluation;

import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.query.*;
import de.uni_leipzig.simba.io.*;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import java.io.File;
import java.util.HashMap;
import org.apache.log4j.*;

/**
 * Implements automatic testing for SPATIAL as implemented in the TotalOderMapper
 * @author ngonga
 */
public class TotalOrderMapperEval {

    static Logger logger = Logger.getLogger("LIMES");

    public static void usage()
    {
        System.out.println("====================");
        System.out.println("===     Usage    ===");
        System.out.println("====================");
        System.out.println("1. Config file");
        System.out.println("2. Min threshold");
        System.out.println("3. Max threshold");
        System.out.println("4. Min granularity");
        System.out.println("5. Max granularity");
        System.out.println("6. Number of repetitions");
    }
    public static void main(String args[]) {
        int maxThreshold = 1000;
        int minThreshold = 1;
        int fromGranularity = 1;
        int toGranularity = 1025;
        int repetitions = 5;
        if (args.length == 0) {
            usage();
            return;
        }
        if (args.length >= 2) {
            minThreshold = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            maxThreshold = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            fromGranularity = Integer.parseInt(args[3]);
        }
        if (args.length >= 5) {
            toGranularity = Integer.parseInt(args[4]);
        }
        if (args.length >= 6) {
            repetitions = Integer.parseInt(args[5]);
        }
        if (new File(args[0]).exists()) {
            if (new File(args[0]).isDirectory()) {
                String[] fileNames = new File(args[0]).list();
                for (int i = 0; i < fileNames.length; i++) {
                    run(args[0] + "/" + fileNames[i], minThreshold, maxThreshold, fromGranularity, toGranularity, repetitions);
                }
            } else {
                run(args[0], minThreshold, maxThreshold, fromGranularity, toGranularity, repetitions);
            }
        } else {
            logger.fatal("Input file " + args[0] + " does not exist.");
            System.exit(1);
        }
    }

    public static void run(String configFile, int minThreshold, int maxThreshold, int minGranularity, int maxGranularity,
            int repetitions) {
        System.out.println("Experiment "+configFile);
        System.out.println("Max threshold =  "+maxThreshold);
        System.out.println("Max granularity =  "+maxGranularity);
        System.out.println("Max repetitions =  "+repetitions);
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
        HybridCache source = new HybridCache();
        source = getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        HybridCache target = new HybridCache();
        target = getData(cr.getTargetInfo());

        //logger.info("Content of sourceInfo\n"+sourceInfo);
        //logger.info("Content of targetInfo\n"+targetInfo);
        //System.exit(1);
        //2.3 Swap targetInfo and sourceInfo if targetInfo size is larger than sourceInfo size
        HybridCache help;
        KBInfo swap;
        String var;

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

        //Mapping mapping = (new PPJoinPlusPlus()).getMapping(sourceInfo, targetInfo, cr.sourceInfo.var, cr.targetInfo.var, cr.metricExpression, cr.verificationThreshold);


        HashMap<Integer, Double> times = new HashMap<Integer, Double>();
        long begin, end;
        String result = "\n\n";
        for (double threshold = minThreshold; threshold < maxThreshold; threshold = threshold * 2) {
            result = result + "\nDistance Threshold = " + threshold + "\n=========\n";
            for (int granularity = minGranularity; granularity <= maxGranularity; granularity = granularity * 2) {
                result = result + "Granularity = "+granularity + "\n";
                for (int j = 0; j < repetitions; j++) {
                    logger.info("Processing granularity = " + granularity);
                    begin = System.currentTimeMillis();
                    SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper(cr.executionPlan,
                            cr.sourceInfo, cr.targetInfo, source, target, new LinearFilter(), cr.granularity);
                    //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
                    //Mapping mapping = mapper.getLinks(cr.metricExpression, 1 / (1 + threshold));
                    Mapping mapping = mapper.getLinks(cr.metricExpression, threshold);
                    end = System.currentTimeMillis();
                    logger.info("Required " + (end - begin)+ " ms.");
                    result = result + (end - begin) + "\t";
                }
                result = result + "\n";
            }
        }
        //get Writer ready

        logger.info("Runtimes" + result);
        System.out.println("Runtimes" + result);
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
