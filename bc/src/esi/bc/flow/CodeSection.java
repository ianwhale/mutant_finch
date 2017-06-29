package esi.bc.flow;

import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

/**
 * Code section in a method.
 *
 * Just method name and limits, no actual instructions.
 *
 * @author Michael Orlov
 */
public class CodeSection implements Comparable<CodeSection> {
	public final Method method;

	// Inclusive indexes
	public final int    start;
	public final int    end;

	/**
	 * Defines a code section.
	 *
	 * The indexes are inclusive indexes in
	 * {@link MethodNode#instructions}.
	 *
	 * @param method method specification (e.g. [fact,(I)I])
	 * @param start inclusive start index
	 * @param end inclusive end index
	 *   (can be start-1 for empty section before start)
	 */
	public CodeSection(Method method, int start, int end) {
		if (start < 0  ||  start > end+1)
			throw new IndexOutOfBoundsException("Bad instruction indexes " + start + "-" + end);

		this.method = method;
		this.start  = start;
		this.end    = end;
	}

	@Override
	public boolean equals(Object obj) {
		CodeSection other = (CodeSection) obj;

		return method.equals(other.method)
			&& start == other.start
			&& end   == other.end;
	}

	public int size() {
		return end - start + 1;
	}

	@Override
	public String toString() {
		return method + "[" + start + "-" + end + "]";
	}

	/**
	 * NOTE: not {@link #equals(Object)}-consistent.
	 * @see Comparable
	 */
	@Override
	public int compareTo(CodeSection o) {
		return size() - o.size();
	}

}
