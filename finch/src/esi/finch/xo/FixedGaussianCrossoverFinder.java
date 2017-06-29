package esi.finch.xo;

import java.util.List;
import java.util.Map;

import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.CodeSection;
import esi.bc.xo.CompatibleCrossover;
import esi.util.SpecializedConstants;

/**
 * Gaussian picker of code sections.
 *
 * First, a section size closest to |N(0,sigma)| among the available
 * sizes (in the paper, "sizes" is described as method length), and then a random
 * section of given size is chosen.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.FixedGaussianCrossoverFinder.sigma</code>: sigma
 * </ul>
 *
 * @author Michael Orlov
 */
public class FixedGaussianCrossoverFinder extends RandomCrossoverFinder {

	private static final float SIGMA = SpecializedConstants.getFloat(FixedGaussianCrossoverFinder.class, "sigma");

	public FixedGaussianCrossoverFinder(AnalyzedMethodNode alphaMethod, AnalyzedMethodNode betaMethod,
			CompatibleCrossover xo, MersenneTwisterFast random, int maxSize) {
		super(alphaMethod, betaMethod, xo, random, maxSize);
	}

	@Override
	protected CodeSection pickCodeSection(Map<Integer, List<CodeSection>> sections, Integer[] keys) {
		// FACTOR * sigma = method size
		float target = (float) Math.abs(random.nextGaussian()) * SIGMA;

		// Search for closest section size, using the sorted order of entries
		List<CodeSection> list = null;
		float             diff = Float.POSITIVE_INFINITY;

		for (List<CodeSection> aList: sections.values()) {
			float aDiff = Math.abs(aList.get(0).size() - target);

			if (aDiff < diff) {
				list = aList;
				diff = aDiff;
			}
			else
				break;
		}

		// Get random segment of chosen size
		assert list != null;
		CodeSection sec = list.get(random.nextInt(list.size()));

		return sec;
	}

}
