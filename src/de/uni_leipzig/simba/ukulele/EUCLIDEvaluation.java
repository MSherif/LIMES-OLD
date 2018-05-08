package de.uni_leipzig.simba.ukulele;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.keydiscovery.model.CandidateNode;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.DisjunctiveMeshSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.LinearMeshSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

public class EUCLIDEvaluation {

	FileWriter writer;
	public EUCLIDEvaluation() throws IOException {
		writer = new FileWriter("EUCLID_ROCER.csv", false);
		EvaluationResult res = new EvaluationResult();
		writer.write("Eval;"+res.getHeader());
		writer.write(System.getProperty("line.separator"));
		writer.flush();
		
	}
	public void close() throws IOException {
		writer.close();
	}
	/**
	 * Limits data of the Caches to those properties conatined in the PropertyMapping and updates the data accordingly.
	 * @param data
	 * @param propMap 
	 * @return an updated EvaluationData instance.
	 */
	public static EvaluationData limitCachesForEUCLID(EvaluationData data, PropertyMapping propMap) {
		// first source
		Set<String> sPs = data.getSourceCache().getAllProperties();
		Set<String> notSPs = new HashSet<String>();
		for(String sp : sPs) {
			if(!propMap.containsSourceProp(sp))
				notSPs.add(sp);
		}
		for(Instance i : data.getSourceCache().getAllInstances()) {
			for(String sP : notSPs)
				i.removePropery(sP);
		}
		// then target
		sPs = data.getTargetCache().getAllProperties();
		notSPs.clear();
		for(String sp : sPs) {
			if(!propMap.containsTargetProp(sp))
				notSPs.add(sp);
		}
		for(Instance i : data.getTargetCache().getAllInstances()) {
			for(String sP : notSPs)
				i.removePropery(sP);
		}
		return data;
	}
	
	
	public void runExperiment(DataSets ds, EvaluationParameter params) throws IOException {
		EvaluationData data = DataSetChooser.getData(ds);		
		// get Candidates
		List<Set<CandidateNode>> rockerResult = RockerRunner.runRocker(data, params.rockerCoverage);		
		String types[] =  {"linear", "disjunctive", "conjunctive"};

		EvaluationData dataROCKER =DataSetChooser.getData(ds);
		dataROCKER = limitCachesForEUCLID(dataROCKER, Evaluation.buildPropertyMappingForEAGLE(rockerResult.get(0), rockerResult.get(1)));
		writer.write(dataROCKER.getName());			
		writer.write(System.getProperty("line.separator"));
		for(String type : types) {
			//first ROCKER+EUCLID
			EvaluationResult resRE = runEuclid(dataROCKER, params, type);			
			writer.write("ROCER;"+resRE.toString());			
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
			//second plain EUCLID
			EvaluationResult resE = runEuclid(data, params, type);
			writer.write("plain;"+resE.toString());			
			writer.write(System.getProperty("line.separator"));
			// write to file
			writer.flush();
		}

	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		EvaluationParameter params = new EvaluationParameter();
		DataSets datas[] = {
//				DataSets.PERSON1,
				DataSets.PERSON2,
//				DataSets.RESTAURANTS_FIXED,
				DataSets.DBLPACM,
				DataSets.ABTBUY,
				DataSets.DBPLINKEDMDB,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPSCHOLAR,
		};
		EUCLIDEvaluation eval = new EUCLIDEvaluation();
		eval.setOutStreams("ukele_euclid");
		for(DataSets ds : datas) {
			eval.runExperiment(ds, params);
		}
		eval.close();
	}
	
	
	public EvaluationResult runEuclid(EvaluationData data, EvaluationParameter params, String type) {
		MeshBasedSelfConfigurator lsc;
		EvaluationResult result = new EvaluationResult();
        if (type.toLowerCase().startsWith("l")) {
            lsc = new LinearMeshSelfConfigurator(data.getSourceCache(), data.getTargetCache(), params.euclidCoverage, 1d);
            result.type = "linear";
        } else if (type.toLowerCase().startsWith("d")) {
            lsc = new DisjunctiveMeshSelfConfigurator(data.getSourceCache(), data.getTargetCache(), params.euclidCoverage, 1d);
            result.type = "disjunctive";
        } else {
            lsc = new MeshBasedSelfConfigurator(data.getSourceCache(), data.getTargetCache(), params.euclidCoverage, 1d);
            result.type = "conjunctive";
        }
        lsc.setMeasure(new PseudoMeasures());
        System.out.println("Running EUCLID "+type+" coverage="+params.euclidCoverage+". Computing simple classifiers...");
        long begin = System.currentTimeMillis();
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();
        long middle = System.currentTimeMillis();
        result.durationInitial = middle-begin;
        System.out.println(cp.size()+" simple classifiers computed in "+(middle-begin)+" ms. Computing complex classifier for "+params.euclidIterations+" iterations...");
        ComplexClassifier cc = lsc.getZoomedHillTop(5, params.euclidIterations, cp);
        long end = System.currentTimeMillis();
        result.durationMS = end - begin;
        System.out.println("Eculid finished after "+(end-begin)+" ms = "+(end-begin)/1000+" s." );
        System.out.println("Complex Classifier= "+cc);
        System.out.println("Mapping size = "+cc.mapping.size());
        
        PRFCalculator prf = new PRFCalculator();
        result.fMeasure = prf.fScore(cc.mapping, data.getReferenceMapping());
        result.precision = prf.precision(cc.mapping, data.getReferenceMapping());
        result.recall = prf.recall(cc.mapping, data.getReferenceMapping());
        result.classifier = cc.toString();
		return result;
	}

	
	public class EvaluationResult {
		public static final String SEP = ";";
		public String type = "";
		public String classifier = "";
		public double fMeasure = 0d;
		public double precision = 0d;
		public double recall = 0d;
		public long durationMS = 0l;
		public long durationInitial = 0l;
		
		public String getHeader() {
			String out = "type"+SEP+
					"f"+SEP+
					"d (ms)"+SEP+
					"d (s)"+SEP+
					"d initials ms"+SEP+
					"p"+SEP+
					"r"+SEP+
					"classifier";
			return out;
		}
		@Override
		public String toString() {
			String out = ""+type+SEP+
					fMeasure+SEP+
					durationMS+SEP+
					(durationMS/1000)+SEP+
					durationInitial+SEP+
					precision+SEP+
					recall+SEP+
					classifier;
			return out;
		}
	}
	
	public void setOutStreams(String name) {
		try {
			File stdFile = new File(name+"_stdOut.txt");
			PrintStream stdOut;
			stdOut = new PrintStream(new FileOutputStream(stdFile, false));
			File errFile = new File(name+"_errOut.txt");
			PrintStream errOut = new PrintStream(new FileOutputStream(errFile, false));
			System.setErr(errOut);
			System.setOut(stdOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
