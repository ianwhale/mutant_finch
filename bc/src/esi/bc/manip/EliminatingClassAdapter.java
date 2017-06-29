package esi.bc.manip;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

/**
 * A class adapter that eliminates dead code and other
 * unnecessary code, like [GOTO x; LABEL x].
 *
 * Requires correct frames in code.
 *
 * @author Michael Orlov
 */
public class EliminatingClassAdapter extends ClassAdapter {

	private final Method  method;

	// DCE performed on designated method
	private       boolean eliminated;

	private       String  className;

	/**
	 * Creates a class adapter that removes unreachable code.
	 *
	 * @param cv class visitor to which calls are delegated
	 * @param method method in which to eliminate unreachable code
	 */
	public EliminatingClassAdapter(ClassVisitor cv, Method method) {
		super(cv);
		this.method = method;

		eliminated = false;
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

		// Use eliminating method adapter if it's the designated method
		if (method.equals(new Method(name, desc))) {
			assert !eliminated : "Two methods with same name";
			assert className != null;

			// Prepend eliminating method adapter to the received visitor
			mv = new EliminatingMethodAdapter(mv, className, access, name, desc, signature, exceptions);
			eliminated = true;
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		if (!eliminated)
			throw new IllegalArgumentException("Unable to locate method " + method);

		super.visitEnd();
	}

}
