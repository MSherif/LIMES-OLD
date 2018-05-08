package de.uni_leipzig.simba.grecall.oracle;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openjena.atlas.logging.Log;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.histogram.DataGenerator;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.util.Pair;
//import de.uni_leipzig.simba.grecall.oracle.Oracle.LinkSpecification;
import de.uni_leipzig.simba.grecall.util.StatisticsBase;
import de.uni_leipzig.simba.grecall.util.DatasetConfiguration;
import de.uni_leipzig.simba.mapper.AtomicMapper;
import de.uni_leipzig.simba.measures.MeasureFactory;

public class InitOracle {
    static Logger logger = Logger.getLogger("LIMES");

    public static void createOracle(String[] arguments) {

	DatasetConfiguration dataCr = null;
	if (arguments.length != 0) {
	    String[] DatasetNames = arguments;
	    dataCr = new DatasetConfiguration(DatasetNames);
	} else
	    dataCr = new DatasetConfiguration();

	// for each dataset
	for (String DatasetName : dataCr.getDatasets()) {

	    dataCr.setCurrentData(DatasetName);
	    if (dataCr.getCurrentDataset() == null)
		continue;

	    SimpleOracle or = new SimpleOracle(DatasetName);
	    logger.info(dataCr.getSource().size());
	    logger.info(dataCr.getTarget().size());
	    logger.info("Current dataset: " + DatasetName);
	    HashMap<String, String> FeaturePairs = dataCr.createFeaturePairs();
	    or.createOracle(FeaturePairs, dataCr.getSource(), dataCr.getTarget());

	    logger.info("Writing statistics for " + DatasetName);
	    // or.writeStatisticsPerIteration();

	}
    }

    public static void main(String args[]) {
	createOracle(args);

    }

}
