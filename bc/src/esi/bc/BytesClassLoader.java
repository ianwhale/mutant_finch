package esi.bc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Class loader that can create a class from arrays of bytes.
 *
 * Definition of new classes with names that are equal to existing classes
 * is explicitly forbidden by throwing {@link LinkageError}.
 *
 * NOTE: Two classes that are loaded by two different {@link ClassLoader}s
 * do not "see" each other. Thus, it is better to create {@link BytesClassLoader}
 * on demand where possible. This prevents name clashes, and also allows garbage
 * collection for unnecessary loaded classes.
 */
public class BytesClassLoader extends ClassLoader {

	private final ClassLoader			parent;

	private final Map<String, byte[]>	store;
	private final File					saveDir;

	/**
	 * Creates a new bytes class loader.
	 *
	 * @param name the binary name of the class
	 * @param bytes bytes array
	 */
	public BytesClassLoader(String name, byte[] bytes) {
		this(name, bytes, null);
	}

	/**
	 * Creates a new bytes class loader that will save the class file
	 * in supplied directory upon class creation.
	 *
	 * @param name the binary name of the class
	 * @param bytes bytes array
	 * @param saveDir directory for saved class files
	 */
	public BytesClassLoader(String name, byte[] bytes, File saveDir) {
		this(Collections.singletonMap(name, bytes), saveDir);
	}

	/**
	 * Creates a new bytes class loader with extensible definitions database.
	 */
	public BytesClassLoader() {
		this(null);
	}

	/**
	 * Creates a new bytes class loader with extensible definitions database.
	 *
	 * The class files will be saved in supplied directory upon creation.
	 */
	public BytesClassLoader(File saveDir) {
		this(new HashMap<String, byte[]>(), saveDir);
	}

	// The real constructor
	private BytesClassLoader(Map<String, byte[]> store, File saveDir) {
		super(BytesClassLoader.class.getClassLoader());
		parent = BytesClassLoader.class.getClassLoader();

		for (String name: store.keySet())
			checkName(name);

		this.store   = store;
		this.saveDir = saveDir;
	}

	/**
	 * Adds definition to classes database.
	 *
	 * Works only if the class loader was created with
	 * {@link #BytesClassLoader()} or {@link #BytesClassLoader(File)}.
	 *
	 * @param name the binary name of the class
	 * @param bytes bytes array
	 * @throws UnsupportedOperationException if classes store cannot be extended
	 */
	public void addDefinition(String name, byte[] bytes) {
		if (! store.containsKey(name)) {
			checkName(name);
			store.put(name, bytes);
		}
		else
			throw new IllegalArgumentException("Class " + name + " already defined");
	}

	private void checkName(String name) {
		try {
			parent.loadClass(name);
			throw new LinkageError("Class " + name + " already exists in parent class loader");
		} catch (ClassNotFoundException e) {
			// OK
		}
	}

	/**
	 * Creates a class from the supplied bytes array, and then writes
	 * the byte array to the provided directory, if it is non-<code>null</code>.
	 *
	 * The file name is class name + ".class".
	 *
	 * Thus, no writing will occur in case of illegal bytecode.
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// define new class if name matches
		if (store.containsKey(name)) {
			byte[]   bytes = store.get(name);
			Class<?> klass = defineClass(name, bytes, 0, bytes.length);

			// Write to given file
			if (saveDir != null)
				try {
					assert !klass.getSimpleName().isEmpty();
					File output = new File(saveDir, klass.getSimpleName() + ".class");
					OutputStream out = new BufferedOutputStream(
							new FileOutputStream(output));
					out.write(bytes);
					out.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			return klass;
		}
		else
			// throws exception
			return super.findClass(name);
	}

}
