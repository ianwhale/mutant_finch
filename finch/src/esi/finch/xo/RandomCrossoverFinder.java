package esi.finch.xo;

import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.CodeSection;
import esi.bc.xo.CompatibleCrossover;
import esi.util.Config;
import esi.util.SpecializedConstants;

/**
 * Random search for crossover locations.
 *
 * Supports:
 * <ul>
 * <li> Maximum number of tries
 * <li> Maximum size of post-crosover method (approximate)
 * </ul>
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.RandomCrossoverFinder.xo-tries</code>: maximum number of tries
 * </ul>
 *
 * @author Michael Orlov
 */
public abstract class RandomCrossoverFinder extends CrossoverFinder {

	private   static final Log log      = Config.getLogger();
	protected static final int    XO_TRIES = SpecializedConstants.getInt(RandomCrossoverFinder.class, "xo-tries");

	private final Map<Integer, List<CodeSection>>	alphaSections;
	private final Map<Integer, List<CodeSection>>	betaSections;
	private final CompatibleCrossover				xo;

	protected final MersenneTwisterFast				random;

	private final int		maxSize;		// 0 -> not used
	private final int       alphaSize;

	private final Integer[]	alphaKeys;
	private final Integer[] betaKeys;

	public RandomCrossoverFinder(AnalyzedMethodNode alphaMethod, AnalyzedMethodNode betaMethod,
								CompatibleCrossover xo, MersenneTwisterFast random,
								int maxSize) {
		super(alphaMethod, betaMethod);
		assert alphaBranches != betaBranches;

		this.alphaSections = alphaBranches.getSortedSections();
		this.betaSections  = betaBranches.getSortedSections();
		this.xo            = xo;
		this.random        = random;

		this.maxSize       = maxSize;
		alphaSize          = alphaMethod.instructions.size();

		alphaKeys = this.alphaSections.keySet().toArray(new Integer[0]);
		betaKeys  = this.betaSections .keySet().toArray(new Integer[0]);

		assert alphaKeys[0] == 0;
		assert betaKeys [0] == 0;
	}

	@Override
	public Sections getSuggestion() {
		for (int t = 1;  t <= XO_TRIES;  ++t) {
			CodeSection alpha = pickCodeSection(alphaSections, alphaKeys);
			CodeSection beta  = pickCodeSection(betaSections,  betaKeys);

			// Skip crossovers resulting in over-maximum size methods
			// (the computation is an approximation, due to frame nodes)
			if (maxSize != 0  &&  alphaSize - alpha.size() + beta.size() > maxSize)
				continue;

			// Success - produce sections pair
			if (xo.isCompatible(alpha, beta)) {
				log.trace("Crossover found after " + t + " attempts");
				return new Sections(alpha, beta);
			}
		}

		// null indicates failure
		log.warn("Compatible crossover: DEST=" + alphaBranches.getName() + ", SRC=" + betaBranches.getName()
				+ " failed after " + XO_TRIES + " attempts");
		return null;
	}

	/**
	 * Randomly picks a code section (e.g., Gaussian or Uniform distribution of
	 * section sizes).
	 *
	 * @param sections lists of code sections by sections sizes
	 * @param keys sorted array of section sizes (convenience parameter)
	 * @return chosen section
	 */
	protected abstract CodeSection pickCodeSection(Map<Integer, List<CodeSection>> sections, Integer[] keys);

}
