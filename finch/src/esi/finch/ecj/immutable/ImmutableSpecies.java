package esi.finch.ecj.immutable;

import org.apache.commons.logging.Log;

import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.simple.SimpleInitializer;
import ec.util.Parameter;
import esi.util.Config;

/**
 * Species with immutable individuals.
 *
 * <p>Default base: <tt>immutable.species</tt>.
 * Parameters:
 * <ul>
 * <li><tt>xo-prob</tt>,
 * <li><tt>mut-prob</tt>.
 * </ul>
 *
 * @author Michael Orlov
 */
public class ImmutableSpecies extends Species {

	private static final Log    log = Config.getLogger();

	private static final long   serialVersionUID    = 1L;
    private static final String P_IMMUTABLE_SPECIES = "species";

    private static final String P_XO_PROB  = "xo-prob";
    private static final String P_MUT_PROB = "mut-prob";

	// Initial values (filled in setup)
    private float	xoProb;
    private float	mutProb;

	/**
	 * Initializes new individuals with {@link ImmutableIndividual#reset(EvolutionState, int)},
	 * which typically happens only in generation 0.
	 *
	 * @see ec.Species#newIndividual(ec.EvolutionState, int)
	 * @see SimpleInitializer#initialPopulation(EvolutionState, int)
	 */
	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
		Individual ind = super.newIndividual(state, thread);
		((ImmutableIndividual<?>) ind).reset(state, thread);

		return ind;
	}

	@Override
	public Parameter defaultBase() {
		return ImmutableDefaults.base().push(P_IMMUTABLE_SPECIES);
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		// Set up BreedingPipeline, Individual, and Fitness prototypes
		super.setup(state, base);

		Parameter def = defaultBase();

		// Crossover probability
		xoProb = state.parameters.getFloatWithMax(
				base.push(P_XO_PROB), def.push(P_XO_PROB), 0.0, 1.0);
		if (xoProb == 0.0f - 1)
			state.output.error("Incorrect crossover probability", base.push(P_XO_PROB), def.push(P_XO_PROB));

		// Mutation probability
		mutProb = state.parameters.getFloatWithMax(
				base.push(P_MUT_PROB), def.push(P_MUT_PROB), 0.0, 1.0);
		if (mutProb == 0.0f - 1)
			state.output.error("Incorrect mutation probability", base.push(P_MUT_PROB), def.push(P_MUT_PROB));

		log.info("Species set up:"
				+ "\n    xo="  + xoProb
				+ "\n    mut=" + mutProb);
	}

	/**
	 * @return crossover probability, in [0.0, 1.0]
	 */
	public float getXoProb() {
		return xoProb;
	}

	/**
	 * @return mutation probability, in [0.0, 1.0]
	 */
	public float getMutProb() {
		return mutProb;
	}

}
