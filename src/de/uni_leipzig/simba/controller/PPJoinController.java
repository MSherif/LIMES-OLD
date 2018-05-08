/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.controller;

import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.MappingUtils;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.*;
import de.uni_leipzig.simba.io.rdfconfig.RDFConfigReader;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

import java.io.File;

import org.apache.log4j.*;

import com.google.common.collect.Range;

/**
 * Just for tests
 * @author ngonga
 */
public class PPJoinController {

    static Logger logger = Logger.getLogger("LIMES");

    public static void main(String args[]) {
        if(args.length == 0)
        {
            logger.fatal("No configuration file specified.");
            System.exit(1);
        }
        if (new File(args[0]).exists()) {
            run(args[0]);
        } else {
            logger.fatal("Input file " + args[0] + " does not exist.");
            System.exit(1);
        }
    }

    /** Runs LIMES and returns a mappinng
     *
     * @param configFile XML Config für LIMES
     * @return Resulting mapping
     */
    public static Mapping getMapping(String configFile) {

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
        ConfigReader cr = createConfigReader(configFile);
        cr.validateAndRead(configFile);

        Mapping result = getMapping(cr);
        return result;
    }

    public static Mapping getMapping(ConfigReader cr) {
        logger.info(cr.getSourceInfo());
        logger.info(cr.getTargetInfo());
        //System.exit(1);

        //2. Fill caches using the query module
        //2.1 First sourceInfo
        logger.info("Loading source data ...");
        HybridCache source = new HybridCache();
        source = HybridCache.getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        HybridCache target = new HybridCache();
        target = HybridCache.getData(cr.getTargetInfo());

        //logger.info("Content of sourceInfo\n"+sourceInfo);
        //logger.info("Content of targetInfo\n"+targetInfo);
        //System.exit(1);
        //2.3 Swap targetInfo and sourceInfo if targetInfo size is larger than sourceInfo size
        HybridCache help;
        KBInfo swap;
        String var;

        //No need for swapping anymore as operations are symmetric
        /*
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
        }*/

        //Mapping mapping = (new PPJoinPlusPlus()).getMapping(sourceInfo, targetInfo, cr.sourceInfo.var, cr.targetInfo.var, cr.metricExpression, cr.verificationThreshold);
        SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper(cr.executionPlan,
                cr.sourceInfo, cr.targetInfo, source, target, new LinearFilter(), cr.granularity);
        //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
        logger.info("Getting links ...");
        long time = System.currentTimeMillis();
        Mapping mapping = mapper.getLinks(cr.metricExpression, cr.verificationThreshold);
        logger.info("Got links in " + (System.currentTimeMillis() - time) + "ms.");
        //get Writer ready
        if(cr.executionPlan.toLowerCase().startsWith("oneton"))
            mapping = mapping.getBestOneToNMapping();
        else if (cr.executionPlan.toLowerCase().startsWith("onetoone"))
            mapping = Mapping.getBestOneToOneMappings(mapping);
        return mapping;
    }

    /**
     * @param configFile
     * @return either an XML or RDF ConfigReader based on the configuration file extension
     * @author sherif
     */
    private static ConfigReader createConfigReader(String configFile) {
        ConfigReader cr = null;
        if(configFile.endsWith(".xml")){
            cr = new ConfigReader();
        }else if(configFile.endsWith(".ttl") ||
                configFile.endsWith(".nt") ||
                configFile.endsWith(".rdf") ||
                configFile.endsWith(".n3")){
            cr = new RDFConfigReader();
        }else{
            logger.error("configuration file type not implemented");
            System.exit(1);
        }
        return cr;
    }

    public static void run(String configFile) {

        long startTime = System.currentTimeMillis();
        //0. configure logger
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            String logFilename = configFile.substring(0, configFile.lastIndexOf(".")) + ".log";
            System.out.println("logFilename: " + logFilename);
            FileAppender fileAppender = new FileAppender(layout, logFilename, false);
            fileAppender.setLayout(layout);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender." + e);
        }

        logger.setLevel(Level.DEBUG);

        //1. Read configFile
        ConfigReader cr = createConfigReader(configFile);
        cr.validateAndRead(configFile);

//        logger.info(cr.getSourceInfo());
//        logger.info(cr.getTargetInfo());
//        //System.exit(1);
//
//        //2. Fill caches using the query module
//        //2.1 First sourceInfo
//        logger.info("Loading source data ...");
//        HybridCache source;
//        source = HybridCache.getData(cr.getSourceInfo());
//
//        //2.2 Then targetInfo
//        logger.info("Loading target data ...");
//        HybridCache target;
//        target = HybridCache.getData(cr.getTargetInfo());
//
        //logger.info("Content of sourceInfo\n"+sourceInfo);
        //logger.info("Content of targetInfo\n"+targetInfo);
        //System.exit(1);
        //2.3 Swap targetInfo and sourceInfo if targetInfo size is larger than sourceInfo size

