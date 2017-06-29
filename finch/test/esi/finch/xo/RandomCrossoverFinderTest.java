package esi.finch.xo;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.commons.Method;

import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.test.Fact;
import esi.bc.test.FactLong;
import esi.bc.xo.CompatibleCrossover;
import esi.bc.xo.TypeVerifier;
import esi.finch.xo.CrossoverFinder.Sections;
import esi.util.Config;

public class RandomCrossoverFinderTest {

	private static MersenneTwisterFast random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(1234);
	}

	@Test
	public void xoTries() {
		assertTrue(RandomCrossoverFinder.XO_TRIES > 0);
	}

	@Test
	public void getSuggestion() throws IOException {
		AnalyzedClassNode cn1  = AnalyzedClassNode.readClass(Fact.class);
		AnalyzedClassNode cn2  = AnalyzedClassNode.readClass(FactLong.class);
		TypeVerifier verifier = new TypeVerifier(cn1, cn2);

		AnalyzedMethodNode mn1 = cn1.findMethod(new Method("fact", "(I)I"));
		AnalyzedMethodNode mn2 = cn2.findMethod(new Method("fact", "(J)J"));

		CompatibleCrossover xo         = new CompatibleCrossover(mn1, mn2, verifier);
		CrossoverFinder     xoFinder   = new UniformCrossoverFinder(mn1, mn2, xo, random, 50);
		Sections            xoSections = xoFinder.getSuggestion();

		assertNotNull(xoSections);
		assertTrue(xo.isCompatible(xoSections.alpha, xoSections.beta));
	}

	@Test
	public void getComplexSuggestion() throws IOException {
		AnalyzedClassNode cn1 = getNode("FinchPlayer_G46_T1_9199");
		AnalyzedClassNode cn2 = getNode("FinchPlayer_G48_T1_9662");
		TypeVerifier verifier = new TypeVerifier(cn1, cn2);

		AnalyzedMethodNode mn1 = cn1.findMethod(new Method("makeMove", "()V"));
		AnalyzedMethodNode mn2 = cn2.findMethod(new Method("makeMove", "()V"));

		assertEquals(224, mn1.instructions.size());
		assertEquals(251, mn2.instructions.size());

		// Fails with UniformCrossoverFinder
		CompatibleCrossover xo         = new CompatibleCrossover(mn1, mn2, verifier);
		CrossoverFinder     xoFinder   = new FixedGaussianCrossoverFinder(mn1, mn2, xo, random, 276);
		Sections            xoSections = xoFinder.getSuggestion();

		assertNotNull(xoSections);
	}

	private AnalyzedClassNode getNode(String shortClassName) throws IOException {
		return AnalyzedClassNode.readClass(Config.DIR_IN_TESTS.resolve(shortClassName + ".class").openStream(), false, false);
	}

}
