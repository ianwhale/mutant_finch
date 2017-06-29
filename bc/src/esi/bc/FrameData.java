package esi.bc;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import esi.bc.util.TypeRep;

/**
 * Class holding frame data for each instruction (insn):
 *   - stack:  			operand stack
 *   - locals: 			local variables array
 *   - popDepth:  		amount of operands to be popped from the stack by the insn
 *   - varsRead:		Set of VarAccess - which variables will be read by the insn, and what is the data type
 *   - varsWritten:		Set of VarAccess - which variables will be written by the insn, and what is the data type
 *
 *  Note: 	There is only one insn in which both varReadType and varWriteType are not null.
 *  		This is the IINC insn, where varReadType == varWriteType == Opcodes.INTEGER
 *  		As with other insn's, only one local variable is effected (its index is stored in varNumber)
 *
 * @author Kfir Wolfson
 * @author Michael Orlov
 */
public class FrameData {

	/**
	 * Prefix used for internal names of uninitialized types.
	 */
	public static final String UNINITIALIZED_PREFIX = "U/";

	/**
	 * popDepth (Amount of operands to be popped from the stack by the insn) per Opcode
	 * statically generated using the code at the bottom of the class
	 * exceptions:
	 *    -  opcode < 0 (debug instructions) should be dealt with without this array.
	 *    -  opcode = MULTIANEWARRAY has a parameterized popDepth: the array dimension.
	 *    -  opcode = INVOKExxx, popDepth depends on the number of method arguments
	 *
	 */
	static final int[] popDepthPerOpcode;

	/**
	 * <code>List</code> of the operand stack slots for execution
	 * frame. This field is never <tt>null</tt>.
	 *
	 * @see AnalyzerAdapter#stack
	 */
	private final List<Object> stack;

	/**
	 * <code>List</code> of the local variable slots for execution
	 * frame. This field is never <tt>null</tt>.
	 *
	 * @see AnalyzerAdapter#locals
	 */
	private final List<Object> locals;

	/**
	 * Amount of operands to be poped from the stack by the insn
	 */
	private final int          popDepth;


	/**
	 * A set of local variable indexes read during instruction execution.
	 */
	private final Set<Integer> varsRead;

	/**
	 * A set of local variable indexes written during instruction execution.
	 */
	private final Set<Integer> varsWritten;


	/////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor copies stack and locals Lists by ref, since these are static Integers or Strings
	 * If they contain a Label (in the case of an uninitialized object) then
	 * it is replaced by a String of the object type, acquired from the "uninitializedTypes" map
	 * in the AnalyzerAdapter, prefixed by a "U"
	 *
	 * @param stack stack, must not be null
	 * @param locals, must not be null
	 * @param uninitializedTypes uninitialized types map
	 * @param insn the instruction prior to which stack and locals hold
	 */
	public FrameData(List<Object> stack, List<Object> locals, Map<Label, String> uninitializedTypes, AbstractInsnNode insn) {
		if (stack == null  ||  locals == null)
			throw new IllegalArgumentException("stack and locals must not be null");

		// Handle operand stack and local variables
		this.stack  = transformFrameList(stack, uninitializedTypes);
		this.locals = transformFrameList(locals, uninitializedTypes);

		// Debug instructions (e.g. LineNumber, FrameInsn, Label) have opcode = -1,
		// and remain with the empty read/written vars and zero popDepth.
		if (insn.getOpcode() >= 0) {
			popDepth = getPopDepth(insn);

			// Set varsRead, varsWritten
			varsRead    = new TreeSet<Integer>();
			varsWritten = new TreeSet<Integer>();
			setVarReadWrite(insn);
		}
		else {
			popDepth    = 0;
			varsRead    = Collections.emptySet();
			varsWritten = Collections.emptySet();
		}
	}

	/**
	 * Creates a pseudo-frame data for formal method parameters.
	 *
	 * Here, we rely on the fact that this frame has no
	 * unassigned variables. Other frames can have such
	 * variables represented as {@link Opcodes#TOP}.
	 *
	 * @param params list of function parameters
	 */
	public FrameData(List<Object> params) {
		stack       = Collections.emptyList();
		locals      = new ArrayList<Object>(params);
		varsRead    = Collections.emptySet();

		varsWritten = new TreeSet<Integer>();
		for (int var = 0;  var < locals.size();  ++var)
			varsWritten.add(var);

		popDepth    = 0;
	}

	/**
	 * Creates a frame data with stack and locals copied
	 * from other frame data. Pop depth is set to 0, and
	 * variable access sets are empty.
	 *
	 * @param source frame data source
	 */
	public FrameData(FrameData source) {
		stack       = source.stack;
		locals      = source.locals;

		popDepth    = 0;
		varsRead    = Collections.emptySet();
		varsWritten = Collections.emptySet();
	}

