package esi.finch.ecj.string;

import java.util.Arrays;

import org.apache.commons.logging.Log;

import ec.EvolutionState;
import ec.Individual;
import ec.util.MersenneTwisterFast;
import ec.util.Parameter;
import esi.finch.ecj.immutable.ImmutableIndividual;
import esi.finch.ecj.immutable.ImmutableSpecies;
import esi.util.Config;

/**
 * String individual.
 *
 * @author Michael Orlov
 */
public class StringIndividual extends Individual implements ImmutableIndividual<StringIndividual> {

	// Counters for testing purposes
	public  static int   xoChanged;
	public  static int   xoSame;
	public  static int   mutChanged;
	public  static int   mutSame;

	private static final Log log = Config.getLogger();

	private static final long   serialVersionUID    = 1L;
	private static final String P_STRING_INDIVIDUAL = "ind";

	private static final String P_SIZE              = "size";

	private int    size;
	private String initGenome;

	// Genome is a string
	private String genome;

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);

        Parameter def = defaultBase();

        size = state.parameters.getInt(base.push(P_SIZE), def.push(P_SIZE), 1);
		if (size == 1-1)
			state.output.error("Non-positive genome size", base.push(P_SIZE), def.push(P_SIZE));

		char[] letters = new char[size];
		Arrays.fill(letters, '-');
		initGenome = new String(letters);


		log.info("Individual set up: size=" + size);
	}

	@Override
	public StringIndividual clone() {
		// Genome is immutable so parent's clone is fine
		return (StringIndividual) super.clone();
	}

	// used by problem for evaluation
	public String getGenome() {
		return genome;
	}

	// Used by species (usually, generation 0 fill)
	public void reset(EvolutionState state, int thread) {
		assert !evaluated  &&  genome == null;
		genome = initGenome;

		log.trace("Reset: " + genome);
	}

	@Override
	public boolean isInitial() {
		return genome == initGenome;
	}

	// Used by mutation pipeline
	public StringIndividual mutate(EvolutionState state, int thread) {
		ImmutableSpecies    species = (ImmutableSpecies) this.species;
		MersenneTwisterFast random  = state.random[thread];

		StringIndividual res = this;

		for (int i = 0;  i < genome.length();  ++i)
			if (random.nextBoolean(species.getMutProb())) {
				if (res == this)
					res = clone();

				char letter   = (char) ('A' + random.nextInt('Z'-'A'+1));
				int  position = random.nextInt(genome.length());

				res.genome    = res.genome.substring(0, position) + letter + res.genome.substring(position+1);
				res.evaluated = false;
			}

		if (res == this)
			++mutSame;
		else
			++mutChanged;

		log.trace("Mutation: " + ((res == this) ? "same" : "changed") + " individual");
		return res;
	}

	// Used by crossover pipeline
	public StringIndividual crossover(StringIndividual other, EvolutionState state, int thread) {
		assert evaluated  &&  other.evaluated;

		ImmutableSpecies    species = (ImmutableSpecies) this.species;
		MersenneTwisterFast random  = state.random[thread];

		StringIndividual res = this;

		if (random.nextFloat() < species.getXoProb()) {
			res = clone();
			int position = random.nextInt(genome.length());

			res.genome    = res.genome.substring(0, position) + other.genome.substring(position);
			res.evaluated = false;

			++xoChanged;
		}
		else
			++xoSame;

		log.trace("Crossover: " + ((res == this) ? "same" : "changed") + " individual");
		return res;
	}

	@Override
	public long size() {
		return genome.length();
	}

	@Override
	public String toString() {
		return "String = " + genome;
	}

	@Override
	public String genotypeToStringForHumans() {
		return "Nice string: {{{ " + genome + " }}}";
	}

	@Override
	public boolean equals(Object ind) {
		assert ind instanceof String;
		return genome.equals(ind);
	}

	@Override
	public int hashCode() {
		return genome.hashCode();
	}

	@Override
	public Parameter defaultBase() {
		return StringDefaults.base().push(P_STRING_INDIVIDUAL);
	}

}
