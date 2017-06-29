package esi.bc.manip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

/**
 * Class writer that supports remapping of names when frames
 * are recomputed.
 *
 * Names remapping can cause problems during frames recomputation
 * ({@link ClassWriter#COMPUTE_FRAMES}) when a unification happens
 * against new name, and {@link Class#forName(String)} fails.
 *
 * @author Michael Orlov
 */
public class RemappingClassWriter extends ClassWriter {

	private Map<String, String>	map;
	private Map<String, String> reverseMap;

	/**
	 * Creates a remapping class writer. The map keys are old
	 * names, for which {@link Class#forName(String)} works,
	 * and map values are the new names.
	 *
	 * @param flags flags, usually {@link ClassWriter#COMPUTE_FRAMES}
	 * @param map same map given to {@link RemappingClassAdapter}
	 */
	public RemappingClassWriter(int flags, Map<String, String> map) {
		super(flags);

		this.map   = map;

		reverseMap = new HashMap<String, String>(map.size());
		for (Map.Entry<String, String> mapping: map.entrySet())
			reverseMap.put(mapping.getValue(), mapping.getKey());

		assert reverseMap.size() == map.size();
	}

	/**
	 * Creates a remapping class writer with a single mapping.
	 *
	 * @param flags flags, usually {@link ClassWriter#COMPUTE_FRAMES}
	 * @param oldName old name, for which {@link Class#forName(String)} works
	 * @param newName new name
	 */
	public RemappingClassWriter(int flags, String oldName, String newName) {
		this(flags, Collections.singletonMap(oldName, newName));
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		if (reverseMap.containsKey(type1))
			type1 = reverseMap.get(type1);

		if (reverseMap.containsKey(type2))
			type2 = reverseMap.get(type2);

		String common = super.getCommonSuperClass(type1, type2);
		if (map.containsKey(common))
			common = map.get(common);

		return common;
	}

}
