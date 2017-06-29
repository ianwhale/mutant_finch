package esi.finch.ecj.string;

import static org.junit.Assert.*;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Problem;
import ec.coevolve.GroupedProblemForm;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import esi.finch.ecj.Experiment;
import esi.util.Config;

public class StringIndividualTest {

	private static final Log log = Config.getLogger();
	private static final int GENOME_SIZE = 10;

	@BeforeClass
	public static void setUpBeforeClass() {
		StringIndividual.xoChanged  = 0;
		StringIndividual.xoSame     = 0;
		StringIndividual.mutChanged = 0;
		StringIndividual.mutSame    = 0;
	}

	public static class TestProblem extends Problem implements SimpleProblemForm, GroupedProblemForm {
		private static final long serialVersionUID = 1L;

		private static final String TARGET = "DEVOLUTION";

		// "Belongs" to SimpleProblemForm
		@Override
		public void evaluate(EvolutionState state, Individual ind,
				int subpopulation, int threadnum) {
			// don't evaluate if already evaluated
			if (! ind.evaluated) {
				log.trace("Evaluating: " + ind);

				// sanity checks
				// (can also use state.output for logs)
				assertEquals(GENOME_SIZE, state.parameters.getInt(new Parameter("pop.subpop.0.species.ind.size"), new Parameter("string.ind.size")));
				assertTrue(ind         instanceof StringIndividual);
				assertTrue(ind.fitness instanceof SimpleFitness);

				StringIndividual sind = (StringIndividual) ind;
				SimpleFitness    sfit = (SimpleFitness) ind.fitness;

				// string sanity checks
				String genome = sind.getGenome();
				assertEquals(GENOME_SIZE, genome.length());

				// evaluate individual (higher fitness is better)
				int fitness = 0;

				// detect special initial individuals
				// (whole generation 0, and some also stay further after crossover)
				if (sind.isInitial())
					log.trace("Skipping evaluation of initial individual");
				else
					for (int i = 0;  i < genome.length();  ++i)
						if (genome.charAt(i) == TARGET.charAt(i))
							fitness += 1;

				boolean ideal = (fitness == TARGET.length());
				if (ideal)
					log.debug("Ideal individual found");

				// report fitness (ideal if full match)
				sfit.setFitness(state, fitness, ideal);

				// mark as evaluated
				sind.evaluated = true;
			}
			else
				// When neither crossover nor mutation change the individual
				log.trace("Already evaluated: " + ind);
		}

		// Called in SimpleStatistics.finalStatistics (for SimpleProblemForm, which we implement))
		@Override
		public void describe(EvolutionState state, Individual ind,
				int subpopulation, int threadnum, int lg) {
			assertTrue(ind.evaluated);
			log.info("Best individual: " + ind);

			float xoProb  = state.parameters.getFloat(new Parameter("pop.subpop.0.species.xo-prob"),  new Parameter("immutable.species.xo-prob"),  0);
			float mutProb = state.parameters.getFloat(new Parameter("pop.subpop.0.species.mut-prob"), new Parameter("immutable.species.mut-prob"), 0);
			assertTrue(xoProb  != -1);
			assertTrue(mutProb != -1);

			float xoProp  = (float) StringIndividual.xoChanged  / (StringIndividual.xoChanged  + StringIndividual.xoSame);
			float mutProp = (float) StringIndividual.mutChanged / (StringIndividual.mutChanged + StringIndividual.mutSame);

			float xoPropEstimate  = xoProb;
			float mutPropEstimate = 1 - (float) Math.pow(1 - mutProb, GENOME_SIZE);

			assertEquals(xoPropEstimate,  xoProp,  0.1);
			assertEquals(mutPropEstimate, mutProp, 0.1);
		}

		// "Belongs" to GroupedProblemForm
		@Override
		public void preprocessPopulation(EvolutionState state, Population pop, boolean countVictoriesOnly) {
			assertEquals(1, pop.subpops.length);
			assertTrue(countVictoriesOnly);

			for (int i = 0;  i < pop.subpops.length;  ++i) {
				int subPopSize = pop.subpops[i].individuals.length;
				assertEquals(subPopSize, state.parameters.getInt(new Parameter("pop.subpop.0.size"), new Parameter("ec.subpop.size")));

				// Test for power of 2
				assertEquals(0, subPopSize & (subPopSize - 1));
				assertTrue(subPopSize > 0);

				for (int j = 0;  j < pop.subpops[i].individuals.length; ++j) {
					StringIndividual sind = (StringIndividual) pop.subpops[i].individuals[j];
					SimpleFitness    sfit = (SimpleFitness) sind.fitness;
					assertTrue(state.generation != 0  ||  !sind.evaluated);

					// Reset fitness (0 - no tournaments won)
					sfit.setFitness(state, 0, false);
					sind.evaluated = false;
				}
			}
		}

