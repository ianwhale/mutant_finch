package esi.bc.manip;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.logging.Log;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.xml.ASMContentHandler;
import org.objectweb.asm.xml.SAXClassAdapter;

import esi.bc.AnalyzedClassNode;
import esi.util.Config;

/**
 * A class that makes all methods in a given class interruptible,
 * by inserting a given static method call before each
 * backward jump (supporting conditional branches, unconditional
 * GOTOs, and switch constructs), and before calls to methods that
 * are not in standard Java library (<code>java</code> namespace).
 *
 * The input class nodes do not have to be {@link AnalyzedClassNode}s.
 *
 * @author Michael Orlov
 */
public class CodeInterrupter extends CodeProducer {

	private static final Log log = Config.getLogger();

	/**
	 * Creates a new code interrupter.
	 *
	 * The code passes through the following pipeline:
	 * <ul>
	 * <li> Source {@link ClassNode}
	 * <li> {@link InterrupterClassAdapter}
	 *        [via {@link InterrupterMethodAdapter}, frames are unaffected]
	 * <li> {@link RemappingClassAdapter}
	 *        [code changed: does not inherit from {@link LocalVariablesSorter}]
	 * <li> {@link SAXClassAdapter}
	 *        [in order to preserve structure of original class nodes]
	 *        (maxs are recomputed)
	 * <li> Bytes array
	 * </ul>
	 *
	 * @param name full name of the new class ({@link Class#getName()})
	 * @param callbackClass binary callback class name
	 * @param callbackMethod static callback method name
	 * @param callbackArg argument to pass to the callback method
	 * @param cn source class node
	 */
	public CodeInterrupter(String name, ClassNode cn, String callbackClass, String callbackMethod, int callbackArg) {
		super(name);

		String internalName    = name.replace('.', '/');
		String oldInternalName = cn.name;

		log.trace("Interrupting: SRC=" + oldInternalName.replace('/', '.') + ", RES="  + name);

		// Pass through XML intermediate result, recomputing MAXs.
		// This is necessary because Labels (and instructions they point to)
		// are changed during byte code generation (instruction resizing process).
		ByteArrayOutputStream bytesOut   = new ByteArrayOutputStream();
		ASMContentHandler     handler    = new ASMContentHandler(bytesOut, true);
		ClassVisitor          saxAdapter = new SAXClassAdapter(handler, false);

		// Pass through renamer after transforming
		Map<String, String> namesMap = Collections.singletonMap(oldInternalName, internalName);
		ClassAdapter renamingAdapter = new RemappingClassAdapter(saxAdapter, new SimpleRemapper(namesMap));

		// Make methods interruptible using the interrupter adapter
		ClassAdapter interrupterAdapter = new InterrupterClassAdapter(renamingAdapter, callbackClass, callbackMethod, callbackArg);
		cn.accept(interrupterAdapter);

		setBytes(bytesOut.toByteArray());
	}

	/**
	 * Creates a new code interrupter.
	 *
	 * The code passes through the following pipeline:
	 * <ul>
	 * <li> Source {@link ClassReader}
	 *        [debug information is not skipped]
	 * <li> {@link InterrupterClassAdapter}
	 *        [via {@link InterrupterMethodAdapter}, frames are unaffected]
	 * <li> {@link RemappingClassAdapter}
	 *        [code changed: does not inherit from {@link LocalVariablesSorter}]
	 * <li> {@link ClassWriter}
	 *        (maxs are recomputed)
	 * <li> Bytes array
	 * </ul>
	 *
	 * @param name full name of the new class ({@link Class#getName()})
	 * @param callbackClass binary callback class name
	 * @param callbackMethod static callback method name
	 * @param callbackArg argument to pass to the callback method
	 * @param cr class reader
	 */
	public CodeInterrupter(String name, ClassReader cr, String callbackClass, String callbackMethod, int callbackArg) {
		super(name);

		String internalName    = name.replace('.', '/');
		String oldInternalName = cr.getClassName();

		log.trace("Interrupting: SRC=" + oldInternalName.replace('/', '.') + ", RES="  + name);

		// Need to recompute MAXs
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		// Pass through renamer after transforming
		Map<String, String> namesMap = Collections.singletonMap(oldInternalName, internalName);
		ClassAdapter renamingAdapter = new RemappingClassAdapter(cw, new SimpleRemapper(namesMap));

		// Make methods interruptible using the interrupter adapter
		ClassAdapter interrupterAdapter = new InterrupterClassAdapter(renamingAdapter, callbackClass, callbackMethod, callbackArg);
		cr.accept(interrupterAdapter, 0);

		setBytes(cw.toByteArray());
	}

}
