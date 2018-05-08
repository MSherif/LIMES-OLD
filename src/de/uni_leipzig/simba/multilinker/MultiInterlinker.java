/**
 *
 */
package de.uni_leipzig.simba.multilinker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/*import org.apache.commons.collections15.multimap.MultiHashMap;
*/import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.FileManager;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.controller.PPJoinController;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.genetics.learner.GeneticActiveLearner;
import de.uni_leipzig.simba.genetics.learner.LinkSpecificationLearner;
import de.uni_leipzig.simba.genetics.learner.LinkSpecificationLearnerFactory;
import de.uni_leipzig.simba.genetics.learner.SupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.learner.UnSupervisedLearnerParameters;
import de.uni_leipzig.simba.genetics.selfconfig.BasicGeneticSelfConfigurator;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.learning.oracle.oracle.Oracle;
import de.uni_leipzig.simba.learning.oracle.oracle.SimpleOracle;
import de.uni_leipzig.simba.selfconfig.*;

/**
 * @author sherif
 * DEPRECATED. Please use MultiLinker
 */
public class MultiInterlinker {

    static Logger logger = Logger.getLogger("LIMES");
    protected PPJoinController ppJoinController;
    protected BasicGeneticSelfConfigurator bgsConfiger;
    static long datasetSize;
    Map<String, Double> results = new TreeMap<String, Double>();
    Mapping optimalMap = new Mapping();
    //	Map<KBInfo, Cache> KB2Cache = new HashMap<KBInfo, Cache>();
    static Map<String, Cache> KB2Cache = new HashMap<String, Cache>();
    double epsilon = 0d;

    /**
     * @return the ppJoinController
     */
    public PPJoinController getPpJoinController() {
        return ppJoinController;
    }

    /**
     * @param ppJoinController the ppJoinController to set
     */
    public void setPpJoinController(PPJoinController ppJoinController) {
        this.ppJoinController = ppJoinController;
    }

    public Mapping getUnsupervisedMappings(String fileName, String sourceDatasetFile, String targetDatasetFile) {
        Mapping map = new Mapping();
        if (new File(fileName).exists()) {

           

            ConfigReader cR = new ConfigReader();
            cR.validateAndRead(fileName);
            cR.sourceInfo.endpoint = sourceDatasetFile;
            cR.sourceInfo.id = sourceDatasetFile.substring(sourceDatasetFile.lastIndexOf("/") + 1, sourceDatasetFile.lastIndexOf("."));
            cR.targetInfo.endpoint = targetDatasetFile;
            cR.targetInfo.id = targetDatasetFile.substring(targetDatasetFile.lastIndexOf("/") + 1, targetDatasetFile.lastIndexOf("."));

//            params.put("sourceInfo", cR.sourceInfo);
//            params.put("targetInfo", cR.targetInfo);
            System.out.println("Source: " + cR.sourceInfo);
            System.out.println("Target: " + cR.targetInfo);

            Cache sourceCash = HybridCache.getData(cR.getSourceInfo());
//            Cache targetCash = HybridCache.getData(cR.getTargetInfo());
//
//
//            params.put("sourceCache", sourceCash);
//            params.put("targetCache", targetCash);

            PropertyMapping pM = new PropertyMapping();
            for (String sourceProperty : cR.getSourceInfo().properties) {
                for (String targetProperty : cR.getTargetInfo().properties) {
                    pM.addStringPropertyMatch(sourceProperty, targetProperty);
                }
            }
            UnSupervisedLearnerParameters params = new UnSupervisedLearnerParameters(cR, pM);
            params.setPropertyMapping(pM);
            params.setPFMBetaValue(1.0d);
            params.setGenerations(3);
            params.setPopulationSize(5);
            params.setCrossoverRate(0.4f);
            params.setMutationRate(0.4f);
            params.setPseudoFMeasure(new PseudoMeasures());

            bgsConfiger = new BasicGeneticSelfConfigurator();
            datasetSize = sourceCash.getAllInstances().size();
            try {
                Metric m = bgsConfiger.learn(params);
                map = bgsConfiger.getMapping();
                //					System.out.println(map);http://www.java2s.com/Code/Jar/c/Downloadcollectionsgeneric401srcjar.htm
                System.out.println("Source ID:         " + cR.getSourceInfo().id);
                System.out.println("Target ID:         " + cR.getTargetInfo().id);
                System.out.println("Metric:            " + m);
                System.out.println("Total Maping size: " + map.getNumberofMappings() + " (map.getNumberofMappings() how come that this no bigger than the cash size!!!)");
                System.out.println("Optimal map size:  " + datasetSize + " ( sourceCash.getAllInstances().size() )");
                System.out.println("mapping Accuricy:  " + MappingMath.computePrecision(map, datasetSize));

            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            logger.fatal("Input file " + fileName + " does not exist.");
            System.exit(1);
        }
        return map;
    }

    public static Mapping getDeterministicUnsupervisedMappings(String fileName, String sourceDatasetFile, String targetDatasetFile) {
        Mapping map = new Mapping();
        if (new File(fileName).exists()) {

            HashMap<String, Object> params = new HashMap<String, Object>();

            ConfigReader cR = new ConfigReader();
            cR.validateAndRead(fileName);
//            cR.sourceInfo.endpoint = sourceDatasetFile;
            cR.sourceInfo.id = sourceDatasetFile.substring(sourceDatasetFile.lastIndexOf("/") + 1, sourceDatasetFile.lastIndexOf("."));
//            cR.targetInfo.endpoint = targetDatasetFile;
            cR.targetInfo.id = targetDatasetFile.substring(targetDatasetFile.lastIndexOf("/") + 1, targetDatasetFile.lastIndexOf("."));

            params.put("sourceInfo", cR.sourceInfo);
            params.put("targetInfo", cR.targetInfo);
            System.out.println("Source: " + cR.sourceInfo);
            System.out.println("Target: " + cR.targetInfo);

            //Check if the caches loaded before
            Cache sourceCache, targetCache;
            if (KB2Cache.containsKey(cR.getSourceInfo())) {
                sourceCache = KB2Cache.get(cR.getSourceInfo());
            } else {
                sourceCache = HybridCache.getData(cR.getSourceInfo());
                //				KB2Cache.put(cR.getSourceInfo(), sourceCache);
                KB2Cache.put(cR.sourceInfo.id, sourceCache);
            }
            if (KB2Cache.containsKey(cR.getTargetInfo())) {
                targetCache = KB2Cache.get(cR.getTargetInfo());
            } else {
                targetCache = HybridCache.getData(cR.getTargetInfo());
                //				KB2Cache.put(cR.getTargetInfo(), targetCache);
                KB2Cache.put(cR.targetInfo.id, targetCache);
            }
            datasetSize = sourceCache.getAllInstances().size();

            //			TreeSet<String> values = sourceCache.getInstance("sw").getProperty(RDFS.label.toString());
            //			System.out.println(values);
            //
            //
            //			//			values.retainAll(new TreeSet<String>());values.add()
            //			System.exit(1);

            //Classifiers
            MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);
            //			MeshBasedSelfConfigurator bsc = new LinearMeshSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);
            //			MeshBasedSelfConfigurator bsc = new DisjunctiveMeshSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);

            //Pseudo-F-measu
            //			bsc.setMeasure("reference");
            bsc.setMeasure("own");

            //List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
            List<SimpleClassifier> cp = new ArrayList<SimpleClassifier>();
            for (String property : cR.sourceInfo.properties) {
//                cp.add(new SimpleClassifier("jaccard", 1.0, property, property));
                cp.add(new SimpleClassifier("levenshtein", 1.0, property, property));
                cp.add(new SimpleClassifier("trigrams", 1.0, property, property));
            }

            ComplexClassifier cc = bsc.getZoomedHillTop(5, 2, cp);
            logger.info("Mapping size is " + cc.mapping.getNumberofMappings());
            map = cc.mapping;
            //            logger.info("Precision is "+computePrecision(map, source.getAllInstances().size()));
            //            System.out.println(map);
            //            System.exit(1);
        } else {
            logger.fatal("Input file " + fileName + " does not exist.");
            System.exit(1);
        }
        return map;
    }

