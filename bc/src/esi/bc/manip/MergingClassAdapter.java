package esi.bc.manip;

import java.util.Arrays;
import org.apache.commons.logging.Log;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;

import esi.bc.flow.CodeSection;
import esi.util.Config;

/**
 * A class adapter that can replace a section of code in a method
 * with another code section in another (or same) method.
 *
 * @author Michael Orlov
 */
public class MergingClassAdapter extends ClassAdapter {

	private static final Log log = Config.getLogger();

	private final CodeSection        destSection;
	private final AbstractInsnNode[] srcInstructions;

	// srcInstructions injected into destSection
	private boolean injected;

	private String  className;

	/**
	 * Creates a class adapter that replaces a code section with given instructions.
	 *
	 * @param cv class visitor to which calls are delegated
	 * @param destSection destination section to remove
	 * @param srcInstructions instructions to inject in place of removed section
	 */
	public MergingClassAdapter(ClassVisitor cv, CodeSection destSection, AbstractInsnNode[] srcInstructions) {
		super(cv);

		this.destSection     = destSection;
		this.srcInstructions = srcInstructions;

		injected = false;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		className = name;

		log.trace("Merging to class " + name
				+ (signature == null  ?
						""  :  " <" + signature + ">")
				+ ("java/lang/Object".equals(superName)  ?
						""  :  " extends " + superName)
				+ (interfaces == null  ||  interfaces.length == 0  ?
						""  :  " implements " + Arrays.asList(interfaces)));

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		// Use merging adapter if it's the destination method
		if (destSection.method.equals(new Method(name, desc))) {
			assert !injected : "Two methods with same name";
			assert className != null;

			// Prepend merging method adapter to the received visitor
			mv = new MergingMethodAdapter(mv, destSection.start, destSection.end, srcInstructions);
			injected = true;
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		if (!injected)
			throw new IllegalArgumentException("Unable to locate destination method " + destSection.method);

		super.visitEnd();
	}

}
