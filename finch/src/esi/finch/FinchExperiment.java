package esi.finch;

import java.io.File;
import java.net.URL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import esi.finch.ecj.Experiment;
import esi.finch.ecj.bc.BytecodeProblem;
import esi.util.Config;
import esi.util.SandBox;

/**
 * FINCH experiment runner.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.FinchExperiment.params</code>: parameter file, relative
 * 		to this class
 * </ul>
 *
 * @author Michael Orlov
 */
public class FinchExperiment {

	private static final Log log = Config.getLogger();

	private static final String	VERSION_NAME  = "FINCH";
	private static final int	VERSION_MAJOR = 0;
	private static final int	VERSION_MINOR = 0;

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		Options     opts  = new Options();
		OptionGroup group = new OptionGroup();
		OptionGroup seed_group = new OptionGroup();
		
		group.addOption(OptionBuilder.withLongOpt("params")
				.withDescription("experiment parameters URL (relative to esi/finch)")
				.hasArg().withArgName("url").create('p'));
		group.addOption(OptionBuilder.withLongOpt("help")
				.withDescription("print this message").create('h'));
		group.addOption(OptionBuilder.withLongOpt("version")
				.withDescription("print the version information and exit").create('v'));
		opts.addOptionGroup(group);

		seed_group.addOption(OptionBuilder.withLongOpt("seed")
				.withDescription("seed for thread 0, thread n will have the provided seed plus n as its seed")
				.hasArg().withArgName("seed").create('s'));
		opts.addOptionGroup(seed_group);
		
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine line = parser.parse(opts, args);

			if (line.hasOption("h")  ||  line.getOptions().length == 0) {
				HelpFormatter format = new HelpFormatter();
				format.setLeftPadding(4);
				format.setWidth(79);

				format.printHelp("bin/finch", opts, true);
			}
			else if (line.hasOption("v")) {
				System.out.println(VERSION_NAME + " v" + VERSION_MAJOR + "." + VERSION_MINOR);
			}
			else if (line.hasOption("p")) {

				int seed = -1;
				if (line.hasOption("s")) {
					seed = Integer.parseInt(line.getOptionValue("s"));
				}
				
				if (seed > -1) {
					// Mark the out with the seed. 					
					File baseOutDir = new File(Config.esiProperties.getProperty("dir.out.base").toString());
					
					String ecj_out_dir = Config.esiProperties.getProperty("dir.out.ecj").toString() + File.separator + Integer.toString(seed);
					
					new File(baseOutDir, ecj_out_dir).mkdirs();
					
					Config.DIR_OUT_ECJ = new File(baseOutDir, ecj_out_dir);
				}
				
				String paramsOpt = line.getOptionValue("p");

				URL params = FinchExperiment.class.getResource(paramsOpt);
				if (params == null)
					throw new RuntimeException("Cannot load " + paramsOpt);
				
				Experiment exp = new Experiment(params, BytecodeProblem.class, seed);
				exp.run();

				SandBox.shutdown();
			}
			else {
				log.fatal("Experiment paramete"
						+ "rs URL not specified");
			}
		} catch (ParseException e) {
			log.fatal("Incorrect command-line arguments: " + e.getMessage());
		}
	}

}