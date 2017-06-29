package esi.finch.mut;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;
import org.junit.BeforeClass;
import org.objectweb.asm.commons.Method;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.test.Fact;

public class DeletionMutatorTest {

	private static MersenneTwisterFast random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(1234);
	}
	
	@Test
	public void testDeleteInstruction() throws IOException {
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));
		
		int before = mn.instructions.size();
		
		DeletionMutator.deleteInstruction(mn, 10);
		
		assertEquals(before - 1, mn.instructions.size());
		
	}
}
