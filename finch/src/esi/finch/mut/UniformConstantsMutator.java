package esi.finch.mut;

import ec.util.MersenneTwisterFast;
import esi.bc.manip.IdentityConstantsMutator;
import esi.util.SpecializedConstants;

/**
 * Uniform constants mutator.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.UniformConstantsMutator.limit</code>: range limit
 * </ul>
 *
 * <p>Mutation:
 * <ul>
 * <li><code>double</code>: mutated constants are in [-limit, limit] range
 * </ul>
 *
 * @author Michael Orlov
 */
public class UniformConstantsMutator extends IdentityConstantsMutator {

	private static final float LIMIT = SpecializedConstants.getFloat(UniformConstantsMutator.class, "limit");

	private final float					mutProb;
	private final MersenneTwisterFast	random;

	/**
	 * Cerates a new uniform constants mutator.
	 *
	 * @param mutProb per-constant mutation probability (point mutation)
	 * @param random randomness source
	 */
	public UniformConstantsMutator(float mutProb, MersenneTwisterFast random) {
		this.mutProb = mutProb;
		this.random  = random;
	}

	@Override
	public double mutate(double x) {
		if (random.nextFloat() < mutProb)
			x = (random.nextDouble() - 0.5) * 2 * LIMIT;

		return x;
	}

}
