/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.controller;

import de.uni_leipzig.simba.io.serializer.TabSeparatedSerializer;
import algorithms.Correspondence;
import algorithms.ppjoinplus.PPJoinPlus;
import java.io.File;
import de.uni_leipzig.simba.cache.*;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.log4j.*;

/** DEPRECATED.
 * Not compatible anymore with library (EDJoinPlus.jar).
 * See commented code below.
 * 
 * @author ngonga
 */
@Deprecated
public class SimpleSimilarityPPJoinController extends Controller {

        static Logger logger = Logger.getLogger("LIMES");

    public static void main(String args[]) {
        if (new File(args[0]).exists()) {
            run(args[0]);
        } else {
            logger.fatal("Input file "+args[0]+" does not exist.");
            System.exit(1);
        }
    }

    public static void run(String configFile) {
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

        //3. Read metrics
        char metricName = cr.metricExpression.charAt(0);

        //3.1 fill objects from sourceInfo in entry
        logger.info("Filling objects from source knowledge base.");
        HashMap<Integer, String> sourceMap = new HashMap<Integer, String>();
        ArrayList<String> uris = source.getAllUris();
        ArrayList<String> entries = new ArrayList<String>();
        Instance instance;
        int counter = 0, border=0;
        for(int i=0; i < uris.size(); i++)
        {
            instance = source.getInstance(uris.get(i));
            for(String s: instance.getProperty(cr.sourceInfo.properties.get(0)))
            {
                sourceMap.put(counter, uris.get(i));
                entries.add(s);
                counter++;
            }
        }

        //3.2 fill objects from targetInfo in entries
         logger.info("Filling objects from target knowledge base.");
        HashMap<Integer, String> targetMap = new HashMap<Integer, String>();
        border = counter-1;
        uris = target.getAllUris();
        for(int i=0; i < uris.size(); i++)
        {
            instance = target.getInstance(uris.get(i));
            for(String s: instance.getProperty(cr.targetInfo.properties.get(0)))
            {
                targetMap.put(counter, uris.get(i));
                entries.add(s);
                counter++;
            }
        }

        String[] entryArray = new String[entries.size()];
        for(int i=0; i<entries.size(); i++)
        {
            entryArray[i] = entries.get(i);
        }
        logger.info("Launching PPJoinX");
        LinkedList <Correspondence> result = PPJoinPlus.start(metricName, cr.verificationThreshold, 2, entryArray);
        logger.info("PPJoinX complete.");
        //4. Filter and write the results;
        //4.1 Get Writer ready
        logger.info("Beginning to serialize results ...");
        Serializer serializer = new TabSeparatedSerializer();
        
        Mapping accepted = new Mapping();
        Mapping toReview = new Mapping();
        
        
        logger.info("Mapping carried out in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        logger.info("Done.");
        //4.2 Filter
        Correspondence corr;
        for(int i=0; i < result.size(); i++)
        {
            if((i+1)%1000 == 0) logger.info("Serialized "+((i*100)/result.size())+"% of the results");
            corr = result.get(i);
//            if((corr.getFirstObject() <= border && corr.getSecondObject() > border) ||
//                    (corr.getFirstObject() > border && corr.getSecondObject() <= border))
//            {
//                if(corr.getSimilarity() >= cr.acceptanceThreshold)
//                {
//                    if(corr.getFirstObject() <= border)
//                        accepted.add(sourceMap.get(corr.getFirstObject()), targetMap.get(corr.getSecondObject()), corr.getSimilarity());
//                    else
//                        accepted.add(sourceMap.get(corr.getSecondObject()), targetMap.get(corr.getFirstObject()), corr.getSimilarity());
//                }
//                else
//                {
//                    if(corr.getFirstObject() <= border)
//                        toReview.add(sourceMap.get(corr.getFirstObject()), targetMap.get(corr.getSecondObject()), corr.getSimilarity());
//                    else
//                        toReview.add(sourceMap.get(corr.getSecondObject()), targetMap.get(corr.getFirstObject()), corr.getSimilarity());
//                }
//            }
        }
        //close writers
        serializer.writeToFile(accepted, cr.acceptanceRelation, cr.acceptanceFile);
        serializer.writeToFile(toReview, cr.verificationRelation, cr.verificationFile);
        
        logger.info("Serialize completed");
        
        //6. Write some stats
        logger.info("Required " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds overall.");

        //7. DONE
        logger.info("Done.");
    }

}

