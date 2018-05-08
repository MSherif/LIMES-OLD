package de.uni_leipzig.simba.learning.learner;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.Serializer;
import de.uni_leipzig.simba.io.SerializerFactory;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.DisjunctiveMeshSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.LinearMeshSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.PseudoMeasures;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

/**
 * Basic interface to run EUCLIDs with LIMES' config files.
 * @author Klaus Lyko
 *
 */
public class EuclidMain {
	
	/**
	 * Main method runs a EUCLID on the defined data sets.
	 * @param configFile LIMES' config file
	 * @param coverage Coverage of properties to be regarded for simple classifiers
	 * @param type either "linear", "disjunctive" or "conjunctive"
	 * @param maxIterations
	 * @return
	 */
	public Mapping runEuclid(File configFile, double coverage, String type, int maxIterations) {
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile.getAbsolutePath());
		Cache sC = HybridCache.getData(cR.getSourceInfo());
		Cache tC = HybridCache.getData(cR.getTargetInfo());
		return runEuclid(sC, tC, coverage, type, maxIterations);
	}
	
	public Mapping runEuclid(Cache sC, Cache tC, double coverage, String type, int iterations) {	
		MeshBasedSelfConfigurator lsc;
        if (type.toLowerCase().startsWith("l")) {
            lsc = new LinearMeshSelfConfigurator(sC, tC, coverage, 1d);
        } else if (type.toLowerCase().startsWith("d")) {
            lsc = new DisjunctiveMeshSelfConfigurator(sC, tC, coverage, 1d);
        } else {
            lsc = new MeshBasedSelfConfigurator(sC, tC, coverage, 1d);
        }
        lsc.setMeasure(new PseudoMeasures());
        System.out.println("Running EUCLID "+type+" coverage="+coverage+". Computing simple classifiers...");
        long begin = System.currentTimeMillis();
        List<SimpleClassifier> cp = lsc.getBestInitialClassifiers();
        
        long middle = System.currentTimeMillis();
        System.out.println(cp.size()+" simple classifiers computed in "+(middle-begin)+" ms. Computing complex classifier for "+iterations+" iterations...");
        int scCount = 0;
        for(SimpleClassifier sc: cp) {
        	System.out.println(scCount+++" simple classifier : "+sc);
        }
        ComplexClassifier cc = lsc.getZoomedHillTop(5, iterations, cp);
     	
        long end = System.currentTimeMillis();
        System.out.println("Eculid finished after "+(end-begin)+" ms = "+(end-begin)/1000+" s." );
        System.out.println("Complex Classifier= "+cc);
        System.out.println("Mapping size = "+cc.mapping.size());
		return cc.mapping;
	}
	
	public static void main(String[] args) {
		CommandLineParser parser = new BasicParser();
		Options options = getCLIOptions();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);

			// configure files
			String testFile = cmd.getOptionValue("c");
			File f = new File(testFile);
			String mapFile = cmd.getOptionValue("mapfile", "EuclidMapping.nt");
			
			// parse other parameters
			String type = cmd.getOptionValue("type","linear");//also possible: disjunctive, conjunctive
			int iterations = Integer.parseInt(cmd.getOptionValue("iterations","20"));
			double coverage = Double.parseDouble(cmd.getOptionValue("coverage","0.8"));
			
			//run EUCLID
			EuclidMain em = new EuclidMain();
			Mapping map = em.runEuclid(f, coverage, type, iterations);
			
			// serialize mapping to file
			Serializer serial = SerializerFactory.getSerializer("nt"); // also support csv, ttl, tab
			serial.writeToFile(map, "owl:sameAs", mapFile);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			HelpFormatter lvFormater = new HelpFormatter();

			lvFormater.printHelp("Programm_Name", getCLIOptions());
		}
	}

	
	/**
	 * Runtime options.
	 * @return
	 */
	public static Options getCLIOptions() {
		Options options = new Options();
		options.addOption(OptionBuilder
        		.withLongOpt("config")
        		.withDescription("Path to LIMES XML config.")
        		.isRequired()
        		.hasArg()
        		.create("c"));
		options.addOption(OptionBuilder
        		.withLongOpt("mapfile")
        		.withDescription("Path to the NT file the computed mapping is serialized to.")
        		.hasArg()
        		.create("m"));
		options.addOption("coverage", true, "If set specifiy EUCLIDs coverage parameter.");
		options.addOption("iterations", true, "If set specify number of EUCLIDs iterations.");
		options.addOption("type", true, "If set specifies the EUCLID type: linear, disjunctive or conjunctive");
		return options;
	}
	
}
