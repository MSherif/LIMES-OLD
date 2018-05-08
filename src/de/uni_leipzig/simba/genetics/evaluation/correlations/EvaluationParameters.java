package de.uni_leipzig.simba.genetics.evaluation.correlations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;

/**
 * Central class to configure parameters for the eval.
 * 
 * @author Lyko
 * @deprecated
 */
public class EvaluationParameters {

	public static HashMap<String, Object> params = new HashMap<String, Object>();
	public static List<EvaluationData> datasets = new LinkedList<EvaluationData>();

	static {// genetic params
		params.put("runs", 5);
		params.put("cycles", 10);
		params.put("oracleSize", 10);
		params.put("generations", 100);
		params.put("populationSize", 20);
		params.put("mutationRate", 0.5f);
		params.put("preserveFittest", false);
		params.put("granularity", 2);
		params.put("trainingDataSize", 10);
	}

	static { // eval datasets
	 datasets.add(DataSetChooser.getData(DataSets.DBLPACM));
	 datasets.add(DataSetChooser.getData(DataSets.ABTBUY));
	 datasets.add(DataSetChooser.getData(DataSets.PERSON1_CSV));
	 datasets.add(DataSetChooser.getData(DataSets.PERSON2_CSV));
	 datasets.add(DataSetChooser.getData(DataSets.RESTAURANTS_CSV));

	}

}
