package esi.finch.probs;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.Config;
import esi.util.SandBox;

/**
 * Evaluator of {@link SimpleSymbolicRegression}, with multi-layered approach
 * to F1...F9 polynomials.
 *
 * <ul>
 * <li> Evaluation:      20 samples in [-1, 1]
 * <li> Tolerance:       a hit if <= 1e-8
 * <li> Ideal degree d:  if 20 hits for degree d
 * </ul>
 *
 * @author Michael Orlov
 * @see <a href="http://dx.doi.org/10.1109/CEC.2006.1688570">Solving Symbolic Regression Problems Using Incremental Evaluation in Genetic Programming</a> (IEEE CEC 2006)
 */
public class PolynomialSymbolicRegressionEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	private static final int    SAMPLES   = 20;
	private static final double TOLERANCE = 0.00000001;
	private static final int	DEGREE    = 9;


	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Method   method = ind.getMethod();
		Class<?> klass  = method.getDeclaringClass();

		Object instance;
		try {
			instance = klass.newInstance();
		} catch (InstantiationException e) {
			throw new Error("Unexpected exception", e);
		} catch (IllegalAccessException e) {
			throw new Error("Unexpected exception", e);
		}

		SandBox sandbox = new SandBox(instance, method, timeout);

		final int    samples   = SAMPLES;
		boolean      valid     = true;

		int          degree    = 0;
		int          hits      = 0;

		for (int i = 0;  valid  &&  i < samples;  ++i) {
			// x = [ -1, 1 )
			double x = getSample(random, i);
			SandBox.Result result = sandbox.call(x);

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
				assert result.retvalue instanceof Number;
				Number num = (Number) result.retvalue;
				double res = num.doubleValue();

				// Non-numeric result invalidates the individual
				if (Double.isInfinite(res)  ||  Double.isNaN(res))
					valid = false;
				else {
					int match = maxMatch(x, res);

					if (match > degree) {
						degree = match;
						hits   = 1;
					}
					else if (match == degree)
						++hits;
				}
			}

		}

		// In ECJ, higher fitness is better
		float fitness;
		if (valid) {
			// Not all-hits -> no fitness
			if (hits != samples)
				fitness = 0;
			// All-hits -> degree + small parsimony pressure
			else
				fitness = degree + 1.0f / ind.size();
		}
		else
			fitness = Float.NaN;

		// Fitness is in [0, DEGREE) range
		assert !valid  ||  (fitness >= 0  &&  fitness < DEGREE + 1);

		// If all samples resulted in a hit, the individual is ideal
		boolean ideal = (degree == DEGREE)  &&  (hits == samples);

		Result res = new Result(fitness, ideal);
		return res;
	}

	// NOTE: [-1, 1] sample range is tricky, because other ranges
	// drive evolution to use EXP
	protected double getSample(MersenneTwisterFast random, int index) {
		// [-1, 1)
		return 2 * random.nextDouble() - 1;
	}

	protected double getValue(double x, double degree) {
		double acc = 0;

		for (int i = 0;  i < degree;  ++i)
			acc = acc * x + x;

		return acc;
	}

	// Package-level access for testing purposes
	int maxMatch(double x, double y) {
		int match = 0;

		for (int degree = 1;  degree <= DEGREE;  ++degree)
			if (Math.abs(getValue(x, degree) - y)  <  TOLERANCE)
				match = degree;

		return match;
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}

}
