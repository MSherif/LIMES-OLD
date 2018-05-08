/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.costs;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.histogram.*;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.mapper.AtomicMapper;
import de.uni_leipzig.simba.measures.MeasureFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author ngonga
 */
public class Histogram {

    Cache source, target;
    Map<Integer, Cache> sources;
    Map<Integer, Cache> targets;
    DataGenerator sourceGenerator, targetGenerator;
    List<Integer> corpusSizes;
    List<Double> thresholds;
    int iterations;
    HistogramEntry[][][] histogramData;
    double averageSourceStringLenght = 0;
    double averageTargetStringLenght = 0;
    static Logger logger = Logger.getLogger("LIMES");

    public Histogram(List<Integer> corpusSizes, List<Double> thresholds, GeneratorType type, List<String> generatorAttributes, int iterations) {
        //init sources and targets
        sources = new HashMap<Integer, Cache>();
        targets = new HashMap<Integer, Cache>();
        this.corpusSizes = corpusSizes;
        this.thresholds = thresholds;
        this.iterations = iterations;
        //init generator
        if (type.equals(GeneratorType.DUMPBASED)) {
            sourceGenerator = new DumpBasedGenerator(generatorAttributes.get(0));
            targetGenerator = new DumpBasedGenerator(generatorAttributes.get(0));
        } else if (type.equals(GeneratorType.RANDOMSTRING)) {
            sourceGenerator = new RandomStringGenerator(Integer.parseInt(generatorAttributes.get(0)), Integer.parseInt(generatorAttributes.get(1)));
            targetGenerator = new RandomStringGenerator(Integer.parseInt(generatorAttributes.get(0)), Integer.parseInt(generatorAttributes.get(1)));
        } else if (type.equals(GeneratorType.RANDOMNUMBER)) {
            sourceGenerator = new RandomNumberGenerator(Integer.parseInt(generatorAttributes.get(0)), Integer.parseInt(generatorAttributes.get(1)));
            targetGenerator = new RandomNumberGenerator(Integer.parseInt(generatorAttributes.get(0)), Integer.parseInt(generatorAttributes.get(1)));
        }
        //generate data
        //generateData();
    }

    public void generateData() {
        sources = new HashMap<Integer, Cache>();
        targets = new HashMap<Integer, Cache>();

        for (int size : corpusSizes) {
            sources.put(size, sourceGenerator.generateData(size));
            targets.put(size, targetGenerator.generateData(size));
        }
    }

    public void generateData(int sourceSize, int targetSize) {
        source = sourceGenerator.generateData(sourceSize);
        target = targetGenerator.generateData(targetSize);
    }

    

