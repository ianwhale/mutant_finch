package esi.bc.flow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.Opcodes;

import esi.bc.FrameData;
import esi.bc.util.TypeRep;

/**
 * Represents the frame actions taken by one or more JVM instructions.
 *
 * Instances of this class are immutable.
 *
 * @author Kfir Wolfson
 * @author Michael Orlov
 */
public class FrameActions {
	// Bogus variable (TOP w/o matching J or D)
	public static  final Integer BOGUS = -1;

	private static final FrameActions emptyAction;

	private       FrameData		frameBefore;
	private       FrameData		frameAfter;
	private final int			stackDelta;

			// Package-level access for testing
			final int       	popDepth;

	// These sets are immutable!
	private final Set<Integer>	varsRead;
	private final Set<Integer>	varsWritten;
	private final Set<Integer>	varsWrittenAlways;

	static {
		FrameData emptyFrame = new FrameData(Collections.emptyList());
		emptyAction = new FrameActions(emptyFrame, emptyFrame);
	}

	/**
	 * Creates a frame actions object that corresponds to a single
	 * instruction.
	 *
	 * It is assumed that the "before" frame state corresponds
	 * to the current instruction, for variables access data.
	 *
	 * @param before the frame state before instruction execution
	 *        (comes from current instruction)
	 * @param after the frame state after instruction execution
	 *        (comes from subsequent instruction)
	 */
	public FrameActions(FrameData before, FrameData after) {
		// Initialize here, since it's used in toString(), and doesn't cost anything
		frameBefore = before;
		frameAfter  = after;

		stackDelta  = after.getStack().size() - before.getStack().size();

		// "before" comes from the current instruction
		popDepth          = before.getPopDepth();
		varsRead          = before.getVarsRead();
		varsWritten       = before.getVarsWritten();
		varsWrittenAlways = varsWritten;
	}

	/**
	 * Creates a frames actions object from successive frames actions,
	 * where the first frames actions can have several possibilities.
	 *
	 * This constructor is usually called when next frames
	 * actions corresponds to a single instruction, however,
	 * this is not strictly necessary.
	 *
	 * Before/after frame data are not initialized, use
	 * {@link #setFrameData(FrameData, FrameData)}.
	 *
	 * @param sources possible first frames actions, can be empty
	 * @param next successive frames actions
	 * @param formerNext former successive frames actions (can be <code>null</code>)
	 */
	public FrameActions(List<FrameActions> sources, FrameActions next, FrameActions formerNext) {
		// NOTE: frameBefore and frameAfter are not initialized
		// (Otherwise choosing the "before" frame data is problematic)

		// If there are no sources, just copy everything from "next"
		if (sources.isEmpty()) {
			stackDelta        = next.stackDelta;
			popDepth          = next.popDepth;
			varsRead          = next.varsRead;
			varsWritten       = next.varsWritten;
			varsWrittenAlways = next.varsWrittenAlways;
		}
		else {
			// Handle stack and pop depth
			stackDelta  = sources.get(0).stackDelta + next.stackDelta;

			int popDepth = -1;
			for (FrameActions first: sources) {
				// Note: stacks difference is always the same
				popDepth = Math.max(popDepth, Math.max(first.popDepth, next.popDepth - first.stackDelta));
			}

			assert popDepth >= 0;
			this.popDepth = popDepth;

			// Handle variables

			// read?    <- union of vars read? in first frames
			// written! <- intersection of vars written! in first frames
			varsRead          = new TreeSet<Integer>(sources.get(0).varsRead);
			varsWrittenAlways = new TreeSet<Integer>(sources.get(0).varsWrittenAlways);

			boolean skip = true;
			for (FrameActions first: sources) {
				if (!skip) {
					varsRead.addAll(first.varsRead);
					varsWrittenAlways.retainAll(first.varsWrittenAlways);
				}
				else
					skip = false;
			}

			// written! <- additional intersection of vars written! in former next
			if (formerNext != null)
				varsWrittenAlways.retainAll(formerNext.varsWrittenAlways);

			// read? <- read? + ((vars read? in next frame) - (intersection of vars written! in first frames))
			Set<Integer> readNext = new TreeSet<Integer>(next.varsRead);
			readNext.removeAll(varsWrittenAlways);
			varsRead.addAll(readNext);

			// written! <- written! + (vars written! in next frame))
			varsWrittenAlways.addAll(next.varsWrittenAlways);

			// writes <- vars written in next frame
			// writes <- writes + ((vars written in any first frame) -? writes)
			// (thus order doesn't matter)
			varsWritten = new TreeSet<Integer>(next.varsWritten);
			for (FrameActions first: sources)
				varsWritten.addAll(first.varsWritten);
		}
	}

