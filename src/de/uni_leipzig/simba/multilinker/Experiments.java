/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.multilinker;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class Experiments {
	
    static boolean printToLogFile = false;
    static Cache restaurant = (Cache) DataSetChooser.getData(DataSets.RESTAURANTS_CSV).getSourceCache();
    static Cache person1 = (Cache) DataSetChooser.getData(DataSets.PERSON1_CSV).getSourceCache();
    static Cache person2 = (Cache) DataSetChooser.getData(DataSets.PERSON2_CSV).getSourceCache();
    static Cache dblp = (Cache) DataSetChooser.getData(DataSets.DBLPACM).getSourceCache();
    static Cache acm = (Cache) DataSetChooser.getData(DataSets.DBLPACM).getTargetCache();
    static Cache abtBuy = (Cache) DataSetChooser.getData(DataSets.ABTBUY).getSourceCache();
    static Cache scholar = (Cache) DataSetChooser.getData(DataSets.DBLPSCHOLAR).getTargetCache();
    static Cache amazon = (Cache) DataSetChooser.getData(DataSets.AMAZONGOOGLE).getSourceCache();
    static Cache googleProducts = (Cache) DataSetChooser.getData(DataSets.AMAZONGOOGLE).getTargetCache();

    public static void runExperiment(Cache c, int mappingCount, double destructionRate,
            double rewritingRate, int iterations, boolean batch, String outputFile) {

        File folder = new File(outputFile).getAbsoluteFile().getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        MultiLinker ml = new MultiLinker();
        List<Cache> caches = new ArrayList<Cache>();
        caches.add(c);
        for (int i = 0; i < mappingCount - 1; i++) {
            caches.add(MultiLinker.alterCache(c, destructionRate, rewritingRate));
        }
        ml.multiLinkDataset(caches, iterations, batch);
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
            writer.println("Iteration "+iterations);
            writer.println(ml.resultBuffer);
            ml.resultBuffer = "";
            writer.close();
        } catch (Exception e) {
            System.err.println("Error writing " + outputFile);
        }
    }

    public static void main(String args[]) throws FileNotFoundException {
        String root;
        int exp = -1;
        if (args.length <= 2) {
            System.err.println("arg[0] = root folder; arg[1] = experiment number ");
        }

        root = args[0];
        if (args.length > 1) {
            exp = Integer.parseInt(args[1]);
        }

        for (int mappingCount = 3; mappingCount <= 5; mappingCount++) {
            System.out.println("\n*******************");
            System.out.println("Mapping Count= " + mappingCount);
            System.out.println("*******************\n");
            for (double destructionRate = 0.1d; destructionRate <= 0.5d; destructionRate += 0.1d) {
                for (int repetition = 0; repetition < 5; repetition++) {
                    System.out.println("\n---------------------------------------");
                    System.out.println("Destruction Rate= " + destructionRate * 100 + "%");
                    System.out.println("---------------------------------------\n");
                    if (exp == -1) {
                        runExperiment(restaurant, mappingCount, destructionRate, 0.3, 10, true, root + "/results/restaurant/restaurants_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                        runExperiment(person1, mappingCount, destructionRate, 0.3, 10, true, root + "/results/persons1/person1_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                        runExperiment(person2, mappingCount, destructionRate, 0.3, 10, true, root + "/results/persons2/person2_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                        runExperiment(dblp, mappingCount, destructionRate, 0.3, 10, true, root + "/results/dblp/dblp_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                        runExperiment(acm, mappingCount, destructionRate, 0.3, 10, true, root + "/results/acm/acm_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                        runExperiment(amazon, mappingCount, destructionRate, 0.3, 10, true, root + "/results/amazon/amazon_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 0) {
                        runExperiment(restaurant, mappingCount, destructionRate, 0.3, 10, true, root + "/results/restaurant/restaurants_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 1) {
                        runExperiment(person1, mappingCount, destructionRate, 0.3, 10, true, root + "/results/persons1/person1_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 2) {
                        runExperiment(person2, mappingCount, destructionRate, 0.3, 10, true, root + "/results/persons2/person2_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 3) {
                        runExperiment(dblp, mappingCount, destructionRate, 0.3, 10, true, root + "/results/dblp/dblp_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 4) {
                        runExperiment(acm, mappingCount, destructionRate, 0.3, 10, true, root + "/results/acm/acm_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    } else if (exp == 5) {
                        runExperiment(amazon, mappingCount, destructionRate, 0.3, 10, true, root + "/results/amazon/amazon_mappings" + mappingCount + "_rate_" + (int) (destructionRate * 100) + ".txt");
                    }
                }
            }
        }
    }
}
