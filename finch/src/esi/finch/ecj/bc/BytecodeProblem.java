package esi.finch.ecj.bc;

import java.io.IOException;

import org.apache.commons.logging.Log;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Problem;
import ec.Subpopulation;
import ec.coevolve.CompetitiveEvaluator;
import ec.coevolve.GroupedProblemForm;
import ec.simple.SimpleEvaluator;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import esi.finch.ecj.bc.BytecodeEvaluator.MatchResult;
import esi.finch.ecj.bc.BytecodeEvaluator.Result;
import esi.util.Config;

/**
 * Bytecode problem.
 *
 * <p>Supports both regular evaluation and single-elimination tournament.
 * The bytecode evaluator (<tt>eval-class</tt>) should implement at least one of
 * {@link BytecodeEvaluator#evaluate(BytecodeIndividual, long, long, ec.util.MersenneTwisterFast, int)}
 * and {@link BytecodeEvaluator#evaluate(BytecodeIndividual, BytecodeIndividual, long, long, ec.util.MersenneTwisterFast, int)},
 * with <tt>eval</tt> set to {@link SimpleEvaluator} or {@link CompetitiveEvaluator}.
 *
 * <p>Default base: <tt>bytecode.prob</tt>.
 * Parameters:
 * <ul>
 * <li><tt>eval-class</tt> (name of evaluating class),
 * <li><tt>timeout</tt>    (evaluation timeout in ms),
 * <li><tt>steps</tt>      (optional maximum number of steps).
 * </ul>
 *
 * @author Michael Orlov
 */
public class BytecodeProblem extends Problem implements SimpleProblemForm, GroupedProblemForm {

	private static final long   serialVersionUID   = 1L;
	private static final String P_BYTECODE_PROBLEM = "prob";
	private static final Log    log = Config.getLogger();

	// Parameter names
	private static final String P_EVAL_CLASS = "eval-class";
	private static final String P_TIMEOUT    = "timeout";
	private static final String P_STEPS      = "steps";

	// Initial values (filled in setup)
	private BytecodeEvaluator	evaluator;
	private long				timeout;
	private long				steps;		// 0 = no limit

	@Override
	public void evaluate(EvolutionState state, Individual ind,
			int subpopulation, int threadnum) {
		// don't evaluate if already evaluated
		if (! ind.evaluated) {
			log.trace("Evaluating: " + ind);

			assert ind         instanceof BytecodeIndividual;
			assert ind.fitness instanceof SimpleFitness;
			SimpleFitness sfit = (SimpleFitness) ind.fitness;

			// evaluate individual (higher fitness is better)
			// and report fitness (ideal if full match)
			Result result = evaluator.evaluate((BytecodeIndividual) ind, timeout, steps, state.random[threadnum], threadnum);
			sfit.setFitness(state, result.fitness, result.ideal);

			log.trace(sfit.fitnessToStringForHumans() + ", " + ind);

			if (result.ideal)
				log.debug("Ideal individual: "  + ind);

			// mark as evaluated
			ind.evaluated = true;
		}
		else
			// When neither crossover nor mutation change the individual
			log.trace("Already evaluated: " + ind);
	}

	// Invoked even when the competitive evaluator is used
	@Override
	public void describe(EvolutionState state, Individual ind,
			int subpopulation, int threadnum, int lg) {
		assert ind.evaluated;
		assert ind instanceof BytecodeIndividual;

		BytecodeIndividual bind = (BytecodeIndividual) ind;
		try {
			bind.saveClass(Config.DIR_OUT_ECJ);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected I/O error", e);
		}

		log.info("Best individual: " + ind);
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);

		Parameter def = defaultBase();

		// Load evaluator class
		evaluator = (BytecodeEvaluator) state.parameters.getInstanceForParameter(base.push(P_EVAL_CLASS), def.push(P_EVAL_CLASS), BytecodeEvaluator.class);