	/**
	 * Checks whether other actions have same reads and writes,
	 * and same pop depth.
	 *
	 * Should be called with actions for equal before/after frames.
	 *
	 * @param other another frames actions, can be null
	 * @return true if same reads and writes
	 */
	public boolean equalAccesses(FrameActions other) {
		if (other == null)
			return false;

		assert stackDelta == other.stackDelta;

		return popDepth == other.popDepth
			&& varsRead         .equals(other.varsRead)
			&& varsWritten      .equals(other.varsWritten)
			&& varsWrittenAlways.equals(other.varsWrittenAlways);
	}

	/**
	 * @return the stack pops (up to pop depth)
	 */
	public List<Object> getStackPops() {
		List<Object> beforeStack = frameBefore.getStack();
		return beforeStack.subList(beforeStack.size() - popDepth, beforeStack.size());
	}

	/**
	 * @return the stack pushes (up to pop depth)
	 */
	public List<Object> getStackPushes() {
		List<Object> beforeStack = frameBefore.getStack();
		List<Object> afterStack  = frameAfter.getStack();

		return afterStack.subList(beforeStack.size() - popDepth, afterStack.size());
	}

	/**
	 * Returns stack pops that are at least the given size,
	 * and also at least the pop depth. Null is returned if
	 * required pop depth is greater than "before" stack.
	 *
	 * @param minPopDepth minimal pop depth
	 * @return the stack pops, or null
	 */
	public List<Object> getStackPops(int minPopDepth) {
		List<Object> beforeStack = frameBefore.getStack();
		if (minPopDepth > beforeStack.size())
			return null;

		return beforeStack.subList(beforeStack.size() - Math.max(popDepth, minPopDepth), beforeStack.size());
	}

	/**
	 * Returns stack pushes that correspond to pops of at least
	 * the given size that are also at least the pop depth.
	 * Null is returned if required pop depth is greater than
	 * "before" stack.
	 *
	 * @param minPopDepth minimal pop depth
	 * @return the stack pushes, or null
	 */
	public List<Object> getStackPushes(int minPopDepth) {
		List<Object> beforeStack = frameBefore.getStack();
		if (minPopDepth > beforeStack.size())
			return null;

		List<Object> afterStack  = frameAfter.getStack();

		return afterStack.subList(beforeStack.size() - Math.max(popDepth, minPopDepth), afterStack.size());
	}

	/**
	 * Returns the variables that <em>can</em> be read
	 * in this action, if written before this action.
	 *
	 * This method is expensive and should be cached.
	 *
	 * @return map of read variables to types
	 */
	public Map<Integer, Object> getVarsRead() {
		Map<Integer, Object> reads = varsToMap(varsRead, frameBefore.getLocals());
		assert !reads.containsValue(BOGUS);

		return reads;
	}

	/**
	 * Returns the variables that <em>can</em> be written
	 * in this action.
	 *
	 * This method is expensive and should be cached.
	 *
	 * @return map of written variables to types
	 */
	public Map<Integer, Object> getVarsWritten() {
		return varsToMap(varsWritten, frameAfter.getLocals());
	}

	/**
	 * Returns the variables that <em>will</em> be written
	 * in this action.
	 *
	 * This method is expensive and should be cached.
	 *
	 * @return map of written variables to types
	 */
	public Map<Integer, Object> getVarsWrittenAlways() {
		return varsToMap(varsWrittenAlways, frameAfter.getLocals());
	}

