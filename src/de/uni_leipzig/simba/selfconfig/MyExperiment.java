/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
 *
 * @author ngonga
 */
public class MyExperiment {
	
	String pathToOAEIDataSets = "resources/";
	
	
    public static String runAll(String source, String target, String reference, double coverage, double beta, Measure measure, long maxDuration) {
        String output = "";
        output = output + run(source, target, reference, coverage, beta, measure, "linear", maxDuration) + "\n";
        output = output + run(source, target, reference, coverage, beta, measure, "conjunctive", maxDuration) + "\n";
        output = output + run(source, target, reference, coverage, beta, measure, "disjunctive", maxDuration);
        return output;
    }
    
    public static String runAll(Cache sC, Cache tC, Mapping reference, double coverage, double beta, Measure measure, long maxDuration) {
        String output = "";
        output = output + run(sC, tC, reference, coverage, beta, measure, "linear", maxDuration) + "\n";
        output = output + run(sC, tC, reference, coverage, beta, measure, "conjunctive", maxDuration) + "\n";
        output = output + run(sC, tC, reference, coverage, beta, measure, "disjunctive", maxDuration);
        return output;
    }

    public static String run(String source, String target, String reference, double coverage, double beta, Measure measure, String type, long maxDuration) {
        Cache s, t;
        Mapping r;
        System.out.println("\n\n"+source+"...");
        if (source.endsWith("nt")) {
            if (source.contains("person")) {
                s = Experiment.readOAEIFile(source, "-Person");
                t = Experiment.readOAEIFile(target, "-Person");
                r = Experiment.readOAEIMapping(reference);
            } else {
                s = Experiment.readOAEIFile(source, "-Restaurant");
                t = Experiment.readOAEIFile(target, "-Restaurant");
                r = Experiment.readOAEIMapping(reference);
            }
        } else {
            s = Experiment.readFile(source);
            t = Experiment.readFile(target);
            r = Experiment.readReference(reference);
        }
        
        return run(s,t,r, coverage, beta, measure, type, maxDuration);
    }

    /**
     * Runs EUCLID selfconfiguration with
     * @param s Source Cache
     * @param t Target Cache
     * @param r	Reference Mapping, e.g. gold standard to compute real f-measures
     * @param coverage
     * @param beta Beta for Pseudo F-Measure (in [0,2])
     * @param measure Pseudo F-Measure to use
     * @param type linear / conjunctive / disjunctive
     * @return
     */
    public static String run(Cache s, Cache t, Mapping r, double coverage, double beta, Measure measure, String type, long maxDuration) {
            
        MeshBasedSelfConfigurator lsc;
        if (type.toLowerCase().startsWith("l")) {
            lsc = new LinearMeshSelfConfigurator(s, t, coverage, beta);
        } else if (type.toLowerCase().startsWith("d")) {
            lsc = new DisjunctiveMeshSelfConfigurator(s, t, coverage, beta);
        } else {
            lsc = new MeshBasedSelfConfigurator(s, t, coverage, beta);
        }

        lsc.setMeasure(measure);
        long begin = System.currentTimeMillis();
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();
        long middle = System.currentTimeMillis();
        String output = "Type\tIterations\tMapping time\tRuntime\tTotaltime\tPseudo-F" +
        		"\tPrecision\tRecall\tReal F" +
        		"\tPrecision_1to1\tRecall_1to1\tReal F_1to1" +
        		"\tCC\n";
        long duration = 0;
        int iteration = 1;
        while(duration <= maxDuration) {//run for 10 minutes 
        	ComplexClassifier cc = lsc.getZoomedHillTop(5, iteration, cp);
        	long end = System.currentTimeMillis();
        	Mapping m = cc.mapping;
            Mapping m_1to1 = Mapping.getBestOneToOneMappings(m);
            PRFCalculator prf = new PRFCalculator();
            double precision = prf.precision(m, r);
            double recall = prf.recall(m, r);
            double f = prf.fScore(m, r);
            double precision_1to1 = prf.precision(m_1to1, r);
            double recall_1to1 = prf.recall(m_1to1, r);
            double f_1to1 = prf.fScore(m_1to1, r);
            iteration++;
            duration = (end - begin) / 1000;
            output += type + "\t"+ iteration +"\t" + (middle - begin) / 1000 + "\t" + (end - middle) / 1000 + "\t" + (end - begin) / 1000 + "\t" + cc.fMeasure + "\t"+ 
            precision + "\t" + recall + "\t" + f + "\t" +
            precision_1to1 + "\t" + recall_1to1 + "\t" + f_1to1 + "\t" +
            cc+"\n";
    	}       
        return output;
    }

