package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;
import ec.util.MersenneTwisterFast;
import esi.bc.manip.InstructionsMutator;
import org.objectweb.asm.tree.AbstractInsnNode;

public class CopyMutator implements InstructionsMutator {
	private final MersenneTwisterFast random;
	private final float mutProb;
	
	public CopyMutator(MersenneTwisterFast random, float mutProb) {
		this.random = random;
		this.mutProb = mutProb;
	}
	
	public void mutate(MethodNode node) {
		CopyMutator.copyInstruction(node, 
				random.nextInt(node.instructions.size() - 1), 
				random.nextInt(node.instructions.size() - 1)); // -1 so we don't mess with RETURN. 
	}
	
	/**
	 * Copy a the instruction at origin and insert it after destination. 
	 * @param node
	 * @param origin
	 * @param destination
	 */
	public static void copyInstruction(MethodNode node, int origin, int destination) {
		AbstractInsnNode insn = node.instructions.get(origin);
		insn = insn.clone(Helpers.cloneLabels(node));
		node.instructions.insert(node.instructions.get(destination), insn);
	}
}