	// Helper for variable getters
	private static Map<Integer, Object> varsToMap(Set<Integer> vars, List<Object> locals) {
		Map<Integer, Object> map = new TreeMap<Integer, Object>();

		for (int var: vars) {
			if (var < locals.size())
				map.put(var, locals.get(var));
			else
				// Should happen with varsWritten only
				map.put(var, Opcodes.TOP);
		}

		// Go over mappings in sorted order
		Map.Entry<Integer, Object> last = null;
		for (Map.Entry<Integer, Object> entry: map.entrySet()) {
			if (Opcodes.TOP.equals(entry.getValue())
				&&  !(last != null
					  &&  last.getKey() == entry.getKey()-1
					  &&  (Opcodes.LONG.equals(last.getValue())
						   ||  Opcodes.DOUBLE.equals(last.getValue()))))
				entry.setValue(BOGUS);

			last = entry;
		}

		return map;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		List<Object>         stackPops         = getStackPops();
		List<Object>         stackPushes       = getStackPushes();
		Map<Integer, Object> varsRead          = getVarsRead();
		Map<Integer, Object> varsWritten       = getVarsWritten();
		Map<Integer, Object> varsWrittenAlways = getVarsWrittenAlways();

		if (! stackPops.isEmpty())
			buf.append("stack pops:    ")
			   .append(TypeRep.typeListToString(stackPops))
			   .append('\n');

		if (! stackPushes.isEmpty())
			buf.append("stack pushes:  ")
			   .append(TypeRep.typeListToString(stackPushes))
			   .append('\n');

		if (! varsRead.isEmpty())
			buf.append("vars read:     ")
			   .append(TypeRep.typeMapToString(varsRead))
			   .append('\n');

		if (! varsWrittenAlways.isEmpty())
			buf.append("vars written!: ")
			   .append(TypeRep.typeMapToString(varsWrittenAlways))
			   .append('\n');

		if (! varsWritten.equals(varsWrittenAlways))
			buf.append("vars written:  ")
			   .append(TypeRep.typeMapToString(varsWritten))
			   .append('\n');

		if (buf.length() == 0)
			buf.append("Empty frame action")
			   .append('\n');

		return buf.toString();
	}

	/**
	 * Returns a succinct representation of difference in pop depth,
	 * read and written variables w.r.t. given frame actions.
	 *
	 * E.g., <tt>P+2 R+[3] W+[1, 3] W!-[2]</tt>.
	 *
	 * Before and after frame data needs not be initialized, and this
	 * method can be used  during control flow analysis.
	 *
	 * @param prev previous version of frame actions
	 * @return a succinct representation of the difference.
	 */
	public String difference(FrameActions prev) {
		StringBuilder buf = new StringBuilder();

		int popDiff = popDepth - prev.popDepth;
		CharSequence readDiff          = setDifference(prev.varsRead,          varsRead,          false);
		CharSequence writtenDiff       = setDifference(prev.varsWritten,       varsWritten,       false);
		CharSequence writtenAlwaysDiff = setDifference(prev.varsWrittenAlways, varsWrittenAlways, true);

		assert popDiff >= 0;
		if (popDiff != 0)
			buf.append("P+").append(popDiff).append(' ');

		if (readDiff.length() != 0)
			buf.append('R').append(readDiff).append(' ');
		if (writtenDiff.length() != 0)
			buf.append('W').append(writtenDiff).append(' ');
		if (writtenAlwaysDiff.length() != 0)
			buf.append("W!").append(writtenAlwaysDiff).append(' ');

		if (buf.length() != 0)
			buf.deleteCharAt(buf.length()-1);

		return buf.toString();
	}

	// Returns set difference formatted as -[a, b, c]+[d, e, f]
	private String setDifference(Set<?> before, Set<?> after, boolean canShrink) {
		Set<?> beforeCopy = new TreeSet<Object>(before);
		Set<?> afterCopy  = new TreeSet<Object>(after);

		beforeCopy.removeAll(after);
		afterCopy.removeAll(before);

		assert canShrink  ||  beforeCopy.isEmpty();
		return (beforeCopy.isEmpty()  ?  ""  :  ("-" + beforeCopy))
			 + (afterCopy.isEmpty()   ?  ""  :  ("+" + afterCopy));
	}

	/**
	 * Returns an empty action with empty before/after frame data,
	 * zero pop depth, and empty variables access sets.
	 *
	 * @return empty frame action
	 */
	public static FrameActions getEmptyAction() {
		return emptyAction;
	}

	/**
	 * Sets "before" and "after" frame data. Should be called
	 * on the final combined frame actions.
	 *
	 * @param before frame state before the first instruction in the segment
	 * @param after frame state before the first instruction after the segment
	 */
	public void setFrameData(FrameData before, FrameData after) {
		assert after.getStack().size() - before.getStack().size() == stackDelta;
		assert frameBefore == null  ||  frameBefore == before;
		assert frameAfter  == null  ||  frameAfter.getStack().size() == after.getStack().size();

		frameBefore = before;
		frameAfter  = after;
	}

}
