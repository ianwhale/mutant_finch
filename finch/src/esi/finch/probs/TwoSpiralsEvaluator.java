package esi.finch.probs;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.Config;
import esi.util.SandBox;

/**
 * Evaluator for two intertwined spirals.
 *
 * @author Michael Orlov
 * @see <a href="http://www.cs.cmu.edu/afs/cs/project/ai-repository/ai/areas/neural/bench/cmu/">CMU Neural Network Benchmark Database</a>
 */
public class TwoSpiralsEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	// NOTE: changing number of points won't result in conforming dataset.
	// See the "density" parameter in the CMU Benchmark.
	private static final int    POINTS = 97;
	private static final double RADIUS = 1.0;

	private static final String COUNT_NAME = "count";

	// Xs and Ys for the first spiral. In the second spiral, use -Xs and -Ys.
	private double xs[];
	private double ys[];

	public TwoSpiralsEvaluator() {
		xs = new double[POINTS];
		ys = new double[POINTS];

		double maxRadius = RADIUS;

		for (int n = 0;  n < xs.length;  ++n) {
			double r = maxRadius * (8 + n) / (8 + xs.length - 1);
			double a = Math.PI * (8 + n) / 16;

			xs[n] = r * Math.cos(a);
			ys[n] = r * Math.sin(a);
		}
	}

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Class<?> klass = ind.getMethod().getDeclaringClass();

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

		SandBox sandbox = new SandBox(instance, counter, timeout);

		boolean      valid     = true;
		int          hits      = 0;

		SandBox.Result result = sandbox.call(xs, ys);

		// Timeout invalidates the individual
		if (result == null) {
			log.debug("Timeout in " + ind);
			valid = false;
		}
		// An exception invalidates the individual
		else if (result.exception != null) {
			log.debug("Exception: " + result.exception + " in " + ind);
			valid = false;
		}
		else {
			// Get result (assert also checks non-null)
			assert result.retvalue instanceof Integer;
			hits = (Integer) result.retvalue;
		}

		// In ECJ, higher fitness is better
		float fitness = valid  ?  hits /* + 1.0f / ind.size() */ :  Float.NaN;
		assert !valid  ||  (fitness >= 0  &&  fitness < 2 * POINTS + 1);

		// If all samples resulted in a hit, the individual is ideal
		boolean ideal = (hits == 2 * POINTS);

		Result res = new Result(fitness, ideal);
		return res;
	}

	// Package-level access for testing purposes
	double getX(int n) {
		return xs[n];
	}

	// Package-level access for testing purposes
	double getY(int n) {
		return ys[n];
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}

}
