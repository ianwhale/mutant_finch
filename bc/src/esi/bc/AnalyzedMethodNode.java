package esi.bc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import esi.bc.flow.FrameActions;
import esi.bc.util.InstructionRep;

/**
 * A method node that tracks per-instruction frames and
 * local variables using {@link AnalyzerAdapter}. Since
 * {@link AnalyzerAdapter} relies on expanded frames,
 * these need to be present in the class file.
 *
 * @see AnalyzedClassNode#readClass(Class, boolean, boolean)
 * @see <a href="https://jdk.dev.java.net/verifier.html">Type Checking Verifier</a>
 *
 * @author Kfir Wolfson
 * @author Michael Orlov
 */
public class AnalyzedMethodNode extends MethodNode {
	// AnalyzerAdapter which tracks the current frame info
			final AnalyzerAdapter aa;
	private final String fullName;

	// Frame data (one per instruction), elements can be null
	private final List<FrameData>			framesData;

	// Frames actions (one per instruction), elements can be null
	private final ArrayList<FrameActions>	framesActions;

	// Frame action equivalent to method parameters writes
	private 	  FrameActions				parametersAction;

	// Next indexes for each instruction (labelIndexes are only branches via label)
	private final ArrayList<Set<Integer>>	nextIndexes;
	private final ArrayList<Set<Integer>>	nextLabelIndexes;

	public AnalyzedMethodNode(
			final String owner,		// the class containing the method
			final int access,
			final String name,
			final String desc,
			final String signature,
			final String[] exceptions)
	{
		super(access, name, desc, signature, exceptions);
		aa       = new AnalyzerAdapter(owner, access, name, desc, this);
		fullName = owner.replace('/', '.') + "." + name + desc;

		framesData    = new ArrayList<FrameData>();
		framesActions = new ArrayList<FrameActions>();

		nextIndexes      = new ArrayList<Set<Integer>>(0);
		nextLabelIndexes = new ArrayList<Set<Integer>>(0);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	//Overriding methods to run saveFrameData() whenever super.X() adds to "instructions"

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		super.visitVarInsn(opcode, var);
		saveFrameData();


	}

	@Override
	public void visitFrame(
			final int type,
			final int nLocal,
			final Object[] local,
			final int nStack,
			final Object[] stack)
	{
		super.visitFrame(type, nLocal, local, nStack, stack);
		saveFrameData();
	}

	@Override
	public void visitInsn(final int opcode) {
		super.visitInsn(opcode);
		saveFrameData();
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		super.visitIntInsn(opcode, operand);
		saveFrameData();
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		super.visitTypeInsn(opcode, type);
		saveFrameData();
	}

	@Override
	public void visitFieldInsn(
			final int opcode,
			final String owner,
			final String name,
			final String desc)
	{
		super.visitFieldInsn(opcode, owner, name, desc);
		saveFrameData();
	}

	@Override
	public void visitMethodInsn(
			final int opcode,
			final String owner,
			final String name,
			final String desc)
	{
		super.visitMethodInsn(opcode, owner, name, desc);
		saveFrameData();
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		super.visitJumpInsn(opcode, label);
		saveFrameData();
	}

	@Override
	public void visitLabel(final Label label) {
		super.visitLabel(label);
		saveFrameData();
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		super.visitLdcInsn(cst);
		saveFrameData();
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		super.visitIincInsn(var, increment);
		saveFrameData();
	}

	@Override
	public void visitTableSwitchInsn(
			final int min,
			final int max,
			final Label dflt,
			final Label[] labels)
	{
		super.visitTableSwitchInsn(min, max, dflt, labels);
		saveFrameData();
	}

	@Override
	public void visitLookupSwitchInsn(
			final Label dflt,
			final int[] keys,
			final Label[] labels)
	{
		super.visitLookupSwitchInsn(dflt, keys, labels);
		saveFrameData();
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		super.visitMultiANewArrayInsn(desc, dims);
		saveFrameData();
	}


	@Override
	public void visitLineNumber(final int line, final Label start) {
    	super.visitLineNumber(line, start);
        saveFrameData();
    }

