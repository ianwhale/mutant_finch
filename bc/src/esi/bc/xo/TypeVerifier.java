package esi.bc.xo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import esi.bc.FrameData;

/**
 * Implementation of is-(same-or)-narrower-than relations.
 *
 * @author Michael Orlov
 */
public class TypeVerifier extends SimpleVerifier {

	private Map<String, String> typesMap;

	/**
	 * Constructs a type verifier that is aware of the type
	 * of this class node (which may be unavailable via {@link Class#forName(String)}.
	 *
	 * @param classNode class node
	 */
	public TypeVerifier(ClassNode classNode) {
		super(Type.getObjectType(classNode.name),
			  (classNode.superName == null) ? null : Type.getObjectType(classNode.superName),
			  getTypes(classNode.interfaces),
			  false);
		typesMap = Collections.emptyMap();
	}

	/**
	 * Constructs a type verifier that views additional class type
	 * as the type of the prime class node.
	 *
	 * @param classNode class node
	 * @param altClassNode alternative class node
	 * @see #TypeVerifier(ClassNode)
	 */
	public TypeVerifier(ClassNode classNode, ClassNode altClassNode) {
		this(classNode);
		typesMap = Collections.singletonMap(altClassNode.name, classNode.name);
	}

	// Converts list of interface internal names to list of types
	private static List<Type> getTypes(List<?> internalNames) {
		List<Type> types = new ArrayList<Type>(internalNames.size());

		for (Object internalName: internalNames)
			types.add(Type.getObjectType((String) internalName));

		return types;
	}

	/**
	 * Checks whether given "reads" mappings are contained in the "writes" mappings
	 * as narrower-than types.
	 *
	 * @param writes variable index to type mapping
	 * @param reads variable index to type mapping
	 * @param strict whether all reads must be contained in writes
	 *		  (otherwise extra reads are ignored)
	 * @return whether writes are narrower than reads
	 */
	public boolean isNarrowerThan(Map<Integer, Object> writes, Map<Integer, Object> reads, boolean strict) {
		for (Map.Entry<Integer, Object> read: reads.entrySet()) {
			Object writeType = writes.get(read.getKey());

			if (writeType == null) {
				if (strict)
					return false;
			}
			else if (! isNarrowerThan(writeType, read.getValue()))
				return false;
		}

		return true;
	}

	/**
	 * Checks whether every element in the first list is narrower
	 * than every corresponding element in the second list.
	 *
	 * @param as first list
	 * @param bs second list
	 * @return whether as are narrower than bs
	 * @throws IndexOutOfBoundsException if lists are of different lengths
	 */
	public boolean isNarrowerThan(List<Object> as, List<Object> bs) {
		if (as.size() != bs.size())
			throw new IndexOutOfBoundsException("Lists are of different lengths: " + as.size() + " and " + bs.size());

		Iterator<Object> bIter = bs.iterator();
		for (Object a: as)
			if (! isNarrowerThan(a, bIter.next()))
				return false;

		return true;
	}

	/**
	 * Returns true if the first argument has same type as the second
	 * argument, or is below it in hierarchy.
	 *
	 * It is assumed that primitive types are expressed with integers
	 * such as {@link Opcodes#DOUBLE}, and more complex types are
	 * {@link String} internal names. See {@link AnalyzerAdapter#locals}.
	 * {@link Opcodes#NULL} is explicitly supported.
	 *
	 * {@link Opcodes#UNINITIALIZED_THIS} is currently not supported.
	 * Perhaps it will work as-is.
	 *
	 * @param a left parameter in is-narrower-than relation
	 * @param b right parameter in is-narrower-than relation
	 * @return whether a is same or narrower than b
	 */
	public boolean isNarrowerThan(Object a, Object b) {
		assert (a instanceof Integer)  ||  (a instanceof String);
		assert (b instanceof Integer)  ||  (b instanceof String);

		// Primitives are only compatible if equal (including BOGUS)
		if ((a instanceof Integer)  &&  (b instanceof Integer))
			return a.equals(b);

		// Handle strings as internal names
		else if ((a instanceof String)  &&  (b instanceof String)) {
			String aType = (String) a;
			String bType = (String) b;

			// Handle uninitialized types (even NULL is not narrower than U-type)
			if (aType.startsWith(FrameData.UNINITIALIZED_PREFIX)  ||  bType.startsWith(FrameData.UNINITIALIZED_PREFIX))
				return a.equals(b);
			else {
				// Map types if necessary
				if (typesMap.containsKey(aType))
					aType = typesMap.get(aType);

				if (typesMap.containsKey(bType))
					bType = typesMap.get(bType);

				return isAssignableFrom(Type.getObjectType(bType),
										Type.getObjectType(aType));
			}
		}

		// Assumes that primitives are not Strings
		else if (Opcodes.NULL.equals(a))
			// NULL is narrower than all objects except U-types
			return !((String) b).startsWith(FrameData.UNINITIALIZED_PREFIX);

		// All "true" cases should have been taken care of
		else
			assert !a.equals(b);
			return a.equals(b);
	}

}
