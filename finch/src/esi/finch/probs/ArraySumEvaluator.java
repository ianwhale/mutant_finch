package esi.finch.probs;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.Config;
import esi.util.SandBox;

/**
 * Evaluator for {@link ArraySum}.
 *
 * @author Michael Orlov
 */
public class ArraySumEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	private static final String DIFF_NAME = "getDifference";

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Class<?> klass = ind.getInterruptibleMethod(steps, threadnum, 0).getDeclaringClass();

		// Extract relevant methods
		Method diff;
		try {
			diff = klass.getDeclaredMethod(DIFF_NAME, int[][].class);
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

		// If not converting method to interruptible,
		// must use old sandbox due to uninterruptible infinite loops
		SandBox        sandbox = new SandBox(instance, diff, timeout);
		SandBox.Result result  = sandbox.call((Object) ArraySum.testLists);

		boolean valid   = true;
		int     diffSum = -1;

		// Timeout invalidates the individual
		if (result == null) {
			// FINE log level, since loops shouldn't cause timeouts (due to steps limits)
			log.debug("Timeout in " + ind);
			valid = false;
		}
		// An exception invalidates the individual
		else if (result.exception != null) {
			// FINER log level, due to integer division, array access, and steps limits
			log.trace("Exception: " + result.exception + " in " + ind);
			valid = false;
		}
		else {
			// Get result (assert also checks non-null)
			assert result.retvalue instanceof Integer;
			diffSum = (Integer) result.retvalue;

			// Protect against integer overflow in getDifference()
			if (diffSum < 0)
				diffSum = Integer.MAX_VALUE;
		}

		// In ECJ, higher fitness is better
		float fitness = valid  ?  -diffSum + 1.0f / ind.size()  :  Float.NaN;
		assert !valid  ||  fitness <= 0.1;

		// If all tests resulted in correct sum, the individual is ideal
		boolean ideal = (diffSum == 0);

		Result res = new Result(fitness, ideal);
		return res;
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}

}
