package esi.bc.flow;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.commons.Method;

import esi.bc.AnalyzedMethodNode;
import esi.bc.FrameData;

/**
 * Frames actions computation for a given range
 * that uses control flow.
 *
 * @author Michael Orlov
 */
public class CodeAccesses {

	// private final static Log log = Config.getLogger();

	private final AnalyzedMethodNode method;

	/**
	 * Creates code accesses flow analyzer for
	 * a given method. Can be reused for different
	 * ranges.
	 *
	 * @param method method node
	 */
	public CodeAccesses(AnalyzedMethodNode method) {
		this.method = method;
	}

	/**
	 * Returns combined frames actions for a given code section.
	 *
	 * End of code segment is taken as the last instruction before
	 * the last *RETURN.
	 *
	 * Parameters write is not simulated.
	 *
	 * @see #getSection(int, int, boolean)
	 */
	public FrameActions getSection(int start) {
		// Do not simulate parameters write
		return getSection(start, method.getIndexBeforeReturn());
	}

	/**
	 * Returns combined frames actions for a given code section.
	 *
	 * Parameters write is not simulated.
	 *
	 * @see #getSection(int, int, boolean)
	 */
	public FrameActions getSection(int start, int end) {
		return getSection(start, end, false);
	}

	/**
	 * Returns combined frames actions for a given code section.
	 *
	 * Uses control flow analysis which is executed from the
	 * starting point, until frames actions stabilize for all
	 * relevant ranges.
	 *
	 * @param start first instruction in the range
	 * @param end last instruction in the range (can be start-1 for empty section)
	 * @param useParams whether write of all formal parameters is simulated
	 * @return frames actions, <code>null</code> if [end] or [end]+1 is unreachable from [start]
	 */
	public FrameActions getSection(int start, int end, boolean useParams) {
		if (start < 0  ||  start > end+1  ||  end >= method.instructions.size())
			throw new IllegalArgumentException("Illegal range: " + start + "-" + end);
		if (useParams  &&  start != 0)
			throw new IllegalArgumentException("Parameters write simulation is intended for start=0");

		// log.trace("Computing accesses: " + method.getFullName() + "[" + start + "-" + end + "]"
		//			+ (useParams  ?  "+P"  :  ""));

		// Take care of [end]=[start]-1 case
		if (start-1 == end)
			return useParams ?
					method.getParametersAction()
					: FrameActions.getEmptyAction();

		// Frame state before / after segment, is not null if all instructions are reachable
		if (end == method.instructions.size()-1)
			return null;

		FrameData before = method.getFrameData(start);
		FrameData after  = method.getFrameData(end + 1);

		if (before == null  ||  after == null)
			return null;

		// Per-instruction aggregated actions
		FrameActions[] actions = new FrameActions[method.instructions.size()];

		// Has a transition to [end]+1
		boolean hasEndTransition = false;

		// Per-instruction sets of incoming indexes
		// NOTE: it is not precomputed in AnalyzedMethodNode, since we
		// only want relevant incoming branches.
		List<Set<Integer>> incoming = new ArrayList<Set<Integer>>(actions.length);
		for (int i = 0;  i < actions.length;  ++i)
			incoming.add(new TreeSet<Integer>());

		// BFS-style...
		Queue<Integer> q = new LinkedList<Integer>();
		q.add(start);

		while (! q.isEmpty()) {
			// Index of current node
			int index = q.remove();

			// Current (single) frame action
			FrameActions next = method.getFrameActions(index);
			// Do not follow real instruction with NULL frame actions
			if (next == null  &&  method.instructions.get(index).getOpcode() >= 0)
				continue;

			// Source (already combined) frame actions
			Set<Integer> incomingIndexes = incoming.get(index);
			assert index == start  ||  !incomingIndexes.isEmpty();

			List<FrameActions> first = new ArrayList<FrameActions>(incomingIndexes.size());
			for (int inIndex: incomingIndexes)
				first.add(actions[inIndex]);

			// Possibly simulate parameter writes
			if (useParams  &&  index == start)
				first.add(method.getParametersAction());

			// Combined (sources -> single) frame action
			FrameActions combined = new FrameActions(first, next, actions[index]);

			// Continue traversing flow only if combined actions are fresh
			if (!combined.equalAccesses(actions[index])) {
				/*
				if (log.isTraceEnabled()) {
					if (actions[index] != null)
						log.trace("Updating index " + index + ": "
								+ combined.difference(actions[index]));
					else
						log.trace("Initializing index " + index + ": "
								+ combined.difference(FrameActions.getEmptyAction()));
				}
				*/

				// Update combined actions
				actions[index] = combined;

				// Expand node
				for (int dest: method.getNextIndexes(index)) {
					// Do not add node in [end]->[end]+1 transition
					if (!(index == end  &&  dest == end + 1)) {
						// Add destination to queue
						q.add(dest);

						// Destination should know about this source
						incoming.get(dest).add(index);
					}
					// Only [end]->[end]+1 is "end transition"
					else
						hasEndTransition = true;
				}
			}
		}

		if (! hasEndTransition)
			return null;

		actions[end].setFrameData(before, after);
		return actions[end];
	}

	public Method getMethod() {
		return new Method(method.name, method.desc);
	}

}
