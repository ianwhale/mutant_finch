package esi.bc.manip;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import esi.bc.BytesClassLoader;
import esi.bc.manip.test.Rename;
import esi.bc.manip.test.RenameAnnotation;
import esi.bc.manip.test.RenameException;
import esi.bc.manip.test.RenameGeneric;
import esi.util.Config;

public class RemappingTest {

	private static Map<String, String> map;

	@BeforeClass
	public static void setUpBeforeClass() {
		map = new HashMap<String, String>();

		String className  = Type.getInternalName(Rename.class);
		String exceptName = Type.getInternalName(RenameException.class);
		String annotName  = Type.getInternalName(RenameAnnotation.class);
		String generName  = Type.getInternalName(RenameGeneric.class);

		for (String name: new String[]{ className, exceptName, annotName, generName })
			map.put(name, name + "Test");
	}

	@Test
	public void rename() throws Exception {
		// TODO: check annotation (in Rename.class)
		Remapper remapper = new SimpleRemapper(map);

		BytesClassLoader loader = new BytesClassLoader(Config.DIR_OUT_TESTS);

		Class<?> klass = renameClass(Rename.class, remapper, loader);
		renameClass(RenameException.class,  remapper, loader);
		renameClass(RenameAnnotation.class, remapper, loader);

		// Check verification
		klass.newInstance();

		Class<?> generClass = renameClass(RenameGeneric.class, remapper);
		generClass.newInstance();
	}

	@Test
	public void noClash() throws Exception {
		String className  = Type.getInternalName(Rename.class);
		Remapper remapper = new SimpleRemapper(className, className + "Test");

		renameClass(Rename.class, remapper, false);
		renameClass(Rename.class, remapper, false);
	}

	@Test(expected = LinkageError.class)
	public void clash() throws Exception {
		ClassReader  reader  = new ClassReader(Type.getInternalName(Rename.class));
		ClassWriter  writer  = new RemappingClassWriter(ClassWriter.COMPUTE_FRAMES, map);
		ClassAdapter renamer = new RemappingClassAdapter(writer, new SimpleRemapper(map));
		reader.accept(renamer, ClassReader.EXPAND_FRAMES);

		BytesClassLoader loader = new BytesClassLoader();
		loader.addDefinition(Rename.class.getName() + "Test", writer.toByteArray());
		loader.addDefinition(Rename.class.getName() + "Test1", writer.toByteArray());

		// Originally, the test checked actual clash, here it's a name incompatibility
		loader.loadClass(Rename.class.getName() + "Test");
		loader.loadClass(Rename.class.getName() + "Test");
		loader.loadClass(Rename.class.getName() + "Test1");
	}

	private Class<?> renameClass(Class<?> klass, Remapper remapper) throws Exception {
		return renameClass(klass, remapper, true);
	}

	private Class<?> renameClass(Class<?> klass, Remapper remapper, boolean save) throws Exception {
		ClassReader  reader  = new ClassReader(Type.getInternalName(klass));
		ClassWriter  writer  = new RemappingClassWriter(ClassWriter.COMPUTE_FRAMES, map);
		ClassAdapter renamer = new RemappingClassAdapter(writer, remapper);
		reader.accept(renamer, ClassReader.EXPAND_FRAMES);

		BytesClassLoader loader = new BytesClassLoader(klass.getName() + "Test", writer.toByteArray(),
				save ? Config.DIR_OUT_TESTS : null);
		Class<?> newClass = loader.loadClass(klass.getName() + "Test");

		assertEquals(klass.getName() + "Test", newClass.getName());

		return newClass;
	}

	private Class<?> renameClass(Class<?> klass, Remapper remapper, BytesClassLoader loader) throws Exception {
		ClassReader  reader  = new ClassReader(Type.getInternalName(klass));
		ClassWriter  writer  = new RemappingClassWriter(ClassWriter.COMPUTE_FRAMES, map);
		ClassAdapter renamer = new RemappingClassAdapter(writer, remapper);
		reader.accept(renamer, ClassReader.EXPAND_FRAMES);

		loader.addDefinition(klass.getName() + "Test", writer.toByteArray());
		Class<?> newClass = loader.loadClass(klass.getName() + "Test");

		assertEquals(klass.getName() + "Test", newClass.getName());

		return newClass;
	}

}
