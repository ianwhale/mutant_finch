package esi.finch.ecj.immutable;

import org.apache.commons.logging.Log;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import esi.util.Config;

/**
 * A unidirectional crossover pipeline for immutable individuals.
 *
 * Always produces one individual from two sources.
 *
 * If no crossover occurred, the individual from the first source is cloned
 * in order to have separate {@link Individual#fitness} and {@link Individual#evaluated}.
 *
 * Default base: <tt>immutable.xo</tt>.
 *
 * @author Michael Orlov
 */
public class ImmutableCrossoverPipeline extends BreedingPipeline {

	private static final Log log = Config.getLogger();

	private static final long   serialVersionUID = 1L;
	private static final String P_XO_PIPELINE    = "xo";

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {
		// Shouldn't happen, but handle max=0
		if (max < 1)
			return 0;
		// For simpler code, insist on producing single individuals
		if (min > 1)
			throw new IllegalArgumentException("Can produce only one individual");

		// Get two source individuals
		Individual[] src = new Individual[2];
		sources[0].produce(1, 1, 0, subpopulation, src, state, thread);
		sources[1].produce(1, 1, 1, subpopulation, src, state, thread);

		// Produce crossed-over individual
		// <? extends Individual> doesn't work for some reason
		@SuppressWarnings("unchecked")
		Individual res = ((ImmutableIndividual<Individual>) src[0]).crossover(src[1], state, thread);

		// If same individual was returned, clone it, so that it has separate fitness and evaluated status
		// This prevents problems in e.g., single-elimination tournament
		if (res == src[0])
			res = (Individual) src[0].clone();

		// Put result into inds
		inds[start] = res;

		return 1;
	}

	@Override
	public Parameter defaultBase() {
		return ImmutableDefaults.base().push(P_XO_PIPELINE);
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);
		log.info("Crossover pipeline set up");
	}

	@Override
	public int numSources() {
		return 2;
	}

}
