package esi.finch.probs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.finch.probs.ArtificialAnt.OperationsLimit;
import esi.util.Config;
import esi.util.SandBox;

/**
 * Evaluator of {@link ArtificialAnt}.
 *
 * <ul>
 * <li> Evaluation: at most 400 steps
 * <li> Ideal:      if all food pellets eaten
 * </ul>
 *
 * @author Michael Orlov
 */
public class ArtificialAntEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	// Necessary minimum is 55
	private static final int    MAX_STEPS    = 100;

	private static final String GO_NAME		 = "go";
	private static final String GETTER_NAME  = "getEatenCount";
	private static final String CHECKER_NAME = "ateAll";

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Class<?> klass = ind.getMethod().getDeclaringClass();

		// Extract relevant methods
		Method go, getter, checker;
		try {
			go      = klass.getDeclaredMethod(GO_NAME);
			getter  = klass.getDeclaredMethod(GETTER_NAME);
			checker = klass.getDeclaredMethod(CHECKER_NAME);
		} catch (Exception e) {
			throw new Error("Unexpected exception", e);
		}

		// Create an artificial ant instance
		Object instance;
		try {
			Constructor<?> konst = klass.getConstructor(Integer.TYPE);
			instance = konst.newInstance(MAX_STEPS);

			// Save instance for representation purposes
			ind.setInfo(instance);
		} catch (Exception e) {
			throw new Error("Unexpected exception", e);
		}

		SandBox sandbox = new SandBox(instance, go, timeout);

		// Execute the "go" method (exception is thrown when maximum steps reached)
		boolean ideal = false;
		boolean valid = true;
		int     eaten = 0;

		SandBox.Result result = sandbox.call();

		// Timeout invalidates the individual
		if (result == null) {
			log.debug("Timeout in " + ind);
			valid = false;
		}
		// Unexpected exception invalidates the individual
		else if (result.exception != null  &&  !(result.exception instanceof OperationsLimit)) {
			log.debug("Exception: " + result.exception + " in " + ind);
			valid = false;
		}
		else {
			// Make sure result is ok (it is void, so null is returned)
			assert (result.hasRetvalue()  &&  result.retvalue == null)
				|| (result.exception instanceof OperationsLimit);

			// Get count of eaten food
			eaten = (Integer) invoke(instance, getter);
			ideal = (Boolean) invoke(instance, checker);
		}

		// Higher fitness is better
		float fitness = valid  ?  eaten  :  Float.NaN;

		// Parsimony
		//if (valid)
		//	fitness += 1.0f / ind.size();

		assert !valid  ||  fitness >= 0;

		Result res = new Result(fitness, ideal);
		return res;
	}

	private Object invoke(Object instance, Method method, Object... args) {
		try {
			return method.invoke(instance, args);
		} catch (Exception e) {
			throw new Error("Unexpected exception", e);
		}
	}

	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}

}