	@Override
	public void visitEnd() {
		super.visitEnd();

		// Compute the next indexes for each instruction
		nextIndexes.ensureCapacity(instructions.size());
		nextLabelIndexes.ensureCapacity(instructions.size());
		for (int index = 0;  index < instructions.size();  ++index) {
			nextIndexes     .add(computeNextIndexes(index, false));
			nextLabelIndexes.add(computeNextIndexes(index, true));
		}

		// Make a backward pass copying frame data to pseudo-instructions w/o frame data
		for (int index = framesData.size()-2;  index >= 0;  --index)
			if (framesData.get(index) == null  &&  instructions.get(index).getOpcode() < 0)
				framesData.set(index, new FrameData(framesData.get(index + 1)));

		// Make a backward pass locating LABEL...FRAME constructs and updating frame data
		for (int index = framesData.size()-2;  index >= 0;  --index)
			if (instructions.get(index).getType() == AbstractInsnNode.FRAME) {
				// Frame after the instruction (no vars accesses, and pop depth = 0)
				FrameData afterFrame  = new FrameData(framesData.get(index + 1));

				--index;
				for ( ;  index >= 0  &&  (instructions.get(index).getType() == AbstractInsnNode.LABEL
						||  instructions.get(index).getType() == AbstractInsnNode.LINE);  --index)
					framesData.set(index+1, afterFrame);
				++index;
			}

		// Construct per-instruction frame actions
		framesActions.ensureCapacity(framesData.size());
		for (int index = 0;  index < framesData.size();  ++index) {
			FrameActions action = null;

			FrameData before = framesData.get(index);
			if (before != null) {
				// Choose one of the next indexes (not necessarily subsequent instruction)
				Set<Integer> nextIndexes = getNextIndexes(index);
				if (! nextIndexes.isEmpty()) {
					FrameData after = framesData.get(nextIndexes.iterator().next());

					if (after != null)
						action = new FrameActions(before, after);
				}
			}

			framesActions.add(action);
		}

		// Check that parameters action was constructed
		assert parametersAction != null;
	}

	/**
	 * @param index instruction index
	 * @return frames action of instruction at given index
	 */
	public FrameActions getFrameActions(int index) {
		return framesActions.get(index);
	}

	/**
	 * @param index instruction index
	 * @return frame state before instruction at given index
	 */
	public FrameData getFrameData(int index) {
		return framesData.get(index);
	}

	/**
	 * @return frames action representing parameters write
	 */
	public FrameActions getParametersAction() {
		return parametersAction;
	}

	/**
	 * @return index of last instruction before *RETURN
	 */
	public int getIndexBeforeReturn() {
		// Index of last instruction
		int last = instructions.size() - 1;

		// Index of last instruction before *RETURN
		int end;

		// *RETURN + LABEL (normal situation)
		if (instructions.get(last).getType() == AbstractInsnNode.LABEL
				&& isReturn(instructions.get(last-1)))
			end = last - 2;
		// Just *RETURN (reading with SKIP_DEBUG, or no debug info)
		else if (AnalyzedMethodNode.isReturn(instructions.get(last)))
			end = last - 1;
		// Doesn't end with return
		else
			throw new RuntimeException("Method does not end in *RETURN");

		return end;
	}

	/**
	 * Returns a set of possible destinations of the instruction
	 * at given index.
	 *
	 * Providing index of the last instruction typically
	 * results in an empty set.
	 *
	 * The sets are precomputed during method analysis.
	 * JSR/RET are not handled.
	 *
	 * @param index instruction index
	 * @return set of destinations
	 */
	public Set<Integer> getNextIndexes(int index) {
		return nextIndexes.get(index);
	}

	/**
	 * Returns a set of possible destinations of the instruction
	 * at given index, where only transitions via labels are
	 * considered (thus, next instruction will usually not be
	 * in the set).
	 *
	 * @param index instruction index
	 * @return set of destinations
	 * @see #getNextIndexes(int)
	 */
	public Set<Integer> getNextLabelIndexes(int index) {
		return nextLabelIndexes.get(index);
	}

