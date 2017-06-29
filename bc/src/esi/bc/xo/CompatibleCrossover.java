package esi.bc.xo;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.CodeAccesses;
import esi.bc.flow.CodeSection;
import esi.bc.flow.FrameActions;

/**
 * Compatible bytecode crossover checks.
 *
 * @author Michael Orlov
 */
public class CompatibleCrossover {

	private final CodeAccesses  alphaAccesses;
	private final CodeAccesses	betaAccesses;
	private final TypeVerifier	typeVerifier;

	/**
	 * Constructs a compatible crossover tester.
	 *
	 * @param destNode destination method node
	 * @param srcNode source method node
	 * @param destSection destination (ALPHA) section in corresponding method node
	 * @param srcSection source (BETA) section in corresponding method node
	 * @param typeVerifier types compatibility checker
	 */
	public CompatibleCrossover(AnalyzedMethodNode destNode, AnalyzedMethodNode srcNode, TypeVerifier typeVerifier) {
		alphaAccesses = new CodeAccesses(destNode);
		betaAccesses  = new CodeAccesses(srcNode);
		this.typeVerifier = typeVerifier;
	}

	/**
	 * Checks whether replacing destination section with source section is
	 * compatible with respect to operand stack and local variables.
	 *
	 * @param destSection destination (ALPHA) section in destination method node
	 * @param srcSection source (BETA) section in source method node
	 * @return whether crossover is good
	 */
	public boolean isCompatible(CodeSection destSection, CodeSection srcSection) {
		assert destSection.method.equals(alphaAccesses.getMethod());
		assert srcSection .method.equals(betaAccesses .getMethod());

		FrameActions betaActions = betaAccesses.getSection(srcSection.start, srcSection.end);

		return isStackCompatible(destSection, betaActions)
			&& isLocalsCompatible(destSection, betaActions);
	}

	// For testing purposes
	boolean isStackCompatible(CodeSection destSection, CodeSection srcSection) {
		FrameActions betaActions = betaAccesses.getSection(srcSection.start, srcSection.end);
		return isStackCompatible(destSection, betaActions);
	}

	// For testing purposes
	boolean isLocalsCompatible(CodeSection destSection, CodeSection srcSection) {
		FrameActions betaActions = betaAccesses.getSection(srcSection.start, srcSection.end);
		return isLocalsCompatible(destSection, betaActions);
	}

	/**
	 * @return whether code sections are stack-compatible
	 */
	private boolean isStackCompatible(CodeSection alphaSection, FrameActions betaActions) {
		// Requirement 3(d): Unreachable [end] or [end]+1 of beta
		if (betaActions == null)
			return false;
		// Also check alpha, because otherwise alpha's post-stack is not reliable
		FrameActions alpha = alphaAccesses.getSection(alphaSection.start, alphaSection.end);
		if (alpha == null)
			return false;

		// BETA pops and pushes
		List<Object> betaStackPops    = betaActions.getStackPops();
		List<Object> betaStackPushes  = betaActions.getStackPushes();
		final int popBeta  = betaStackPops.size();
		final int pushBeta = betaStackPushes.size();

		// ALPHA pops and pushes of at least BETA-depth
		List<Object> alphaStackPops   = alpha.getStackPops(popBeta);

		// Requirement 1(a)
		if (alphaStackPops == null)
			return false;

		List<Object> alphaStackPushes = alpha.getStackPushes(popBeta);
		final int popAlpha  = alphaStackPops.size();
		final int pushAlpha = alphaStackPushes.size();

		int delta     = popAlpha - popBeta;
		assert delta >= 0;

		// Implicit requirement in 1(b-c)
		if (pushAlpha - pushBeta != delta)
			return false;

		// Requirement 1(b) [i]
		if (! typeVerifier.isNarrowerThan(alphaStackPops.subList(delta, popAlpha),
										  betaStackPops))
			return false;

		// Requirement 1(b) [ii]
		if (! typeVerifier.isNarrowerThan(betaStackPushes,
										  alphaStackPushes.subList(delta, pushAlpha)))
			return false;

		// Requirement 1(c)
		if (! typeVerifier.isNarrowerThan(alphaStackPops.subList(0, delta),
										  alphaStackPushes.subList(0, delta)))
			return false;

		return true;
	}

	/**
	 * @return whether code sections are local variables-compatible
	 */
	private boolean isLocalsCompatible(CodeSection alphaSection, FrameActions betaActions) {
		// Implicit requirement 3(e)
		FrameActions postAlpha = alphaAccesses.getSection(alphaSection.end+1);
		if (postAlpha == null)
			return false;

		// Requirement 2(a)
		if (! typeVerifier.isNarrowerThan(betaActions.getVarsWritten(), postAlpha.getVarsRead(), false))
			return false;

		// Requirement 3(c): Unreachable [start] of alpha
		FrameActions preAlpha = alphaAccesses.getSection(0, alphaSection.start-1, true);
		if (preAlpha == null)
			return false;

		// Requirement 2(b)
		Map<Integer, Object> postAlphaRead = new TreeMap<Integer, Object>(postAlpha.getVarsRead());
		postAlphaRead.keySet().removeAll(betaActions.getVarsWrittenAlways().keySet());
		if (! typeVerifier.isNarrowerThan(preAlpha.getVarsWrittenAlways(), postAlphaRead, true))
			return false;

		// Requirement 2(c)
		if (! typeVerifier.isNarrowerThan(preAlpha.getVarsWrittenAlways(), betaActions.getVarsRead(), true))
			return false;

		return true;
	}

}