	/**
	 * Transforms a list of types that can contain labels.
	 *
	 * Package-level access for testing.
	 *
	 * If the list contains a {@link Label} (in case of an uninitialized object),
	 * it is replaced by a String of the object type, acquired from the uninitialized types
	 * map in {@link AnalyzerAdapter}, prefixed by a "U".
	 *
	 * @param frameList non-null source list to copy from
	 * @param uninitializedTypes map used to translate labels to Strings
	 */
	List<Object> transformFrameList(List<Object> frameList, Map<Label, String> uninitializedTypes) {
		List<Object> transformed = new ArrayList<Object>(frameList.size());

		for (Object type: frameList) {
			if (type instanceof Label) {
				String typeString = uninitializedTypes.get(type);
				if (typeString == null)
					throw new RuntimeException("label not found in uninitializedTypes map");

				// Here we implicitly rely on distinctiveness of default hashCode()
				// If this becomes a problem (unlikely), Label->random map can be used.
				// TODO: take care of multiple labels mapping to same uninitialized type
				// (is this possible in actual scenario?)
				transformed.add(UNINITIALIZED_PREFIX + type + "/" + typeString);
			}
			else {
				transformed.add(type);
			}
		}

		return transformed;
	}

	/**
	 * Computes the popdepth of the given instruction, using the precomputed
	 * table.
	 *
	 * {@link Opcodes#MULTIANEWARRAY}, and the four invocation instructions
	 * {@link Opcodes#INVOKEINTERFACE}, {@link Opcodes#INVOKESPECIAL},
	 * {@link Opcodes#INVOKEVIRTUAL} and {@link Opcodes#INVOKESTATIC}
	 * are handled separately.
	 *
	 * @param insn the instruction, should be non-debug (non-negative opcode)
	 * @return its pop depth
	 */
	private int getPopDepth(AbstractInsnNode insn) {
		int opcode   = insn.getOpcode();
		int popDepth = 0;

		// Compute popDepth
		switch (opcode) {

		// popDepth is the dimension ("dims") of the MultiANewArrayInsnNode
		case Opcodes.MULTIANEWARRAY:
			popDepth = ((MultiANewArrayInsnNode) insn).dims;
			break;

		// Method invocation
		case Opcodes.INVOKESPECIAL:
			popDepth = 1 + getArgumentsSize((MethodInsnNode) insn);

			// INVOKESPECIAL -> <init> on U, <init> on "U/...", private, super.xxx()
			String methodName = ((MethodInsnNode) insn).name;
			if ("<init>".equals(methodName)) {
				// Type of instance argument
				Object type = stack.get(stack.size() - popDepth);
				assert Opcodes.UNINITIALIZED_THIS.equals(type)
					|| ((type instanceof String)
						&& ((String) type).startsWith(UNINITIALIZED_PREFIX));

				// There can be more on the stack (typically 1 more for NEW)
				int firstOccurrence = stack.indexOf(type);
				popDepth += (stack.size() - popDepth) - firstOccurrence;
			}
			break;

		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKEDYNAMIC:
			// Non-static methods take an object instance as argument
			popDepth = 1;
		case Opcodes.INVOKESTATIC:
			popDepth += getArgumentsSize((MethodInsnNode) insn);
			break;

		// Field storage instructions
		case Opcodes.PUTFIELD:
			popDepth = 1;
		case Opcodes.PUTSTATIC:
			popDepth += Type.getType(((FieldInsnNode) insn).desc).getSize();
			break;

		// For all other opcodes, use predefined popDepthPerOpcode array
		default:
			popDepth = popDepthPerOpcode[opcode];
		}

		return popDepth;
	}

	/**
	 * Sums up the method argument sizes.
	 */
	private int getArgumentsSize(MethodInsnNode insn) {
		int size = 0;
		for (Type type: Type.getArgumentTypes(insn.desc))
			size += type.getSize();

		return size;
	}

	/**
	 * Sets varsRead and varsWritten fields according to instruction type.
	 */
	private void setVarReadWrite(AbstractInsnNode insn) {
		if (insn.getType() == AbstractInsnNode.IINC_INSN) {
			int var = ((IincInsnNode) insn).var;

			varsRead.add(var);
			varsWritten.add(var);
		}
		else if (insn.getType() == AbstractInsnNode.VAR_INSN) {
			int var = ((VarInsnNode) insn).var;

			switch (insn.getOpcode()) {
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
				// Assuming TOP in the subsequent variable
				varsRead.add(var + 1);
			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.ALOAD:
				varsRead.add(var);
				break;

			case Opcodes.LSTORE:
			case Opcodes.DSTORE:
				// Assuming TOP in the subsequent variable
				varsWritten.add(var + 1);
			case Opcodes.ISTORE:
			case Opcodes.FSTORE:
			case Opcodes.ASTORE:
				varsWritten.add(var);
				break;
			}
		}
	}

	/**
	 * @return stack before this instruction (immutable)
	 */
	public List<Object> getStack() {
		return Collections.unmodifiableList(stack);
	}

	/**
	 * @return local variables before this instruction (immutable)
	 */
	public List<Object> getLocals() {
		return Collections.unmodifiableList(locals);
	}

