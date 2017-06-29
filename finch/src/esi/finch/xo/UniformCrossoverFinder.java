package esi.finch.xo;

import java.util.List;
import java.util.Map;

import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.CodeSection;
import esi.bc.xo.CompatibleCrossover;

/**
 * Uniform picker of crossover sections.
 *
 * First, a section size is chosen randomly among the available sizes,
 * and then a random section of given size is chosen.
 *
 * @author Michael Orlov
 */
public class UniformCrossoverFinder extends RandomCrossoverFinder {

	public UniformCrossoverFinder(AnalyzedMethodNode alphaMethod, AnalyzedMethodNode betaMethod,
			CompatibleCrossover xo, MersenneTwisterFast random, int maxSize) {
		super(alphaMethod, betaMethod, xo, random, maxSize);
	}

	@Override
	protected CodeSection pickCodeSection(Map<Integer, List<CodeSection>> sections, Integer[] keys) {
		// Pick segment size
		int key = keys[random.nextInt(keys.length)];

		// Get random segment of that size
		List<CodeSection> list = sections.get(key);
		CodeSection       sec  = list.get(random.nextInt(list.size()));

		return sec;
	}

}
