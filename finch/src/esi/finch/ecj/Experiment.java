package esi.finch.ecj;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import ec.Evolve;
import ec.Problem;
import esi.util.Config;

/**
 * A class that runs an ECJ experiment.
 *
 * @author Michael Orlov
 */
public class Experiment {

	private static final Log log = Config.getLogger();

	private String LOG_NAME           	   = "ecj.log";
	private static final String CHECKPOINT_PREFIX  = "cp";

	private final List<String> args;
	private final String       descriptor;

	/**
	 * Prepares a new ECJ experiment.
	 *
	 * @param params ECJ parameters file
	 * @param problem problem class
	 */
	public Experiment(URL params, Class<? extends Problem> problem, int seed) {
		args = new ArrayList<String>();

		// Add the parameters argument
		if (params == null)
			throw new Error("Parameters resource failed to load");

		args.add("-file");
		args.add(params.getPath());

		this.LOG_NAME = "ecj.log";
		
		// We must assume there are only 4 threads, this is bad practice but I promise not to change the threads if you do.
		if (seed > -1) {
			for (int i = 0; i < 4; i++) {
				addParameter("seed." + Integer.toString(i), Integer.toString(i + seed));
			}
			
			this.LOG_NAME = "ecj" + Integer.toString(seed) + ".log";
		}
		
		// Problem class
		addParameter("eval.problem", problem.getName());

		// Checkpoint file prefix (implicitly relative to current directory)
		addParameter("prefix", new File(Config.DIR_OUT_ECJ, CHECKPOINT_PREFIX).getPath());

		// Statistics log (current directory denoted by '$')
		addParameter("stat.file", "$" + new File(Config.DIR_OUT_ECJ, LOG_NAME).getPath());

		log.info("Initial seed: " + seed);
		
		// Build a descriptive name
		String paramsFile = params.getFile();
		descriptor = problem.getName() + " / " + paramsFile.substring(paramsFile.lastIndexOf('/') + 1);

		log.info("Experiment configured:" + getParameters());
	}

	/**
	 * Restores an ECJ experiment from a checkpoint.
	 *
	 * @param checkpointNum checkpoint number
	 */
	public Experiment(int checkpointNum) {
		args = new ArrayList<String>();

		// Construct checkpoint name, cp.123.gz
		String cpName = CHECKPOINT_PREFIX + "." + checkpointNum + ".gz";
		File   cpFile = new File(Config.DIR_OUT_ECJ, cpName);
		if (!cpFile.exists())
			throw new Error("Checkpoint file " + cpFile.getAbsolutePath() + " does not exist");

		descriptor = "[ " + cpName + " ]";

		// Checkpoint file (implicitly relative to current directory)
		args.add("-checkpoint");
		args.add(cpFile.getPath());

		log.info("Experiment configured:" + getParameters());
	}

	private void addParameter(String key, String value) {
		args.add("-p");
		args.add(key + "=" + value);
	}

	private String getParameters() {
		StringBuilder buf = new StringBuilder();

		boolean name = true;
		for (String param: args) {
			if (name)
				buf.append("\n    ").append(param).append(" ");
			else
				buf.append(param);

			name = !name;
		}

		return buf.toString();
	}

	public void run() {
		log.info("Running experiment " + descriptor);

		Evolve.main(args.toArray(new String[0]));

		log.info("Experiment completed");
	}

}
