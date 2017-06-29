package esi.bc.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InstructionRepTest {

	@Test
	public void toStringAbstractInsn() {
		AbstractInsnNode insn = new LdcInsnNode("str");
		String           rep  = InstructionRep.toString(insn);

		assertEquals("LDC \"str\"", rep);
	}

	@Test
	public void toStringJumpInsn() {
		Label            label = new Label();
		AbstractInsnNode insn  = new JumpInsnNode(Opcodes.GOTO, new LabelNode(label));
		String           rep   = InstructionRep.toString(insn);

		assertEquals("GOTO " + label + ":", rep);
	}

}
