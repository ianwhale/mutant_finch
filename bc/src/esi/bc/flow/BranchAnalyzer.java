package esi.bc.flow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.objectweb.asm.commons.Method;

import esi.bc.AnalyzedMethodNode;

/**
 * Code sections without outgoing / without incoming branches.
 *
 * @author Michael Orlov
 */
public class BranchAnalyzer implements Iterable<CodeSection> {

	private final AnalyzedMethodNode	methodNode;
	private final Method				method;

	private final int[]	offsets;
	private final int[]	backOffsets;

	/**
	 * Creates a branch analyzer.
	 *
	 * If the boolean parameter is false, sections without outgoing branches
	 * are considered, otherwise it's sections without incoming branches.
	 *
	 * @param methodNode analyzed method node
	 * @param incoming whether to consider incoming branches instead of outgoing
	 */
	public BranchAnalyzer(AnalyzedMethodNode methodNode, boolean incoming) {
		this.methodNode = methodNode;
		this.method     = new Method(methodNode.name, methodNode.desc);

		// Allocate forward and back offsets, fill with default (index->index)
		offsets     = new int[methodNode.getIndexBeforeReturn() + 1];
		for (int index = 0;  index < offsets.length;  ++index)
			offsets[index] = index;
		backOffsets = offsets.clone();

		// Fill initial forward / back offsets
		for (int index = 0;  index < offsets.length;  ++index) {
			for (int branch: methodNode.getNextLabelIndexes(index)) {
				int from = incoming ? branch : index;
				int to   = incoming ? index  : branch;

				offsets    [from] = Math.max(offsets[from],     to);
				backOffsets[from] = Math.min(backOffsets[from], to);
			}
		}

		// Aggregate branches
		aggregateForwardIndexes(offsets);
		aggregateBackwardIndexes(backOffsets);
	}

	/**
	 * Performs forward flow aggregation. After the function
	 * completes, each offsets[i] contains minimal offset >=i
	 * such that no cell between i and offsets[i] contains
	 * value > offsets[i].
	 *
	 * Linear running time.
	 * Package-level access for testing purposes.
	 *
	 * @param offsets next forward offsets or -1's
	 */
	static void aggregateForwardIndexes(int[] offsets) {
		for (int index = 0;  index < offsets.length;  ++index) {
			aggregateIndexes(index, offsets, 1);
			index = offsets[index];
		}
	}

	/**
	 * Similar to {@link #aggregateForwardIndexes(int[])},
	 * but offsets should be backward.
	 *
	 * @param offsets next backward offsets or -1's
	 */
	static void aggregateBackwardIndexes(int[] offsets) {
		for (int index = offsets.length-1;  index >= 0;  --index) {
			aggregateIndexes(index, offsets, -1);
			index = offsets[index];
		}
	}

	private static void aggregateIndexes(int index, int[] offsets, int inc) {
		// can put -1 if no next index
		int nextIndex = offsets[index];

		if (nextIndex >= 0) {
			assert index*inc <= nextIndex*inc;

			// traverse inner indexes until exit from inner region
			int runner = index + inc;
			while (runner*inc <= nextIndex*inc) {
				aggregateIndexes(runner, offsets, inc);
				runner = offsets[runner] + inc;
			}

			// last runner just prior to increment
			offsets[index] = runner - inc;
		}
		else
			offsets[index] = index;
	}


	/**
	 * Extends the end in [start,end] minimal steps further.
	 *
	 * Linear running time in length of extension.
	 * Package-level access for testing purposes.
	 *
	 * @param start range start, inclusive
	 * @param end range end, inclusive (can be start-1)
	 * @param offsets offsets that were aggregated with
	 *   {@link CodeAccesses#aggregateForwardIndexes(int[])}
	 * @param backOffsets back offsets that were aggregated with
	 *   {@link CodeAccesses#aggregateForwardIndexes(int[])}
	 * @return new end, or -1
	 */
	static int extendRange(int start, int end, int[] offsets, int[] backOffsets) {
		assert 0 <= start  &&  start <= end+1  &&  end < offsets.length;
		if (end+1 < offsets.length) {
			int newEnd = offsets[end+1];
			for (int i = end+1;  i <= newEnd;  ++i)
				if (backOffsets[i] < start)
					return -1;

			return newEnd;
		}
		else
			return -1;
	}

	/**
	 * Iteration over legal code sections.
	 */
	private class CodeSectionIterator implements Iterator<CodeSection> {
		private int start =  0;
		private int end   = -1;

		private boolean ready = true;
		private boolean done  = (start >= offsets.length);

		@Override
		public boolean hasNext() {
			if (!ready  &&  !done) {
				// -1 if [end] is at limit, or backjump before [start]
				end = extendRange(start, end, offsets, backOffsets);
				if (end == -1)
					end = start++;

				done  = (start >= offsets.length);
				ready = true;
			}

			return ready;
		}

		@Override
		public CodeSection next() {
			if (! hasNext())
				throw new NoSuchElementException();

			ready = false;
			return new CodeSection(method, start, end);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Pure forward iterator");
		}
	}

	/**
	 * @return an iterator over code sections without
	 *  outgoing / incoming branches
	 */
	@Override
	public Iterator<CodeSection> iterator() {
		return new CodeSectionIterator();
	}

	/**
	 * @return all legal sections, sorted by size
	 */
	public Map<Integer, List<CodeSection>> getSortedSections() {
		Map<Integer, List<CodeSection>> map = new TreeMap<Integer, List<CodeSection>>();

		for (CodeSection section: this) {
			int key = section.size();
			assert key >= 0;

			List<CodeSection> list = map.get(key);

			if (list == null) {
				list = new ArrayList<CodeSection>();
				map.put(key, list);
			}

			list.add(section);
		}

		return map;
	}

	/**
	 * Counts number of outgoing branches in given section.
	 * The section is assumed to have been produced by this branch analyzer.
	 *
	 * Only outgoing branches are counted, regardless of this branch
	 * analyzer type.
	 *
	 * @param section code section
	 * @return number of outgoing branches
	 */
	public int getBranchesCount(CodeSection section) {
		assert method.equals(section.method);

		int branches = 0;

		// Count outgoing branches regardless of branch analyzer type
		for (int index = section.start;  index <= section.end;  ++index)
			branches += methodNode.getNextLabelIndexes(index).size();

		return branches;
	}

	public String getName() {
		return methodNode.getFullName();
	}

}
