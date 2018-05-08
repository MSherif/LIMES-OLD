package de.uni_leipzig.simba.grecall.oracle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.util.TreeMap;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.grecall.optimizer.RecallOptimizerFactory;
import de.uni_leipzig.simba.grecall.optimizer.recalloptimizer.Optimizer;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.grecall.util.StatisticsBase;
import de.uni_leipzig.simba.mapper.AtomicMapper;
import de.uni_leipzig.simba.measures.MeasureFactory;
import de.uni_leipzig.simba.specification.LinkSpec;

public class SimpleOracle {

    static Logger logger = Logger.getLogger("LIMES");

    protected List<Double> threshold = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);
    // protected List<Double> threshold = Arrays.asList(0.1,0.2,0.3,0.4);
    protected List<String> measuresString = Arrays.asList("cosine", "levenshtein", "qgrams", "trigrams", "jaccard",
	    "overlap");
    // protected List<String> measuresString = Arrays.asList("trigrams",
    // "levenshtein");
    protected List<String> measuresNumeric = Arrays.asList("euclidean");
    protected List<String> measuresDate = Arrays.asList("daysim", "datesim", "yearsim");
    protected List<String> measuresPointSet = Arrays.asList("hausdorff");
    protected int iterations = 5;

    protected static final String FILE_HEADER = "Link Specification\tMetric\tThreshold\tRuntime\tMappingSize\tSelectivity";
    protected static final String TAB_DELIMITER = "\t";
    protected static final String NEW_LINE_SEPARATOR = "\n";

    // iteration - set of queries represented as <Key,StatisticsBase>
    private HashMap<Integer, LinkedList<DiffPair<LinkSpec, StatisticsBase>>> OraclePerIteration = new HashMap<Integer, LinkedList<DiffPair<LinkSpec, StatisticsBase>>>();
    // query: presented as <Key,avg Statistics>
    private LinkedHashMap<LinkSpec, StatisticsBase> OraclePerLS = new LinkedHashMap<LinkSpec, StatisticsBase>();

    private String baseDirectory;
    private String DatasetName;

    public SimpleOracle(String DatasetName) {
	this.DatasetName = DatasetName;
	this.baseDirectory = "datasets/" + this.DatasetName;
	File dirName = new File(this.baseDirectory);
	if (!dirName.isDirectory()) {
	    try {
		dirName.mkdir();
	    } catch (SecurityException se) {
	    }
	}
	this.baseDirectory = "datasets/" + this.DatasetName + "/oracle";
	dirName = new File(this.baseDirectory);
	if (!dirName.isDirectory()) {
	    try {
		dirName.mkdir();
	    } catch (SecurityException se) {
	    }
	}
    }

    public void setKey(int it) {
	if (this.OraclePerIteration.get(it) == null) {
	    this.OraclePerIteration.put(it, new LinkedList());
	}

    }

    public void setValue(int it, String metric, double thres, long runtime, float mappingSize, float selectivity) {

	LinkedList<DiffPair<LinkSpec, StatisticsBase>> pairList = this.OraclePerIteration.get(it);
	DiffPair<LinkSpec, StatisticsBase> pair = new DiffPair(new LinkSpec(metric, thres),
		new StatisticsBase(runtime, mappingSize, selectivity));
	pairList.add(pair);
    }

    public String getBaseDirectory() {
	return this.baseDirectory;
    }

    public double returnNextThreshold(double thrs) {
	for (double f : this.threshold) {
	    if ((float) thrs < (float) f)
		return f;
	}
	return -0.1d;
    }
    public double returnPreviousThreshold(double thrs) {
	double t = 0d;
	for (double f : this.threshold) {
	    if ((float) thrs > (float) f)
		t = f;
	}
	return t;
    }
    public float askOracleForRuntime(LinkSpec spec) {
	float runtime = -1.0f;
	Iterator specIt = this.OraclePerLS.entrySet().iterator();
	while (specIt.hasNext()) {
	    Map.Entry entry = (Map.Entry) specIt.next();
	    LinkSpec currentSpec = (LinkSpec) entry.getKey();
	    StatisticsBase statistics = (StatisticsBase) entry.getValue();
	    if ((currentSpec.getFilterExpression()).equals(spec.getFilterExpression().replace("\"", ""))
		    && currentSpec.threshold == spec.threshold) {
		runtime = statistics.runtime;
		break;
	    }

	}
	return runtime;

    }

    public float askOracleForSelectivity(LinkSpec spec) {
	float selectivity = -1.0f;
	Iterator specIt = this.OraclePerLS.entrySet().iterator();
	while (specIt.hasNext()) {
	    Map.Entry entry = (Map.Entry) specIt.next();
	    LinkSpec currentSpec = (LinkSpec) entry.getKey();
	    StatisticsBase statistics = (StatisticsBase) entry.getValue();

	    if ((currentSpec.getFilterExpression()).equals(spec.getFilterExpression().replace("\"", ""))
		    && currentSpec.threshold == spec.threshold) {
		selectivity = statistics.selectivity;
		break;
	    }

	}
	return selectivity;

    }

    public void createOracle(HashMap<String, String> FeaturePairs, Cache source, Cache target) {

	ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
	for (int it = 0; it < iterations; it++) {
	    Iterator FeaturePair = FeaturePairs.entrySet().iterator();
	    // logger.info(metric + features+" "+thres+" "+it);
	    logger.info(" ---> Iteration: " + it);
	    while (FeaturePair.hasNext()) {

		Map.Entry entry = (Map.Entry) FeaturePair.next();
		String features = (String) entry.getKey();
		String type = (String) entry.getValue();
		logger.info("Working on pair: " + features);
		List<String> metrics = null;
		if (type.equalsIgnoreCase("string"))
		    metrics = measuresString;
		else if (type.equalsIgnoreCase("numeric"))
		    metrics = measuresNumeric;
		else if (type.equalsIgnoreCase("date"))
		    metrics = measuresDate;
		else if (type.equalsIgnoreCase("pointset"))
		    metrics = measuresPointSet;
		// each string metric
		for (String metric : metrics) {
		    logger.info("with similarity: " + metric);
		    // for each threshold
		    for (double thres : threshold) {
			// for each iteration
			logger.info("Threshold: " + thres);
			ExecutionPlanner p = new CanonicalPlanner();
			Mapping m = new Mapping();
			LinkSpec spec = new LinkSpec(metric + features, thres);

			// compute runtime
			long runtime = System.currentTimeMillis();
			NestedPlan np = p.plan(spec);
			m = ee.runNestedPlan(np);
			runtime = System.currentTimeMillis() - runtime;

			long mappingSize = m.getNumberofMappings();
			float selectivity = (float) mappingSize / (float) (source.size() * target.size());

			this.setKey(it);
			this.setValue(it, metric + features, thres, runtime, mappingSize, selectivity);

			writeStatisticsPerIteration();
			this.OraclePerIteration.remove(it);
			logger.info("-------");
		    }
		    logger.info("-------");
		}
		logger.info("-------");
	    }

	}
    }

    public void loadOracle(String ExtensionName) {
	BufferedReader br = null;
	try {
	    String csvFile = this.baseDirectory + "/" + ExtensionName + "/statistics.csv";
	    br = new BufferedReader(new FileReader(csvFile));

	    String line = "";
	    line = br.readLine();
	    while ((line = br.readLine()) != null) {
		String[] fullEntry = line.split(TAB_DELIMITER);

		// System.out.println(fullEntry[0]);
		LinkSpec currentLS = new LinkSpec(fullEntry[1], Double.parseDouble(fullEntry[2]));
		// runtime, mappingsize, selectivity
		StatisticsBase currentStBase = new StatisticsBase(Float.parseFloat(fullEntry[3]),
			Float.parseFloat(fullEntry[4]), Float.parseFloat(fullEntry[5]));
		this.OraclePerLS.put(currentLS, currentStBase);
	    }

	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}

	System.out.println("Done");

    }

    public void writeStatisticsPerIteration() {
	int counter = 0;
	try {

	    Iterator entries = this.OraclePerIteration.entrySet().iterator();
	    while (entries.hasNext()) {
		Map.Entry entry = (Map.Entry) entries.next();
		Integer iteration = (Integer) entry.getKey();

		@SuppressWarnings("unchecked")
		LinkedList<DiffPair<LinkSpec, StatisticsBase>> pairList = (LinkedList<DiffPair<LinkSpec, StatisticsBase>>) entry
			.getValue();

		String DirName = this.baseDirectory + "/" + iteration;
		File dirName = new File(DirName);
		if (!dirName.isDirectory()) {
		    try {
			dirName.mkdir();
		    } catch (SecurityException se) {
		    }
		}

		String FileName = DirName + "/" + "statistics.csv";
		File f = new File(FileName);
		FileWriter fileWriter = null;

		try {
		    if (f.exists()) {
			fileWriter = new FileWriter(f, true);
		    } else {
			fileWriter = new FileWriter(f);
			fileWriter.append(FILE_HEADER.toString());
			fileWriter.append(NEW_LINE_SEPARATOR);
		    }

		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}

		for (DiffPair pair : pairList) {
		    counter++;
		    LinkSpec key = (LinkSpec) pair.getX();
		    StatisticsBase stBase = (StatisticsBase) pair.getY();
		    try {
			fileWriter.append(key.getFilterExpression() + ">=" + key.threshold);
			fileWriter.append(TAB_DELIMITER);
			fileWriter.append(key.getFilterExpression());
			fileWriter.append(TAB_DELIMITER);
			DecimalFormat df = new DecimalFormat("#");
			df.setMaximumFractionDigits(50);
			fileWriter.append(df.format(key.threshold));
			fileWriter.append(TAB_DELIMITER);
			fileWriter.append(df.format(stBase.runtime));
			fileWriter.append(TAB_DELIMITER);
			fileWriter.append(df.format(stBase.mappingSize));
			fileWriter.append(TAB_DELIMITER);
			fileWriter.append(df.format(stBase.selectivity));
			fileWriter.append(NEW_LINE_SEPARATOR);

			fileWriter.flush();
		    } catch (Exception e) {
			e.printStackTrace();
		    }

		}
		fileWriter.close();
	    }

	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	logger.info("Number of experiments: " + counter);

    }

    public void UpdateLS(String type, LinkSpec currentLS, StatisticsBase currentStBase) {
	Iterator it = this.OraclePerLS.entrySet().iterator();
	LinkSpec newKey = null;
	StatisticsBase newStBase = null;
	boolean flag = false;

	// search OraclePerLS for the LS
	while (it.hasNext()) {
	    Map.Entry entry = (Map.Entry) it.next();
	    LinkSpec spec = (LinkSpec) entry.getKey();

	    if (spec.getFilterExpression().equals(currentLS.getFilterExpression())
		    && spec.threshold == currentLS.threshold) {
		// there is already an entry for this query, obtain it
		newKey = spec;
		newStBase = (StatisticsBase) entry.getValue();
		flag = true;
		break;
	    }

	} // LS not found in Oracle
	if (flag == false) {
	    newKey = new LinkSpec(currentLS.getFilterExpression(), currentLS.threshold);
	    newStBase = new StatisticsBase();
	    this.OraclePerLS.put(newKey, newStBase);

	}
	if (type.equals("avg"))
	    Avg(newStBase, currentStBase);
	else if (type.equals("min"))
	    Min(newStBase, currentStBase);
	else if (type.equals("max"))
	    Max(newStBase, currentStBase);

    }

    public void Max(StatisticsBase newStBase, StatisticsBase currentStBase) {

	newStBase.mappingSize = Math.max((float) currentStBase.mappingSize, newStBase.mappingSize);
	newStBase.runtime = Math.max((float) currentStBase.runtime, newStBase.runtime);
	newStBase.selectivity = Math.max((float) currentStBase.selectivity, newStBase.selectivity);
    }

    public void Min(StatisticsBase newStBase, StatisticsBase currentStBase) {
	if (newStBase.mappingSize == 0)
	    newStBase.mappingSize = Long.MAX_VALUE;
	if (newStBase.runtime == 0)
	    newStBase.runtime = Long.MAX_VALUE;
	if (newStBase.selectivity == 0)
	    newStBase.selectivity = Long.MAX_VALUE;

	newStBase.mappingSize = Math.min((float) currentStBase.mappingSize, newStBase.mappingSize);
	newStBase.runtime = Math.min((float) currentStBase.runtime, newStBase.runtime);
	newStBase.selectivity = Math.min((float) currentStBase.selectivity, newStBase.selectivity);
    }

    public void Avg(StatisticsBase newStBase, StatisticsBase currentStBase) {
	newStBase.mappingSize += ((float) currentStBase.mappingSize / (float) (iterations));
	newStBase.runtime += ((float) currentStBase.runtime / (float) (iterations));
	newStBase.selectivity += ((float) currentStBase.selectivity / (float) (iterations));
    }

    public void setOraclePerLS(String type, String DatasetName) {

	BufferedReader br = null;
	for (int it = 0; it < iterations; it++) {
	    try {
		String csvFile = this.baseDirectory + "/" + it + "/statistics.csv";
		br = new BufferedReader(new FileReader(csvFile));

		String line = "";
		line = br.readLine();
		while ((line = br.readLine()) != null) {
		    String[] fullEntry = line.split(TAB_DELIMITER);

		    LinkSpec currentLS = new LinkSpec(fullEntry[1], Double.parseDouble(fullEntry[2]));
		    // runtime, mappingsize, selectivity
		    StatisticsBase currentStBase = new StatisticsBase(Float.parseFloat(fullEntry[3]),
			    Float.parseFloat(fullEntry[4]), Float.parseFloat(fullEntry[5]));
		    UpdateLS(type, currentLS, currentStBase);

		}

	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		if (br != null) {
		    try {
			br.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }

	    System.out.println("Done");
	}

    }

    public void writeStatisticsPerLS(String DirNameExtension) {

	String DirName = this.baseDirectory + "/" + DirNameExtension + "/";
	logger.info(DirName);
	File dirName = new File(DirName);
	if (!dirName.isDirectory()) {
	    try {
		dirName.mkdir();
	    } catch (SecurityException se) {
	    }
	}
	String FileName = DirName + "/" + "statistics.csv";
	File f = new File(FileName);
	FileWriter fileWriter = null;

	try {
	    if (f.exists()) {
		fileWriter = new FileWriter(f, true);
	    } else {
		fileWriter = new FileWriter(f);
		fileWriter.append(FILE_HEADER.toString());
		fileWriter.append(NEW_LINE_SEPARATOR);
	    }

	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	int counter = 0;
	Iterator reverseEntries = OraclePerLS.entrySet().iterator();
	while (reverseEntries.hasNext()) {

	    Map.Entry entry = (Map.Entry) reverseEntries.next();
	    LinkSpec key = (LinkSpec) entry.getKey();
	    StatisticsBase stBase = (StatisticsBase) entry.getValue();

	    try {
		counter++;
		fileWriter.append(key.getFilterExpression() + ">=" + key.threshold);
		fileWriter.append(TAB_DELIMITER);

		fileWriter.append(key.getFilterExpression());
		fileWriter.append(TAB_DELIMITER);

		DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(50);
		fileWriter.append(df.format(key.threshold));
		fileWriter.append(TAB_DELIMITER);
		df = new DecimalFormat("#");
		df.setMaximumFractionDigits(50);
		fileWriter.append(df.format(stBase.runtime));
		fileWriter.append(TAB_DELIMITER);
		df = new DecimalFormat("#");
		df.setMaximumFractionDigits(50);
		fileWriter.append(df.format(stBase.mappingSize));
		fileWriter.append(TAB_DELIMITER);
		df = new DecimalFormat("#");
		df.setMaximumFractionDigits(50);
		fileWriter.append(df.format(stBase.selectivity));
		fileWriter.append(NEW_LINE_SEPARATOR);
		fileWriter.flush();
	    } catch (Exception e) {
		e.printStackTrace();
	    }

	}
	logger.info("Number of LS: " + counter);
    }
}
