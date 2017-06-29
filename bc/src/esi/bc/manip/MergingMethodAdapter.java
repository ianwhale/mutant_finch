package esi.bc.manip;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import esi.bc.AnalyzedMethodNode;

/**
 * NOTE: index-incrementing visits in merging method adapter are only
 * for instructions that are present in {@link MethodNode#instructions}.
 *
 * This is important, because the adapter relies on compatibility
 * of instruction indexes.
 *
 * @see AnalyzedMethodNode
 */
public class MergingMethodAdapter extends MethodAdapter {

	// Destination code section to remove (inclusive indexes)
	private final int                start;
	private final int                end;

	// Source instructions list
	private final AbstractInsnNode[] srcInstructions;

	// Real and alternative (empty) visitor
	private final MethodVisitor      realVisitor;
	private final MethodVisitor      altVisitor;

	// Index of currently visited instruction
	private       int                index;

	// Destination section removed
	private       boolean            removed;

	/**
	 * Creates a method adapter that replaces a section given by indexes
	 * with the supplied instructions. The indexes are the same as in
	 * {@link MethodNode#instructions}.
	 *
	 * @param mv method visitor to which calls are delegated
	 * @param start start of section to remove (inclusive)
	 * @param end end of section to remove (inclusive, can be start-1)
	 * @param srcInstructions list of instructions to inject
	 */
	public MergingMethodAdapter(MethodVisitor mv, int start, int end, AbstractInsnNode[] srcInstructions) {
		super(mv);

		this.start = start;
		this.end   = end;
		this.srcInstructions = srcInstructions;

		realVisitor = mv;
		altVisitor  = new EmptyVisitor();

		index   = -1;
		removed = false;
	}

	private void startInstruction() {
		if (index == start) {
			// Start removing code
			assert !removed : "Attempting to remove code section second time";

			if (end != start-1)
				mv = altVisitor;

			removed = true;
		}
	}

	private void endInstruction() {
		if (index == end) {
			if (end != start-1) {
				assert mv == altVisitor  : "Non-empty visitor at end of destination section";
				// Stop removing code
				mv = realVisitor;
			}
			else
				assert mv == realVisitor : "Empty visitor at end of empty destination section";

			// Insert code
			for (AbstractInsnNode insn: srcInstructions)
				// Frames are not visited, since they are now invalid
				if (insn.getType() != AbstractInsnNode.FRAME)
					insn.accept(mv);
		}

		++index;
	}

	@Override
	public void visitCode() {
		super.visitCode();
		endInstruction();
	}

	@Override
	public void visitEnd() {
		startInstruction();

		if (index-1 < start  &&  index != start)
			throw new IndexOutOfBoundsException("Did not reach start of destination section");

		if (index-1 < end)
			throw new IndexOutOfBoundsException("Did not reach end of destination section");

		assert removed           : "Failed to remove destination section";
		assert mv == realVisitor : "Empty visitor after end of destination section";

		super.visitEnd();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		startInstruction();
		super.visitFieldInsn(opcode, owner, name, desc);
		endInstruction();
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		startInstruction();

		// Frames are not visited, since they are now invalid
		// super.visitFrame(type, nLocal, local, nStack, stack);

		endInstruction();
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		startInstruction();
		super.visitIincInsn(var, increment);
		endInstruction();
	}

	@Override
	public void visitInsn(int opcode) {
		startInstruction();
		super.visitInsn(opcode);
		endInstruction();
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		startInstruction();
		super.visitIntInsn(opcode, operand);
		endInstruction();
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		startInstruction();
		super.visitJumpInsn(opcode, label);
		endInstruction();
	}

	@Override
	public void visitLabel(Label label) {
		startInstruction();
		super.visitLabel(label);
		endInstruction();
	}

	@Override
	public void visitLdcInsn(Object cst) {
		startInstruction();
		super.visitLdcInsn(cst);
		endInstruction();
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		startInstruction();
		super.visitLineNumber(line, start);
		endInstruction();
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		startInstruction();
		super.visitLookupSwitchInsn(dflt, keys, labels);
		endInstruction();
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		startInstruction();
		super.visitMethodInsn(opcode, owner, name, desc);
		endInstruction();
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		startInstruction();
		super.visitMultiANewArrayInsn(desc, dims);
		endInstruction();
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt,
			Label[] labels) {
		startInstruction();
		super.visitTableSwitchInsn(min, max, dflt, labels);
		endInstruction();
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		startInstruction();
		super.visitTypeInsn(opcode, type);
		endInstruction();
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		startInstruction();
		super.visitVarInsn(opcode, var);
		endInstruction();
	}

}
