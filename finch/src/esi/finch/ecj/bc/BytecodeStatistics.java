package esi.finch.ecj.bc;

import org.apache.commons.logging.Log;

import ec.EvolutionState;
import ec.simple.SimpleStatistics;
import esi.util.Config;

/**
 * Extended variant of {@link SimpleStatistics}.
 *
 * <p>Extensions:
 * <ul>
 * <li>Prints population size
 * </ul>
 *
 * @author Michael Orlov
 */
public class BytecodeStatistics extends SimpleStatistics {

	private static final long	serialVersionUID = 1L;
	private static final Log	log = Config.getLogger();

	@Override
	public void postInitializationStatistics(EvolutionState state) {
		super.postInitializationStatistics(state);

		if (log.isInfoEnabled()) {
			StringBuilder buf = new StringBuilder("Population initialized:");

			for (int i = 0;  i < state.population.subpops.length;  ++i)
				buf.append("\n    sub-pop ")
				   .append(i)
				   .append(" size=")
				   .append(state.population.subpops[i].individuals.length);

			log.info(buf.toString());
		}
	}

	@Override
	public void finalStatistics(EvolutionState state, int result) {
		super.finalStatistics(state, result);

		// SandBox shutdown was moved from here to FinchExperiment
		// in order to enable multiple bytecode evolutions in tests.
	}

}
