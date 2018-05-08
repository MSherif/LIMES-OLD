package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;

public class TestDataSetChooser {
	
	@Test
	public void testGetData() {
		HashMap<DataSets, Boolean> map = new HashMap<DataSets, Boolean>();
		for(DataSets ds : DataSets.values()) {
			EvaluationData hp = DataSetChooser.getData(ds);
			if(hp.getConfigReader() == null) {
				map.put(ds, false);
				System.out.println("ConfigReader in DataSet "+ hp.getName() +" is null.");
			}
			if(hp.getPropertyMapping() == null || !hp.getPropertyMapping().wasSet()) {
				map.put(ds, false);
				System.out.println("PropertyMapping in DataSet "+ hp.getName() +" is null or not set.");
				break;
			}
			if(hp.getSourceCache() == null || hp.getSourceCache().size() == 0) {
				map.put(ds, false);
				System.out.println("SourceCache in DataSet "+ hp.getName() +" is null or empty");
			}
			if(hp.getTargetCache() == null || hp.getTargetCache().size() == 0) {
				map.put(ds, false);
				System.out.println("TargetCache in DataSet "+ hp.getName() +" is null or empty");
			}
			if(hp.getReferenceMapping() == null || hp.getReferenceMapping().size() == 0) {
				map.put(ds, false);
				System.out.println("ReferenceMapping in DataSet "+ hp.getName() +" is null or empty");
			}
		
		}
		if(map.size()>0) {
			for(DataSets ds : map.keySet()) {
				System.out.println("DataSets "+ds+" has wrong values ("+ds.name()+")");
			}
			assertTrue(false);
		}else {
			assertTrue(true);
		}
	}
}
