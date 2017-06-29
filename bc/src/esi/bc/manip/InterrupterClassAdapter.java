package esi.bc.manip;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A class adapter that adds a call to a static callback method before each
 * non-standard library method call and each backward jump in every method.
 *
 * This allows e.g., {@link Thread#interrupt()} to interrupt any method
 * in the class if the callback invokes {@link Thread#sleep(long)},
 * as long as the execution is not stuck in an external method.
 *
 * @author Michael Orlov
 */
public class InterrupterClassAdapter extends ClassAdapter {

	private final String callbackClass;
	private final String callbackMethod;
	private final int    callbackArg;

	/**
	 * Creates a class adapter that makes methods interruptible.
	 *
	 * @param cv class visitor to which calls are delegated
	 * @param callbackClass binary callback class name
	 * @param callbackMethod static callback method name
	 * @param callbackArg argument to pass to the callback method
	 */
	public InterrupterClassAdapter(ClassVisitor cv,
			String callbackClass, String callbackMethod, int callbackArg) {
		super(cv);

		this.callbackClass  = callbackClass.replace('.', '/');
		this.callbackMethod = callbackMethod;
		this.callbackArg    = callbackArg;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new InterrupterMethodAdapter(mv, callbackClass, callbackMethod, callbackArg);
	}

}
