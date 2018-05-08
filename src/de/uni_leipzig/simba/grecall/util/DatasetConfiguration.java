package de.uni_leipzig.simba.grecall.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.specification.LinkSpec;

public class DatasetConfiguration {
    private List<String> datasets = new ArrayList<String>();
    private EvaluationData currentDataset = null;
    private List<String> toyDatasetNames = Arrays.asList("DBLPACM", "DBLPSCHOLAR", "PERSON1", "PERSON2", "PERSON1_CSV",
	    "PERSON2_CSV", "RESTAURANTS_CSV", "RESTAURANTS", "RESTAURANTS_FIXED", "ABTBUY", "AMAZONGOOGLE",
	    "DBPLINKEDMDB", "DRUGS", "OAEI2014BOOKS", "TOWNS", "VILLAGES", "MOVIES");

    public DatasetConfiguration() {
	// this.datasets = Arrays.asList("DBLPACM");
	this.datasets = Arrays.asList("DBLPACM", "DBLPSCHOLAR", "PERSON1", "PERSON2", "PERSON1_CSV", "PERSON2_CSV",
		"RESTAURANTS_CSV", "RESTAURANTS", "RESTAURANTS_FIXED", "ABTBUY", "AMAZONGOOGLE", "DBPLINKEDMDB",
		"DRUGS", "OAEI2014BOOKS", "TOWNS", "VILLAGES", "MOVIES");
    }

    public DatasetConfiguration(String[] names) {
	for (String s : names)
	    if (toyDatasetNames.contains(s))
		this.datasets.add(s);

    }

    public EvaluationData getCurrentDataset() {
	return this.currentDataset;
    }

    public List<String> getDatasets() {
	return this.datasets;
    }

    // add more datasets here
    public void setCurrentData(String DatasetName) {
	DataSets d = null;
	if (DatasetName.equalsIgnoreCase("DBLPACM")) {
	    d = DataSets.DBLPACM;
	} else if (DatasetName.equalsIgnoreCase("DBLPSCHOLAR")) {
	    d = DataSets.DBLPSCHOLAR;
	} else if (DatasetName.equalsIgnoreCase("PERSON1")) {
	    d = DataSets.PERSON1;
	} else if (DatasetName.equalsIgnoreCase("PERSON2")) {
	    d = DataSets.PERSON2;
	} else if (DatasetName.equalsIgnoreCase("RESTAURANTS")) {
	    d = DataSets.RESTAURANTS;
	} else if (DatasetName.equals("RESTAURANTS_FIXED")) {
	    d = DataSets.RESTAURANTS_FIXED;
	} else if (DatasetName.equals("ABTBUY")) {
	    d = DataSets.ABTBUY;
	} else if (DatasetName.equals("AMAZONGOOGLE")) {
	    d = DataSets.AMAZONGOOGLE;
	} else if (DatasetName.equals("DBPLINKEDMDB")) {
	    d = DataSets.DBPLINKEDMDB;
	} else if (DatasetName.equals("DRUGS")) {
	    d = DataSets.DRUGS;
	} else if (DatasetName.equals("PERSON1_CSV")) {
	    d = DataSets.PERSON1_CSV;
	} else if (DatasetName.equals("PERSON2_CSV")) {
	    d = DataSets.PERSON2_CSV;
	} else if (DatasetName.equals("RESTAURANTS_CSV")) {
	    d = DataSets.RESTAURANTS_CSV;
	} else if (DatasetName.equals("OAEI2014BOOKS")) {
	    d = DataSets.OAEI2014BOOKS;
	} else if (DatasetName.equals("TOWNS")) {
	    d = DataSets.TOWNS;
	} else if (DatasetName.equals("VILLAGES")) {
	    d = DataSets.VILLAGES;
	} else if (DatasetName.equals("MOVIES")) {
	    d = DataSets.MOVIES;
	}

	else {
	    System.out.println("Experiment " + DatasetName + " Not implemented yet");
	    this.currentDataset = null;
	    return;
	}

	this.currentDataset = DataSetChooser.getData(d);

    }

    public List<Pair<String>> getStringFeatures() {
	return this.currentDataset.getPropertyMapping().stringPropPairs;
    }

    public List<Pair<String>> getNumericFeatures() {
	return this.currentDataset.getPropertyMapping().numberPropPairs;
    }

    public List<Pair<String>> getDateFeatures() {
	return this.currentDataset.getPropertyMapping().datePropPairs;
    }

    private List<Pair<String>> getPointSetFeatures() {
	return this.currentDataset.getPropertyMapping().pointsetPropPairs;
    }

    public HashMap<String, String> createFeaturePairs() {
	HashMap<String, List<Pair<String>>> labels = new HashMap<String, List<Pair<String>>>();
	labels.put("string", this.getStringFeatures());
	labels.put("numeric", this.getNumericFeatures());
	labels.put("date", this.getDateFeatures());
	labels.put("pointset", this.getPointSetFeatures());
	
	HashMap<String, String> featurePairs = new HashMap<String, String>();

	Iterator entries = labels.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry entry = (Map.Entry) entries.next();
	    
	    String key = (String) entry.getKey();
	    List<Pair<String>> values = (List<Pair<String>>) entry.getValue();
	    
	    for (Pair<String> pair : values) {
		featurePairs.put("(x." + pair.a + ",y." + pair.b + ")", key);
	    }
	}

	return featurePairs;

    }

    public Cache getSource() {
	return this.currentDataset.getSourceCache();
    }

    public Cache getTarget() {
	return this.currentDataset.getTargetCache();
    }

    public List<String> getSpecifications(String FileName) {

	List<String> result = new ArrayList<String>();
	try {
	    @SuppressWarnings("resource")
	    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FileName), "UTF8"));
	    String s = reader.readLine();
	    while (s != null) {
		s = s.replace("\"", "");
		if (s.contains(">=")) {
		    result.add(s);
		}
		s = reader.readLine();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return result;

    }
}
