package esi.bc.manip;

import org.objectweb.asm.tree.MethodNode;

/**
 * Interface for dealing with general instruction mutator type. 
 * @author Ian Whalen
 *
 */
public interface InstructionsMutator {

	/**
	 * All instruction mutators have a mutate method that accepts an initialized {@link MethodNode}.
	 * @param node initialized {@link MethodNode}
	 */
	public void mutate(MethodNode node);
	
}
