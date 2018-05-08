package de.uni_leipzig.simba.selfconfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;

/**
 * Evaluate Euclid based on supervised batch learning.
 * Supervised version of the standard EUCLID evaluation @see e.uni_leipzig.simba.selfconfig.MyExperiment.
 * @author Klaus Lyko
 *
 */
public class EuclidSupervisedExperiment {
	
	String pathToOAEIDataSets = "resources/";
	/**Specifies the maximal number of links used for training. E.g. in case of large reference data.*/
	int maxTrainingData = 100;
	/**Specifies the percetage of reference data which is used as training data.*/
	float percentTrainingData = 0.2f;
	public long maxDuration = 600; // in seconds
	public Map<String, EuclidEvaluationMemory> results = new HashMap<String, EuclidEvaluationMemory>();
	public static String[] types = new String[]{"linear","conjunctive","disjunctive"}; 
	public EuclidSupervisedExperiment(float percentTrainingData, int maxTrainingData) {
		this.percentTrainingData = percentTrainingData;
		this.maxTrainingData = maxTrainingData;
	}
	
	/**
	 * Runs batched EUCLID using all 3 classifiers: linear, conjunctive and disjunctive.
	 * @param source
	 * @param target
	 * @param reference
	 * @param coverage
	 * @param beta
	 * @param trainingData
	 * @return
	 */
    public String runAll(Cache source, Cache target, Mapping reference, double coverage, double beta, Mapping trainingData, String name) {
        String output = "";
        for(String s:types)
        	output = output + run(source, target, reference, coverage, beta,  s, trainingData) + "\n";
//        output = output + run(source, target, reference, coverage, beta,  "conjunctive", trainingData) + "\n";
//        output = output + run(source, target, reference, coverage, beta,  "disjunctive", trainingData);
        //additional lof into single files
        writeToFile(name+"\n"+output, "_"+name);
        return output;
    }


    /**
     * Runs Euclid using a single classifier (type).
     * @param s Full Cache of source KB.
     * @param t Full Cache of target KB.
     * @param r Full reference Mapping to calculate final F-Scores and other statistics.
     * @param coverage
     * @param beta
     * @param type Specifies EUCLIDs classifier, whether linear, conjunctive, or disjunctive
     * @param trainingData Training data evaluated by an oracle. This mapping also specifies the trimmed cache version used for learning.
     * @return
     */
    public String run(Cache s, Cache t, Mapping r, double coverage, double beta, String type, Mapping trainingData) {
            
        MeshBasedSelfConfigurator lsc;
        if (type.toLowerCase().startsWith("l")) {
        	System.out.println("Running linear EUCLID....");
            lsc = new LinearMeshSelfConfigurator(s, t, coverage, beta);
        } else if (type.toLowerCase().startsWith("d")) {
        	System.out.println("Running disjunctive EUCLID....");
            lsc = new DisjunctiveMeshSelfConfigurator(s, t, coverage, beta);            
        } else {
        	System.out.println("Running mesh EUCLID....");
            lsc = new MeshBasedSelfConfigurator(s, t, coverage, beta);
        }
        // set trainingData
        lsc.asked = trainingData;
        lsc.setSupervisedBatch(trainingData);
        String output ="reference.size="+trainingData.size()+" Trimmed Caches to "+lsc.source.size()+" and "+lsc.target.size()+" \n";
        long begin = System.currentTimeMillis();
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();
//        long middle = System.currentTimeMillis();
        output += "Type\tIterations\tMapping time\tRuntime\tTotaltime\tPseudo-F" +
        		"\tPrecision_1to1\tRecall_1to1\tReal F_1to1" +
        		"\tPrecision\tRecall\tReal F\t" +
        		"CC\n";
//        long duration = 0;

        double f = 0d;
        ComplexClassifier cc = lsc.getZoomedHillTop(5, maxDuration, cp);
        long end = System.currentTimeMillis();
        Mapping m = cc.mapping;
        Mapping m_1to1 = Mapping.getBestOneToOneMappings(m);
            
        PRFCalculator prf = new PRFCalculator();
        double precision_1to1 = prf.precision(m_1to1, r);
        double recall_1to1 = prf.recall(m_1to1, r);
        double f_1to1 = prf.fScore(m_1to1, r);
        prf = new PRFCalculator();
        double precision = prf.precision(m, r);
        double recall = prf.recall(m, r);
        f = prf.fScore(m, r);
//        duration = (end - begin) / 1000;
        output += type + 
            		(end - begin) / 1000 + "\t" +
            		cc.fMeasure + "\t" +
            		precision_1to1 + "\t" +
           		 	recall_1to1 + "\t" +
           		 	f_1to1 + "\t" +
            		 precision + "\t" +
            		 recall + "\t" +
            		 f + "\t" +
            		cc+"\n";
    	
        EuclidEvaluationMemory mem = new EuclidEvaluationMemory();
        mem.euclidAlgorithm = type;
        mem.fullFScore = f;
        results.put(type, mem);
        return output;
    }


    public static void main(String args[]) {
        String result = "";//"Type\tBeta\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\n";
        
        EuclidSupervisedExperiment evaluator = new EuclidSupervisedExperiment(0.2f, 100);
        
        DataSets[] datas = {
        		DataSets.PERSON1,
        		DataSets.RESTAURANTS,
        		DataSets.ABTBUY,
        		DataSets.DBLPSCHOLAR,
        		DataSets.DBLPACM,
        		DataSets.PERSON2,
        		};
        
        for(DataSets data : datas) {
        	EvaluationData ds = DataSetChooser.getData(data);
        
            int trainingDataSize = Math.min((int) (ds.getReferenceMapping().size()*evaluator.percentTrainingData), evaluator.maxTrainingData);
        	
            System.out.println("Using "+evaluator.percentTrainingData+" (max "+evaluator.maxTrainingData+") of "+ds.getReferenceMapping().size+" := "+trainingDataSize);
        	Mapping trainingData = ExampleOracleTrimmer.trimExamplesRandomly(ds.getReferenceMapping(), trainingDataSize);
            System.out.println("TrainingData:"+trainingData.size());
            result = result + ds.getName()+" with "+trainingData.size()+"training examples \n";
            result = result + evaluator.runAll(ds.getSourceCache(),
                    ds.getTargetCache(),
                    ds.getReferenceMapping(),
                    0.6, 1d, trainingData,
                    ds.getName());
        }
        
        
        System.out.println(result);
 
        writeToFile(result, "_complete");
    }
    
    /**
     * Just writes string containing evaluation results to a file.
     * @param result
     * @return true if writing was succesful.
     */
    public static boolean writeToFile(String result, String nameExtansion) {
    	String folder = "resources/results/";
		String fileName = "BatchedEuclidLog"+nameExtansion+".csv";
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
    
    public class EuclidEvaluationMemory {
    	public String euclidAlgorithm = "";
    	public double fullFScore = 0d;
    }
}