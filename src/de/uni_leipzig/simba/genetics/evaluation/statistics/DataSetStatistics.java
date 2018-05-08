package de.uni_leipzig.simba.genetics.evaluation.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;

/**
 * Get statistic data about datasets.
 * @author Klaus Lyko
 *
 */
public class DataSetStatistics {

	static Logger logger = Logger.getLogger("LIMES");
	EvaluationData data;
	
	public DataSetStatistics(EvaluationData data) throws Exception {
		this.data = data;
		if(data.getSourceCache() == null || data.getTargetCache() == null || data.getReferenceMapping() == null) {
			logger.error("One of the required fields isn't set!");
			throw new Exception("Required data field is null.");
		}
		if(data.getSourceCache().size() == 0 || data.getTargetCache().size() == 0 || data.getReferenceMapping().size() == 0) {
			logger.error("One of the required fields(Cache or Reference Mapping) is empty.");
			throw new Exception("Required data field is empty.");
		}
	}
	
	public Map<String, String> getData() {
		Map<String, String> answer = new HashMap<String,String>();

		answer.put("Source Cache size()", ""+data.getSourceCache().getAllInstances().size());
		answer.put("Target Cache size()", ""+data.getTargetCache().getAllInstances().size());

		double d1 = data.getSourceCache().size();
		double d2 = data.getTargetCache().size();
		
		answer.put("Proportion sC/tC", ""+(d1/d2));
		answer.put("Reference Map size()", ""+data.getReferenceMapping().size());
		
		int missingSourceInst = 0;int missingTargetInst = 0;
		
		// s --> t
		
		Mapping ref = data.getReferenceMapping();
		int ref_sIs = ref.map.keySet().size();
		HashSet<String> ref_targetInst = new HashSet<String>();
		int numberOfDoubledtInstances = 0;	
		Statistics stat = new Statistics();
		for(String uri : ref.map.keySet()) {
			if(!data.getSourceCache().getAllUris().contains(uri)) {
				missingSourceInst++;
			}
			stat.add(ref.map.get(uri).size());
			for(String tinst : ref.map.get(uri).keySet()) {
				if(!data.getTargetCache().getAllUris().contains(tinst)) {
					missingTargetInst++;
				}
				boolean newInst = ref_targetInst.add(tinst);
				if(!newInst)
					numberOfDoubledtInstances++;
			}
			
		}
		answer.put("missing source instances", ""+missingSourceInst);
		answer.put("missing target instances", ""+missingTargetInst);
		answer.put("# mapped Source Instances", ""+ref_sIs);
		answer.put("# mapped Target Instances", ""+(ref_targetInst.size()+numberOfDoubledtInstances));
		answer.put("double mapped tInstances", ""+numberOfDoubledtInstances);
		answer.put("mean number of target instances a sC is mapped to", ""+stat.mean);
		
		// t --> s
		
				ref = data.getReferenceMapping().reverseSourceTarget();
				ref_sIs = ref.map.keySet().size();
				ref_targetInst = new HashSet<String>();
				numberOfDoubledtInstances = 0;	
				stat = new Statistics();
				for(String uri : ref.map.keySet()) {
					if(!data.getSourceCache().getAllUris().contains(uri)) {
						missingSourceInst++;
					}
					stat.add(ref.map.get(uri).size());
					for(String tinst : ref.map.get(uri).keySet()) {
						if(!data.getTargetCache().getAllUris().contains(tinst)) {
							missingTargetInst++;
						}
						boolean newInst = ref_targetInst.add(tinst);
						if(!newInst)
							numberOfDoubledtInstances++;
					}
					
				}
//				answer.put("missing source instances", ""+missingSourceInst);
//				answer.put("missing target instances", ""+missingTargetInst);
//				answer.put("# mapped Source Instances", ""+ref_sIs);
//				answer.put("# mapped Target Instances", ""+(ref_targetInst.size()+numberOfDoubledtInstances));
				answer.put("double mapped sInstances", ""+numberOfDoubledtInstances);
				answer.put("mean number of source instances a tC is mapped to", ""+stat.mean);
		
		
		
		return answer;
	}
	
	static String getPrint(	Map<String, String> answer ) {
		String o = "";
		for(Entry<String, String> e : answer.entrySet()) {
			o+=""+System.getProperty("line.separator");
			o+=e.getKey() + " = " + e.getValue();
		}
		return o;
	}
	
	
	public static Collection<EvaluationData> getDataSets() {
		Collection<EvaluationData> coll = new Vector();
		coll.add(DataSetChooser.getData(DataSets.PERSON1));
		coll.add(DataSetChooser.getData(DataSets.PERSON2));
//		coll.add(DataSetChooser.getData(DataSets.PERSON2_CSV));
		coll.add(DataSetChooser.getData(DataSets.RESTAURANTS));
//		coll.add(DataSetChooser.getData(DataSets.RESTAURANTS_CSV));
		coll.add(DataSetChooser.getData(DataSets.ABTBUY));
		coll.add(DataSetChooser.getData(DataSets.DBLPACM));
		coll.add(DataSetChooser.getData(DataSets.AMAZONGOOGLE));
		coll.add(DataSetChooser.getData(DataSets.DBLPSCHOLAR));
		coll.add(DataSetChooser.getData(DataSets.DBPLINKEDMDB));
		return coll;
	}
	
	
	public static void main(String args[]) {
		Collection<EvaluationData> datasets = getDataSets();
		for(EvaluationData data : datasets) {
			try {
				DataSetStatistics stat = new DataSetStatistics(data);
				System.out.print("==="+data.getName()+">>>>>>>>>>>>>>>");
				System.out.println(getPrint(stat.getData()));
			} catch (Exception e) {
				System.err.println("Error for dataset "+data.getName());
				e.printStackTrace();
			}
		}
	}
	
}
