package esi.bc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A node that represents a class.
 * Uses {@link AnalyzedMethodNode} to include frame data per instruction.
 * Extends and based on {@link ClassNode} by Eric Bruneton.
 *
 * The difference between this class and {@link ClassNode} is that {@link ClassNode#methods}
 * holds a list of {@link AnalyzedMethodNode} instead of {@link MethodNode}.
 *
 * @see <a href="https://jdk.dev.java.net/verifier.html">Type Checking Verifier</a>
 *
 * @author Kfir Wolfson
 * @author Michael Orlov
 */
public class AnalyzedClassNode extends ClassNode {

	@SuppressWarnings("unchecked")
	@Override
	public MethodVisitor visitMethod(
			final int access,
			final String name,
			final String desc,
			final String signature,
			final String[] exceptions)
	{
		// Owner is the class name
		String owner = this.name;

		AnalyzedMethodNode mn = new AnalyzedMethodNode(owner, access,
				name,
				desc,
				signature,
				exceptions);

		// methods is a raw List type
		methods.add(mn);

		return mn.aa;
	}

	/**
	 * Creates an analyzed class node from the given class.
	 *
	 * The class must contain frames, and thus Jasmin-compiled classes
	 * are usually not suitable. Local variables and line numbers are
	 * read as-is.
	 *
	 * @param klass class to read
	 * @return analyzed class node
	 * @throws IOException if class cannot be read as a resource
	 * @see #readClass(Class, boolean, boolean)
	 */
	public static AnalyzedClassNode readClass(Class<?> klass) throws IOException {
		return readClass(klass, false);
	}

	/**
	 * Creates an analyzed class node from the given class.
	 *
	 * The class must contain frames, and thus Jasmin-compiled classes
	 * are usually not suitable.
	 *
	 * @param klass class to read
	 * @param skipDebug skip local variables and line numbers
	 * @return analyzed class node
	 * @throws IOException if class cannot be read as a resource
	 * @see #readClass(Class, boolean, boolean)
	 */
	public static AnalyzedClassNode readClass(Class<?> klass, boolean skipDebug) throws IOException {
		return readClass(klass, skipDebug, false);
	}

	/**
	 * Creates an analyzed class node from the given class.
	 *
	 * @param klass class to read
	 * @param skipDebug skip local variables and line numbers
	 * @param computeFrames recompute frames (necessary for Jasmin-compiled classes)
	 * @return analyzed class node
	 * @throws IOException if class cannot be read as a resource
	 */
	public static AnalyzedClassNode readClass(Class<?> klass, boolean skipDebug, boolean computeFrames) throws IOException {
		return readClass(klass.getName(), skipDebug, computeFrames);
	}

	/**
	 * Creates an analyzed class node from the given class.
	 *
	 * @param className full name of the class to read (dot-separated)
	 * @param skipDebug skip local variables and line numbers
	 * @param computeFrames recompute frames (necessary for Jasmin-compiled classes)
	 * @return analyzed class node
	 * @throws IOException if class cannot be read as a resource
	 */
	public static AnalyzedClassNode readClass(String className, boolean skipDebug, boolean computeFrames) throws IOException {
		return readClass(ClassLoader.getSystemResourceAsStream(className.replace('.', '/') + ".class"),
						 skipDebug, computeFrames);
	}

	/**
	 * Creates an analyzed class node from the given input stream.
	 *
	 * @param in input stream of class file contents
	 * @param skipDebug skip local variables and line numbers
	 * @param computeFrames recompute frames (necessary for Jasmin-compiled classes)
	 * @return analyzed class node
	 * @throws IOException if class cannot be read as a resource
	 */
	public static AnalyzedClassNode readClass(InputStream in, boolean skipDebug, boolean computeFrames) throws IOException {
		int  skipDebugFlag = (skipDebug ? ClassReader.SKIP_DEBUG : 0);
		ClassReader reader = new ClassReader(new BufferedInputStream(in));

		// If frames must be computed, reread through writer
		if (computeFrames) {
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			reader.accept(writer, ClassReader.SKIP_FRAMES | skipDebugFlag);

			reader = new ClassReader(writer.toByteArray());
		}

		// Read into class node while expanding frames
		AnalyzedClassNode classNode = new AnalyzedClassNode();
		reader.accept(classNode, ClassReader.EXPAND_FRAMES | skipDebugFlag);

		return classNode;
	}

	/**
	 * Locates a specified method in this class node.
	 * The method must be present.
	 *
	 * @param method method to search for
	 * @return the method node
	 */
	public AnalyzedMethodNode findMethod(Method method) {
		AnalyzedMethodNode methodNode = null;

		// Go over all methods, asserting if same method appears twice
		for (Object m: methods) {
			AnalyzedMethodNode maybeMethod = (AnalyzedMethodNode) m;

			if (method.equals(new Method(maybeMethod.name, maybeMethod.desc))) {
				assert methodNode == null;
				methodNode = maybeMethod;
			}
		}

		// Check that the method was found
		assert methodNode != null;
		return methodNode;
	}

}
