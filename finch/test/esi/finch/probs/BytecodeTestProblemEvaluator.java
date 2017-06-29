package esi.finch.probs;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.Config;
import esi.util.SandBox;

public class BytecodeTestProblemEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	private static final int    SAMPLES   = 20;
	private static final double TOLERANCE = 0.01;

	// Assuming single thread...
	private int evals = 0;

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Method   method = ind.getMethod();
		Class<?> klass  = method.getDeclaringClass();

		Object instance;
		try {
			instance = klass.newInstance();
		} catch (InstantiationException e) {
			fail();
			throw new Error("Unexpected exception", e);
		} catch (IllegalAccessException e) {
			fail();
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
			double x = getSample(random);
			double y = getSample(random);
			double z = getValue(x, y);

			SandBox.Result result = sandbox.call(x, y);

			// Timeout shouldn't happen
			assertNotNull(result);
			// An exception invalidates the individual (division, out of bounds, steps limits)
			if (result.exception != null) {
				log.debug("Exception: " + result.exception + " in " + ind);
				valid = false;
			}
			else {
				// Get result (assert also checks non-null)
				assertTrue(result.retvalue instanceof Double);
				double res = (Double) result.retvalue;

				// Non-numeric result invalidates the individual
				if (Double.isInfinite(res)  ||  Double.isNaN(res))
					valid = false;
				else {
					double diff = Math.abs(res - z);
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
		if (ideal)
			assertTrue(-fitness <= samples * tolerance);

		Result res = new Result(fitness, ideal);
		assertTrue(res.fitness <= 0);

		ind.setInfo("Evaluation: " + (++evals) + (ideal ? ", Ideal" : ""));

		return res;
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Result res1 = evaluate(ind1, timeout, steps, random, threadnum);
		Result res2 = evaluate(ind2, timeout, steps, random, threadnum);

		float diff = res1.fitness - res2.fitness;
		return new MatchResult(diff);
	}

	protected double getSample(MersenneTwisterFast random) {
		// [-1, 1)
		return 2 * random.nextDouble() - 1;
	}

	protected double getValue(double x, double y) {
		return x*x+y;
	}

}