    /**
     * Compute histogram for a given measure
     *
     * @param measure
     * @param folder Folder for data and output
     */
    public void computeHistogram(String measure, String folder) {
        histogramData = new HistogramEntry[corpusSizes.size()][corpusSizes.size()][thresholds.size()];
        for (int i = 0; i < corpusSizes.size(); i++) {
            for (int j = 0; j < corpusSizes.size(); j++) {
                for (int k = 0; k < thresholds.size(); k++) {
                    histogramData[i][j][k] = new HistogramEntry();
                }
            }
        }
        StringBuffer buffer = new StringBuffer();
        Mapping m;
        AtomicMapper mapper = MeasureFactory.getMapper(measure);
        for (int iteration = 0; iteration < iterations; iteration++) {
            logger.info("Running iteration "+iteration+" of "+iterations);
//            generateData();
            int sourceCount = 0;
            for (int sourceSize : corpusSizes) {
                int targetCount = 0;
                for (int targetSize : corpusSizes) {
                    int thresholdCount = 0;
                    generateData(sourceSize, targetSize);
                    for (double threshold : thresholds) {
                        long runtime = System.currentTimeMillis();
                        long memory = Runtime.getRuntime().totalMemory();

                        m = mapper.getMapping(source, target, "?x", "?y",
                                measure + "(x." + DataGenerator.LABEL + ", y." + DataGenerator.LABEL + ")", threshold);
                        runtime = System.currentTimeMillis() - runtime;
                        memory = Runtime.getRuntime().totalMemory() - memory;
//                        buffer = buffer.append(sourceSize).append("\t").append(sourceSize * sourceSize).append("\t").append(targetSize).
//                                append("\t").append(targetSize * targetSize).append("\t").append(sourceSize * targetSize).
//                                append("\t").append(threshold).append("\t").append(runtime).append("\t").append(memory).
//                                append("\t").append(m.getNumberofMappings()).append("\n");
                        buffer = buffer.append(sourceSize).append("\t").append(targetSize).
                                append("\t").append(sourceGenerator.getMean()).append("\t").append(sourceGenerator.getStandardDeviation()).
                                append("\t").append(targetGenerator.getMean()).append("\t").append(targetGenerator.getStandardDeviation()).
                                append("\t").append(threshold).append("\t").
                                append(runtime).append("\t").append(memory).append("\t").append(m.getNumberofMappings()).append("\n");

                        histogramData[sourceCount][targetCount][thresholdCount].addMemory(memory);
                        histogramData[sourceCount][targetCount][thresholdCount].addRuntime(runtime);
                        histogramData[sourceCount][targetCount][thresholdCount].addMappingSize(m.getNumberofMappings());
                        thresholdCount++;
                    }
                    targetCount++;
                }
                sourceCount++;
            }
        }

        //write training data for linear regression
        for (int i = 0; i < corpusSizes.size(); i++) {
            for (int j = 0; j < corpusSizes.size(); j++) {
                for (int k = 0; k < thresholds.size(); k++) {
                    histogramData[i][j][k].memory = histogramData[i][j][k].memory / iterations;
                    histogramData[i][j][k].runtime = histogramData[i][j][k].runtime / iterations;
                    histogramData[i][j][k].mappingSize = histogramData[i][j][k].mappingSize / iterations;
                }
            }
        }

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/linearRegression_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer1 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/runtime_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer2 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/mappings_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer3 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/memory_" + measure + "_" + sourceGenerator.getName())));
            //write data for linear regression
            writer.println(buffer);

            //writer data for heatmaps
            for (int i = 0; i < corpusSizes.size(); i++) {
                for (int j = 0; j < corpusSizes.size(); j++) {
                    writer1.print(histogramData[i][j][0].runtime + "\t");
                    writer2.print(histogramData[i][j][0].mappingSize + "\t");
                    writer3.print(histogramData[i][j][0].memory + "\t");
                }
                writer1.println();
                writer2.println();
                writer3.println();
            }
            writer.close();
            writer.close();
            writer1.close();
            writer2.close();
            writer3.close();
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Error writing histogram data for " + mapper.getName());
        }

    }

    public void computeFilterHistogram(String measure, String folder) {
        histogramData = new HistogramEntry[corpusSizes.size()][corpusSizes.size()][thresholds.size()];
        for (int i = 0; i < corpusSizes.size(); i++) {
            for (int j = 0; j < corpusSizes.size(); j++) {
                for (int k = 0; k < thresholds.size(); k++) {
                    histogramData[i][j][k] = new HistogramEntry();
                }
            }
        }
        StringBuffer buffer = new StringBuffer();
        Mapping m;
        AtomicMapper mapper = MeasureFactory.getMapper(measure);

        for (int iteration = 0; iteration < iterations; iteration++) {
//            generateData();
            int sourceCount = 0;
            for (int sourceSize : corpusSizes) {
                int targetCount = 0;
                for (int targetSize : corpusSizes) {
                    int thresholdCount = 0;
                    generateData(sourceSize, targetSize);
                    for (double threshold : thresholds) {
                        m = mapper.getMapping(source, target, "?x", "?y",
                                measure + "(x." + DataGenerator.LABEL + ", y." + DataGenerator.LABEL + ")", threshold);

                        long runtime = System.currentTimeMillis();
                        long memory = Runtime.getRuntime().totalMemory();
                        Filter filter = new LinearFilter();
                        m = filter.filter(m, measure + "(x." + DataGenerator.LABEL + ", y." + DataGenerator.LABEL + ")",
                                threshold, source, target, "?x", "?y");
                        runtime = System.currentTimeMillis() - runtime;
                        System.gc();
                        memory = Runtime.getRuntime().totalMemory() - memory;
//                        buffer = buffer.append(sourceSize).append("\t").append(sourceSize * sourceSize).append("\t").append(targetSize).
//                                append("\t").append(targetSize * targetSize).append("\t").append(sourceSize * targetSize).
//                                append("\t").append(threshold).append("\t").append(runtime).append("\t").append(memory).
//                                append("\t").append(m.getNumberofMappings()).append("\n");
//                        
                        buffer = buffer.append(sourceSize).append("\t").append(targetSize).append("\t").
                                append("\t").append(sourceGenerator.getMean()).append("\t").append(sourceGenerator.getStandardDeviation()).
                                append("\t").append(targetGenerator.getMean()).append("\t").append(targetGenerator.getStandardDeviation()).
                                append(threshold).append("\t").
                                append(runtime).append("\t").append(memory).append("\t").append(m.getNumberofMappings()).append("\n");

                        histogramData[sourceCount][targetCount][thresholdCount].addMemory(memory);
                        histogramData[sourceCount][targetCount][thresholdCount].addRuntime(runtime);
                        histogramData[sourceCount][targetCount][thresholdCount].addMappingSize(m.getNumberofMappings());
                        thresholdCount++;
                    }
                    targetCount++;
                }
                sourceCount++;
            }
        }

        //write training data for linear regression
        for (int i = 0; i < corpusSizes.size(); i++) {
            for (int j = 0; j < corpusSizes.size(); j++) {
                for (int k = 0; k < thresholds.size(); k++) {
                    histogramData[i][j][k].memory = histogramData[i][j][k].memory / iterations;
                    histogramData[i][j][k].runtime = histogramData[i][j][k].runtime / iterations;
                    histogramData[i][j][k].mappingSize = histogramData[i][j][k].mappingSize / iterations;
                }
            }
        }

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/filter_linearRegression_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer1 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/filter_runtime_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer2 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/filter_mappings_" + measure + "_" + sourceGenerator.getName())));
            PrintWriter writer3 = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/filter_memory_" + measure + "_" + sourceGenerator.getName())));
            //write data for linear regression
            writer.println(buffer);

            //writer data for heatmaps
            for (int i = 0; i < corpusSizes.size(); i++) {
                for (int j = 0; j < corpusSizes.size(); j++) {
                    writer1.print(histogramData[i][j][0].runtime + "\t");
                    writer2.print(histogramData[i][j][0].mappingSize + "\t");
                    writer3.print(histogramData[i][j][0].memory + "\t");
                }
                writer1.println();
                writer2.println();
                writer3.println();
            }
            writer.close();
            writer.close();
            writer1.close();
            writer2.close();
            writer3.close();
        } catch (Exception e) {
            logger.warn("Error writing histogram data for " + mapper.getName());
        }
    }

    public static void main(String args[]) {

        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            FileAppender fileAppender = new FileAppender(layout, "histogram.log", false);
            fileAppender.setLayout(layout);
            logger.addAppender(fileAppender);
        } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }

        List<Integer> corpusSize = new ArrayList<Integer>();
        for (int i = 1000; i <= 10000; i = i + 1000) {
            corpusSize.add(i);
        }
        int iterations = 10, lengthMax = 20, lengthMin = 3, min = 1, max = 1000000;

        List<Double> thresholds = new ArrayList<Double>();
        thresholds.add(0.5);
        thresholds.add(0.6);
        thresholds.add(0.7);
        thresholds.add(0.8);
        thresholds.add(0.9);

        List<String> measures = Arrays.asList("trigrams", "levensthein", "qgrams", "jaccard");
        List<String> languages = Arrays.asList("de", "fr", "rand", "en");
        List<String> dump = new ArrayList<String>();
        String basePath = "/home/ngonga/devan/helios/metrics";
        //basePath = "E:/Work/Papers/Eigene/2012/ISWC_HELIOS/Data/";
        Histogram cc;
        String path;
        //GERMAN
        logger.info("Processing German");

        logger.info("German data dumpbased processing");
        dump.add(basePath + "/labels_en_uris_de.nt");
        cc = new MetricHistogram(corpusSize, thresholds, GeneratorType.DUMPBASED, dump, iterations);
        logger.info("Running Trigrams ... ");
        path = basePath + "/de";
        cc.computeHistogram("trigrams", path);
        cc.computeFilterHistogram("trigrams", path);
        logger.info("Running Jaccard ... ");        
        cc.computeHistogram("jaccard", path);
        cc.computeFilterHistogram("jaccard", path);
        
        logger.info("Running EDJoin ... ");
        cc.computeHistogram("levenshtein", path);
        cc.computeFilterHistogram("levenshtein", path);

        logger.info("Running FastNGram ... ");
        cc.computeHistogram("qgrams", path);
        cc.computeFilterHistogram("qgrams", path);

        //FRENCH
        dump = new ArrayList<String>();
        dump.add(basePath + "/labels_en_uris_fr.nt");

        logger.info("French data dumpbased processing");
        cc = new Histogram(corpusSize, thresholds, GeneratorType.DUMPBASED, dump, iterations);
        path = basePath + "/fr";

        logger.info("Running PPJoinPlus ... ");
        cc.computeHistogram("trigrams", path);
        cc.computeFilterHistogram("trigrams", path);
        cc.computeHistogram("jaccard", path);
        cc.computeFilterHistogram("jaccard", path);

        logger.info("Running EDJoin ... ");
        cc.computeHistogram("levenshtein", path);
        cc.computeFilterHistogram("levenshtein", path);

        logger.info("Running FastNGram ... ");
        cc.computeHistogram("qgrams", path);
        cc.computeFilterHistogram("qgrams", path);

        //ENGLISH
        dump = new ArrayList<String>();
        path = basePath + "/en";

        logger.info("Running PPJoinPlus ... ");
        dump.add(basePath + "/labels_en.nt");
        cc.computeHistogram("trigrams", path);
        cc.computeFilterHistogram("trigrams", path);
        cc.computeHistogram("jaccard", path);
        cc.computeFilterHistogram("jaccard", path);

        logger.info("Running EDJoin ... ");
        cc.computeHistogram("levenshtein", path);
        cc.computeFilterHistogram("levenshtein", path);

        logger.info("Running FastNGram ... ");
        cc.computeHistogram("qgrams", path);
        cc.computeFilterHistogram("qgrams", path);


        //RANDOM STRINGS
        logger.info("Random string processing");
        dump = new ArrayList<String>();
        dump.add(lengthMin + "");
        dump.add(lengthMax + "");

        path = basePath + "/rand";

        cc = new Histogram(corpusSize, thresholds, GeneratorType.RANDOMSTRING, dump, iterations);
        logger.info("Running PPJoinPlus ... ");
        cc.computeHistogram("trigrams", path);
        cc.computeFilterHistogram("trigrams", path);
        cc.computeHistogram("jaccard", path);
        cc.computeFilterHistogram("jaccard", path);

        logger.info("Running EDJoin ... ");
        cc.computeHistogram("levenshtein", path);
        cc.computeFilterHistogram("levenshtein", path);

        logger.info("Running FastNGram ... ");
        cc.computeHistogram("qgrams", path);
        cc.computeFilterHistogram("qgrams", path);

        //Numbers
        logger.info("Processing numbers");
        dump = new ArrayList<String>();
        dump.add(min + "");
        dump.add(max + "");
        path = basePath + "/numb";

        cc = new Histogram(corpusSize, thresholds, GeneratorType.RANDOMNUMBER, dump, iterations);
        String measure = "euclidean";
        logger.info("Running " + MeasureFactory.getMapper(measure).getName());
        cc.computeHistogram(measure, path);
        cc.computeFilterHistogram(measure, path);
        logger.info("Done.");
    }
}