	/**
	 * @return the popDepth
	 */
	public int getPopDepth() {
		return popDepth;
	}

	/**
	 * @return variables read in this instruction (immutable)
	 */
	public Set<Integer> getVarsRead() {
		return Collections.unmodifiableSet(varsRead);
	}


	/**
	 * @return variables written in this instruction (immutable)
	 */
	public Set<Integer> getVarsWritten() {
		return Collections.unmodifiableSet(varsWritten);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append("stack [")
		   .append(TypeRep.typeListToString(stack))
		   .append("], vars [")
		   .append(TypeRep.typeListToString(locals))
		   .append("], pops ").append(popDepth)
		   .append(", reads ")
		   .append(varsRead)
		   .append(", writes ")
		   .append(varsWritten);

		return buf.toString();
	}


	// Code to generate static field popDepthPerOpcode
	static {
		popDepthPerOpcode = new int[1 << Byte.SIZE];
		for (int opc = 0;  opc < popDepthPerOpcode.length;  ++opc)
			popDepthPerOpcode[opc] = computePopDepth(opc);
	}

	private static int computePopDepth(int opcode) {
		int popDepth;

		switch (opcode) {

		// Pop a and i from stack, and push a[i]
		case Opcodes.IALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
		case Opcodes.LALOAD:
		case Opcodes.FALOAD:
		case Opcodes.DALOAD:
		case Opcodes.AALOAD:
			popDepth = 2;
			break;

		// NEG
		case Opcodes.INEG:
		case Opcodes.FNEG:
			popDepth = 1;
			break;
		case Opcodes.LNEG:
		case Opcodes.DNEG:
			popDepth = 2;
			break;

		// I2[B|C|S]
		// [I|F]2[F/I|L|D]
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
		case Opcodes.I2L:
		case Opcodes.F2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.F2D:
		case Opcodes.F2I:
			popDepth = 1;
			break;

		// [L|D]2[L/D|I|F]
		case Opcodes.D2L:
		case Opcodes.L2D:
		case Opcodes.L2I:
		case Opcodes.D2I:
		case Opcodes.L2F:
		case Opcodes.D2F:
			popDepth = 2;
			break;

		// Pop 1 and store
		case Opcodes.ISTORE:
		case Opcodes.FSTORE:
		case Opcodes.ASTORE:
			popDepth = 1;
			break;

		// Pop 2 and store
		case Opcodes.LSTORE:
		case Opcodes.DSTORE:
			popDepth = 2;
			break;

		// Pop a, i and j=[I|F|A] from stack and set a[i]=j
		case Opcodes.IASTORE:
		case Opcodes.FASTORE:
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			popDepth = 3;
			break;

		// Pop a, i and j=[D|L] from stack and set a[i]=j
		case Opcodes.LASTORE:
		case Opcodes.DASTORE:
			popDepth = 4;
			break;

		// One-word IFs, RETURNs, and MONITORs
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
		case Opcodes.IRETURN:
		case Opcodes.FRETURN:
		case Opcodes.ARETURN:
		case Opcodes.ATHROW:
		case Opcodes.TABLESWITCH:
		case Opcodes.LOOKUPSWITCH:
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			popDepth = 1;
			break;

		// Two-word IFs and RETURNs
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
		case Opcodes.LRETURN:
		case Opcodes.DRETURN:
			popDepth = 2;
			break;

		// POP, DUP and SWAP variants
		case Opcodes.POP:
		case Opcodes.DUP:
			popDepth = 1;
			break;
		case Opcodes.POP2:
		case Opcodes.DUP_X1:
		case Opcodes.DUP2:
		case Opcodes.SWAP:
			popDepth = 2;
			break;
		case Opcodes.DUP_X2:
		case Opcodes.DUP2_X1:
			popDepth = 3;
			break;
		case Opcodes.DUP2_X2:
			popDepth = 4;
			break;

		// Arithmetic and logic
		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.IAND:
		case Opcodes.IOR:
		case Opcodes.IXOR:
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.FADD:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
		case Opcodes.FREM:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
			popDepth = 2;
			break;

		// Arithmetic and logic on L or D
		case Opcodes.LADD:
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
		case Opcodes.LCMP:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			popDepth = 4;
			break;

		// Shifts for L
		case Opcodes.LSHL:
		case Opcodes.LSHR:
		case Opcodes.LUSHR:
			popDepth = 3;
			break;

		// Special ops on references
		case Opcodes.ARRAYLENGTH:
		case Opcodes.INSTANCEOF:
			popDepth = 1;
			break;

		// Fields
		case Opcodes.GETFIELD:
			popDepth = 1;
			break;

		// [A]NEWARRAY and CHECKCAST
		case Opcodes.NEWARRAY:
		case Opcodes.ANEWARRAY:
		case Opcodes.CHECKCAST:
			popDepth = 1;
			break;

		// All other opcodes
		default:
			popDepth = 0;
		}

		return popDepth;
	}

}
