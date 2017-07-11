package esi.finch.probs;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.Config;
import esi.util.SandBox;

/**
 * Evaluator of {@link SimpleSymbolicRegression}.
 *
 * <ul>
 * <li> Evaluation: 20 samples in [-1, 1]
 * <li> Tolerance:  a hit if <= 0.01
 * <li> Ideal:      if 20 hits
 * </ul>
 *
 * @author Michael Orlov
 */
public class SimpleSymbolicRegressionEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	private static final int    SAMPLES   = 20;
	private static final double TOLERANCE = 0.01;

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Tracker.maybe_print();
		
		Method method;
		try {
			method = ind.getMethod();
		}
		catch (ClassFormatError e) {
			System.out.println(e);
			Tracker.verify.getAndIncrement();
			return new Result(Float.MIN_VALUE, false);
		}
		
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
		double sumDiff = 0;

		final int    samples   = SAMPLES;
		final double tolerance = TOLERANCE;
		boolean      valid     = true;
		int          hits      = 0;

		for (int i = 0;  valid  &&  i < samples;  ++i) {
			// x = [ -1, 1 )
			double x = getSample(random, i);
			double y = getValue(x);

			SandBox.Result result = sandbox.call(x);

			// Timeout invalidates the individual
			if (result == null) {
				Tracker.timeout.getAndIncrement();
				log.debug("Timeout in " + ind);
				valid = false;
			}
			// An exception invalidates the individual
			else if (result.exception != null) {
				Tracker.runtime.getAndIncrement();
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
					double diff = Math.abs(res - y);
					sumDiff += diff;

					if (diff <= tolerance)
						++hits;
				}
			}

		}

		// In ECJ, higher fitness is better, so negate
		float fitness = valid  ?  (float) (-sumDiff)  :  Float.NaN;

		// If all samples resulted in a hit, the individual is ideal
		boolean ideal = (hits == samples);
		assert !ideal  ||  -fitness <= samples * tolerance;

		Result res = new Result(fitness, ideal);
		assert res.fitness <= 0;

		return res;
	}

	protected double getSample(MersenneTwisterFast random, int index) {
		// [-1, 1)
		return 2 * random.nextDouble() - 1;
	}

	protected double getValue(double x) {
		return x*x*x*x + x*x*x + x*x + x;
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}

}
