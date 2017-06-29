package esi.bc.manip;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Method adapter that adds a call to static callback method
 * before each non-JRE method call and backward jump.
 *
 * @author Michael Orlov
 */
public class InterrupterMethodAdapter extends MethodAdapter {

	// Prefix of methods in Java standard library
	private static final String JAVA_LIB_PREFIX = "java/";

	private final String callbackClass;
	private final String callbackMethod;
	private final int    callbackArg;

	// Labels seen so far (so reference to them is "backward"
	private final Set<Label> seenLabels;

	public InterrupterMethodAdapter(MethodVisitor mv, String callbackClass, String callbackMethod, int callbackArg) {
		super(mv);

		this.callbackClass  = callbackClass;
		this.callbackMethod = callbackMethod;
		this.callbackArg    = callbackArg;

		seenLabels = new HashSet<Label>();
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);
		seenLabels.add(label);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (isBackward(label))
			insertBlock();

		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (isBackward(dflt)  ||  hasBackward(labels))
			insertBlock();

		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
		if (isBackward(dflt)  ||  hasBackward(labels))
			insertBlock();

		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		// Trust JRE methods not to call back methods in class
		if (! owner.startsWith(JAVA_LIB_PREFIX))
			insertBlock();

		super.visitMethodInsn(opcode, owner, name, desc);
	}

	// Whether a label is at or before the current instruction
	private boolean isBackward(Label label) {
		return seenLabels.contains(label);
	}

	// Whether one of the labels is backward
	private boolean hasBackward(Label[] labels) {
		for (Label label: labels)
			if (isBackward(label))
				return true;

		return false;
	}

	// Insert Thread.sleep(0)
	private void insertBlock() {
		generate(callbackArg);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, callbackClass, callbackMethod, "(I)V");
	}

	// Taken from ConstantsMethodAdapter (tested in ConstantsAdapterTest)
	private void generate(int x) {
		if (x >= -1  &&  x <= 5)
			super.visitInsn(Opcodes.ICONST_0 + x);
		else if (x >= Byte.MIN_VALUE  &&  x <= Byte.MAX_VALUE)
			super.visitIntInsn(Opcodes.BIPUSH, x);
		else if (x >= Short.MIN_VALUE  &&  x <= Short.MAX_VALUE)
			super.visitIntInsn(Opcodes.SIPUSH, x);
		else
			super.visitLdcInsn(x);
	}

}
