package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;
import ec.util.MersenneTwisterFast;
import esi.bc.manip.InstructionsMutator;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MoveMutator implements InstructionsMutator {
	private final MersenneTwisterFast random;
	private final float mutProb;
	
	public MoveMutator(MersenneTwisterFast random, float mutProb) {
		this.random = random;
		this.mutProb = mutProb;
	}
	
	/**
	 * Move a random instruction to a new, random location in the method. 
	 * 
	 * @param node
	 */
	public void mutate(MethodNode node) {
		int origin = getValidIndex(node);
		int destination = getValidIndex(node);
		
		// Make sure we actually move the instruction.
		while (destination != origin) {
			destination = getValidIndex(node);
		}
		
		CopyMutator.copyInstruction(node, 
				origin, 
				destination);
	}
	
	/**
	 * Get the index of a valid instruction. 
	 * Valid here means the opcode is at least 0. 
	 * Invalid instructions come from things like line number labels in the bytecode (I think). 
	 * @param node
	 * @return idx 
	 */
	public int getValidIndex(MethodNode node) {
		int size = node.instructions.size() - 1; // -1 so we don't mess with RETURN. 
		int idx = random.nextInt(size);
		AbstractInsnNode insn = node.instructions.get(idx);
		
		while (insn.getOpcode() < 0) {
			idx = random.nextInt(size);
			insn = node.instructions.get(idx);
		}
		
		return idx;
	}
	
	/**
	 * Moves the instruction at "origin" to the position after "destination". 
	 * @param node
	 * @param origin
	 * @param destination
	 */
	public static void moveInstruction(MethodNode node, int origin, int destination) {
		AbstractInsnNode insn = node.instructions.get(origin);
		node.instructions.remove(insn);
		node.instructions.insert(node.instructions.get(destination), insn);
	}
}

