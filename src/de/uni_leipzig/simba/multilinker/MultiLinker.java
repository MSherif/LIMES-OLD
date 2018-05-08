/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.multilinker;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.String;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class MultiLinker {

    /**
     * Computes all mappings for a list of caches
     *
     */
    static double coverage = 0.6;
    static double beta = 2d;
    public static Measure fmeasure = new PseudoMeasures(true);
//    static String fmeasure = "own";
    static Logger logger = Logger.getLogger("LIMES");
    static boolean useSupervisedLearning = true;
//    BufferedWriter log;
    public String resultBuffer = "";

    /**
     * Computes mappings to the list of caches
     *
     * @param caches Caches for linking
     * @return
     */
    public static MappingMatrix getMappings(List<Cache> caches) {
        MappingMatrix matrix = new MappingMatrix(caches.size());
        for (int i = 0; i < caches.size(); i++) {
            for (int j = i + 1; j < caches.size(); j++) {
                if (useSupervisedLearning) {
                    matrix.addMapping(i, j, getDeterministicUnsupervisedMappings(caches.get(i), caches.get(j)));
                }
            }
        }
        return matrix;
    }

    /**
     * Computes initial mappings
     *
     */
    public static Mapping getDeterministicUnsupervisedMappings(Cache source, Cache target) {
        Mapping map = new Mapping();
        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, coverage, beta);
        bsc.setMeasure(fmeasure);
        Set<String> properties = source.getAllProperties();

        //List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
        List<SimpleClassifier> cp = new ArrayList<SimpleClassifier>();
        for (String property : properties) {
            //cp.add(new SimpleClassifier("jaccard", 1.0, property, property));
//            cp.add(new SimpleClassifier("levenshtein", 1.0, property, property));
            cp.add(new SimpleClassifier("trigrams", 1.0, property, property));
        }

        ComplexClassifier cc = bsc.getZoomedHillTop(5, 3, cp);
//        logger.info("Mapping size is " + cc.mapping.getNumberofMappings());
//        logger.info("F-measure is " + cc.fMeasure);
//        System.out.println("Mapping size is " + cc.mapping.getNumberofMappings());
//        System.out.println("F-measure is " + cc.fMeasure);
        map = cc.mapping;
        return Mapping.getBestOneToOneMappings(map);
    }

    public static double getErrorRate(Cache c1, Cache c2) {
        double count = 0;
        for (String uri : c1.getAllUris()) {
            if (c2.containsUri(uri)) {
                if (!equivalent(c1.getInstance(uri), c2.getInstance(uri))) {
                    count++;
                }
            } else {
                count++;
            }
        }

        for (String uri : c2.getAllUris()) {
            if (c1.containsUri(uri)) {
                if (!equivalent(c2.getInstance(uri), c1.getInstance(uri))) {
                    count++;
                }
            } else {
                count++;
            }
        }
        return count / (c1.size() + c2.size());
    }

    public double getErrorRate(List<Cache> caches) {
        double error = 0d;
        double e = 0d;
        for (int i = 0; i < caches.size(); i++) {
            for (int j = i + 1; j < caches.size(); j++) {
                e = getErrorRate(caches.get(i), caches.get(j));
//                System.out.println("Error rate[" + i + ", " + j + "] " + e);
                error = error + e;
            }
        }
        return 2 * error / (caches.size() * (caches.size() - 1));
    }

    public static String misspell(String s, double rate) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            double r = Math.random();
            if (r <= rate) {
                result = result + 'X';
            } else {
                result = result + s.charAt(i);
            }
        }
        return result;
    }

    public static String abbreviate(String s, double rate) {
        String result = "";
        String[] split = s.split(" ");
        for (int i = 0; i < split.length; i++) {
            double r = Math.random();
            if (r <= rate) {
                result = result + split[i].charAt(0) + "." + " ";
            } else {
                result = result + split[i] + " ";
            }
        }
        return result.substring(0, result.length() - 1);
    }

    public static String permute(String s, double rate) {
        String result = "";
        String[] split = s.split(" ");
        String[] rr = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            double r = Math.random();
            if (r <= rate) {
                int index = (int) Math.floor(Math.random() * split.length);
                rr[index] = split[i];
                rr[i] = split[index];
            } else {
                rr[i] = split[i];
            }
        }
        for (int i = 0; i < rr.length; i++) {
            result = result + rr[i] + " ";
        }
        return result.substring(0, result.length() - 1);
    }

    public static String getRandomElement(Set<String> set) {
        List<String> p = new ArrayList<String>(set);
        int index = (int) Math.ceil(((double) p.size()) * Math.random()) - 1;
        //        if (index > (p.size() - 1)) {
        //            index = p.size() - 1;
        //        }
        return p.get(index);
    }

    public static Mapping getVotingMapping(int i, int j, MappingMatrix matrix) {
        //copy
        Mapping m = matrix.getMapping(i, j).reverseSourceTarget().reverseSourceTarget();
        for (int k = 0; k < matrix.mappings.length; k++) {
            if (k != i && k != j) {
                m = MappingMath.add(m, MappingMath.multiply(matrix.getMapping(i, k), matrix.getMapping(k, j), true), true);
            }
        }
        //        return m.trim();
        return Mapping.getBestOneToOneMappings(m.scale((double) matrix.mappings.length - 1));
    }

    public static MappingMatrix computeVotingMatrices(MappingMatrix matrix) {
        MappingMatrix result = new MappingMatrix(matrix.mappings.length);
        for (int i = 0; i < matrix.mappings.length; i++) {
            for (int j = i + 1; j < matrix.mappings.length; j++) {
                result.addMapping(i, j, getVotingMapping(i, j, matrix));
            }
        }
        return result;
    }

    public static Cache alterCache(Cache c, double alterationRate, double operatorRate) {
        double r;
        MemoryCache result = new MemoryCache();
        for (String uri : c.getAllUris()) {
            r = Math.random();
            if (r <= alterationRate) {
                //get random property
                Instance instance = c.getInstance(uri).copy();
                String p = getRandomElement(instance.getAllProperties());
//                int operator = (int) Math.floor(Math.random() * 3);
                String value;
//                if (operator == 0) {
                value = misspell(getRandomElement(instance.getProperty(p)), operatorRate);
//                } else if (operator == 1) {
//                    value = abbreviate(getRandomElement(instance.getProperty(p)), operatorRate);
//                } else {
//                    value = permute(getRandomElement(instance.getProperty(p)), operatorRate);
//                }
                //System.out.println("Misspelled = " + value);
                instance.replaceProperty(p, new TreeSet<String>());
                instance.addProperty(p, value);
                result.addInstance(instance);
            } else {
                result.addInstance(c.getInstance(uri).copy());
            }
        }
        return result;
    }

    public static double getPseudoFmeasure(MappingMatrix m, Cache source) {
        double average = 0d;
        double realAverage = 0d;
        double f, realF;
        for (int i = 0; i < m.mappings.length; i++) {
            for (int j = i + 1; j < m.mappings.length; j++) {
                f = new PseudoMeasures().getPseudoFMeasure(source.getAllUris(), source.getAllUris(), Mapping.getBestOneToOneMappings(m.getMapping(i, j)), beta);
                realF = MappingMath.computeFMeasure(Mapping.getBestOneToOneMappings(m.getMapping(i, j)), source.getAllUris().size());
//                System.out.println("Pseudo = [" + i + ", " + j + "]\t" + f + "\t Real = " + realF);
                average = average + f;
            }
        }
        return 2 * average / ((double) m.mappings.length * (m.mappings.length - 1));
    }

    public static double getFmeasure(MappingMatrix m, Cache source) {
        double average = 0d;
        double realAverage = 0d;
        double f, realF;
        for (int i = 0; i < m.mappings.length; i++) {
            for (int j = i + 1; j < m.mappings.length; j++) {
                realF = MappingMath.computeFMeasure(Mapping.getBestOneToOneMappings(m.getMapping(i, j)), source.getAllUris().size());
                average = average + realF;
            }
        }
        return 2 * average / ((double) m.mappings.length * (m.mappings.length - 1));
    }

    public static double getPrecision(MappingMatrix m, Cache source) {
        double average = 0d;
        double realAverage = 0d;
        double f, realP;
        for (int i = 0; i < m.mappings.length; i++) {
            for (int j = i + 1; j < m.mappings.length; j++) {
                realP = MappingMath.computeFMeasure(Mapping.getBestOneToOneMappings(m.getMapping(i, j)), source.getAllUris().size());
                average = average + realP;
            }
        }
        return 2 * average / ((double) m.mappings.length * (m.mappings.length - 1));
    }

    public static double getRecall(MappingMatrix m, Cache source) {
        double average = 0d;
        double realAverage = 0d;
        double f, realR;
        for (int i = 0; i < m.mappings.length; i++) {
            for (int j = i + 1; j < m.mappings.length; j++) {
                realR = MappingMath.computeRecall(Mapping.getBestOneToOneMappings(m.getMapping(i, j)), source.getAllUris().size());
                average = average + realR;
            }
        }
        return 2 * average / ((double) m.mappings.length * (m.mappings.length - 1));
    }

    /**
     * Picks the worst link in a mapping
     *
     * @param m
     * @return Pair that yields worse link
     */
    public static Pair<String> getWorstLink(Mapping m) {
        double min = Double.MAX_VALUE;
        Pair<String> result = null;
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                //System.out.println(s + "\t" + t + "\t" + m.getSimilarity(s, t));
                double v = m.getSimilarity(s, t);
                if (v < min) {
                    result = new Pair(s, t);
                    min = v;
                }
            }
        }
        return result;
    }

    /**
     * Picks the worst link in a mapping
     *
     * @param m
     * @return Pair that yields worse link
     */
    public static Set<Pair<String>> getWorstLinks(Mapping m) {
        double min = Double.MAX_VALUE;
        Set<Pair<String>> result = null;
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                //System.out.println(s + "\t" + t + "\t" + m.getSimilarity(s, t));
                double v = m.getSimilarity(s, t);
                if (v < min) {
                    result = new HashSet<Pair<String>>();
                    result.add(new Pair(s, t));
                    min = v;
                } else if (v == min) {
                    result.add(new Pair(s, t));
                }
            }
        }
        return result;
    }

    /**
     * Returns true when a data item was fixed, else false
     *
     * @param caches Data to fix
     * @param votingMatrices Voting matrices
     * @param mappings Current link results
     * @param i Source in which data is to be corrected
     * @param j Target in which data is to be corrected
     * @return True if something was fixed, else false
     */
    public static boolean fixCaches(List<Cache> caches, MappingMatrix votingMatrices, MappingMatrix mappings, int i, int j, Pair<String> worstLink) {
        //find reason for the link
        int reasonSource = i;
        int reasonTarget = j;
        String keyReason = null;
        double min = mappings.getMapping(i, j).getSimilarity(worstLink.a, worstLink.b);
	

        for (int k = 0; k < caches.size(); k++) {
            if (i != k && k != j) {
                if (mappings.getMapping(i, k).map.containsKey(worstLink.a)) {
                    Set<String> keys = mappings.getMapping(i, k).map.get(worstLink.a).keySet();
                    for (String key : keys) {
                        double v = mappings.getMapping(i, k).getSimilarity(worstLink.a, key) * mappings.getMapping(k, j).getSimilarity(key, worstLink.b);
                        if (v < min) {
                            if (mappings.getMapping(i, k).getSimilarity(worstLink.a, key) < mappings.getMapping(k, j).getSimilarity(key, worstLink.b)) {
                                reasonSource = i;
                                reasonTarget = k;
                            } else {
                                reasonSource = k;
                                reasonTarget = j;
                            }
                            keyReason = key;
                            min = v;
                        }
                    }
                } else //found a missing entry in the mapping
                {
                    min = 0;
                    reasonSource = i;
                    reasonTarget = k;
                    keyReason = worstLink.a;
//                    System.out.println("Missing link between " + worstLink.a + " and " + worstLink.b + " in (" + reasonSource + ", " + reasonTarget + ")");
                    break;
                }
            }
        }

        if (min == 1d) {
            return false;
        }
//        System.out.println("Fixing " + worstLink + " due to reason " + keyReason + " with (source, target) = (" + reasonSource + ", " + reasonTarget + ")");
//        System.out.println("Broken link source = " + caches.get(reasonSource).getInstance(worstLink.a));
//        System.out.println("Broken link target = " + caches.get(reasonTarget).getInstance(worstLink.b));
        double sim1 = getAverageSimilarity(mappings, reasonSource, worstLink.a, true);
        double sim2 = getAverageSimilarity(mappings, reasonTarget, worstLink.b, false);


        if (sim1 > sim2) //a is better than b, then replace b by a
        {
//            System.out.println("Broken target data is " + caches.get(reasonTarget).getInstance(worstLink.b));
            caches.get(reasonTarget).replaceInstance(worstLink.b, caches.get(reasonSource).getInstance(worstLink.a));
        } else {
//            System.out.println("Broken source data");
            caches.get(reasonSource).replaceInstance(worstLink.a, caches.get(reasonTarget).getInstance(worstLink.b));
        }
        return true;
    }

    public static boolean fixCaches(List<Cache> caches, MappingMatrix votingMatrices, MappingMatrix mappings) {
        double min = 1d;
        int source = 0, target = 1;
        Pair<String> worst = null;
        for (int i = 0; i < caches.size(); i++) {
            for (int j = i + 1; j < caches.size(); j++) {
                Pair<String> link = getWorstLink(votingMatrices.getMapping(i, j));
                double v = votingMatrices.getMapping(i, j).getSimilarity(link.a, link.b);
                if (v < min) {
                    worst = link;
                    min = v;
                    source = i;
                    target = j;
                }
            }
        }
//        System.out.println("Fixing link " + worst + " between " + source + " and " + target);
        return fixCaches(caches, votingMatrices, mappings, source, target, worst);
    }

    public boolean fixCachesInBatch(List<Cache> caches, MappingMatrix votingMatrices, MappingMatrix mappings) {
        double min = 1d;
        int source = 0, target = 1;
        List<Pair<String>> worst = null;
        for (int i = 0; i < caches.size(); i++) {
            for (int j = i + 1; j < caches.size(); j++) {
                List<Pair<String>> links = new ArrayList<Pair<String>>(getWorstLinks(votingMatrices.getMapping(i, j)));
                double v = votingMatrices.getMapping(i, j).getSimilarity(links.get(0).a, links.get(0).b);
                if (v < min) {
                    worst = links;
                    min = v;
                    source = i;
                    target = j;
                }
            }
        }
        if (worst != null) {
            resultBuffer = resultBuffer + worst.size() + "\n";
            for (Pair<String> worstLink : worst) {
//                System.out.println("Fixing link " + worstLink + " between " + source + " and " + target);
                fixCaches(caches, votingMatrices, mappings, source, target, worstLink);
            }
            return true;
        } else {
            resultBuffer = resultBuffer + "0\n";
            return false;
        }
    }

    public static double getAverageSimilarity(MappingMatrix matrix, String uri, boolean asSource) {
        Mapping m;
        double value = 0;
        double count = 0;
        for (int i = 0; i < matrix.mappings.length; i++) {
            for (int j = i + 1; j < matrix.mappings.length; j++) {
                m = matrix.getMapping(i, j);
                if (!asSource) {
                    m = m.reverseSourceTarget();
                }
                Set<String> keys = m.map.get(uri).keySet();
                for (String key : keys) {
                    value = value + m.getSimilarity(uri, key);
                    count++;
                }
            }
        }
        return value / count;
    }

    public static double getAverageSimilarity(MappingMatrix matrix, int index, String uri, boolean asSource) {
        Mapping m;
        double value = 0;

        for (int j = 0; j < matrix.mappings.length; j++) {
            if (index != j) {
                m = matrix.getMapping(index, j);
                if (!asSource) {
                    m = m.reverseSourceTarget();
                }
                if (m.map.containsKey(uri)) {
                    Set<String> keys = m.map.get(uri).keySet();
                    for (String key : keys) {
                        value = value + m.getSimilarity(uri, key);
                    }
                }
            }
        }
        return value / (matrix.mappings.length - 1);
    }

    void multiLinkDataset(List<Cache> caches, int iterations, boolean batch, String logfile) throws IOException {

        if (!logfile.isEmpty()) {
            File file = new File(logfile);
            file.delete();
            PrintStream fileOut = new PrintStream(new FileOutputStream(logfile));
            System.setOut(fileOut);
        }
        multiLinkDataset(caches, iterations, batch);
    }

    public void multiLinkDataset(List<Cache> caches, int iterations, boolean batch) {
        double oldF, newF = 0d;
        double oldTime, newTime = System.currentTimeMillis();
        resultBuffer = resultBuffer + "Pseudo\tPrecision\tRecall\tF-Measure\tError rate\tRuntime\tLinks\n";

        for (int i = 0; i < iterations; i++) {
            MappingMatrix matrix = getMappings(caches);
            oldF = newF;
            oldTime = newTime;
            newTime = System.currentTimeMillis();
            newF = getPseudoFmeasure(matrix, caches.get(0));
//            resultBuffer = resultBuffer + "Pseudo:\t" + newF + "\treal:\t"
//                    + getFmeasure(matrix, caches.get(0)) + "\t Error rate:\t" + getErrorRate(caches) + "\tRuntime:\t" + (newTime - oldTime);
            double p = getPrecision(matrix, caches.get(0));
            double r = getRecall(matrix, caches.get(0));
            double f = 2*p*r/(p+r);
            resultBuffer = resultBuffer + newF + "\t" + p + "\t" + r + "\t" + f + "\t" + getErrorRate(caches) + "\t" + (newTime - oldTime) + "\t";
            System.out.println("**" + resultBuffer);
            if (newF - oldF <= 0) {
                break;
            }
            MappingMatrix votingMatrix = computeVotingMatrices(matrix);
            if (batch) {
                fixCachesInBatch(caches, votingMatrix, matrix);
            } else {
                fixCaches(caches, votingMatrix, matrix);
            }
        }
        resultBuffer = resultBuffer + "\n";
    }

    void multiLinkToyData() throws IOException {
        List<Cache> caches = new ArrayList<Cache>();
        for (int i = 0; i < 5; i++) {
            caches.add(ToyData.generateToyData(i));
        }
        multiLinkDataset(caches, 10, true);
//		multiLinkDataset(caches);
    }

    static boolean includes(Instance a, Instance b) {
        Set<String> properties = b.getAllProperties();
        for (String p : properties) {
            Set<String> values = b.getProperty(p);
            Set<String> aValues = a.getProperty(p);
            for (String v : values) {
                if (!aValues.contains(v)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean equivalent(Instance a, Instance b) {
        return includes(a, b) && includes(b, a);
    }

    void multiLinkPeel() throws IOException {
        String datasetPath = "E:/Work/Data/Peel/musicDatasets/";
        String peelSpec = datasetPath + "peelSpecs.xml";
        String[] datasetFiles = {"peel_0.ttl", "peel_1.ttl", "peel_2.ttl", "peel_3.ttl", "peel_4.ttl"};

        // fill caches
        List<Cache> caches = new ArrayList<Cache>();
        ConfigReader cR = new ConfigReader();
        cR.validateAndRead(peelSpec);
        for (int i = 0; i < datasetFiles.length; i++) {
            cR.sourceInfo.endpoint = datasetPath + datasetFiles[i];
            cR.sourceInfo.id = datasetFiles[i].substring(0, datasetFiles[i].lastIndexOf("."));
            caches.add(HybridCache.getData(cR.getSourceInfo()));
        }

        multiLinkDataset(caches, 10, false, "E:/Work/Data/Peel/musicDatasets/PeelMultiLinkerLog.txt");
//		multiLinkDataset(caches);
    }

    public static void main(String args[]) throws IOException {
        System.out.println(permute("John Smith Paul Delagarde", 0.5));
        MultiLinker ml = new MultiLinker();
        ml.multiLinkToyData();
//		ml.multiLinkPeel();

    }
    ////     Cache c = ToyData.generateToyData();
    ////     System.out.println(getRandomElement(c.getAllProperties()));
    //        List<Cache> caches = new ArrayList<Cache>();
    //        for (int i = 0; i < 5; i++) {
    ////            caches.add(alterCache(ToyData.generateToyData(), 0.5, 0.3));
    //            caches.add(ToyData.generateToyData(i));
    //        }
    //
    //        for (int i = 0; i < 10; i++) {
    //            MappingMatrix matrix = getMappings(caches);
    //            System.out.println("==>" + getPseudoFmeasure(matrix, caches.get(0)));
    //
    ////            System.out.println("M = " + matrix.getMapping(1, 2));
    //            MappingMatrix votingMatrix = computeVotingMatrices(matrix);
    ////            System.out.println("VM = " + votingMatrix.getMapping(1, 2));
    ////        System.out.println(votingMatrix.getMapping(0,1));
    //
    //            //worse link
    //
    //
    //            //matrix iteration
    ////            matrix = computeVotingMatrices(matrix);
    ////            System.out.println("New matrix = "+matrix.getSubMatrix(0.5));
    ////            System.out.println("==>" + getPseudoFmeasure(matrix, caches.get(0)));
    //
    ////            Pair<String> worst = getWorstLink(votingMatrix.getMapping(1, 2));
    ////            System.out.println("Worst = " + worst + "\n" + caches.get(1).getInstance(worst.a) + "\n" + caches.get(2).getInstance(worst.b));
    //            boolean fixed = fixCaches(caches, votingMatrix, matrix);
    ////            if(!fixed) break;
    ////            System.out.println("Fixed link\n" + caches.get(1).getInstance(worst.a) + "\n" + caches.get(2).getInstance(worst.b));
    //        }
    //    }
}
