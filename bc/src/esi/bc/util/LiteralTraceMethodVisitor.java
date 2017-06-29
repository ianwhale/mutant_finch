package esi.bc.util;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * A trace visitor that is suitable for tracing a single instruction.
 *
 * @author Michael Orlov
 */
public class LiteralTraceMethodVisitor extends TraceMethodVisitor {

	/**
	 * Creates a trace visitor with no call delegation.
	 */
	public LiteralTraceMethodVisitor() {
	}

	/**
	 * Creates a trace visitor with call delegation.
	 *
	 * @param mv method visitor to which calls are delegated.
	 */
	public LiteralTraceMethodVisitor(MethodVisitor mv) {
		super(mv);
	}

	/**
	 * Appends a label using its hash value. This is suitable
	 * for using the trace visitor on single instructions,
	 * since default behavior of {@link TraceMethodVisitor}
	 * results in <code>L0</code> labels.
	 *
	 * @see org.objectweb.asm.util.TraceMethodVisitor#appendLabel(org.objectweb.asm.Label)
	 */
	@Override
	protected void appendLabel(Label l) {
		buf.append(l + ":");
	}

}