		// "Belongs" to GroupedProblemForm
		@Override
		public void postprocessPopulation(EvolutionState state, Population pop, boolean countVictoriesOnly) {
			assertEquals(1, pop.subpops.length);
			assertTrue(countVictoriesOnly);

			for (int i = 0;  i < pop.subpops.length;  ++i) {
				int subPopSize = pop.subpops[i].individuals.length;
				assertEquals(subPopSize, state.parameters.getInt(new Parameter("pop.subpop.0.size"), new Parameter("ec.subpop.size")));

				// Test for power of 2
				assertEquals(0, subPopSize & (subPopSize - 1));
				assertTrue(subPopSize > 0);

				for (int j = 0;  j < pop.subpops[i].individuals.length; ++j) {
					StringIndividual sind = (StringIndividual) pop.subpops[i].individuals[j];
					SimpleFitness    sfit = (SimpleFitness) sind.fitness;
					assertFalse(sind.evaluated);

					// Indicate that the individual is now evaluated
					// (Note that the tournament could have happened several times by now)
					sind.evaluated = true;
					sfit.setFitness(state, sfit.fitness() + state.generation, sfit.isIdealFitness());
				}
			}
		}

		// "Belongs" to GroupedProblemForm
		@Override
		public void evaluate(EvolutionState state, Individual[] ind,
				boolean[] updateFitness, boolean countVictoriesOnly,
				int[] subpops, int threadnum) {
			assertEquals(2, ind.length);
			assertEquals(2, updateFitness.length);

			assertTrue(updateFitness[0]);
			assertTrue(updateFitness[1]);
			assertTrue(countVictoriesOnly);

			assertEquals(2, subpops.length);
			assertEquals(0, subpops[0]);
			assertEquals(0, subpops[1]);

			assertTrue(ind[0]         instanceof StringIndividual);
			assertTrue(ind[1]         instanceof StringIndividual);
			assertTrue(ind[0].fitness instanceof SimpleFitness);
			assertTrue(ind[1].fitness instanceof SimpleFitness);

			StringIndividual sind1 = (StringIndividual) ind[0];
			StringIndividual sind2 = (StringIndividual) ind[1];
			SimpleFitness    sfit1 = (SimpleFitness) sind1.fitness;
			SimpleFitness    sfit2 = (SimpleFitness) sind2.fitness;

			assertNotSame(sind1, sind2);
			assertNotSame(sfit1, sfit2);

			// string sanity checks
			String genome1 = sind1.getGenome();
			assertEquals(GENOME_SIZE, genome1.length());
			String genome2 = sind2.getGenome();
			assertEquals(GENOME_SIZE, genome2.length());

			// evaluate individuals (higher fitness is better)
			int hiddenFitness1 = 0;
			int hiddenFitness2 = 0;

			// detect special initial individuals
			// (whole generation 0, and some also stay further after crossover)
			if (sind1.isInitial())
				log.trace("Skipping evaluation of initial individual 1");
			else
				for (int i = 0;  i < genome1.length();  ++i)
					if (genome1.charAt(i) == TARGET.charAt(i))
						hiddenFitness1 += 1;
			boolean ideal1 = (hiddenFitness1 == TARGET.length());
			if (ideal1)
				log.trace("Ideal individual found");

			if (sind2.isInitial())
				log.trace("Skipping evaluation of initial individual 2");
			else
				for (int i = 0;  i < genome2.length();  ++i)
					if (genome2.charAt(i) == TARGET.charAt(i))
						hiddenFitness2 += 1;
			boolean ideal2 = (hiddenFitness2 == TARGET.length());
			if (ideal2)
				log.trace("Ideal individual found");

			assertTrue(sfit1.fitness() >= 0);
			assertTrue(sfit2.fitness() >= 0);
			assertTrue(sfit1.equivalentTo(sfit2));

			// Simulate competition: increment fitness of the winner
			// NOTE: CompetitiveEvaluator#runComplete() does not check ideal fitness
			if (hiddenFitness1 > hiddenFitness2  ||
					((hiddenFitness1 == hiddenFitness2)  &&  state.random[threadnum].nextBoolean())) {
				sfit1.setFitness(state, sfit1.fitness() + 1, ideal1);

				assertTrue(sfit1.betterThan(sfit2));
				log.trace(genome1 + " vs " + genome2 + " -> " + genome1 + " " + sfit1.fitness());
			}
			else {
				sfit2.setFitness(state, sfit2.fitness() + 1, ideal2);

				assertTrue(sfit2.betterThan(sfit1));
				log.trace(genome1 + " vs " + genome2 + " -> " + genome2 + " " + sfit2.fitness());
			}

		}
	}

	@Test
	public void testExperiment() {
		URL params = StringIndividualTest.class.getResource("test-string.params");
		assertNotNull(params);

		Experiment exp = new Experiment(params, TestProblem.class, 0);
		exp.run();
	}

	@Test
	public void testSingleEliminationExperiment() {
		URL params = StringIndividualTest.class.getResource("test-string-set.params");
		assertNotNull(params);

		Experiment exp = new Experiment(params, TestProblem.class, 0);
		exp.run();
	}

}
