/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.evaluation;

import com.mxgraph.util.mxCellRenderer;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.execution.rewriter.Rewriter;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.specification.LinkSpec;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author ngonga
 */
public class Experiment {

    static Logger logger = Logger.getLogger("LIMES");
    Cache source;
    Cache target;
    ConfigReader cr;
    ExecutionEngine ee;
    LinkSpec spec;
    Mapping cMapping;
    Mapping hMapping;

    public void init(String configFile) {
        long startTime = System.currentTimeMillis();
        //0. configure logger
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            FileAppender fileAppender = new FileAppender(layout, configFile.replaceAll(".xml", "") + ".log", false);
            fileAppender.setLayout(layout);
            logger.removeAllAppenders();
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }

        logger.setLevel(Level.DEBUG);

        //1. Read configFile
        cr = new ConfigReader();
        cr.validateAndRead(configFile);

        logger.info(cr.getSourceInfo());
        logger.info(cr.getTargetInfo());
        //System.exit(1);

        //2. Fill caches using the query module
        //2.1 First sourceInfo
        logger.info("Loading source data ...");
        source = HybridCache.getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        target = HybridCache.getData(cr.getTargetInfo());

        spec = new LinkSpec();
        spec.readSpec(cr.metricExpression, cr.verificationThreshold);
        ee = new ExecutionEngine(source, target, cr.sourceInfo.var, cr.targetInfo.var);

    }

    /**
     * Runs an experiment given a planner and whether the spec should be
     * rewritten
     *
     * @param planner Name of planner to use
     * @param rewrite Boolean
     * @return Pair of longs (runtime, mappingsize)
     */
    public List<Long> runExperiment(String planner, boolean rewrite, int iterations, String rootNameForPictures) {
        ExecutionPlanner p;
        Mapping m = new Mapping();
        long begin, end, duration = Long.MAX_VALUE;
        for (int i = 0; i < iterations; i++) {
            begin = System.currentTimeMillis();

            // create planner        
            if (planner.startsWith("c")) {
                p = new CanonicalPlanner();
            } else {
                p = new HeliosPlanner(source, target);
            }

            // rewrite spec if required
            if (rewrite) {
                Rewriter rewriter = new AlgebraicRewriter();
                spec = rewriter.rewrite(spec);
            }

            //generate plan and run
            NestedPlan np = p.plan(spec);
            m = ee.runNestedPlan(np);
            if (p instanceof CanonicalPlanner) {
                cMapping = m;
            } else {
                hMapping = m;
            }
            end = System.currentTimeMillis();
            duration = Math.min(duration, (end - begin));
            try {
                BufferedImage image = mxCellRenderer.createBufferedImage(np.getGraph(), null, 1, Color.white, true, null);
                ImageIO.write(image, "png", new File(rootNameForPictures + "_planner_" + planner + "_rewrite_" + rewrite + ".png"));
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("Was unable to draw for " + planner + " planner with rewrite = " + rewrite);
            }
        }
        //return results
        ArrayList<Long> result = new ArrayList<Long>();
        result.add(duration);
        result.add((long) m.getNumberofMappings());
        return result;
    }

    public static Map<String, List> runAllConfigs(String configFile, int iterations) {
        Experiment exp = new Experiment();
        exp.init(configFile);
        Map<String, List> result = new HashMap<String, List>();
        result.put("File", new ArrayList(Arrays.asList(new String[]{configFile})));
        result.put("SourceSize", new ArrayList<Long>(Arrays.asList(new Long[]{(long) exp.source.size()})));
        result.put("TargetSize", new ArrayList<Long>(Arrays.asList(new Long[]{(long) exp.target.size()})));
        result.put("C", exp.runExperiment("canonical", false, iterations, configFile));
        result.put("C+HR", exp.runExperiment("canonical", true, iterations, configFile));
        result.put("HP", exp.runExperiment("helios", false, iterations, configFile));
        result.put("HP+HR", exp.runExperiment("helios", true, iterations, configFile));
        result.put("Size", new ArrayList(Arrays.asList(new String[]{exp.spec.size()+""})));
//        System.out.println(SetOperations.difference(exp.hMapping, exp.cMapping));
        return result;
    }

    public static void runAllConfigsOnFolder(String folder, int iterations) {
        List<Map<String, List>> results = new ArrayList<Map<String, List>>();
        File f = new File(folder);
        if (f.isDirectory()) {
            String[] files = f.list();
            for (int i = 0; i < files.length; i++) {
                File f2 = new File(folder + "/" + files[i]);
                if (f2.isDirectory()) {
                    if (new File(f2 + "/spec.xml").exists()) {
                        try {
                            Map<String, List> result = runAllConfigs(folder + "/" + files[i] + "/spec.xml", iterations);
                            results.add(result);
                            logger.info("Adding " + result);
                        } catch (Exception e) {
                            logger.warn("Could not run " + files[i]);
                        }
                    }
                }
            }
        }

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/results_helios.csv")));
            if (!results.isEmpty()) {
                List<String> keys = new ArrayList<String>(results.get(0).keySet());
                for (String key : keys) {
                    writer.print(key + "\t");
                }
                writer.println();
                for (Map<String, List> r : results) {
                    for (String key : keys) {
                        writer.print(r.get(key).get(0) + "\t");
                    }
                    writer.println();
                }
            }
            writer.close();
        } catch (Exception e) {
            logger.warn("Error writing the following results\n\n " + results);
        }
    }

    public static void main(String args[]) {
        if (args.length > 1) {
            System.out.println("Running " + args[1] + " iterations on all specs in folder " + args[0]);
            runAllConfigsOnFolder(args[0], Integer.parseInt(args[1]));
        }
//        else System.out.println(runAllConfigs("E:/Work/Java/TransferLearning/finalSpecs/climb-linkedgeodata-venues-mountains/spec.xml", 1));
        else System.out.println(runAllConfigs("E:/Work/Papers/Eigene/2012/ISWC_HELIOS/Data/specs/geoknow_spec1.xml", 1));
    }
}