    public Mapping getDeterministicUnsupervisedMappings(Cache sourceCache, Cache targetCache) {
        Mapping map = new Mapping();
        datasetSize = sourceCache.getAllInstances().size();

        //Classifiers
        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);
        //			MeshBasedSelfConfigurator bsc = new LinearMeshSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);
        //			MeshBasedSelfConfigurator bsc = new DisjunctiveMeshSelfConfigurator(sourceCache, targetCache, 0.6, 1.0);

        //Pseudo-F-measu
        //			bsc.setMeasure("reference");
        bsc.setMeasure("own");

        List<SimpleClassifier> cp = new ArrayList<SimpleClassifier>();
        for (String property : sourceCache.getAllProperties()) {
            //cp.add(new SimpleClassifier("jaccard", 1.0, property, property));
            cp.add(new SimpleClassifier("levenshtein", 1.0, property, property));
            cp.add(new SimpleClassifier("trigrams", 1.0, property, property));
        }

        ComplexClassifier cc = bsc.getZoomedHillTop(5, 5, cp);
        logger.info("Mapping size is " + cc.mapping.getNumberofMappings());
        map = cc.mapping;
        return map;
    }

    public Model loadModel(String fileNameOrUri) {
        Model model = ModelFactory.createDefaultModel();
        java.io.InputStream in = FileManager.get().open(fileNameOrUri);
        if (in == null) {
            throw new IllegalArgumentException(
                    "File/URI: " + fileNameOrUri + " not found");
        }
        if (fileNameOrUri.endsWith(".ttl")) {
            System.out.println("Opening Turtle file");
            model.read(in, null, "TTL");
        } else if (fileNameOrUri.endsWith(".rdf")) {
            System.out.println("Opening RDFXML file");
            model.read(in, null);
        } else if (fileNameOrUri.endsWith(".nt")) {
            System.out.println("Opening N-Triples file");
            model.read(in, null, "N-TRIPLE");
        } else {
            System.out.println("Content negotiation to get RDFXML from " + fileNameOrUri);
            model.read(fileNameOrUri);
        }

        System.out.println("loading " + fileNameOrUri + " is done!!");
        System.out.println();
        return model;
    }

    /**
     * @author Sherif take one File (baseFileName), to get set of benchmarked
     * files with different destruction values by destroying the set of
     * properties to the specified destructionValue
     * @throws IOException
     */
    @Test
    //Test dataset contains only 10 triples
    public void getPeelTestBenchmarks() throws IOException {
        String peelFile = "/media/lod2/datasetsMapping/Musik/peelTest.ttl";

        List<Property> props = new ArrayList<Property>();
        props.add(ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"));
        props.add(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/name"));
        props.add(ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"));

        List<Double> destructionValues = new ArrayList<Double>();
        destructionValues.add(0.25);
        destructionValues.add(0.5);

        //		System.out.println(BenchmarkGenerator.getBenchmarkedFiles(peelFile, props, destructionValues));
    }

    //	@Test
    //	public void getPeelBenchmarks() throws IOException {
    //		String peelFile = "/mypartition2/musicDatasets/peel.ttl";
    //
    //		List<Property> props = new ArrayList<Property>();
    //		props.add(ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"));
    //		props.add(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/name"));
    //		props.add(ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"));
    //
    //		List<Double> destructionValues = new ArrayList<Double>();
    //		destructionValues.add(0.1);
    //		destructionValues.add(0.1);
    //		destructionValues.add(0.1);
    //		destructionValues.add(0.1);
    //		//		System.out.println(BenchmarkGenerator.getBenchmarkedFiles(peelFile, props, destructionValues));
    //	}
    @Test
    //the complete PEEL dataset
    public void old_and_running_multilinkPeel(String datasetPath) {

        // can be generated on fly using getPeelTestBenchmarks()
        //        String datasetPath = "E:/Work/Data/Peel/musicDatasets/";
        String peelSpec = datasetPath + "peelSpecs.xml";
        String[] datasetFiles = {"peel.ttl", "peel_1.ttl", "peel_2.ttl", "peel_3.ttl", "peel_4.ttl"};

        for (int i = 0; i < datasetFiles.length; i++) {
            datasetFiles[i] = datasetPath.concat(datasetFiles[i]);
        }

        //0. Generate the optimal mapping
        //		System.out.println("--------------------- 0.Generating the optimal mapping ---------------------");
        //		Model tmpModel = loadModel(datasetFiles[0]);
        //		String queryString = "SELECT DISTINCT ?x ?v0 "
        //				+ "WHERE { "
        //				+ "?x a <http://xmlns.com/foaf/0.1/Person> . "
        //				+ "?x <http://xmlns.com/foaf/0.1/name> ?v0 . "
        //				+ "}";
        //
        //		com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);
        //		QueryExecution qexec = QueryExecutionFactory.create(query, tmpModel);
        //		try {
        //			ResultSet queryResults = qexec.execSelect();
        //			while (queryResults.hasNext()) {
        //				QuerySolution soln = queryResults.nextSolution();
        //				RDFNode uri = soln.get("?x");       // Get a result variable by name.
        //				optimalMap.add(uri.toString(), uri.toString(), 1d);
        //			}
        //		} finally {
        //			qexec.close();
        //		}

        //1. Get unsupervised mappings for all datasets M(i,j) where i!=j
        //		System.out.println("--------------------- 1.Get unsupervised mappings for all datasets M(i,j) where i!=j ---------------------");
        Mapping m[][] = new Mapping[5][5];
        for (int i = 0; i < m.length; i++) {
            for (int j = i + 1; j < m.length; j++) {
                m[i][j] = getDeterministicUnsupervisedMappings(peelSpec, datasetFiles[i], datasetFiles[j]);
                //m[i][j] = getUnsupervisedMappings(peelSpec, datasetFiles[i], datasetFiles[j]);
                m[j][i] = m[i][j].reverseSourceTarget();
                //				results.put("Precision of M[" + i + "][" + j + "]", computePrecision(m[i][j], datasetSize));
                //				results.put("Recall of M[" + i + "][" + j + "]", computeRecall(m[i][j], datasetSize));
                //				results.put("F1 of M[" + i + "][" + j + "]", computeFMeasure(m[i][j], datasetSize));
            }
        }

        boolean useScores = true;
        int sourceMapIndex = 1;
        int targetMapIndex = 2;
        long ExampleCount = Long.MAX_VALUE;
        int datasetsCount = 5; // n	
        double acceptanceThreshold = 2.39; // k where ( k < n/2 )

        results.put("P of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computePrecision(m[sourceMapIndex][targetMapIndex], datasetSize));
        results.put("R of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computeRecall(m[sourceMapIndex][targetMapIndex], datasetSize));
        results.put("F of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computeFMeasure(m[sourceMapIndex][targetMapIndex], datasetSize));

        //2. Compute voting scores matrix V(i,j)=M(i,j)+A(i,j), where A(i,j)= M(i,k)*M(k,j) for all 
        //		System.out.println("--------------------- 2.Compute voting scores matrix V(i,j)=M(i,j)+A(i,j), where A(i,j)= M(i,k)*M(k,j) for all  ---------------------");
        Mapping votingMatrix = getVotingScores(m, sourceMapIndex, targetMapIndex, useScores);
        //		System.out.println("votingMatrix: " + getMappingAccuracy(votingMatrix, datasetSize));


        //3. Get perfect mapping as the n top examples from E
        //		System.out.println("--------------------- 3.Get perfect mapping  ---------------------");

        //		Mapping posNegExamlpes = getNPosNegExamples(votingMatrix, ExampleCount, datasetsCount, acceptanceThreshold);

        //        for (int i = 1; i < 5; i++) {
        //		Map<Float, Double> test = new TreeMap<Float, Double>();
        double bestF = 0d, bestKF = 0, bestP = 0d, bestKP = 0, bestR = 0d, bestKR = 0d;
        for (float i = 1; i < 5; i += 0.1) {
            Mapping posExamples = getPosExamples(votingMatrix, ExampleCount, datasetsCount, i);
            //            posExamples = Mapping.getBestOneToOneMappings(posExamples);
            double p = MappingMath.computePrecision(posExamples, datasetSize);
            results.put("Precision of votingMatrix with k=" + i, p);
            if (p > bestP) {
                bestP = p;
                bestKP = i;
            }
            double r = MappingMath.computeRecall(posExamples, datasetSize);
            results.put("Recall of votingMatrix with k=" + i, r);
            if (r > bestR) {
                bestR = r;
                bestKR = i;
            }
            double f = MappingMath.computeFMeasure(posExamples, datasetSize);
            results.put("F1 of votingMatrix with k=" + i, f);
            if (f > bestF) {
                bestF = f;
                bestKF = i;
            }
            //			test.put(i, computeFMeasure(posExamples, datasetSize));
        }
        System.out.println(results);
        //		System.out.println(test);
        System.out.println();
        System.out.println("Best F = " + bestF + " at k = " + bestKF);
        System.out.println("Best P = " + bestP + " at k = " + bestKP);
        System.out.println("Best R = " + bestR + " at k = " + bestKR);
        System.exit(1);

        //		//4. Start supervised GenaticActiveLearner using perfect mapping as positive examples
        //		System.out.println("--------------------- 4.Start supervised GenaticActiveLearner using perfect mapping as positive examples  ---------------------");
        //		Mapping GeneticActiveLearnerMapping = getGeneticActiveLearner(peelSpec, datasetFiles[sourceMapIndex], datasetFiles[targetMapIndex], posNegExamlpes);
        //		results.put("P of final mapping", computePrecision(GeneticActiveLearnerMapping, datasetSize));
        //		results.put("R of final mapping", computeRecall(GeneticActiveLearnerMapping, datasetSize));
        //		results.put("F of final mapping", computeFMeasure(GeneticActiveLearnerMapping, datasetSize));
        //		//		//4. Start supervised GenaticBatchLearner using perfect mapping as positive examples
        //		//		Mapping GeneticBatchLearnerMapping=getGeneticBatchLearner(peelSpec, datasetFiles[sourceMapIndex], datasetFiles[targetMapIndex] , posNegExamlpes);
        //		//		results.put("GeneticBatchLearner final mapping", getMappingAccuracy(GeneticBatchLearnerMapping, datasetSize));
    }

    /**
     * @param votingMap
     * @return The positive and negative examples from votingMap with size =
     * exampleCount as the mapping with highest and lowest similarity values
     * respectively
     * @author Sherif
     */
    protected Mapping getNPosNegExamples(Mapping votingMap, int exampleCount) {
        if (votingMap.size() < exampleCount) {
            exampleCount = votingMap.size() / 2;
        }
        int posExCount = exampleCount;
        int negExCount = exampleCount;
        Mapping result = new Mapping();
/*        MultiHashMap<Double, Map<String, String>> errMap = new MultiHashMap<Double, Map<String, String>>();
*/        // Sort the map 
        for (String mapSourceUri : votingMap.map.keySet()) {
            for (String mapTargetUri : votingMap.map.get(mapSourceUri).keySet()) {
                Map<String, String> value = new HashMap<String, String>();
                value.put(mapSourceUri, mapTargetUri);
                Double key = votingMap.getSimilarity(mapSourceUri, mapTargetUri);
               /* errMap.put(key, value);*/
            }
        }
      /*  Mapping negExMap = getNNegExamples(errMap, negExCount);
        Mapping posExMap = getNPosExamples(errMap, posExCount);
        result.map.putAll(posExMap.map);
        result.map.putAll(negExMap.map);
        result.size = posExMap.map.size() + negExMap.map.size();*/
        return result;
    }

    /**
     * @param votingMap
     * @param negExCount
     * @return the negExCount lowest mapping from votingMap
     * @author sherif
     */
    /*protected Mapping getNNegExamples(MultiHashMap<Double, Map<String, String>> votingMap, int negExCount) {
        Mapping result = new Mapping();
        SortedSet<Double> sortedErrValues = new TreeSet<Double>(votingMap.keySet());
        for (double errValue : sortedErrValues) {
            System.out.println(errValue);
            Collection<Map<String, String>> m = votingMap.get(errValue);
            for (Map<String, String> row : m) {
                for (String sourceURI : row.keySet()) {
                    String targetURI = row.get(sourceURI);
                    result.add(sourceURI, targetURI, 0d);
                    negExCount--;
                    if (negExCount == 0) {
                        return result;
                    }
                }
            }
        }
        return result;
    }*/

    /**
     * @param votingMap
     * @param negExCount
     * @return the negExCount highest mapping from votingMap
     * @author Sherif
     */
   /* protected Mapping getNPosExamples(MultiHashMap<Double, Map<String, String>> votingMap, int negExCount) {
        Mapping result = new Mapping();
        //		SortedSet<Double> sortedErrValues = new TreeSet<Double>(errMap.keySet());
        SortedSet<Double> sortedErrValues = new TreeSet<Double>(Collections.reverseOrder());
        sortedErrValues.addAll(votingMap.keySet());
        for (double errValue : sortedErrValues) {
            System.out.println(errValue);
            Collection<Map<String, String>> m = votingMap.get(errValue);
            for (Map<String, String> row : m) {
                for (String sourceURI : row.keySet()) {
                    String targetURI = row.get(sourceURI);
                    result.add(sourceURI, targetURI, 1d);
                    negExCount--;
                    if (negExCount == 0) {
                        return result;
                    }
                }
            }
        }
        return result;
    }
*/
    /**
     * @param votingMap
     * @param exampleCount: the max example count
     * @param k: acceptanceThreshold
     * @param n: no. of datasets
     * @return Positive examples as all links upon which (n-k) agree with k
     * between 0 and n/2 and negative examples as links that are suggested by at
     * most k+1
     * @author Sherif
     */
    protected Mapping getNPosNegExamples(Mapping votingMap, long exampleCount, int n, double k) {
        long posExCount = exampleCount;
        long negExCount = exampleCount;
        Mapping result = new Mapping();
        for (String mapSourceUri : votingMap.map.keySet()) {
            for (String mapTargetUri : votingMap.map.get(mapSourceUri).keySet()) {
                Double sim = votingMap.getSimilarity(mapSourceUri, mapTargetUri);
                if (posExCount > 0 && sim >= (n - k)) {
                    result.add(mapSourceUri, mapTargetUri, 1d);
                    posExCount--;
                } else if (negExCount > 0 && sim <= (k + 1)) {
                    result.add(mapSourceUri, mapTargetUri, 0d);
                    negExCount--;
                }
                if (posExCount == 0 && negExCount == 0) {
                    return result;
                }
            }
        }
        return result;
    }

    protected Mapping getPosExamples(Mapping votingMap, long exampleCount, int n, double k) {

        Mapping result = new Mapping();
        for (String mapSourceUri : votingMap.map.keySet()) {
            for (String mapTargetUri : votingMap.map.get(mapSourceUri).keySet()) {
                Double sim = votingMap.getSimilarity(mapSourceUri, mapTargetUri);
                if (sim >= (n - k)) {
                    result.add(mapSourceUri, mapTargetUri, 1d);
                }
            }
        }
        return result;
    }

    public Mapping _getGeneticActiveLearner(String configFile, String sourceDatasetFile, String targetDatasetFile, Mapping unsupervisedResultMapping) {
        Mapping resultMapping = new Mapping();
        System.out.println("\n Mapping = " + unsupervisedResultMapping);
        Mapping posMap = new Mapping();
        for (String s : unsupervisedResultMapping.map.keySet()) {
            for (String t : unsupervisedResultMapping.map.get(s).keySet()) {
                if (unsupervisedResultMapping.getSimilarity(s, t) == 1) {
                    posMap.add(s, t, 1d);
                }
            }
        }


        Oracle o = new SimpleOracle();
        o.loadData(posMap);
        int size = 10;

        //Read pacifications from the configuration file except for target and destination IDs and end points
        ConfigReader cR = new ConfigReader();

        cR.validateAndRead(configFile);
        //Overwrite target and destination IDs and end points
        cR.sourceInfo.endpoint = sourceDatasetFile;
        // Name the cR.sourceInfo.id after the name of the source file with neither path nor extension
        cR.sourceInfo.id = sourceDatasetFile.substring(
                sourceDatasetFile.lastIndexOf("/") >= 0 ? sourceDatasetFile.lastIndexOf("/") + 1 : 0,
                sourceDatasetFile.lastIndexOf(".") >= 0 ? sourceDatasetFile.lastIndexOf(".") : sourceDatasetFile.length() - 1);
        cR.targetInfo.endpoint = targetDatasetFile;
        // Name the cR.targetInfo.id after the name of the target file with neither path nor extension
        cR.targetInfo.id = targetDatasetFile.substring(
                targetDatasetFile.lastIndexOf("/") >= 0 ? targetDatasetFile.lastIndexOf("/") + 1 : 0,
                targetDatasetFile.lastIndexOf(".") >= 0 ? targetDatasetFile.lastIndexOf(".") : targetDatasetFile.length() - 1);

        //		PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);
        ////////////////////////////
        PropertyMapping propMap = new PropertyMapping();

        int max = Math.max(cR.sourceInfo.properties.size(), cR.targetInfo.properties.size());
        for (int i = 0; i < max; i++) {
            propMap.addStringPropertyMatch(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i));
        }
        System.out.println("propMap: " + propMap);
        /////////////////////////

        //		GeneticActiveLearner learner = new GeneticActiveLearner();
        LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.ACTIVE_LEARNER);


        // params for the learner
//        HashMap<String, Object> param = new HashMap<String, Object>();
//        param.put("populationSize", 20);
//        param.put("generations", 100);
//        param.put("mutationRate", 0.5f);
//        param.put("preserveFittest", false);
//        param.put("propertyMapping", propMap);
//        param.put("trainingDataSize", 100);
//        param.put("granularity", 2);
//        param.put("config", cR);
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(20);
	      params.setGenerations(100);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(false);
	      params.setTrainingDataSize(100);
	      params.setGranularity(2);
        propMap.numberPropPairs = new ArrayList<Pair<String>>();
        propMap.sourceNumberProps = new HashSet<String>();
        propMap.targetNumberProps = new HashSet<String>();

        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        // initial data
        answer = learner.learn(new Mapping());
        Mapping oracleAnswer = new Mapping();

        // looking for answers from oracle aka user
        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                if (o.ask(e1.getKey(), e2.getKey())) {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                } else {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                }
            }
        }

        for (int cycle = 0; cycle < 10; cycle++) {
            System.out.println("Performing learning cycle " + cycle);
            // learn
            answer = learner.learn(oracleAnswer);

            // get best solution so far:
            answerMetric = learner.terminate();
            if (answerMetric.isValid()) {
                resultMapping = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
                System.out.println("Cycle " + cycle + " Accuracy = " + MappingMath.computePrecision(resultMapping, datasetSize));
                results.put("Cycle(" + cycle + ") Metric: " + answerMetric.toString() + " Accuracy: ", MappingMath.computePrecision(resultMapping, datasetSize));
            } else {
                Logger.getLogger("Limes").warn("Method returned no valid metric!");
            }
            System.out.println("Gathering more data from user ... ");
            oracleAnswer = new Mapping();
            for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
                for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                    if (o.ask(e1.getKey(), e2.getKey())) {
                        oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                    } else {
                        oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                    }
                }
            }
        }

        return resultMapping;
    }

    public Mapping getGeneticActiveLearner(String configFile, String sourceDatasetFile, String targetDatasetFile, Mapping unsupervisedResultMapping) {
        int cyclesCount = 10;

        Mapping resultMapping = new Mapping();
        System.out.println("\n Mapping = " + unsupervisedResultMapping);

        //Read pacifications from the configuration file except for target and destination IDs and end points
        ConfigReader cR = new ConfigReader();

        cR.validateAndRead(configFile);
        //Overwrite target and destination IDs and end points
        cR.sourceInfo.endpoint = sourceDatasetFile;
        // Name the cR.sourceInfo.id after the name of the source file with neither path nor extension
        cR.sourceInfo.id = sourceDatasetFile.substring(
                sourceDatasetFile.lastIndexOf("/") >= 0 ? sourceDatasetFile.lastIndexOf("/") + 1 : 0,
                sourceDatasetFile.lastIndexOf(".") >= 0 ? sourceDatasetFile.lastIndexOf(".") : sourceDatasetFile.length() - 1);
        cR.targetInfo.endpoint = targetDatasetFile;
        // Name the cR.targetInfo.id after the name of the target file with neither path nor extension
        cR.targetInfo.id = targetDatasetFile.substring(
                targetDatasetFile.lastIndexOf("/") >= 0 ? targetDatasetFile.lastIndexOf("/") + 1 : 0,
                targetDatasetFile.lastIndexOf(".") >= 0 ? targetDatasetFile.lastIndexOf(".") : targetDatasetFile.length() - 1);

        //		PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);
        ////////////////////////////
        PropertyMapping propMap = new PropertyMapping();

        int max = Math.max(cR.sourceInfo.properties.size(), cR.targetInfo.properties.size());
        for (int i = 0; i < max; i++) {
            propMap.addStringPropertyMatch(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i));
        }
        System.out.println("propMap: " + propMap);
        /////////////////////////

        GeneticActiveLearner learner = new GeneticActiveLearner();

        // params for the learner
