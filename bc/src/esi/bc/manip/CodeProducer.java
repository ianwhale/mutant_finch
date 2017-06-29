package esi.bc.manip;

import java.io.File;

import org.objectweb.asm.ClassReader;

import esi.bc.AnalyzedClassNode;
import esi.bc.BytesClassLoader;

/**
 * An code producer for classes that can produce
 * {@link BytesClassLoader}s.
 *
 * It is necessary because a class loader, once produced, is tied
 * to its classes.
 *
 * @author Michael Orlov
 */
public abstract class CodeProducer {

	/**
	 * Binary name of the class.
	 * @see ClassLoader
	 */
	private final String name;

	/**
	 * Byte serialization of the class.
	 */
	private byte[] bytes;

	/**
	 * Creates a new code producer.
	 *
	 * @param name fully-qualified class name
	 */
	protected CodeProducer(String name) {
		this.name = name;
	}

	/**
	 * @return fully-qualified class name
	 */
	public String getName() {
		return name;
	}

	protected void setBytes(byte[] bytes) {
		assert this.bytes == null;
		this.bytes = bytes;
	}

	/**
	 * @return length of class file in bytes
	 */
	public int size() {
		return bytes.length;
	}

	/**
	 * @return new bytes class loader for the byte-serialized class
	 */
	public BytesClassLoader getClassLoader() {
		assert bytes != null;
		return new BytesClassLoader(name, bytes);
	}

	/**
	 * @param saveDir directory to save the loaded class to
	 * @return new bytes class loader for the byte-serialized class,
	 */
	public BytesClassLoader getClassLoader(File saveDir) {
		assert bytes != null;
		return new BytesClassLoader(name, bytes, saveDir);
	}

	/**
	 * Returns a new analyzed class node, loaded from bytes array.
	 *
	 * Debug information is not skipped while parsing the array,
	 * but if the original classes are read with {@link ClassReader#SKIP_DEBUG}
	 * flag, no debug information will be present in the modified class either.
	 *
	 * @return a new analyzed class node
	 */
	public AnalyzedClassNode getClassNode() {
		ClassReader       reader    = getClassReader();
		AnalyzedClassNode classNode = new AnalyzedClassNode();

		reader.accept(classNode, ClassReader.EXPAND_FRAMES);
		return classNode;
	}

	/**
	 * Returns a new class reader, initialized with the bytes array.
	 *
	 * @return a new class reader
	 */
	public ClassReader getClassReader() {
		assert bytes != null;

		ClassReader cr = new ClassReader(bytes);
		assert name.replace('.', '/').equals(cr.getClassName());

		return cr;
	}

}
