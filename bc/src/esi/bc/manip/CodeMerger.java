package esi.bc.manip;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.xml.ASMContentHandler;
import org.objectweb.asm.xml.SAXClassAdapter;

import esi.bc.AnalyzedClassNode;
import esi.bc.flow.CodeSection;
import esi.util.Config;

/**
 * A class that provides the basic operation of taking a code section
 * in a class and substituting it for another code section in another
 * class.
 *
 * The input class nodes do not have to be {@link AnalyzedClassNode}s.
 *
 * @author Michael Orlov
 */
public class CodeMerger extends CodeProducer {

	private static final Log log = Config.getLogger();

	/**
	 * Creates a new code merger.
	 *
	 * The code passes through the following pipeline:
	 * <ul>
	 * <li> Destination {@link ClassNode}
	 * <li> {@link MergingClassAdapter}
	 *        [via {@link MergingMethodAdapter}, frames become invalid]
	 * <li> {@link SAXClassAdapter}
	 *        [in order to preserve structure of original class nodes]
	 * <li> Bytes array
	 * <li> {@link ClassReader}
	 *        [frames are skipped]
	 * <li> {@link RemappingClassAdapter}
	 *        [code changed: does not inherit from {@link LocalVariablesSorter}]
	 * <li> {@link RemappingClassWriter}
	 *        [frames are recomputed]
	 * <li> {@link ClassReader}
	 *        [frames are expanded]
	 * <li> {@link EliminatingClassAdapter}
	 *        [via {@link EliminatingMethodAdapter}, uses frames]
	 * <li> {@link RemappingClassWriter}
	 *        [frames are recomputed]
	 * <li> Bytes array
	 * </ul>
	 *
	 * @param name full name of the new class ({@link Class#getName()})
	 * @param origName full name of the original class ({@link Class#getName()}),
	 *        for type checking ({@link Class#forName(String)} must work)
	 * @param dest destination class node
	 * @param src source class node
	 * @param destSection destination section description
	 * @param srcSection source section description
	 */
	public CodeMerger(String name, String origName,
			ClassNode dest, ClassNode src,
			CodeSection destSection, CodeSection srcSection)
	{
		super(name);

		String internalName     = name.replace('.', '/');
		String origInternalName = origName.replace('.', '/');

		log.trace("Merging: DEST=" + dest.name.replace('/', '.') + "." + destSection
					+ ", SRC="  + src .name.replace('/', '.') + "." + srcSection
					+ ", RES="  + name);

		// If source and destination class nodes are same,
		// one needs to be duplicated to avoid label problems
		if (dest == src) {
			src = duplicateClassNode(src);
			log.trace("SRC node was duplicated");
		}

		// Pass through XML intermediate result.
		// This is necessary because Labels (and instructions they point to)
		// are changed during byte code generation (instruction resizing process).
		ByteArrayOutputStream bytesOut   = new ByteArrayOutputStream();
		ASMContentHandler     handler    = new ASMContentHandler(bytesOut, false);
		ClassVisitor          saxAdapter = new SAXClassAdapter(handler, false);

		// Extract source section
		AbstractInsnNode[] srcInstructions = getSection(src, srcSection);

		// Merge using the merging adapter
		ClassAdapter mergingAdapter = new MergingClassAdapter(saxAdapter, destSection, srcInstructions);
		dest.accept(mergingAdapter);

		// COMPUTE_FRAMES implies COMPUTE_MAXS
		ClassWriter writer = new RemappingClassWriter(ClassWriter.COMPUTE_FRAMES, origInternalName, internalName);

		// Pass through renamer after reading
		Map<String, String> namesMap = new HashMap<String, String>(2);
		namesMap.put(dest.name, internalName);
		namesMap.put(src.name,  internalName);
		ClassAdapter renamingAdapter = new RemappingClassAdapter(writer, new SimpleRemapper(namesMap));

		// Read while skipping frames (will be recomputed anyway)
		ClassReader reader = new ClassReader(bytesOut.toByteArray());
		reader.accept(renamingAdapter, ClassReader.SKIP_FRAMES);

		// Eliminate unreachable code
		// Frames are correct and don't need recomputation *only* when elimination
		// removes only "simple" instructions. Removing GOTOs requires recomputation
		// W/o GOTOs: ClassWriter  dceWriter  = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassWriter  dceWriter  = new RemappingClassWriter(ClassWriter.COMPUTE_FRAMES, origInternalName, internalName);
		ClassAdapter dceAdapter = new EliminatingClassAdapter(dceWriter, destSection.method);
		ClassReader  dceReader  = new ClassReader(writer.toByteArray());
		dceReader.accept(dceAdapter, 0);

		// Extract bytes (not a cheap operation)
		setBytes(dceWriter.toByteArray());
	}

	/**
	 * Duplicates a class node by passing it through {@link SAXClassAdapter}.
	 * The class node is assumed to have been read with
	 * {@link ClassReader#EXPAND_FRAMES} flag.
	 *
	 * After duplication, methods should contain the same instructions,
	 * but with different labels.
	 *
	 * TODO: get rid of this method, since reliance on there being
	 * a same number of instructions in the duplicated node is wrong.
	 * E.g., {@link AnalyzedClassNode} has a label associated with each
	 * NEW instructions, whereas {@link ClassNode} does not.
	 *
	 * @param classNode class node to duplicate
	 * @return duplicated result
	 */
	static ClassNode duplicateClassNode(ClassNode classNode) {
		// Pass through XML intermediate result.
		ByteArrayOutputStream bytesOut   = new ByteArrayOutputStream();
		ASMContentHandler     handler    = new ASMContentHandler(bytesOut, false);
		ClassVisitor          saxAdapter = new SAXClassAdapter(handler, false);

		classNode.accept(saxAdapter);

		// Assume classNode was read with EXPAND_FRAMES
		// Assume classNode is actually an AnalyzedClassNode
		// (where each NEW has a label)
		ClassReader reader  = new ClassReader(bytesOut.toByteArray());
		ClassNode   dupNode = new AnalyzedClassNode();
		reader.accept(dupNode, ClassReader.EXPAND_FRAMES);

		// Sanity check
		boolean enabled = false;
		assert  enabled = true;
		if (enabled) {
			assert dupNode.methods.size() == classNode.methods.size();

			for (int i = 0;  i < classNode.methods.size();  ++i) {
				MethodNode method    = (MethodNode) classNode.methods.get(i);
				MethodNode dupMethod = (MethodNode) dupNode.methods.get(i);
				assert dupMethod.instructions.size() == method.instructions.size();

				int size = method.instructions.size();
				assert dupMethod.instructions.get(size/2).getOpcode()
					== method.instructions.get(size/2).getOpcode();
			}
		}

		return dupNode;
	}

	private AbstractInsnNode[] getSection(ClassNode src, CodeSection section) {
		// Locate source method node
		MethodNode srcMethod = null;
		for (Object method: src.methods) {
			MethodNode checkMethod = (MethodNode) method;
			if (section.method.equals(new Method(checkMethod.name, checkMethod.desc))) {
				srcMethod = checkMethod;
				break;
			}
		}

		if (srcMethod == null)
			throw new IllegalArgumentException("Unable to locate source method " + section.method);

		// Extract relevant instructions
		AbstractInsnNode[] instructions = new AbstractInsnNode[section.end - section.start + 1];
		for (int i = 0;  i < instructions.length;  ++i)
			instructions[i] = srcMethod.instructions.get(i + section.start);

		return instructions;
	}

}
