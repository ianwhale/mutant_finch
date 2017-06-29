package esi.bc.manip;

import org.apache.commons.logging.Log;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

import esi.util.Config;

/**
 * Method adapter that eliminates dead code and other
 * unnecessary code, like [GOTO x; LABEL x].
 *
 * Based on ch7/sec2/RemoveDeadCodeAdapter.java.
 *
 * @author Michael Orlov
 */
public class EliminatingMethodAdapter extends MethodAdapter {

	private static final Log log = Config.getLogger();

	// Owner class name
	private final String        className;

	// Method node into which the method is read
	private final MethodNode    mn;

	// Next visitor which method node accepts after DCE
	private final MethodVisitor next;

	// Analyzer that does simple analysis of code in method node
	private final Analyzer      an;

	public EliminatingMethodAdapter(MethodVisitor mv, String className,
			int access, String name, String desc,
			String signature, String[] exceptions) {
		super(new MethodNode(access, name, desc, signature, exceptions));

		this.className = className;
		mn   = (MethodNode) this.mv;
		next = mv;
		an   = new Analyzer(new BasicInterpreter());
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		try {
			// Analyze the complete method node
			an.analyze(className, mn);

			// Frame is null iff instruction is unreachable
			Frame[]            frames = an.getFrames();
			AbstractInsnNode[] insns  = mn.instructions.toArray();
			assert frames.length == insns.length;

			// Remove unreachable instructions
			for (int i = 0;  i < insns.length;  ++i) {
				// Do not remove unreachable labels
				// (they do not get translated into code anyway)
				if (frames[i] == null  &&  insns[i].getType() != AbstractInsnNode.LABEL) {
					assert insns[i].getType() < 0  ||  insns[i].getOpcode() == Opcodes.NOP;
					mn.instructions.remove(insns[i]);
				}
			}

			int removedInsns = insns.length - mn.instructions.size();

			// Remove [ GOTO x; LABEL x ] structures
			// This requires frames recomputation!
			if (removedInsns != 0)
				insns = mn.instructions.toArray();

			for (int i = 0;  i+1 < insns.length;  ++i) {
				if (insns[i].getOpcode() == Opcodes.GOTO) {
					LabelNode target = ((JumpInsnNode) insns[i]).label;

					if (insns[i+1] == target)
						mn.instructions.remove(insns[i]);
				}
			}

			int removedGotos = insns.length - mn.instructions.size();

			if (removedInsns != 0  ||  removedGotos != 0)
				log.trace(className + "." + mn.name + ": removed "
						+ removedInsns + " unreachable instructions and "
						+ removedGotos + " superfluous GOTOs");
		} catch (AnalyzerException e) {
			throw new RuntimeException("Exception in Analyzer", e);
		}

		// Propagate code further after DCE
		mn.accept(next);
	}

}
