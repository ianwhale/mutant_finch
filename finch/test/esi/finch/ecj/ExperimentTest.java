package esi.finch.ecj;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import ec.vector.BitVectorIndividual;

public class ExperimentTest {

	public static class TestProblem extends Problem implements SimpleProblemForm {
		private static final long serialVersionUID = 1L;
		private static final int  GENOME_SIZE      = 32;

		@Override
		public void evaluate(EvolutionState state, Individual ind,
				int subpopulation, int threadnum) {
			// don't evaluate if already evaluated
			if (! ind.evaluated) {
				// sanity checks
				// (can also use state.output for logs)
				assertEquals(GENOME_SIZE, state.parameters.getInt(new Parameter("pop.subpop.0.species.genome-size"), null));
				assertTrue(ind         instanceof BitVectorIndividual);
				assertTrue(ind.fitness instanceof SimpleFitness);

				BitVectorIndividual bvind = (BitVectorIndividual) ind;
				SimpleFitness       bvfit = (SimpleFitness) ind.fitness;

				// bit-vector sanity checks
				boolean[] genome = bvind.genome;
				assertEquals(GENOME_SIZE, genome.length);

				// evaluate individual (higher fitness is better)
				float fitness = 0;
				int power = 1, sum = 0;
				for (int i = 0;  i < genome.length;  ++i, power *= 2)
					if (genome[i])
						sum += power;
				fitness = - Math.abs(sum - 12345678);

				// report fitness (never ideal)
				bvfit.setFitness(state, fitness, false);

				// mark as evaluated
				bvind.evaluated = true;
			}
		}

		// Called in SimpleStatistics.finalStatistics
		@Override
		public void describe(EvolutionState state, Individual ind,
				int subpopulation, int threadnum, int log) {
			assertTrue(ind.evaluated);
		}
	}

	@Test
	public void testExperiment() {
		URL params = ExperimentTest.class.getResource("test-simple.params");
		assertNotNull(params);

		// Creates a checkpoint at generation 30
		Experiment exp = new Experiment(params, TestProblem.class, 0);
		exp.run();

		// Reruns the experiment starting from the checkpoint
		exp = new Experiment(30);
		exp.run();
	}

	@Test(expected = Error.class)
	public void testExperimentFail() {
		URL params = ExperimentTest.class.getResource("nonexistent.params");
		assertNull(params);

		new Experiment(params, TestProblem.class, 0);
	}

	@Test(expected = Error.class)
	public void testExperimentCheckpointFail() {
		new Experiment(31);
	}

}
