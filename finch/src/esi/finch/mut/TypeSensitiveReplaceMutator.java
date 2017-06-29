package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import ec.util.MersenneTwisterFast;

public class TypeSensitiveReplaceMutator extends ReplaceMutator {
	
	public TypeSensitiveReplaceMutator(MersenneTwisterFast random, float mutProb) {
		super(random, mutProb);
	}
	
	@Override
	public void replaceInstruction(MethodNode method, int position) {
		AbstractInsnNode insn = method.instructions.get(position);
		
		if (validType(insn.getType())) {
			int[][] opcodes = getOpCodes(insn.getType());
			int opcode = nextIntInRanges(opcodes);
			
			AbstractInsnNode new_insn = InstructionFactory.makeInstruction(method, random, opcode, insn.getType());
			method.instructions.set(insn, new_insn);
		}
		else {
			super.replaceInstruction(method, position);
		}
	}
}
