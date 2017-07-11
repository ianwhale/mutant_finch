package esi.bc.manip;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.logging.Log;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.xml.ASMContentHandler;
import org.objectweb.asm.xml.SAXClassAdapter;

import esi.bc.AnalyzedClassNode;
import esi.util.Config;

/**
 * A class that provides the basic operation of modifying constants
 * in a given method.
 *
 * The input class node does not have to be {@link AnalyzedClassNode}.
 *
 * @author Michael Orlov
 */
public class CodeModifier extends CodeProducer {

	private static final Log log = Config.getLogger();

	/**
	 * Creates a new code modifier.
	 *
	 * The code passes through the following pipeline:
	 * <ul>
	 * <li> Source {@link ClassNode}
	 * <li> {@link ConstantsClassAdapter} using {@link ConstantsCounter}
	 *        [via {@link ConstantsMethodAdapter}, frames+maxs are unaffected]
	 * <li> {@link EmptyVisitor}
	 *
	 * <li> Source {@link ClassNode}
	 * <li> {@link ConstantsClassAdapter} using (visited) {@link ConstantsMutator}
	 *        [via {@link ConstantsMethodAdapter}, frames+maxs are unaffected]
	 * <li> {@link RemappingClassAdapter}
	 *        [code changed: does not inherit from {@link LocalVariablesSorter}]
	 * <li> {@link SAXClassAdapter}
	 *        [in order to preserve structure of original class nodes]
	 * <li> Bytes array
	 * </ul>
	 *
	 * @param name full name of the new class ({@link Class#getName()})
	 * @param cn source class node
	 * @param method method to mutate
	 * @param mutator constants mutator
	 */
	public CodeModifier(String name, ClassNode cn, Method method, ConstantsMutator mutator,
			InstructionsMutator instructions_mutator, boolean mutate_instructions) {
		super(name);

		String internalName    = name.replace('.', '/');
		String oldInternalName = cn.name;

		log.trace("Modifying: SRC=" + oldInternalName.replace('/', '.') + "." + method
				+ ", RES="  + name);
		
		if (mutate_instructions) {
			// Modify instruction(s) using the instructions adapter
			ClassAdapter instructionsAdapter = new InstructionsClassAdapter(new EmptyVisitor(), method, instructions_mutator);
			cn.accept(instructionsAdapter);
		}
		
		// Count constants, and supply them to the mutator
		ConstantsCounter counter = new ConstantsCounter();
		ClassAdapter countingAdapter = new ConstantsClassAdapter(new EmptyVisitor(), method, counter);
		cn.accept(countingAdapter);
		counter.accept(mutator);

		// Pass through XML intermediate result.
		// This is necessary because Labels (and instructions they point to)
		// are changed during byte code generation (instruction resizing process).
		ByteArrayOutputStream bytesOut   = new ByteArrayOutputStream();
		ASMContentHandler     handler    = new ASMContentHandler(bytesOut, false);
		ClassVisitor          saxAdapter = new SAXClassAdapter(handler, false);

		// Pass through renamer after reading
		Map<String, String> namesMap = Collections.singletonMap(oldInternalName, internalName);
		ClassAdapter renamingAdapter = new RemappingClassAdapter(saxAdapter, new SimpleRemapper(namesMap));

		// Modify constants using the constants adapter
		ClassAdapter constantsAdapter = new ConstantsClassAdapter(renamingAdapter, method, mutator);
		cn.accept(constantsAdapter);
		
		setBytes(bytesOut.toByteArray());
	}

}
