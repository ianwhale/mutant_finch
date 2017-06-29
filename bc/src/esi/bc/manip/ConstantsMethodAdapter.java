package esi.bc.manip;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * Method adapter that can change a constant of any type
 * to any other constant of arbitrary type.
 *
 * TODO: Possibly use {@link InstructionAdapter}
 *
 * @author Michael Orlov
 */
public class ConstantsMethodAdapter extends MethodAdapter {

	private final ConstantsMutator	mutator;

	public ConstantsMethodAdapter(MethodVisitor mv, ConstantsMutator mutator) {
		super(mv);
		this.mutator = mutator;
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode >= Opcodes.ICONST_M1  &&  opcode <= Opcodes.ICONST_5)
			generate(mutator.mutate(opcode - Opcodes.ICONST_0));
		else if (opcode >= Opcodes.FCONST_0  &&  opcode <= Opcodes.FCONST_2)
			generate(mutator.mutate((float) (opcode - Opcodes.FCONST_0)));
		else if (opcode >= Opcodes.LCONST_0  &&  opcode <= Opcodes.LCONST_1)
			generate(mutator.mutate((long) (opcode - Opcodes.LCONST_0)));
		else if (opcode >= Opcodes.DCONST_0  &&  opcode <= Opcodes.DCONST_1)
			generate(mutator.mutate((double) (opcode - Opcodes.DCONST_0)));
		else
			super.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.BIPUSH  ||  opcode == Opcodes.SIPUSH)
			generate(mutator.mutate(operand));
		else
			super.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		if (cst instanceof Integer)
			generate(mutator.mutate((Integer) cst));
		else if (cst instanceof Float)
			generate(mutator.mutate((Float) cst));
		else if (cst instanceof Long)
			generate(mutator.mutate((Long) cst));
		else if (cst instanceof Double)
			generate(mutator.mutate((Double) cst));
		else if (cst instanceof String)
			super.visitLdcInsn(mutator.mutate((String) cst));
		else if (cst instanceof Type)
			super.visitLdcInsn(mutator.mutate((Type) cst));
		else
			throw new Error("Unexpected LDC operand: " + cst);
	}

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

	private void generate(float x) {
		if (x == 0f  ||  x == 1f  ||  x == 2f)
			super.visitInsn(Opcodes.FCONST_0 + (int) x);
		else
			super.visitLdcInsn(x);
	}

	private void generate(long x) {
		if (x == 0L  ||  x == 1L)
			super.visitInsn(Opcodes.LCONST_0 + (int) x);
		else
			super.visitLdcInsn(x);
	}

	private void generate(double x) {
		if (x == 0.0  ||  x == 1.0)
			super.visitInsn(Opcodes.DCONST_0 + (int) x);
		else
			super.visitLdcInsn(x);
	}

}