        // TODO This seems to be the same code as getMapping(ConfigReader) - duplication should be justified in a comment or removed ~ Claus 24 June 2015



//        //Mapping mapping = (new PPJoinPlusPlus()).getMapping(sourceInfo, targetInfo, cr.sourceInfo.var, cr.targetInfo.var, cr.metricExpression, cr.verificationThreshold);
//        SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper(cr.executionPlan,
//                cr.sourceInfo, cr.targetInfo, source, target, new LinearFilter(), cr.granularity);
//        //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
//        logger.info("Getting links ...");
//        long time = System.currentTimeMillis();
//        Mapping mapping = mapper.getLinks(cr.metricExpression, cr.verificationThreshold);
//        logger.info("Got links in " + (System.currentTimeMillis() - time) + "ms.");
//        //get Writer ready
//        if(cr.executionPlan.toLowerCase().startsWith("oneton"))
//            mapping = mapping.getBestOneToNMapping();
//        else if (cr.executionPlan.toLowerCase().startsWith("onetoone"))
//            mapping = Mapping.getBestOneToOneMappings(mapping);

        Mapping mapping = getMapping(cr);

        Serializer serializer = SerializerFactory.getSerializer(cr.outputFormat);
        //get folder of config file and set it as default folder for serializer
        File f = new File(configFile).getAbsoluteFile().getParentFile().getAbsoluteFile();
        serializer.setFolderPath(f);
        logger.info("Using " + serializer.getName() + " to serialize");

        Mapping accepted = MappingUtils.extractByThresholdRange(mapping, Range.atLeast(cr.acceptanceThreshold));
        Mapping toReview = MappingUtils.extractByThresholdRange(mapping, Range.closedOpen(cr.verificationThreshold, cr.acceptanceThreshold));

//        //now split results
//        int linkCounter = 0;
//        int reviewCounter = 0;
//        for (String key : mapping.map.keySet()) {
//            for (String value : mapping.map.get(key).keySet()) {
//                if (mapping.map.get(key).get(value) >= cr.acceptanceThreshold) {
//                    linkCounter++;
//                    accepted.add(key, value, mapping.map.get(key).get(value));
//                } else if (mapping.map.get(key).get(value) >= cr.verificationThreshold) {
//                    reviewCounter++;
//                    toReview.add(key, value, mapping.map.get(key).get(value));
//                }
//            }
//        }
//        logger.info("Returned " + linkCounter + " links above acceptance threshold.");
//        logger.info("Returned " + reviewCounter + " links to review.");

        logger.info("Returned " + accepted.size() + " links above acceptance threshold.");
        logger.info("Returned " + toReview.size() + " links to review.");

        //close writers
        serializer.setPrefixes(cr.prefixes);
        serializer.writeToFile(accepted, cr.acceptanceRelation, cr.acceptanceFile);
        serializer.writeToFile(toReview, cr.verificationRelation, cr.verificationFile);

        logger.info("Mapping carried out in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        logger.info("Done.");
    }


    /**
     * This method appears to be no longer used
     * @param configFile
     */
    @Deprecated
    public static void _run(String configFile) {

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
        source = HybridCache.getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        HybridCache target;
        target = HybridCache.getData(cr.getTargetInfo());

        //logger.info("Content of sourceInfo\n"+sourceInfo);
        //logger.info("Content of targetInfo\n"+targetInfo);
        //System.exit(1);
        //2.3 Swap targetInfo and sourceInfo if targetInfo size is larger than sourceInfo size

        //Mapping mapping = (new PPJoinPlusPlus()).getMapping(sourceInfo, targetInfo, cr.sourceInfo.var, cr.targetInfo.var, cr.metricExpression, cr.verificationThreshold);
        SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper(cr.executionPlan,
                cr.sourceInfo, cr.targetInfo, source, target, new LinearFilter(), cr.granularity);
        //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
        logger.info("Getting links ...");
        long time = System.currentTimeMillis();
        Mapping mapping = mapper.getLinks(cr.metricExpression, cr.verificationThreshold);
        logger.info("Got links in " + (System.currentTimeMillis() - time) + "ms.");
        //get Writer ready
        if(cr.executionPlan.toLowerCase().startsWith("oneton"))
            mapping = mapping.getBestOneToNMapping();
        else if (cr.executionPlan.toLowerCase().startsWith("onetoone"))
            mapping = Mapping.getBestOneToOneMappings(mapping);
        Serializer serializer = SerializerFactory.getSerializer(cr.outputFormat);
        //get folder of config file and set it as default folder for serializer
        File f = new File(configFile).getAbsoluteFile().getParentFile().getAbsoluteFile();
        serializer.setFolderPath(f);
        logger.info("Using " + serializer.getName() + " to serialize");

        Mapping accepted = new Mapping();
        Mapping toReview = new Mapping();

        //now split results
        int linkCounter = 0;
        int reviewCounter = 0;
        for (String key : mapping.map.keySet()) {
            for (String value : mapping.map.get(key).keySet()) {
                if (mapping.map.get(key).get(value) >= cr.acceptanceThreshold) {
                    linkCounter++;
                    accepted.add(key, value, mapping.map.get(key).get(value));
                } else if (mapping.map.get(key).get(value) >= cr.verificationThreshold) {
                    reviewCounter++;
                    toReview.add(key, value, mapping.map.get(key).get(value));
                }
            }
        }
        logger.info("Returned " + linkCounter + " links above acceptance threshold.");
        logger.info("Returned " + reviewCounter + " links to review.");

        //close writers
        serializer.setPrefixes(cr.prefixes);
        serializer.writeToFile(accepted, cr.acceptanceRelation, cr.acceptanceFile);
        serializer.writeToFile(toReview, cr.verificationRelation, cr.verificationFile);

        logger.info("Mapping carried out in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        logger.info("Done.");
    }
}