//        HashMap<String, Object> param = new HashMap<String, Object>();
//        param.put("populationSize", 20);
//        param.put("generations", 10);
//        param.put("mutationRate", 0.5f);
//        param.put("preserveFittest", false);
//        param.put("propertyMapping", propMap);
//        param.put("trainingDataSize", 100);
//        param.put("granularity", 2);
//        param.put("config", cR);
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(20);
	      params.setGenerations(10);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(false);
	      params.setTrainingDataSize(100);
	      params.setGranularity(2);
        propMap.numberPropPairs = new ArrayList<Pair<String>>();
        propMap.sourceNumberProps = new HashSet<String>();
        propMap.targetNumberProps = new HashSet<String>();

        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        // initial data
        //		answer = learner.learn(new Mapping());

        for (int cycle = 0; cycle < cyclesCount; cycle++) {
            System.out.println("********************************");
            System.out.println("Performing learning cycle ( " + cycle + " )");
            System.out.println("********************************");
            // learn
            answer = learner.learn(unsupervisedResultMapping);

            // get best solution so far:
            answerMetric = learner.terminate();
            if (answerMetric.isValid()) {
                resultMapping = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
                System.out.println("Cycle " + cycle + " Accuracy = " + MappingMath.computePrecision(resultMapping, datasetSize));
                results.put("Cycle(" + cycle + ")", MappingMath.computePrecision(resultMapping, datasetSize));
                results.put("Cycle(" + cycle + ") Mitric: " + answerMetric.toString(), null);
                PRFCalculator prfC = new PRFCalculator();
                results.put("Cycle(" + cycle + ") F-Score", prfC.fScore(resultMapping, optimalMap));
            } else {
                Logger.getLogger("Limes").warn("Method returned no valid metric!");
            }
        }

        PRFCalculator prfC = new PRFCalculator();
        //		results.put("---------------------", prfC.computeFScore(optimalMap, optimalMap));

        return resultMapping;
    }

    public Mapping getGeneticBatchLearner(String configFile, String sourceDatasetFile, String targetDatasetFile, Mapping unsupervisedResultMapping) {
        Mapping resultMapping = new Mapping();
        System.out.println("\n Mapping = " + unsupervisedResultMapping);
        Mapping posMap = new Mapping();
        for (String s : unsupervisedResultMapping.map.keySet()) {
            for (String t : unsupervisedResultMapping.map.get(s).keySet()) {
                if (unsupervisedResultMapping.getSimilarity(s, t) == 1) {
                    posMap.add(s, t, 1d);
                }
            }
        }


        Oracle o = new SimpleOracle();
        o.loadData(posMap);
        ConfigReader cR = new ConfigReader();
        cR.validateAndRead(configFile);
        //Overwrite target and destination IDs and end points
        cR.sourceInfo.endpoint = sourceDatasetFile;
        // Name the cR.sourceInfo.id after the name of the source file with neither path nor extension
        cR.sourceInfo.id = sourceDatasetFile.substring(
                sourceDatasetFile.lastIndexOf("/") >= 0 ? sourceDatasetFile.lastIndexOf("/") + 1 : 0,
                sourceDatasetFile.lastIndexOf(".") >= 0 ? sourceDatasetFile.lastIndexOf(".") : sourceDatasetFile.length() - 1);
        cR.targetInfo.endpoint = targetDatasetFile;
        // Name the cR.targetInfo.id after the name of the target file with neither path nor extension
        cR.targetInfo.id = targetDatasetFile.substring(
                targetDatasetFile.lastIndexOf("/") >= 0 ? targetDatasetFile.lastIndexOf("/") + 1 : 0,
                targetDatasetFile.lastIndexOf(".") >= 0 ? targetDatasetFile.lastIndexOf(".") : targetDatasetFile.length() - 1);

        //		PropertyMapping propMap = PropMapper.getPropertyMapping(configFile);	
        ////////////////////////////
        PropertyMapping propMap = new PropertyMapping();

        int max = Math.max(cR.sourceInfo.properties.size(), cR.targetInfo.properties.size());
        for (int i = 0; i < max; i++) {
            propMap.addStringPropertyMatch(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i));
        }
        System.out.println("propMap: " + propMap);
        /////////////////////////

        LinkSpecificationLearner learner = LinkSpecificationLearnerFactory.getLinkSpecificationLearner(LinkSpecificationLearnerFactory.BATCH_LEARNER);
        //		GeneticBatchLearner learner = new GeneticBatchLearner();
