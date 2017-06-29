package esi.bc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * Helper class for converting instruction-level primitives
 * to strings.
 *
 * @author Michael Orlov
 */
public class InstructionRep {

	public static String toString(AbstractInsnNode insn) {
		TraceMethodVisitor mv = new LiteralTraceMethodVisitor();

		insn.accept(mv);

		return toString(mv);
	}

	public static String toString(LocalVariableNode var) {
		TraceMethodVisitor mv = new LiteralTraceMethodVisitor();

		var.accept(mv);

		return toString(mv);
	}

	public static String toString(TryCatchBlockNode block) {
		TraceMethodVisitor mv = new LiteralTraceMethodVisitor();

		block.accept(mv);

		return toString(mv);
	}

	private static String toString(TraceMethodVisitor mv) {
		StringWriter rep = new StringWriter();
		PrintWriter  out = new PrintWriter(rep);

		mv.print(out);
		out.flush();

		return rep.toString().trim();
	}

}
