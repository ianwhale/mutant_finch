package esi.finch.mut;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.test.Fact;

import org.junit.BeforeClass;

public class InsertMutatorTest {
	private static MersenneTwisterFast random;
	private static MethodNode fact;
	private static InsertMutator mut; 
	
	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		random = new MersenneTwisterFast(1234);
		
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		fact = cn.findMethod(new Method("fact", "(I)I"));
		
		mut = new InsertMutator(random, 1);
	}
	
	@Test
	public void testNewInstruction() {
		int position = 3;
		
		AbstractInsnNode before = fact.instructions.get(position + 1);
		
		mut.newInstruction(fact, position);
		
		AbstractInsnNode after = fact.instructions.get(position + 1);
		
		assertTrue(before != after);
	}
}
