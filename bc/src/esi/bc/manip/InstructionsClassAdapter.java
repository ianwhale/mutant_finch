package esi.bc.manip;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

public class InstructionsClassAdapter extends ClassAdapter {

	private final Method  method;

	// Mutation performed on designated method.
	private       boolean mutated;

	private InstructionsMutator mutator; 
	
	private       String  className;

	/**
	 * Creates a class adapter that mutates instructions. 
	 *
	 * @param cv class visitor to which calls are delegated
	 * @param method method in which to eliminate unreachable code
	 */
	public InstructionsClassAdapter(ClassVisitor cv, Method method, InstructionsMutator mutator) {
		super(cv);
		
		this.method = method;
		this.mutator = mutator; 
		this.mutated = false;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		// Use mutating method adapter if it's the designated method
		if (method.equals(new Method(name, desc))) {
			assert !mutated : "Two methods with same name";
			assert className != null;

			// Prepend mutating method adapter to the received visitor
			mv = new InstructionsMethodAdapter(mv, className, access, name, desc, signature, exceptions, this.mutator);
			mutated = true;
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		if (!mutated)
			throw new IllegalArgumentException("Unable to locate method " + method);

		super.visitEnd();
	}

}