		// Load non-negative timeout
		timeout = state.parameters.getLong(base.push(P_TIMEOUT), def.push(P_TIMEOUT), 0);
		if (timeout == 0-1)
			state.output.error("Timeout not specified or is incorrect", base.push(P_TIMEOUT), def.push(P_TIMEOUT));

		// Load positive steps (set to 0 if no limit)
		steps = state.parameters.getLong(base.push(P_STEPS), def.push(P_STEPS), 1);
		if (steps == 1-1)
			steps = 0;

		log.info("Bytecode problem set up:"
				+ "\n    eval-class=" + evaluator.getClass().getName()
				+ "\n    timeout="    + timeout + "ms"
				+ "\n    steps="      + steps);
	}

	@Override
	public Parameter defaultBase() {
		return BytecodeDefaults.base().push(P_BYTECODE_PROBLEM);
	}

	@Override
	public void preprocessPopulation(EvolutionState state, Population pop, boolean countVictoriesOnly) {
		// Ensure that it is indeed a single-elimination tournament
		assert countVictoriesOnly;

		for (Subpopulation subpop: pop.subpops) {
			// Test for power of 2
			if (! isPowerOf2(subpop.individuals.length))
				log.warn("Sub-population size is not power of 2");

			for (Individual ind: subpop.individuals) {
				SimpleFitness sfit = (SimpleFitness) ind.fitness;

				// Reset fitness (0 - no tournaments won)
				sfit.setFitness(state, 0, sfit.isIdealFitness());
				ind.evaluated = false;
			}
		}
	}

	@Override
	public void postprocessPopulation(EvolutionState state, Population pop, boolean countVictoriesOnly) {
		// Ensure that it is indeed a single-elimination tournament
		assert countVictoriesOnly;

		for (Subpopulation subpop: pop.subpops) {
			// Test for power of 2
			assert isPowerOf2(subpop.individuals.length);

			for (Individual ind: subpop.individuals) {
				SimpleFitness sfit = (SimpleFitness) ind.fitness;
				assert !ind.evaluated;
				assert sfit.fitness() >= 0;

				// Indicate that the individual is now evaluated (after sfit.fitness() tournaments)
				// The generation component is for the best-of-run computation in SimpleStatistics
				ind.evaluated  = true;
				float genShift = (float) state.generation / state.numGenerations;
				sfit.setFitness(state, sfit.fitness() + genShift, sfit.isIdealFitness());
			}
		}
	}

	@Override
	public void evaluate(EvolutionState state, Individual[] ind,
			boolean[] updateFitness, boolean countVictoriesOnly,
			int[] subpops, int threadnum) {
		// Ensure that it is indeed a single-elimination tournament
		assert ind.length == 2;
		assert updateFitness[0]  &&  updateFitness[1]  &&  countVictoriesOnly;

		// Ensure that we always get cloned individuals with different fitness
		// (This is done in Immutable(Crossover|Mutation)Pipeline)
		BytecodeIndividual bind1 = (BytecodeIndividual) ind[0];
		BytecodeIndividual bind2 = (BytecodeIndividual) ind[1];
		assert bind1 != bind2  &&  bind1.fitness != bind2.fitness;

		// evaluate individuals one against the other
		// and increment fitness of the winner (if draw pick randomly)
		MatchResult result = evaluator.evaluate(bind1, bind2, timeout, steps, state.random[threadnum], threadnum);

		SimpleFitness sfit;
		if (result.isWin()  ||  (result.isDraw()  &&  state.random[threadnum].nextBoolean()))
			sfit = (SimpleFitness) bind1.fitness;
		else
			sfit = (SimpleFitness) bind2.fitness;

		// NOTE: ideal status propagation can be added here using "|"
		sfit.setFitness(state, sfit.fitness() + 1, sfit.isIdealFitness());
	}

	// Package-level access for testing purposes
	static boolean isPowerOf2(int x) {
		return (x & (x - 1))  ==  0  &&  x > 0;
	}

}
