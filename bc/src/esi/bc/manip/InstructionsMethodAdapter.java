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
public class InstructionsMethodAdapter extends MethodAdapter {
	private static final Log log = Config.getLogger();

	// Owner class name
	private final String        className;

	// Method node into which the method is read
	private final MethodNode    mn;

	// Next visitor which method node accepts after mutation
	private final MethodVisitor next;

	// Analyzer that does simple analysis of code in method node
	private final Analyzer      an;

	private InstructionsMutator mutator;
	
	public InstructionsMethodAdapter(MethodVisitor mv, String className,
			int access, String name, String desc,
			String signature, String[] exceptions,
			InstructionsMutator mutator) {
		super(new MethodNode(access, name, desc, signature, exceptions));

		this.className = className;
		this.mutator = mutator;
		mn   = (MethodNode) this.mv;
		next = mv;
		an   = new Analyzer(new BasicInterpreter());
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		
		try {
			// Analyze to initialize the method node. 
			an.analyze(className, mn);
		}
		catch(AnalyzerException e) {
			throw new RuntimeException("Exception in Analyzer", e);
		}

		mutator.mutate(mn);
		
		// Propagate code further after mutation
		mn.accept(next);
	}
}
