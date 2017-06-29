package esi.finch.ecj.bc;

import ec.EvolutionState;
import ec.coevolve.CompetitiveEvaluator;
import ec.util.MersenneTwisterFast;

/**
 * Evaluator of evolved bytecode.
 *
 * Should be instantiable using the empty constructor.
 *
 * @author Michael Orlov
 */
public interface BytecodeEvaluator {

	/**
	 * Evaluation result.
	 */
	final class Result {
		/**
		 * Fitness, guaranteed to be a real number
		 * (not NaN or infinite).
		 */
		public final float fitness;

		/**
		 * Whether the individual can be considered ideal.
		 */
		public final boolean ideal;

		/**
		 * Constructs a result. If given fitness is not
		 * a real number, it is converted to negative float
		 * with maximal absolute value.
		 *
		 * @param fitness the fitness
		 * @param ideal whether the individual is ideal
		 */
		public Result(float fitness, boolean ideal) {
			if (Float.isInfinite(fitness)  ||  Float.isNaN(fitness))
				fitness = -Float.MAX_VALUE;

			this.fitness = fitness;
			this.ideal   = ideal;
		}
	}

	/**
	 * Tournament-of-two evaluation result.
	 *
	 * There is no support for ideal status, see
	 * {@link CompetitiveEvaluator#runComplete(EvolutionState)}
	 */
	final class MatchResult {
		/**
		 * Fitness difference (first - second),
		 * guaranteed to be a real number.
		 */
		public final float fitnessDiff;

		/**
		 * Constructs a tournament result.
		 *
		 * The fitness difference is semantic, it can be simply
		 * -1 for a loss, 0 for a draw, and 1 for a win.
		 *
		 * @param fitnessDiff first individual's fitness minus second, must be a real number
		 */
		public MatchResult(float fitnessDiff) {
			assert !Float.isInfinite(fitnessDiff)  &&  !Float.isNaN(fitnessDiff);
			this.fitnessDiff = fitnessDiff;
		}

		/**
		 * @return whether first individual won the tournament
		 */
		public boolean isWin() {
			return fitnessDiff > 0;
		}

		/**
		 * @return whether first individual lost the tournament
		 */
		public boolean isLoss() {
			return fitnessDiff < 0;
		}

		/**
		 * @return whether the match resulted in a draw
		 */
		public boolean isDraw() {
			return fitnessDiff == 0;
		}
	}

	/**
	 * Evaluates the given individual.
	 *
	 * @param ind individual
	 * @param timeout timeout (for sandbox evaluation)
	 * @param steps maximum number of steps when using interruptible method
	 * @param random random number generator
	 * @param threadnum number of evaluation thread
	 * @return evaluation result
	 * @throws UnsupportedOperationException if not implemented
	 */
	Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum);

	/**
	 * Evaluates one individual against another.
	 *
	 * @param ind1 individual one (it's "win" if ind1 wins)
	 * @param ind2 individual two (it's "loss" if ind2 wins)
	 * @param timeout timeout (for sandbox evaluation)
	 * @param steps maximum number of steps when using interruptible method
	 * @param random random number generator
	 * @param threadnum number of evaluation thread
	 * @return evaluation result
	 * @throws UnsupportedOperationException if not implemented
	 */
	MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum);

}
