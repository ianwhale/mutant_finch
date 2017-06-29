package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import ec.util.MersenneTwisterFast;
import esi.bc.manip.InstructionsMutator;

/**
 * @see also {@link TypeSensitiveReplaceMutator}
 *
 */
public class ReplaceMutator extends MaterialIntroducer implements InstructionsMutator {
	
	public ReplaceMutator(MersenneTwisterFast random, float mutProb) {
		super(random, mutProb);
	}
	
	/**
	 * Replace an instruction at a random location in the method.
	 * 
	 * @param method
	 */
	public void mutate(MethodNode method) {
		if (random.nextFloat() < mutProb) {
			int size = method.instructions.size() - 1; // -1 so we don't mess with RETURN. 
			int position = random.nextInt(size);
			AbstractInsnNode node = method.instructions.get(position);
			
			// Continue while the node selected is not a "fake" instruction, i.e. label, line number reference, etc. 
			while(position < size - 1 && node.getOpcode() < 0) {
				node = method.instructions.get(++position);
			}
			
			replaceInstruction(method, position);
		}
	}
	
	/**
	 * Replace an instruction at the given location with a random instruction.
	 * 
	 * @param method
	 * @param position
	 */
	public void replaceInstruction(MethodNode method, int position) {
		method.instructions.set(method.instructions.get(position), getRandomInsnNode(method));
	}
}
