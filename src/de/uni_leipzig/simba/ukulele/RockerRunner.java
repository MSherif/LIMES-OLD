package de.uni_leipzig.simba.ukulele;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.keydiscovery.RockerKeyDiscovery;
import de.uni_leipzig.simba.keydiscovery.model.CandidateNode;
import de.uni_leipzig.simba.util.CSVtoRDF;

/**
 * @author Klaus Lyko <klaus.lyko@informatik.uni-leipzig.de>
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class RockerRunner {

	
	
	
	private static HashMap<Arg, RockerKeyDiscovery> rockers = new HashMap<RockerRunner.Arg, RockerKeyDiscovery>();
	
	public enum Arg {
		SOURCE(0, "source"), TARGET(1, "target");
		private final int position;
		private final String name;
		Arg(int position, String name) {
			this.position = position;
			this.name = name;
		}
		public int getPosition() {
			return position;
		}
		public String getName() {
			return name;
		}
	}
	
	static {
		rockers.put(Arg.SOURCE, new RockerKeyDiscovery());
		rockers.put(Arg.TARGET, new RockerKeyDiscovery());
	}
	
	/**
	 * Constructs list with two entries, with the results of Rocker for source and target.
	 * @param data
	 * @return
	 */
	public static List<Set<CandidateNode>> runRocker(EvaluationData data, double coverage) {
		List<Set<CandidateNode>> result = new ArrayList<Set<CandidateNode>>();
//		if(ds.equals(DataSets.PERSON1) || ds.equals(DataSets.PERSON2) || ds.equals(DataSets.RESTAURANTS)) {
//			
//		}
		
		File tmp1 = CSVtoRDF.CacheToJenaModelNTSerialization(data.getSourceCache(), "", "", data.getSourceClass(), "temp1.nt");
		result.add(runRocker(data.getName(), tmp1, data.getSourceClass(), Arg.SOURCE, coverage));
		File tmp2 = CSVtoRDF.CacheToJenaModelNTSerialization(data.getTargetCache(), "", "", data.getTargetClass(), "temp2.nt");
		result.add(runRocker(data.getName(), tmp2, data.getTargetClass(), Arg.TARGET, coverage));
		
		return result;
	}
	
	
	
	public static Set<CandidateNode>  runRocker(String name, File f, String rdfType, Arg arg, double coverage) {
		RockerKeyDiscovery rocker = rockers.get(arg);
		rocker.init(name, "file:///"+f.getAbsolutePath(), rdfType, false, coverage, false);
		rocker.run();
		Set<CandidateNode> nodes = rocker.getResults();
//		for(CandidateNode node : nodes) {
//			System.out.println("node "+node.getProperties()+" score:="+node.getScore());
//		}
		return nodes;
	}
	
	public static HashMap<Arg, RockerKeyDiscovery> getRockers() {
		return rockers;
	}
}
