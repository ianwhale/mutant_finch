package esi.finch.mut;

import ec.util.MersenneTwisterFast;
import esi.bc.manip.IdentityConstantsMutator;
import esi.util.SpecializedConstants;

/**
 * Gaussian constants mutator.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.GaussianConstantsMutator.factor</code>: sigma divisor
 * </ul>
 *
 * <p>Mutation:
 * <ul>
 * <li><code>double</code>: mutated constants are modified by adding N(0, 1/factor)
 * </ul>
 *
 * @author Michael Orlov
 */
public class GaussianConstantsMutator extends IdentityConstantsMutator {

	private static final float FACTOR = SpecializedConstants.getFloat(GaussianConstantsMutator.class, "factor");

	private final float					mutProb;
	private final MersenneTwisterFast	random;

	/**
	 * Cerates a new Gaussian constants mutator.
	 *
	 * @param mutProb per-constant mutation probability (point mutation)
	 * @param random randomness source
	 */
	public GaussianConstantsMutator(float mutProb, MersenneTwisterFast random) {
		this.mutProb = mutProb;
		this.random  = random;
	}

	@Override
	public double mutate(double x) {
		x += random.nextGaussian() / FACTOR;

		return x;
	}

}
