package esi.finch.xo;

import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.BranchAnalyzer;
import esi.bc.flow.CodeSection;

/**
 * A finder for crossovers.
 *
 * @author Michael Orlov
 */
public abstract class CrossoverFinder {

	// Alpha and beta branch sections
	protected final BranchAnalyzer alphaBranches;
	protected final BranchAnalyzer betaBranches;

	/**
	 * Representation of destination and source code sections
	 * for crossover.
	 */
	public final class Sections {
		public final CodeSection alpha;
		public final CodeSection beta;

		/**
		 * Creates a pair of code sections.
		 *
		 * @param alpha destination section
		 * @param beta source section
		 */
		public Sections(CodeSection alpha, CodeSection beta) {
			this.alpha = alpha;
			this.beta  = beta;
		}
	}

	public CrossoverFinder(AnalyzedMethodNode alphaMethod, AnalyzedMethodNode betaMethod) {
		alphaBranches = new BranchAnalyzer(alphaMethod, true);
		betaBranches  = new BranchAnalyzer(betaMethod, false);
	}

	/**
	 * Produces a suggestion for crossover.
	 *
	 * @return a crossover suggestion, or <code>null</code>
	 */
	public abstract Sections getSuggestion();

}
