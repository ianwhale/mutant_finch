package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;

import ec.util.MersenneTwisterFast;
import esi.bc.manip.InstructionsMutator;

/**
 * Deletion mutator class.
 * @author Ian Whalen
 *
 */
public class DeletionMutator implements InstructionsMutator {

	private final MersenneTwisterFast random;
	private final float mutProb;
	
	public DeletionMutator(MersenneTwisterFast random, float mutProb) {
		this.random = random;
		this.mutProb = mutProb;
	}
	
	public void mutate(MethodNode node) {
		DeletionMutator.deleteInstruction(node, random.nextInt(node.instructions.size() - 1)); // -1 so we don't mess with RETURN. 
	}
	
	/**
	 * Delete an instruction at a given position.
	 * @param node
	 * @param position
	 */
	public static void deleteInstruction(MethodNode node, int position) {
		node.instructions.remove(node.instructions.get(position));
	}
}
