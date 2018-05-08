package de.uni_leipzig.simba.execution.evaluation.dynamicevaluation;

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

public class DataConfiguration {
    private String dataset;
    private EvaluationData currentDataset = null;
    private List<String> toyDatasetNames = Arrays.asList("DBLPACM", "DBLPSCHOLAR", "ABTBUY", "AMAZONGOOGLE",
	    "TOWNS", "VILLAGES", "MOVIES");


    public DataConfiguration(String name) {
	if (toyDatasetNames.contains(name))
	    this.dataset = name;
	setCurrentData(this.dataset);

    }

    public EvaluationData getDataset() {
	return this.currentDataset;
    }

    public String getDatasetName() {
	return this.dataset;
    }

    // add more datasets here
    public void setCurrentData(String DatasetName) {
	DataSets d = null;
	if (DatasetName.equalsIgnoreCase("DBLPACM")) {
	    d = DataSets.DBLPACM;
	} else if (DatasetName.equalsIgnoreCase("DBLPSCHOLAR")) {
	    d = DataSets.DBLPSCHOLAR;
	} else if (DatasetName.equals("ABTBUY")) {
	    d = DataSets.ABTBUY;
	} else if (DatasetName.equals("AMAZONGOOGLE")) {
	    d = DataSets.AMAZONGOOGLE;
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
