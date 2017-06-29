package esi.finch;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import esi.finch.ecj.Experiment;
import esi.finch.ecj.bc.BytecodeProblem;

public class FinchExperimentTest {

	@Test
	public void experiment() {
		URL params = FinchExperimentTest.class.getResource("probs/test-bytecode.params");
		assertNotNull(params);

		Experiment exp = new Experiment(params, BytecodeProblem.class, 0);
		exp.run();
	}

	@Test
	public void SETexperiment() {
		URL params = FinchExperimentTest.class.getResource("probs/test-bytecode-set.params");
		assertNotNull(params);

		Experiment exp = new Experiment(params, BytecodeProblem.class, 0);
		exp.run();
	}

}
