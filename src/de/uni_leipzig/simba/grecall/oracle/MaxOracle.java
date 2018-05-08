package de.uni_leipzig.simba.grecall.oracle;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.grecall.util.DatasetConfiguration;

public class MaxOracle {
    static Logger logger = Logger.getLogger("LIMES");

    public static void createAvgOracle(String[] arguments) {
	DatasetConfiguration dataCr = null;
	if (arguments.length != 0) {
	    String[] DatasetNames = arguments;
	    dataCr = new DatasetConfiguration(DatasetNames);
	} else
	    dataCr = new DatasetConfiguration();
	for (String DatasetName : dataCr.getDatasets()) {
	    logger.info("Current dataset: " + DatasetName);
	    dataCr.setCurrentData(DatasetName);
	    if (dataCr.getCurrentDataset() == null)
		continue;

	    SimpleOracle or = new SimpleOracle(DatasetName);
	    or.setOraclePerLS("max", DatasetName);
	    or.writeStatisticsPerLS("max");
	}

    }

    public static void main(String args[]) {
	createAvgOracle(args);

    }
}
