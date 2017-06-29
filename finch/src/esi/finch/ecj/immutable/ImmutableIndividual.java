package esi.finch.ecj.immutable;

import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.simple.SimpleInitializer;
import ec.simple.SimpleProblemForm;

/**
 * An individual that is immutable and supports mutation
 * and unidirectional crossover.
 *
 * @author Michael Orlov
 *
 * @param <T> the individual type
 */
public interface ImmutableIndividual<T extends Individual> {

	/**
	 * Resets the individual's genome to some special genome.
	 *
	 * This method is intended for {@link Species#newIndividual(EvolutionState, int)},
	 * and the special genome is intended for detection by, e.g.,
	 * {@link SimpleProblemForm#evaluate(EvolutionState, Individual, int, int)}.
	 *
	 * Typically, it will be called once for each individual in population 0.
	 * If the individual is initialized to the same special genome every
	 * time (the intended usage), no duplicate checking should be enabled
	 * ("subpop.duplicate-retries").
	 *
	 * @param state evolution state
	 * @param thread thread index
	 * @see SimpleInitializer#initialPopulation(EvolutionState, int)
	 */
	void reset(EvolutionState state, int thread);

	/**
	 * Possibly crossover this individual with another individual.
	 * The crossover operation is unidirectional, and produces one
	 * individual.
	 *
	 * If crossover occurs, a new individual should be returned,
	 * with {@link Individual#evaluated} set to <code>false</code>.
	 * Otherwise, <code>this</code> should be returned.
	 *
	 * Crossover probability should probably be extracted from {@link Species}
	 * in the supplied state. Random number generator, indexed by thread
	 * is also in the state.
	 *
	 * @param other other individual for crossover
	 * @param state evolution state
	 * @param thread thread index
	 * @return a new {@link Individual}, or <code>this</code>
	 */
	T crossover(T other, EvolutionState state, int thread);

	/**
	 * Possibly mutate this individual.
	 *
	 * If mutation occurs, a new individual should be returned,
	 * with {@link Individual#evaluated} set to <code>false</code>.
	 * Otherwise, <code>this</code> should be returned.
	 *
	 * Mutation probability should probably be extracted from {@link Species}
	 * in the supplied state. Random number generator, indexed by thread
	 * is also in the state.
	 *
	 * @param state evolution state
	 * @param thread thread index
	 * @return a new {@link Individual}, or <code>this</code>
	 */
	T mutate(EvolutionState state, int thread);

	/**
	 * A predicate returning true if this individual is a special
	 * initial individual, i.e., initialized using {@link #reset(EvolutionState, int)}.
	 *
	 * Typically, this means that its fitness can be assessed directly,
	 * without evaluation.
	 *
	 * @return whether this individual is initial.
	 */
	boolean isInitial();

}