	/**
	 * Actual computation of next indexes.
	 *
	 * TODO: add exception handler as next index for all instructions
	 *       in exception "try" block (sans block start/end labels)
	 *
	 * @param index instruction index
	 * @param labelsOnly whether only to consider transitions via labels
	 * @return set of destinations
	 */
	private Set<Integer> computeNextIndexes(int index, boolean labelsOnly) {
		// NOTE: InsnList.insexOf() is fast
		AbstractInsnNode insn = instructions.get(index);
		Set<Integer> result;

		// Jump instruction: can be GOTO or IF*
		if (insn.getType() == AbstractInsnNode.JUMP_INSN) {
			JumpInsnNode jumpInsn = (JumpInsnNode) insn;

			// GOTO has single destination
			if (jumpInsn.getOpcode() == Opcodes.GOTO)
				result = Collections.singleton(instructions.indexOf(jumpInsn.label));
			// IF* have next + another destination
			else {
				assert index+1 < instructions.size();

				if (labelsOnly)
					result = Collections.singleton(instructions.indexOf(jumpInsn.label));
				else {
					result = new TreeSet<Integer>();

					result.add(index+1);
					result.add(instructions.indexOf(jumpInsn.label));

					result = Collections.unmodifiableSet(result);
				}
			}
		}
		// Return instruction - no destination
		else if (isReturn(insn)  ||  insn.getOpcode() == Opcodes.ATHROW) {
			result = Collections.emptySet();
		}
		// TABLESWITCH - many destinations are possible
		else if (insn.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
			TableSwitchInsnNode tsInsn = (TableSwitchInsnNode) insn;
			result = new TreeSet<Integer>();

			for (Object labelNode: tsInsn.labels)
				result.add(instructions.indexOf((LabelNode) labelNode));
			result.add(instructions.indexOf(tsInsn.dflt));

			result = Collections.unmodifiableSet(result);
		}
		// LOOKUPSWITCH - many destinations are possible
		else if (insn.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
			// Copy/Paste from TABLESWITCH
			LookupSwitchInsnNode luInsn = (LookupSwitchInsnNode) insn;
			result = new TreeSet<Integer>();

			for (Object labelNode: luInsn.labels)
				result.add(instructions.indexOf((LabelNode) labelNode));
			result.add(instructions.indexOf(luInsn.dflt));

			result = Collections.unmodifiableSet(result);
		}
		// Any other instruction
		else {
			// Last instruction (that is not *RETURN) must be a label
			if (index == instructions.size() - 1) {
				assert (insn.getType() == AbstractInsnNode.LABEL);
				result = Collections.emptySet();
			}
			// Otherwise give next instruction (unless labels only)
			else {
				if (labelsOnly)
					result = Collections.emptySet();
				else
					result = Collections.singleton(index+1);
			}
		}

		return result;
	}

	/**
	 * Checks whether an instruction is one of six
	 * *RETURN variants.
	 * @param insn an instruction
	 * @return whether it is a *RETURN
	 */
	private static boolean isReturn(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
		case Opcodes.RETURN:
		case Opcodes.IRETURN:
		case Opcodes.FRETURN:
		case Opcodes.LRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
			return true;
		default:
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		// Method name, descriptor, and access flags
		buf.append("Method: ").append(getFullName()).append('\n')
		   .append("Access: ").append(access).append("\n\n");

		// Local variables
		for (Object var: localVariables)
			buf.append(InstructionRep.toString((LocalVariableNode) var))
			   .append('\n');

		// Try/catch blocks
		for (Object block: tryCatchBlocks)
			buf.append(InstructionRep.toString((TryCatchBlockNode) block))
			   .append('\n');

		// Parameters pseudo-frame
		buf.append("Params:")
		   .append('\n')
		   .append(parametersAction)
		   .append('\n');

		// Print all instructions and their actions frame
		for (int i = 0;  i < instructions.size();  ++i) {
			// Instruction index
			buf.append(i).append(": ");

			// Instruction representation
			AbstractInsnNode insn = instructions.get(i);
			buf.append(InstructionRep.toString(insn));

			Set<Integer> next = getNextIndexes(i);
			buf.append(" -> ")
			.append(next.isEmpty() ? "stop" : next)
			.append('\n');

			// Per-instruction frame data
			buf.append(framesData.get(i) == null ? "NULL frame data" : framesData.get(i))
			   .append('\n');

			// Per-instruction frames actions (except last)
			buf.append(getFrameActions(i) == null ? "NULL frame actions\n" : getFrameActions(i));

			if (i+1 < instructions.size())
			   buf.append('\n');
		}

		return buf.toString();
	}

	/**
	 * @return a nice representation of full method name (includes class name)
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Saves frame data for the last instruction. If it is not the first
	 * instruction, also saves frames actions.
	 *
	 * NOTE: frame data refers to state *before* the instruction.
	 */
	@SuppressWarnings("unchecked")
	private void saveFrameData() {
		AbstractInsnNode insn = instructions.getLast();

		// stack and locals are null for unreachable instructions
		// or non-real instructions after GOTO
		if (aa.stack == null  ||  aa.locals == null) {
			assert aa.stack == null  &&  aa.locals == null;
			framesData.add(null);
		}
		else {
			// aa.stack and aa.locals need conversion to List<Object>
			framesData.add(new FrameData(aa.stack, aa.locals, aa.uninitializedTypes, insn));
		}

		// Locals before first instruction are the parameters
		if (instructions.size() == 1) {
			assert aa.locals != null;

			FrameData parametersFrame = new FrameData(aa.locals);
			parametersAction = new FrameActions(parametersFrame, parametersFrame);
		}
	}

}
