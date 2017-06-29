package esi.finch.ecj.immutable;

import static org.junit.Assert.*;

import org.junit.Test;

public class ImmutablePipelinesTest {

	@Test
	public void numSources() {
		ImmutableCrossoverPipeline xoPipeline = new ImmutableCrossoverPipeline();
		assertEquals(2, xoPipeline.numSources());

		ImmutableMutationPipeline mutPipeline = new ImmutableMutationPipeline();
		assertEquals(1, mutPipeline.numSources());
	}

}