//        HashMap<String, Object> param = new HashMap<String, Object>();
//        param.put("populationSize", 20);
//        param.put("generations", 100);
//        param.put("mutationRate", 0.5f);
//        param.put("preserveFittest", true);
//        param.put("propertyMapping", propMap);
//        param.put("trainingDataSize", 50);
//        param.put("granularity", 2);
//        param.put("config", cR);
		SupervisedLearnerParameters params = new SupervisedLearnerParameters(cR, propMap);
	      params.setPopulationSize(20);
	      params.setGenerations(100);
	      params.setMutationRate(0.5f);
	      params.setPreserveFittestIndividual(true);
	      params.setTrainingDataSize(50);
	      params.setGranularity(2);
        Mapping answer;
        Metric answerMetric;
        try {
            learner.init(cR.getSourceInfo(), cR.getTargetInfo(), params);
        } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        answer = learner.learn(new Mapping());
        Mapping oracleAnswer = new Mapping();

        for (Entry<String, HashMap<String, Double>> e1 : answer.map.entrySet()) {
            for (Entry<String, Double> e2 : e1.getValue().entrySet()) {
                if (o.ask(e1.getKey(), e2.getKey())) {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 1d);
                } else {
                    oracleAnswer.add(e1.getKey(), e2.getKey(), 0d);
                }
            }
        }
        //	System.out.println(oracleAnswer);
        for (int cycle = 0; cycle < 10; cycle++) {
            System.out.println("");
            learner.learn(oracleAnswer);
            logger.info("Learned Cycle " + cycle + " now terminating...");
            answerMetric = learner.terminate();
            PRFCalculator prfC = new PRFCalculator();
            resultMapping = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
            //			Mapping oracleMap = o.getMapping();
            //			logger.info("Computing Mapping of instanceMap:"+resultMapping.size()+" and oracleMap"+oracleMap.size());

            resultMapping = learner.getFitnessFunction().getMapping(answerMetric.getExpression(), answerMetric.getThreshold(), true);
            System.out.println("Cycle " + cycle + " Accuracy = " + MappingMath.computePrecision(resultMapping, datasetSize));
            results.put("Cycle(" + cycle + ")", MappingMath.computePrecision(resultMapping, datasetSize));
            results.put("Cycle(" + cycle + ") Mitric: " + answerMetric.toString(), null);


            //			double fS=prfC.computePrecision(oracleMap, resultMapping);
            //			System.out.println("Cycle "+cycle+"  -  "+answerMetric);
            //			System.out.println("Cycle "+cycle+"  -  F-Score = "+fS);
        }
        return resultMapping;
    }

    /**
     * Iterative votingMapping calculation. Iterates until termination criterion
     * is fulfilled, but atleast once.
     *
     * @param voting_t0 intial votingMatrix
     * @param m
     * @param sourceMapIndex
     * @param targetMapIndex
     * @return final voting Mapping.
     */
    protected Mapping iterateVotingMatrix(Mapping voting_t0, Mapping[][] m, int sourceMapIndex, int targetMapIndex) {
        Mapping voting_t1 = new Mapping();
        boolean useScores = true;
        do {
            m[sourceMapIndex][targetMapIndex] = voting_t0;
            voting_t1 = MappingMath.normalize(getVotingScores(m, sourceMapIndex, targetMapIndex, useScores), m.length);
        } while (!terminationCheck(voting_t1, voting_t0, epsilon));
        return voting_t1;
    }

    /**
     * Termination check for iterated voting matrixes. Note that the voting
     * mapping are normalized, values are divided by the number of mappings.
     *
     * @param m1 Voting Mapping of time t+1
     * @param m0 Voting Mapping of time t0
     * @param epsilon Termination condition
     * @return sum(ij) < epsilon, for all ij in (m1-m0)
     */
    protected boolean terminationCheck(Mapping m1, Mapping m0, double epsilon) {
        // we basically compute the frobenius norm. That is the value = sum(ij) of t1-t0
        // terminate if value < epsilon
        System.out.println("Test Termination check for:\n" + m1 + "\n" + m0);
        double value = 0;
        Mapping diff = MappingMath.subtract(m1, m0, true);
        for (String uri1 : diff.map.keySet()) {
            for (Entry<String, Double> target : diff.map.get(uri1).entrySet()) {
                value += target.getValue();
            }
        }
        System.out.println("Termination check result = " + value);
        return value <= epsilon;
    }

    public static void testIteration() {
        Mapping m1 = new Mapping();
        Mapping m2 = new Mapping();
        boolean useScores = true;

        m1.add("a1", "a2", 0.1);
        m1.add("b1", "b2", 0.1);
        m2.add("b1", "b2", 0.1);
        m2.add("c1", "c2", 0.1);

        Mapping m3 = new Mapping();
        Mapping m4 = new Mapping();
        m3.add("a1", "a2", 0.1);
        m3.add("b1", "b2", 0.1);
        m4.add("b1", "b2", 0.1);
        m4.add("c1", "c2", 0.1);

        Mapping[][] m = new Mapping[2][2];
        m[0][0] = m1;
        m[0][1] = m2;
        m[1][0] = m3;
        m[1][1] = m4;
        for (int i = 0; i < m.length; i++) {
            for (int j = i + 1; j < m.length; j++) {
                m[j][i] = m[i][j].reverseSourceTarget();
            }
        }
        MultiInterlinker l = new MultiInterlinker();
        Mapping voting_t0 = l.getVotingScores(m, 0, 1, useScores);
        System.out.println(m);
        System.out.println(MappingMath.normalize(voting_t0, m.length));
        Mapping voting_tx = l.iterateVotingMatrix(MappingMath.normalize(voting_t0, m.length), m, 0, 1);
        System.out.println(voting_tx);
    }
    //TODO
    //TODO
    //TODO
    //TODO
    //-----------------------------------------------------------------------------------------------------------------

    public void multilinkPeel(String datasetPath) {

        String peelSpec = datasetPath + "peelSpecs.xml";
        String[] datasetFiles = {"peel_0.ttl", "peel_1.ttl", "peel_2.ttl", "peel_3.ttl", "peel_4.ttl"};

        for (int i = 0; i < datasetFiles.length; i++) {
            datasetFiles[i] = datasetPath.concat(datasetFiles[i]);
        }


        //1. Get unsupervised mappings for all datasets M(i,j) where i!=j
        Mapping[][] m = new Mapping[5][5];
        for (int i = 0; i < m.length; i++) {
            for (int j = i + 1; j < m.length; j++) {
                m[i][j] = getDeterministicUnsupervisedMappings(peelSpec, datasetFiles[i], datasetFiles[j]);
                m[j][i] = m[i][j].reverseSourceTarget();
                results.put("P of M[" + i + "," + j + "]", MappingMath.computeFMeasure(m[i][j], datasetSize));
                results.put("R of M[" + i + "," + j + "]", MappingMath.computeRecall(m[i][j], datasetSize));
                results.put("F of M[" + i + "," + j + "]", MappingMath.computeFMeasure(m[i][j], datasetSize));
            }
        }

        //Parameters
        boolean useScores = true;
        int sourceMapIndex = 1;
        int targetMapIndex = 2;
        long ExampleCount = Long.MAX_VALUE;
        int datasetsCount = 5; // n	
        double acceptanceThreshold = 2.79; // k where ( k < n/2 )

        //		results.put("P of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computePrecision(m[sourceMapIndex][targetMapIndex], datasetSize));
        //		results.put("R of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computeRecall(m[sourceMapIndex][targetMapIndex], datasetSize));
        //		results.put("F of M(" + sourceMapIndex + "," + targetMapIndex + ")", MappingMath.computeFMeasure(m[sourceMapIndex][targetMapIndex], datasetSize));

        //2. Compute voting scores matrix V(i,j)=M(i,j)+A(i,j), where A(i,j)= M(i,k)*M(k,j) for all 
        ///////////		Mapping votingMatrix = getScores(m, sourceMapIndex, targetMapIndex, useScores);
        //
        //////////		Mapping posExamples = getPosExamples(votingMatrix, ExampleCount, datasetsCount, acceptanceThreshold);

        //		double p = MappingMath.computePrecision(posExamples, datasetSize);
        //		results.put("Precision of votingMatrix with k=" + acceptanceThreshold, p);
        //		double r = MappingMath.computeRecall(posExamples, datasetSize);
        //		results.put("Recall of votingMatrix with k=" + acceptanceThreshold, r);
        //		double f = MappingMath.computeFMeasure(posExamples, datasetSize);
        //		results.put("F1 of votingMatrix with k=" + acceptanceThreshold, f);

        //find wrong instances between sourceMapIndex, targetMapIndex as all instance with similarity less than (K-n)
        //////////		Mapping wrongMapping = findWrongMapping(votingMatrix, posExamples);

        //		System.out.println("votingMatrix.size"+votingMatrix.size);
        //		System.out.println("wrongMapping.size: "+wrongMapping.size);
        //		System.out.println("M[1][2].size: "+m[1][2].size);
        //		System.exit(1);
        for (int r = 0; r < 10; r++) {
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j > 5; j++) {
                    Mapping votingMatrix = getVotingScores(m, i, j, useScores);
                    Mapping posExamples = getPosExamples(votingMatrix, i, j, acceptanceThreshold);
                    Mapping wrongMapping = findWrongMapping(votingMatrix, posExamples);
                    System.out.println(wrongMapping);
                    System.exit(1);
                    fixMapping(wrongMapping, m, i, j);
                }
            }
        }


        for (int i = 0; i < m.length; i++) {
            for (int j = i + 1; j < m.length; j++) {
                m[i][j] = getDeterministicUnsupervisedMappings(peelSpec, datasetFiles[i], datasetFiles[j]);
                m[j][i] = m[i][j].reverseSourceTarget();
                results.put("P of M[" + i + "," + j + "] Fixed", MappingMath.computeFMeasure(m[i][j], datasetSize));
                results.put("R of M[" + i + "," + j + "] Fixed", MappingMath.computeRecall(m[i][j], datasetSize));
                results.put("F of M[" + i + "," + j + "] Fixed", MappingMath.computeFMeasure(m[i][j], datasetSize));
            }
        }


        //		fixMapping(wrongMapping, m, sourceMapIndex, targetMapIndex);

        //		Mapping fixedMapping = getDeterministicUnsupervisedMappings(peelSpec, datasetFiles[sourceMapIndex], datasetFiles[targetMapIndex]);

        //		p = MappingMath.computePrecision(fixedMapping, datasetSize);
        //		results.put("Precision of fixedMapping with k=" + acceptanceThreshold, p);
        //		r = MappingMath.computeRecall(fixedMapping, datasetSize);
        //		results.put("Recall of fixedMapping with k=" + acceptanceThreshold, r);
        //		f = MappingMath.computeFMeasure(fixedMapping, datasetSize);
        //		results.put("F1 of fixedMapping with k=" + acceptanceThreshold, f);
        //		
        //		Mapping fixedMapping = fixMapping(m, votingMatrix, posExamples);
        //		results.put("Precision of fixedMapping", MappingMath.computePrecision(fixedMapping, datasetSize));
        //		results.put("Recall of fixedMapping", MappingMath.computeRecall(fixedMapping, datasetSize));
        //		results.put("F1 of fixedMapping", MappingMath.computeFMeasure(fixedMapping, datasetSize));

        //		System.out.println(results);
        printResults();
        System.exit(1);
    }

    void printResults() {
        System.out.println("-------------- RESULTS --------------");
        for (Entry<String, Double> entry : results.entrySet()) {
            if (entry.getValue() == null) {
                System.out.println(entry.getKey());
            } else {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    /**
     * @param mapping
     * @param posExamples
     * @return
     * @author sherif
     */
    protected Mapping findWrongMapping(Mapping inputMaping, Mapping refMapping) {
        Mapping wrongMapping = new Mapping();
        for (String inputMapingSourceUri : inputMaping.map.keySet()) {
            for (String inputMapingTargetUri : inputMaping.map.get(inputMapingSourceUri).keySet()) {
                //				System.out.println(inputMapingSourceUri+" ------> "+inputMapingTargetUri);
                if (!refMapping.contains(inputMapingSourceUri, inputMapingTargetUri)) {
                    wrongMapping.add(inputMapingSourceUri, inputMapingTargetUri, inputMaping.getSimilarity(inputMapingSourceUri, inputMapingTargetUri));
                    //					System.out.println("ADDED");
                }
            }
        }
        return wrongMapping;
    }

    //TODO find more generic way to generalize this
    void fixMapping(Mapping wrongMapping, Mapping[][] m, int sourceMapIndex, int targetMapIndex) {

        //TODO take this part as a parameter
        ConfigReader cR = new ConfigReader();
        cR.validateAndRead("/mypartition2/musicDatasets/multilinkingPeelTest/peelSpecs.xml");
        cR.sourceInfo.id = "peel_" + sourceMapIndex;
        cR.targetInfo.id = "peel_" + targetMapIndex;
        String iKB = cR.sourceInfo.id;
        String jKB = cR.targetInfo.id;

        Map<String, Integer> minMIndex = new HashMap<String, Integer>();
        // Find the smallest similarity value min{sim1[i][j], sim2[i][k],sim3[k][j]}
        for (String wrongMappingSourceUri : wrongMapping.map.keySet()) {
            for (String wrongMappingTargetUri : wrongMapping.map.get(wrongMappingSourceUri).keySet()) {
                minMIndex = getMinSimilarityIndex(m, wrongMappingSourceUri, wrongMappingTargetUri, sourceMapIndex, targetMapIndex);
                int minSourceMapIndex = minMIndex.get("sourceIndex");
                int minTargetMapIndex = minMIndex.get("targetIndex");

                if (minSourceMapIndex == sourceMapIndex && minTargetMapIndex == targetMapIndex) {
                    // deal with m[i][j] as the smallest mapping

                    double sumOfAll_i_Sim = 0d, sumOfAll_j_Sim = 0d;
                    for (int k = 0; k < m.length; k++) {
                        if (k != sourceMapIndex && k != targetMapIndex) {
                            sumOfAll_i_Sim += m[minSourceMapIndex][k].getSimilarity(wrongMappingSourceUri, wrongMappingTargetUri);
                            sumOfAll_j_Sim += m[k][minTargetMapIndex].getSimilarity(wrongMappingSourceUri, wrongMappingTargetUri);
                        }
                    }

                    if (sumOfAll_i_Sim > sumOfAll_j_Sim) { // j lose
                        for (String property : cR.getSourceInfo().properties) {
                            if (KB2Cache.get(iKB).getInstance(wrongMappingSourceUri) != null) {
                                TreeSet<String> iValues = KB2Cache.get(iKB).getInstance(wrongMappingSourceUri).getProperty(property);
                                KB2Cache.get(jKB).getInstance(wrongMappingTargetUri).replaceProperty(property, iValues);
                            }
                        }
                    } else if (sumOfAll_j_Sim > sumOfAll_i_Sim) { // i lose
                        for (String property : cR.getSourceInfo().properties) {
                            if (KB2Cache.get(jKB).getInstance(wrongMappingSourceUri) != null) {
                                TreeSet<String> jValues = KB2Cache.get(jKB).getInstance(wrongMappingSourceUri).getProperty(property);
                                KB2Cache.get(iKB).getInstance(wrongMappingTargetUri).replaceProperty(property, jValues);
                            }
                        }
                    }

                } else if (minSourceMapIndex == sourceMapIndex) {
                    // deal with m[i][k] as the smallest mapping, i lose

                    for (String property : cR.getSourceInfo().properties) {
                        if (KB2Cache.get(jKB).getInstance(wrongMappingSourceUri) != null) {
                            TreeSet<String> jValues = KB2Cache.get(jKB).getInstance(wrongMappingSourceUri).getProperty(property);
                            KB2Cache.get(iKB).getInstance(wrongMappingTargetUri).replaceProperty(property, jValues);
                        }
                    }

                } else {
                    // deal with m[k][j] as the smallest mapping, j lose

                    for (String property : cR.getSourceInfo().properties) {
                        if (KB2Cache.get(iKB).getInstance(wrongMappingSourceUri) != null) {
                            TreeSet<String> iValues = KB2Cache.get(iKB).getInstance(wrongMappingSourceUri).getProperty(property);
                            KB2Cache.get(jKB).getInstance(wrongMappingTargetUri).replaceProperty(property, iValues);
                        }
                    }
                }

            }
        }
    }

    Map<String, Integer> getMinSimilarityIndex(Mapping[][] m, String mSourceUri, String mTargetUri, int sourceMapIndex, int targetMapIndex) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        double sim1 = 0d, sim2 = 0d, sim3 = 0d, minSim = 0d;
        Integer sourceIndex = 0, targetIndex = 0;
        sim1 = m[sourceMapIndex][targetMapIndex].getSimilarity(mSourceUri, mTargetUri);
        minSim = sim1;
        sourceIndex = sourceMapIndex;
        targetIndex = targetMapIndex;
        for (int k = 0; k < m.length; k++) {
            if (k != sourceMapIndex && k != targetMapIndex) {
                sim2 = m[sourceMapIndex][k].getSimilarity(mSourceUri, mTargetUri);
                if (sim2 < minSim) {
                    minSim = sim2;
                    targetIndex = k;
                }
                sim3 = m[k][targetMapIndex].getSimilarity(mSourceUri, mTargetUri);
                if (sim3 < minSim) {
                    sourceIndex = k;
                }
            }
        }
        result.put("sourceIndex", sourceIndex);
        result.put("targetIndex", targetIndex);
        return result;
    }

    Mapping _fixMapping(Mapping inputMaping, Mapping refMappting) {
        Mapping resultMapping = new Mapping();
        Mapping reverseInputMapping = refMappting.reverseSourceTarget();
        for (String inputMapingSourceUri : inputMaping.map.keySet()) {
            for (String inputMapingTargetUri : inputMaping.map.get(inputMapingSourceUri).keySet()) {
                if (refMappting.contains(inputMapingSourceUri, inputMapingTargetUri)) {
                    resultMapping.add(inputMapingSourceUri, inputMapingTargetUri, inputMaping.getSimilarity(inputMapingSourceUri, inputMapingTargetUri));
                } else {
                    if (refMappting.map.containsKey(inputMapingSourceUri)) {
                        for (String refMapptingTargetUri : refMappting.map.get(inputMapingSourceUri).keySet()) {
                            resultMapping.add(inputMapingSourceUri, refMapptingTargetUri, refMappting.getSimilarity(inputMapingSourceUri, refMapptingTargetUri));
                        }
                    } else {
                        if (reverseInputMapping.map.containsKey(inputMapingTargetUri)) {
                            for (String refMapptingSourceUri : reverseInputMapping.map.get(inputMapingTargetUri).keySet()) {
                                resultMapping.add(refMapptingSourceUri, inputMapingTargetUri, refMappting.getSimilarity(refMapptingSourceUri, inputMapingTargetUri));
                            }
                        } else {
                            resultMapping.add(inputMapingSourceUri, inputMapingTargetUri, inputMaping.getSimilarity(inputMapingSourceUri, inputMapingTargetUri));
                        }
                    }
                }
            }
        }// TODO Auto-generated method stub
        return resultMapping;
    }

    void _testFixMapping() {
        Mapping m = new Mapping();
        Mapping ref = new Mapping();

        m.add("a1", "a2", 0.1); //true (included in ref)
        m.add("x1", "b2", 0.1); //err1
        m.add("c1", "x2", 0.1); //err2
        m.add("d1", "d2", 0.1); // ? (not included in ref)

        ref.add("a1", "a2", 0.2);
        ref.add("b1", "b2", 0.2);
        ref.add("c1", "c2", 0.2);
        ref.add("e1", "e2", 0.2); // extra (not in m)

        System.out.println("m:");
        System.out.println(m);
        System.out.println("ref:");
        System.out.println(ref);
        System.out.println("fix:");
        System.out.println(_fixMapping(m, ref));
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void main(String args[]) {
        //		testIteration();
        MultiInterlinker multInterlinker = new MultiInterlinker();
//		multInterlinker.testTOyData();
        //		multInterlinker.multilinkPeel(args[0]);
        //		System.out.println("=========== RESULTS ACCURACY ===========");
        //		for (Entry<String, Double> entry : multInterlinker.results.entrySet()) {
        //			if (entry.getValue() == null) {
        //				System.out.println(entry.getKey());
        //			} else {
        //				System.out.println(entry.getKey() + ": " + entry.getValue());
        //			}
        //		}

        //		System.out.println(multInterlinker.resultsAccuracy);

        //		if(args.length == 0)
        //		{
        //			logger.fatal("No configuration file specified.");
        //			System.exit(1);
        //		}
        //		if (new File(args[0]).exists()) {
        //			Mapping m= new Mapping();
        //			multInterlinker.getUnsupervisedMappings(args[0]);
        //			//        	m=multInterlinker.getPpJoinController().getMapping(args[0]);
        //			//        	System.out.println("=============== Mapping =================");
        //			//        	System.out.println(m);
        //		} else {
        //			logger.fatal("Input file " + args[0] + " does not exist.");
        //			System.exit(1);
        //		}
    }

    /**
     *
     * @author sherif
     */
    /**
     * @param votingMap[i,j]
     * @param n number of datasets
     * @param k reverse acceptance threshold
     * @return all wrong mapping under threshold (n-k)
     * @author sherif
     */
    /**
     * @param m[][] for all datasets
     * @return v as votingMatrix for all i,j
     * @author Sherif
     */
    protected Mapping[][] getAllVotingScores(Mapping[][] m, boolean useScores) {
        Mapping[][] v = new Mapping[m.length][m[1].length];
        for (int i = 0; i < v.length; i++) {
            for (int j = i + 1; j < v[i].length; j++) {
                v[i][j] = getVotingScores(m, i, j, useScores);
                v[j][i] = getVotingScores(m, j, i, useScores);
                System.out.println("V[" + i + "," + j + "]");
                System.out.println(v[i][j]);
            }
        }
        return v;
    }

    /**
     * @param m[][] for all datasets
     * @param sourceMapIndex i
     * @param targetMapIndex j
     * @return votingMatrix = m[i][j] + m[i][k] * m[k][j] for all k!=i & k!=j
     * @author Sherif
     */
    protected Mapping getVotingScores(Mapping[][] m, int sourceMapIndex, int targetMapIndex, boolean useScores) {
        Mapping votingMatrix = new Mapping();
        votingMatrix = m[sourceMapIndex][targetMapIndex];
        //		System.out.println("AA: "+votingMatrix.size);
        for (int k = 0; k < m.length; k++) {
            if (k != sourceMapIndex && k != targetMapIndex) {
                votingMatrix = MappingMath.add(votingMatrix, MappingMath.multiply(m[sourceMapIndex][k], m[k][targetMapIndex], useScores), useScores);
                //				System.out.println("k: "+votingMatrix.size);
            }
        }
        //		System.out.println("final: "+votingMatrix.size);
        //		System.exit(1);
        return votingMatrix;
    }

    /**
     * @param votingMap V
     * @param n number of datasets
     * @param k reverse acceptance threshold
     * @return for each V[i,j] return W[i,j] containing all wrong mapping under
     * threshold (n-k)
     * @author sherif
     */
    protected Mapping[][] findAllWrongMapping(Mapping[][] votingMap, int n, int k) {
        Mapping[][] w = new Mapping[votingMap.length][votingMap[0].length];
        for (int i = 0; i < votingMap.length; i++) {
            for (int j = i + 1; j < votingMap[0].length; j++) {
                w[i][j] = findWrongMapping(votingMap[i][j], n, k);
                System.out.println("W[" + i + "," + j + "]=");
                System.out.println(w[i][j]);
            }
        }
        return w;
    }

    protected Mapping findWrongMapping(Mapping votingMap, int n, int k) {
        Mapping result = new Mapping();
        for (String mapSourceUri : votingMap.map.keySet()) {
            for (String mapTargetUri : votingMap.map.get(mapSourceUri).keySet()) {
                Double sim = votingMap.getSimilarity(mapSourceUri, mapTargetUri);
                if (sim < (n - k)) {
                    result.add(mapSourceUri, mapTargetUri, 1d);
                }
            }
        }
        return result;
    }

    /**
     * @param mapping
     * @param m
     * @param cache
     * @param i
     * @param j
     * @author sherif
     */
    protected void _fixCaches(Mapping w, Mapping[][] m, Cache[] cache, int i, int j) {
        double sim = 0d, maxSim = 0d;
        int bestK = 0;
        for (String wrongMappingSourceUri : w.map.keySet()) {
            //			for (String wrongMappingTargetUri : w.map.get(wrongMappingSourceUri).keySet()) { 

            for (int k = 0; k < m.length; k++) {
                if (k != i && k != j) {
                    for (String mIKSourceUri : m[i][k].map.keySet()) {
                        for (String mIKTargetUri : m[i][k].map.get(mIKSourceUri).keySet()) {
                            sim = m[i][k].getSimilarity(wrongMappingSourceUri, mIKTargetUri);
                            if (sim > maxSim) {
                                maxSim = sim;
                                bestK = k;
                            }
                        }
                    }
                }
            }
            System.out.println("bestk: " + bestK);
            // fix wrongMappingSourceUri exist in m[i][j] by replacing its properties by the ones in m[i][bestK] 
            // in other words: replacing properties of cache[i] by ones of cache[bestK]
            for (String property : cache[i].getAllProperties()) {
                if (cache[bestK].getInstance(wrongMappingSourceUri) != null) {
                    TreeSet<String> kValues = cache[bestK].getInstance(wrongMappingSourceUri).getProperty(property);
                    TreeSet<String> iValues = cache[i].getInstance(wrongMappingSourceUri).getProperty(property);// can be removed later, just here for verification 
                    cache[i].getInstance(wrongMappingSourceUri).replaceProperty(property, kValues);
                    System.out.println("i lose, Replacing " + iValues + " --> " + kValues);
                }
            }
        }
        //		}
    }
}
