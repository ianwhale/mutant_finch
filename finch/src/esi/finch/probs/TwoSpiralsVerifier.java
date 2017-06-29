package esi.finch.probs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.commons.logging.Log;

import esi.util.Config;
import esi.util.SandBox;
import esi.util.SpecializedConstants;

/**
 * Verifier for intertwined spirals evolved solutions
 * with different density.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.TwoSpiralsVerifier.results</code>: location of class files
 * 		for verification (should contain package directories as well)
 * </ul>
 *
 * @author Michael Orlov
 */
public class TwoSpiralsVerifier {

	private static final Log log = Config.getLogger();

	private static final int    POINTS  = 97;
	private static final double RADIUS  = 1.0;
	private static final long	TIMEOUT = 1000;

	private static final String RESULTS_DIR = SpecializedConstants.get(TwoSpiralsVerifier.class, "results");
	private static final String COUNT_NAME  = "count";

	// Xs and Ys for the first spiral. In the second spiral, use -Xs and -Ys.
	private double xs[];
	private double ys[];

	public TwoSpiralsVerifier(int density) {
		xs = new double[(POINTS-1) * density + 1];
		ys = new double[(POINTS-1) * density + 1];

		double maxRadius = RADIUS;

		for (int n = 0;  n < xs.length;  ++n) {
			double r = maxRadius * (8 * density + n) / (8 * density + xs.length - 1);
			double a = Math.PI * (8 + n / (double) density) / 16;

			xs[n] = r * Math.cos(a);
			ys[n] = r * Math.sin(a);
		}
	}

	public int countHits(Class<?> klass) {
		// Extract relevant methods
		Method counter;
		try {
			counter = klass.getDeclaredMethod(COUNT_NAME, double[].class, double[].class);
		} catch (Exception e) {
			throw new Error("Unexpected exception", e);
		}

		Object instance;
		try {
			instance = klass.newInstance();
		} catch (InstantiationException e) {
			throw new Error("Unexpected exception", e);
		} catch (IllegalAccessException e) {
			throw new Error("Unexpected exception", e);
		}

		SandBox sandbox = new SandBox(instance, counter, TIMEOUT);

		boolean      valid     = true;
		int          hits      = 0;

		SandBox.Result result = sandbox.call(xs, ys);

		// Timeout invalidates the individual
		if (result == null) {
			log.debug("Timeout");
			valid = false;
		}
		// An exception invalidates the individual
		else if (result.exception != null) {
			log.debug("Exception: " + result.exception);
			valid = false;
		}
		else {
			// Get result (assert also checks non-null)
			assert result.retvalue instanceof Integer;
			hits = (Integer) result.retvalue;
		}

		return valid ? hits : -1;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		URLClassLoader loader = URLClassLoader.newInstance(new URL[] { Config.DIR_IN_CONF.resolveDir(RESULTS_DIR).resolve(".") });

		String   name  = "TwoSpirals_G168_T1_301293";
		Class<?> klass = loader.loadClass(TwoSpirals.class.getPackage().getName() + "." + name);

		log.info("Hits  (1): " + new TwoSpiralsVerifier(1).countHits(klass));
		log.info("Hits  (2): " + new TwoSpiralsVerifier(2).countHits(klass));
		log.info("Hits (10): " + new TwoSpiralsVerifier(10).countHits(klass));

		SandBox.shutdown();
	}

}
