package esi.finch.mut;

import org.objectweb.asm.tree.MethodNode;
import ec.util.MersenneTwisterFast;
import esi.bc.manip.InstructionsMutator;

public class InsertMutator extends MaterialIntroducer implements InstructionsMutator {
	
	public InsertMutator(MersenneTwisterFast random, float mutProb) {
		super(random, mutProb);
	}
	
	/**
	 * Add an instruction at a random location in the method.
	 * 
	 * @param method
	 */
	public void mutate(MethodNode method) {
		if (random.nextFloat() < mutProb) {
			newInstruction(method, random.nextInt(method.instructions.size() - 1)); // -1 so we don't mess with RETURN. 
		}
	}
	
	/**
	 * Add a random instruction at the given location in the method. 
	 * 
	 * @param method
	 * @param position
	 */
	public void newInstruction(MethodNode method, int position) {
		method.instructions.insert(method.instructions.get(position), getRandomInsnNode(method));
	}
}
