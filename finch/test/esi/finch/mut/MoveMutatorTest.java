package esi.finch.mut;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;
import org.junit.BeforeClass;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.test.Fact;

public class MoveMutatorTest {

	private static MersenneTwisterFast random;
	private static MoveMutator mutator;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(1234);
		mutator = new MoveMutator(random, 1);
	}
	
	@Test
	public void testMoveInstruction() throws IOException {
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));
		
		int before = mn.instructions.size();
		
		int origin = mutator.getValidIndex(mn);
		int destination = mutator.getValidIndex(mn);
		
		AbstractInsnNode insn = mn.instructions.get(origin);
		
		MoveMutator.moveInstruction(mn, origin, destination);
		
		assertEquals(before, mn.instructions.size()); // No change in length. 
		assertEquals(insn, mn.instructions.get(destination + 1)); // Make sure node was inserted after destination. 
	}
	
	@Test
	public void testGetValidIndex() throws IOException {
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));
		
		int index = mutator.getValidIndex(mn);
		
		assertTrue(mn.instructions.get(index).getOpcode() >= 0);
	}
}
