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
 * First, a section size is chosen using |N(0,sizes/factor)| among the available
 * sizes (in the paper, "sizes" is described as method length), and then a random
 * section of given size is chosen.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.GaussianCrossoverFinder.factor</code>: sigma divisor
 * </ul>
 *
 * @author Michael Orlov
 */
public class GaussianCrossoverFinder extends RandomCrossoverFinder {

	private static final float FACTOR = SpecializedConstants.getFloat(GaussianCrossoverFinder.class, "factor");

	public GaussianCrossoverFinder(AnalyzedMethodNode alphaMethod, AnalyzedMethodNode betaMethod,
			CompatibleCrossover xo, MersenneTwisterFast random, int maxSize) {
		super(alphaMethod, betaMethod, xo, random, maxSize);
	}

	@Override
	protected CodeSection pickCodeSection(Map<Integer, List<CodeSection>> sections, Integer[] keys) {
		// FACTOR * sigma = method size
		int index = (int) Math.round(Math.abs(random.nextGaussian()) * keys.length / FACTOR);

		// Pick segment size
		int key = keys[Math.min(index, keys.length-1)];

		// Get random segment of that size
		List<CodeSection> list = sections.get(key);
		CodeSection       sec  = list.get(random.nextInt(list.size()));

		return sec;
	}

}
