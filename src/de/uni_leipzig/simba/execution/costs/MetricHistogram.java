/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.costs;

import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.histogram.DataGenerator;
import de.uni_leipzig.simba.execution.histogram.GeneratorType;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.measures.MeasureFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class MetricHistogram extends Histogram {

    public MetricHistogram(List<Integer> corpusSizes, List<Double> thresholds, GeneratorType type, List<String> generatorAttributes, int iterations) {
        super(corpusSizes, thresholds, type, generatorAttributes, iterations);
    }

    public void computeMeasureHistogram(String measure, String folder) {
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
        Measure metric = MeasureFactory.getMeasure(measure);
        for (int iteration = 0; iteration < iterations; iteration++) {
            logger.info("Running iteration " + iteration + " of " + iterations);
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

                        m = new Mapping();
                        double sim;
                        for (Instance sourceInstance : source.getAllInstances()) {
                            for (Instance targetInstance : target.getAllInstances()) {
                                sim = metric.getSimilarity(targetInstance, targetInstance, DataGenerator.LABEL, DataGenerator.LABEL);
                                if (sim >= threshold) {
                                    m.add(sourceInstance.getUri(), targetInstance.getUri(), sim);
                                }
                            }
                        }
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
            logger.warn("Error writing histogram data for " + metric.getName());
        }
    }
}
