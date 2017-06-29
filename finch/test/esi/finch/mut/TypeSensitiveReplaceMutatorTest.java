package esi.finch.mut;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.test.Fact;

public class TypeSensitiveReplaceMutatorTest {
	
	private static MersenneTwisterFast random;
	private static MethodNode fact;
	private static TypeSensitiveReplaceMutator mut; 
	
	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		random = new MersenneTwisterFast(1234);
		
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		fact = cn.findMethod(new Method("fact", "(I)I"));
		
		mut = new TypeSensitiveReplaceMutator(random, 1);
	}
	
	@Test
	public void testReplaceInstruction() {
		int position = 3;
		
		AbstractInsnNode before = fact.instructions.get(position);
		
		mut.replaceInstruction(fact, position);
		
		AbstractInsnNode after = fact.instructions.get(position);
		
		assertTrue(before != after);
		assertEquals(before.getType(), after.getType());
	}
}
