package esi.bc.manip;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

/**
 * A class adapter that can change a constant of any type
 * to any other constant of arbitrary type.
 *
 * @author Michael Orlov
 */
public class ConstantsClassAdapter extends ClassAdapter {

	private final Method			method;
	private final ConstantsMutator	mutator;

	// Constant modification performed on designated method
	private       boolean modified;

	/**
	 * Creates a class adapter that can change constants.
	 *
	 * @param cv class visitor to which calls are delegated
	 * @param method method in which to change constants
	 */
	public ConstantsClassAdapter(ClassVisitor cv, Method method, ConstantsMutator mutator) {
		super(cv);

		this.method  = method;
		this.mutator = mutator;

		modified = false;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		// Use constants method adapter if it's the designated method
		if (method.equals(new Method(name, desc))) {
			assert !modified : "Two methods with same name";

			// Prepend constants method adapter to the received visitor
			mv = new ConstantsMethodAdapter(mv, mutator);
			modified = true;
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		if (!modified)
			throw new IllegalArgumentException("Unable to locate method " + method);

		super.visitEnd();
	}

}
