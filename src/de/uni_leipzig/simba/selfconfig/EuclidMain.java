package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Class to exemplify the usage of the EUCLID algorithm. 
 * Note that maybe packge declarations have to be adjusted!
 * Uses the PERSON1, PERSON2 and DBLP-ACM datasets.
 * @author Klaus Lyko
 *
 */
public class EuclidMain {
    
	/**
	 * Run current experiment for all configurators.
	 * @param sC Source Cache.
	 * @param tC Target Cache.
	 * @param reference	Mapping gold standars to compute real f-measures.
	 * @param coverage min coverage of initial linear classifier.
	 * @param beta Beta value used in the PFM.
	 * @param measure Pseudo F_measure to use.
	 * @return String stating all statistics.
	 */
    public static String runAll(Cache sC, Cache tC, Mapping reference, double coverage, double beta, Measure measure) {
        String output = "";
//        output = output + run(sC, tC, reference, coverage, beta, measure, "linear") + "\n";
//        output = output + run(sC, tC, reference, coverage, beta, measure, "conjunctive") + "\n";
        output = output + run(sC, tC, reference, coverage, beta, measure, "disjunctive");
        return output;
    }

    /**
     * Runs EUCLID selfconfiguration with
     * @param s Source Cache
     * @param t Target Cache
     * @param r	Reference Mapping, e.g. gold standard to compute real f-measures
     * @param coverage min coverage of initial linear classifier.
     * @param beta Beta for Pseudo F-Measure (in [0,2])
     * @param measure Pseudo F-Measure to use
     * @param type linear / conjunctive / disjunctive
     * @return
     */
    public static String run(Cache s, Cache t, Mapping r, double coverage, double beta, Measure measure, String type) {
        // init the self configurator
        MeshBasedSelfConfigurator lsc;
        if (type.toLowerCase().startsWith("l")) {
            lsc = new LinearMeshSelfConfigurator(s, t, coverage, beta);
        } else if (type.toLowerCase().startsWith("d")) {
            lsc = new DisjunctiveMeshSelfConfigurator(s, t, coverage, beta);
        } else {
            lsc = new MeshBasedSelfConfigurator(s, t, coverage, beta);
        }
        // set the corresponding PFM
        lsc.setMeasure(measure);
        
        long begin = System.currentTimeMillis();
        
        // Compute initial classifiers.
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();
        long middle = System.currentTimeMillis();
        String output = "Type\tIterations\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\tCC\n";
    
        long duration = 0; // stop criteria in seconds
        
        int iteration = 1; 
        while(duration <= 60) {//run atleast a iteration at most for 10 minutes 
        	System.out.println("\tRunning iteration"+iteration);
        	//compute classifier
        	ComplexClassifier cc = lsc.getZoomedHillTop(5, iteration, cp);
        	long end = System.currentTimeMillis();
        	// compute statistics
        	Mapping m = cc.mapping;
            m = lsc.getBestOneToOneMapping(m);
            PRFCalculator prf = new PRFCalculator();
            double precision = prf.precision(m, r);
            double recall = prf.recall(m, r);
            double f = prf.fScore(m, r);
            iteration++;
            duration = (end - begin) / 1000;
            // log statistics to String
            output += type + "\t"+ iteration +"\t" + (middle - begin) / 1000 + "\t" + (end - middle) / 1000 + "\t" + (end - begin) / 1000 + "\t" + cc.fMeasure + "\t" + precision + "\t" + recall + "\t" + f + "\t" +cc+"\n";
    	}       
        return output;
    }

    public static void main(String args[]) {
        String result = "";//"Type\tBeta\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\n";
        Measure approach = new PseudoMeasures();
        
        /*##############first step: 
         * Get data: create caches 
         * This is done here by a little helper class. It basically uses the configuration XMLs
         * and constructs/reads the HybridChaches.
         * 
         * Could also be done manually for source and target: 
         * 
         * 	1. read all instance data: 
         * 		create for each instance and Instance.java object(String uri)
		 *      add data: mapping propterty names to its values (both String)
         *  2. Create a memory cache and submit all Instances
         */
        EvaluationData ds = DataSetChooser.getData(DataSets.RESTAURANTS);
        
        result = result + "RESTAURANTS\n";
        System.out.println("Running RESTAURANTS...");
        result = result + runAll(ds.getSourceCache(),
                ds.getTargetCache(),
                ds.getReferenceMapping(),
                0.6, 1d, approach);
        
        System.out.println(result);
        
        /**
         * Be aware that these experiments could take some time due to
         * many properties and thereby a larger search space.
         */
//      result = result + "\n";
//      result = result + "PERSONS1\n";
//      
//      
//      ds = DataSetChooser.getData(DataSets.PERSON1);
//      result = result + runAll(ds.getSourceCache(),
//              ds.getTargetCache(),
//              ds.getReferenceMapping(),
//              0.6, 1d, approach);
//    
//      System.out.println(result);
//        result = result + "\n";
//        result = result + "PERSONS2\n";
//        
//        
//        ds = DataSetChooser.getData(DataSets.PERSON2);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//      
//        System.out.println(result);
//        result = result + "\n";
//        result = result + "DBLP-ACM\n";
//
//  
//        ds = DataSetChooser.getData(DataSets.DBLPACM);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//        
        
        System.out.println(result);
        writeToFile(result);
    }
    
    /**
     * Log statistics to file.
     * @param result
     * @return
     */
    public static boolean writeToFile(String result) {
    	String folder = "resources/results/";
		String fileName = "EuclidLog.csv";
		File file = new File(folder+fileName);
		try {
			FileWriter writer = new FileWriter(file, false);
			writer.write(result);
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();				
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
    }
}
