package esi.finch.mut;

import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.BeforeClass;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.test.Fact;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InstructionFactoryTest {
	
	private static MersenneTwisterFast random;
	private static MethodNode fact;
	
	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		random = new MersenneTwisterFast(1234);
		
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		fact = cn.findMethod(new Method("fact", "(I)I"));
	}
	
	@Test
	public void testMakeInsn() {
		int opcode = 0x00; // No operation. 
		InsnNode node = (InsnNode) InstructionFactory.makeInstruction(fact, random, opcode, AbstractInsnNode.INSN);
		
		assertEquals(AbstractInsnNode.INSN, node.getType());
	}
	

	@Test
	public void testMakeIincInsn() {
		IincInsnNode node = (IincInsnNode) InstructionFactory.makeIincInsn(fact, random);
		
		int locals_size = fact.localVariables.size();
		
		assertTrue((node.incr <= 127) && (node.incr >= -128)); // Increment value is a signed byte.
		assertTrue((node.var < locals_size) && (node.var >= 0)); // Does not go outside locals range. 
	}
	
	@Test
	public void testMakeIntInsn() {
		IntInsnNode node = (IntInsnNode) InstructionFactory.makeIntInsn(random, 0xbc); // NEWARRAY
		assertTrue((node.operand >= 4) && (node.operand <= 11)); // Valid type for new array
		
		node = (IntInsnNode) InstructionFactory.makeIntInsn(random, 0x10); // BIPUSH
		assertTrue((node.operand >= Byte.MIN_VALUE) && (node.operand <= Byte.MAX_VALUE));
		
		node = (IntInsnNode) InstructionFactory.makeIntInsn(random, 0x11); // SIPUSH
		assertTrue((node.operand >= Short.MIN_VALUE) && (node.operand <= Short.MAX_VALUE));
	}
	
	@Test 
	public void testMakeLdcInsn() {
		LdcInsnNode node = (LdcInsnNode) InstructionFactory.makeLdcInsn(random); // LDC

		for (int i = 0; i < 100; i++) {
			if (node.cst instanceof Integer) {
				assertTrue(((Integer)node.cst <= Integer.MAX_VALUE) && ((Integer)node.cst >= Integer.MIN_VALUE));
			}
			else if (node.cst instanceof Float){
				assertTrue(((Float)node.cst <= Float.MAX_VALUE) && ((Float)node.cst >= Float.MIN_VALUE));
			}
			
			else if (node.cst instanceof Long) {
				assertTrue(((Long)node.cst <= Long.MAX_VALUE) && ((Long)node.cst >= Long.MIN_VALUE));
			}
			else { // Double
				assertTrue(((Double)node.cst <= Float.MAX_VALUE) && ((Double)node.cst >= Float.MIN_VALUE));
			}
			
			node = (LdcInsnNode) InstructionFactory.makeLdcInsn(random);
		}
	}
	
	@Test
	public void testMakeVarInsn() {
		VarInsnNode node = (VarInsnNode) InstructionFactory.makeVarInsn(fact, random, 0x15);
		
		int num_locals = fact.localVariables.size();
		
		assertTrue(node.var < num_locals);
	}
}