    public static String testBeta(String source, String target, String reference, double coverage, double beta, Measure measure, String type) {
        long beginning = System.currentTimeMillis();
        Cache s, t;
        Mapping r;
        if (source.endsWith("nt")) {
            if (source.contains("person")) {
                s = Experiment.readOAEIFile(source, "-Person");
                t = Experiment.readOAEIFile(target, "-Person");
                r = Experiment.readOAEIMapping(reference);
            } else {
                s = Experiment.readOAEIFile(source, "-Restaurant");
                t = Experiment.readOAEIFile(target, "-Restaurant");
                r = Experiment.readOAEIMapping(reference);
            }
        } else {
            s = Experiment.readFile(source);
            t = Experiment.readFile(target);
            r = Experiment.readReference(reference);
        }


        MeshBasedSelfConfigurator lsc;

        if (type.toLowerCase().startsWith("l")) {
            lsc = new LinearMeshSelfConfigurator(s, t, coverage, beta);
        } else if (type.toLowerCase().startsWith("d")) {
            lsc = new DisjunctiveMeshSelfConfigurator(s, t, coverage, beta);
        } else {
            lsc = new MeshBasedSelfConfigurator(s, t, coverage, beta);
        }
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();

        String output = "";
        for (double b = 0.1; b < 2.1; b = b + 0.1) {
            long begin = System.currentTimeMillis();
            MeshBasedSelfConfigurator msc;
            if (type.toLowerCase().startsWith("l")) {
                msc = new LinearMeshSelfConfigurator(s, t, coverage, b);
            } else if (type.toLowerCase().startsWith("d")) {
                msc = new DisjunctiveMeshSelfConfigurator(s, t, coverage, b);
            } else {
                msc = new MeshBasedSelfConfigurator(s, t, coverage, b);
            }
            msc.setMeasure(measure);
            long middle = System.currentTimeMillis();

            ComplexClassifier cc = msc.getZoomedHillTop(5, 10, cp);
            Mapping m = cc.mapping;
            m = Mapping.getBestOneToOneMappings(m);
            //m = msc.getBestOneToNMapping(m);
            PRFCalculator prf = new PRFCalculator();
            double precision = prf.precision(m, r);
            double recall = prf.recall(m, r);
            double f = prf.fScore(m, r);
            long end = System.currentTimeMillis();
            
            output = "Type\tBeta\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\t CC\n";

            output = output + type + "\t" + b + "\t" + (middle - begin)/1000 + "\t" + (end - middle)/1000 + "\t" + (end - begin)/1000 + "\t" + cc.fMeasure + "\t" + precision + "\t" + recall + "\t" + f + cc +"\n";
        }
        return output;
    }

    public static void main(String args[]) {
        String result = "";//"Type\tBeta\tMapping time\tRuntime\tTotaltime\tPseudo-F\tPrecision\tRecall\tReal F\n";
        Measure approach = new PseudoMeasures();
//      String classifier = "linear";
        String classifier = "conjunctive";
        
        EvaluationData ds = DataSetChooser.getData(DataSets.PERSON1_CSV);
        
        result = result + "PERSONS1\n";
        
        result = result + runAll(ds.getSourceCache(),
                ds.getTargetCache(),
                ds.getReferenceMapping(),
                0.6, 1d, approach, 600);
//        
//        System.out.println(result);
//        result = result + "\n";
//        result = result + "PERSONS2\n";
//        
//        
//        ds = DataSetChooser.getData(DataSets.PERSON2_CSV);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//        
//        System.out.println(result);
//        result = result + "\n";
//        result = result + "RESTAURANT\n";
//        
//
//        ds = DataSetChooser.getData(DataSets.RESTAURANTS_CSV);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//       
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
//        result = result + "\n";
//        result = result + "DBLP-Scholar\n";
//
//        ds = DataSetChooser.getData(DataSets.DBLPSCHOLAR);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//
//        System.out.println(result);       
//        result = result + "\n";
//        result = result + "Amazon-Googleproducts\n";
//
//        ds = DataSetChooser.getData(DataSets.AMAZONGOOGLE);
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
//
//
//        System.out.println(result);
//        result = result + "\n";
//        result = result + "Abt-Buy\n";
//        ds = DataSetChooser.getData(DataSets.ABTBUY);
//        
//        
//        result = result + runAll(ds.getSourceCache(),
//                ds.getTargetCache(),
//                ds.getReferenceMapping(),
//                0.6, 1d, approach);
        
        System.out.println(result);
        writeToFile(result, false);
    }
    
    public static boolean writeToFile(String result, boolean append) {
    	String folder = "resources/results/";
		String fileName = "EuclidLog.csv";
		File file = new File(folder+fileName);
		try {
			FileWriter writer = new FileWriter(file, append);
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
