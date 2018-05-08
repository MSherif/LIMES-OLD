package de.uni_leipzig.simba.genetics.evaluation.basics;
 
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
 
/**
 * Parse Wrapper for the Apache commons CLI.
 * This class was buildt according to the example at <a href="http://www.zugiart.com/2010/11/simple-gnu-apache-commons-cli-java/">http://www.zugiart.com/</a>
 *
 * @author Klaus Lyko
 *
 */
public class CommandLineConfigurator {
/* attribute */
	/**Name of the generations argument*/
	public static final String GENERATIONS = "generations";
	/**Name of the population size argument*/
	public static final String POPULATION = "population";
	/**Name of the number of runs argument*/
	public static final String RUNS = "runs";
	
	private Options options = new Options();
	private String[] cmdlineArgs=null;
	private CommandLine cmdLine=null;
	private boolean isParsed=false;
 
	/**
	 * Constructor
	 * @param cmdLineArgs Arguments passed to the main().
	 */
	public CommandLineConfigurator(String cmdLineArgs[]) {
		this.cmdlineArgs=cmdLineArgs;
		this.addOption("help", "when specified, will override and print this help message", false, false);
//		addCommonOptions();
	}
 
/* option services */
 
	/**
	 * Adds an option into the command line parser
	 * @param optionName - the option name
	 * @param description - option descriptiuon
	 * @param hasValue - if set to true, --option=value, otherwise, --option is a boolean
	 * @param isMandatory - if set to true, the option must be provided.
	 */
	@SuppressWarnings("static-access")
	public void addOption(String optionName, String description, boolean hasValue, boolean isMandatory ) {
		OptionBuilder opt = OptionBuilder.withLongOpt(optionName);
		opt = opt.withDescription(description);
		opt.withArgName(optionName);
		if( hasValue ) opt = opt.hasArg();
		if( isMandatory ) opt = opt.isRequired();
		options.addOption(opt.create());
	}
 
	/**
	 * Adds common options. Such as to specifiy the number of generations, size of population.
	 */
	public void addCommonOptions() {
		this.addOption(GENERATIONS, "Number of Generations", true, false);
		this.addOption(POPULATION, "Population Size", true, false);
		this.addOption(RUNS, "Number of runs", true, false);
		
	}
	
	// this method is implicitly called by accessor methods
	// and will only be called on first instance.
	private void parse() throws Exception {
		CommandLineParser parser = new GnuParser();
		try { this.cmdLine = parser.parse(this.options, this.cmdlineArgs); }
		catch(MissingOptionException moe){printUsage();}
		this.isParsed=true;
		if( this.cmdLine.hasOption("help")) printUsage();
	}
 
/* accessors & utilities */
 
	public void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("available options as follow:", options );
		System.exit(1);
	}
	public String getString(String optionname) throws Exception { if(!this.isParsed) this.parse(); return this.cmdLine.getOptionValue(optionname); }
	public Integer getInteger(String optionname) throws Exception { return Integer.parseInt(this.getString(optionname));   }
	public Double getDouble(String optionname) throws Exception { return Double.parseDouble(this.getString(optionname)); }
//	public List<String> getList(String optionname, String delimiter) throws Exception {
//		List<String> arrayList = new ArrayList<String>();
//		StringTokenizer tkn = new StringTokenizer(this.getString(optionname),delimiter);
//		while (tkn.hasMoreTokens()) arrayList.add(tkn.nextToken());
//		return arrayList;
//	}
	/**
	 * Checks whether an option was set.
	 * @param optionName The name of the option.
	 * @return true, iff the option was set.
	 * @throws Exception
	 */
	public boolean hasOption(String optionName) throws Exception { if( !this.isParsed) this.parse(); return this.cmdLine.hasOption(optionName); }
 
	/**
	 * Example, to run this class, must
	 * @param args
	 */
	public static void main(String[] args)
	throws Exception
	{
		CommandLineConfigurator cli = new CommandLineConfigurator(args);
		if(cli.hasOption(GENERATIONS))
				System.out.println("gen="+cli.getInteger(GENERATIONS));
		if(cli.hasOption(POPULATION))
			System.out.println("pop="+cli.getInteger(POPULATION));
		if(cli.hasOption(RUNS))
			System.out.println("runs="+cli.getInteger(RUNS));
	}
}