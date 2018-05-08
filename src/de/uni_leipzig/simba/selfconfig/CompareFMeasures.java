package de.uni_leipzig.simba.selfconfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.multilinker.MappingMath;
import de.uni_leipzig.simba.multilinker.MultiLinker;
/**
 * Class to quickly compare PseudoFMeasure iff symmetric precision is any improvement.
 * @author Klaus Lyko *
 */
public class CompareFMeasures {
	
	static File outputFile = new File("resources/results/compareFms.txt");
	
	public static final String key_sym = "symPFM";
	public static final String key_asym = "asymPFM";
	
	public CompareFMeasures() {
		  File folder = outputFile.getAbsoluteFile().getParentFile();
	        if (!folder.exists()) {
	            folder.mkdirs();
	        }
	        try {
	            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
	            writer.println();
	            writer.close();
	        } catch (Exception e) {
	            System.err.println("Error writing " + outputFile);
	        }
	}
    
	public static HashMap<String, Cache> getData() {
		HashMap<String, Cache> data = new HashMap<String, Cache>();
		data.put("restaurant", (Cache) DataSetChooser.getData(DataSets.RESTAURANTS_CSV).getSourceCache());
	    data.put("person1", (Cache) DataSetChooser.getData(DataSets.PERSON1_CSV).getSourceCache());
	    data.put("person2",  (Cache) DataSetChooser.getData(DataSets.PERSON2_CSV).getSourceCache());
	    data.put("dblp", (Cache) DataSetChooser.getData(DataSets.DBLPACM).getSourceCache());
	    data.put("acm", (Cache) DataSetChooser.getData(DataSets.DBLPACM).getTargetCache());
	    data.put("abtBuy", (Cache) DataSetChooser.getData(DataSets.ABTBUY).getSourceCache());
	    data.put("scholar", (Cache) DataSetChooser.getData(DataSets.DBLPSCHOLAR).getTargetCache());
	    data.put("amazon", (Cache) DataSetChooser.getData(DataSets.AMAZONGOOGLE).getSourceCache());
	    data.put("googleProducts", (Cache) DataSetChooser.getData(DataSets.AMAZONGOOGLE).getTargetCache());
	    return data;
	}
//    
    public HashMap<String, ResultSet> testSymmetricFMeasure(Cache sC, Cache tC, String name) {
    	HashMap<String, ResultSet> results = new HashMap<String, ResultSet> ();
    	Measure pfm_sym = new PseudoMeasures(true);    	
    	Measure pfm_asym = new PseudoMeasures(false);
    	MultiLinker.fmeasure = pfm_sym;
    	Mapping sym = MultiLinker.getDeterministicUnsupervisedMappings(sC, tC);
    	System.out.println("SymMap.size():" + sym.size());
    	System.out.println("Symm?"+ MultiLinker.fmeasure.getName());  
    	int optimalSize = sC.getAllUris().size();
    	ResultSet res_sym = new ResultSet(name, MappingMath.computeFMeasure(sym, optimalSize), MappingMath.computePrecision(sym, optimalSize), MappingMath.computeRecall(sym, optimalSize));
    	results.put(key_sym, res_sym);
  
    	MultiLinker.fmeasure = pfm_asym;
    	Mapping asym = MultiLinker.getDeterministicUnsupervisedMappings(sC, tC);
      	ResultSet res_asym = new ResultSet(name, MappingMath.computeFMeasure(asym, optimalSize), MappingMath.computePrecision(asym, optimalSize), MappingMath.computeRecall(asym, optimalSize));
        results.put(key_asym, res_asym);
//    	System.out.println("SymMap.size():" + sym.size());
////    	System.out.println("Symm?"+ MultiLinker.fmeasure.getName());
//    	System.out.println("ASymMap.size():" + asym.size());
//    	System.out.println("Symm?"+ MultiLinker.fmeasure.getName());
    	return results;
    }
    
    public static void main(String args[]) {
    	CompareFMeasures cfm = new CompareFMeasures();
    	HashMap<String, Cache> data = getData();
    	for(Entry<String, Cache> ds : data.entrySet()) {
    		Cache alter = MultiLinker.alterCache(ds.getValue(), 0.3d, 10);
    		HashMap<String, ResultSet> res = cfm.testSymmetricFMeasure(ds.getValue(), alter, ds.getKey());
    		 try {
 	            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
 	            writer.println(ds.getKey());
 	            writer.println(key_sym+": "+res.get(key_sym));
 	            writer.println(key_asym+": "+res.get(key_asym));
 	            writer.close();
 	        } catch (Exception e) {
 	            System.err.println("Error writing " + outputFile + " for dataset "+ds.getKey());
 	        }
    	}
    }
    
//    private static void outputResults(
//			HashMap<String, HashMap<String, ResultSet>> all) {
//		for(Entry<String, HashMap<String, ResultSet>>  e : all.entrySet()) {
//			System.out.println(e.getKey() +"\n =========");
//			System.out.println(" Sym: "+e.getValue().get(key_sym));
//			System.out.println("ASym: "+e.getValue().get(key_asym));
//		}
//	}

	class ResultSet {
    	String name;
    	Double fM;
    	Double prec;
    	Double rec;
    	/**
    	 * 
    	 * @param name
    	 * @param fm
    	 * @param prec
    	 * @param rec
    	 */
    	public ResultSet(String name, double fm, double prec, double rec) {
    		this.name = name;
    		this.fM = fm;
    		this.prec = prec;
    		this.rec = rec;
    	}
    	@Override
    	public String toString() {
    		return "f="+fM+" (prec="+prec+", rec="+rec+")";
    	}
    }
}
