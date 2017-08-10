package esi.finch.mut;

import org.objectweb.asm.commons.Method;
import esi.bc.xo.CompatibleCrossover;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.CodeSection;

/**
 * For verifying mutations. 
 * Uses similar methods to finding a crossover section.
 */
public class MutationVerifier {
	
	private CodeSection originalSection;
	private CodeSection mutantSection;
	private CompatibleCrossover xo;
	
	/**
	 * Uses the entire method to determine if the mutation is valid.
	 * @param original
	 * @param mutant
	 */
	public MutationVerifier(AnalyzedMethodNode original, AnalyzedMethodNode mutant,
			CompatibleCrossover xo) {
		this.xo = xo;
		setupCodeSections(original, mutant,
				0, original.instructions.size() - 1,
				0, mutant.instructions.size() - 1);
	}
	
	/**
	 * Lets the user provide what part of the methods to use.
	 * @param original
	 * @param mutant
	 * @param original_begin
	 * @param original_end
	 * @param mutant_begin
	 * @param mutant_end
	 */
	public MutationVerifier(AnalyzedMethodNode original, AnalyzedMethodNode mutant,
			CompatibleCrossover xo, int original_begin, int original_end, 
									int mutant_begin, int mutant_end) {
		this.xo = xo;
		setupCodeSections(original, mutant, original_begin, original_end, mutant_begin, mutant_end);
	}
	
	/**
	 * Initialize the code sections. 
	 * @param original
	 * @param mutant
	 * @param original_begin
	 * @param original_end
	 * @param mutant_begin
	 * @param mutant_end
	 */
	private void setupCodeSections(AnalyzedMethodNode original, AnalyzedMethodNode mutant,
			int original_begin, int original_end, int mutant_begin, int mutant_end) {
		
		originalSection = new CodeSection(new Method(original.name, original.desc), 
										 original_begin, original_end);
		mutantSection = new CodeSection(new Method(mutant.name, mutant.desc), 
				 mutant_begin, mutant_end);
	}
	
	public boolean isValidMutation() {
		return xo.isCompatible(originalSection, mutantSection);
	}
}
