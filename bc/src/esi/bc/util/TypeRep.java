package esi.bc.util;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import esi.bc.flow.FrameActions;

public class TypeRep {

	/**
	 * Type of data should be either {@link Integer} or {@link String}.
	 * {@link Label}s should have been converted to {@link String}s previously.
	 *
	 * Integer data types should be one of the constants from
	 * the {@link Opcodes} interface:
	 * <ul>
	 * <li> {@link Opcodes#TOP}
	 * <li> {@link Opcodes#INTEGER}
	 * <li> {@link Opcodes#FLOAT}
	 * <li> {@link Opcodes#DOUBLE}
	 * <li> {@link Opcodes#LONG}
	 * <li> {@link Opcodes#NULL}
	 * <li> {@link Opcodes#UNINITIALIZED_THIS}
	 * <li> {@link FrameActions#BOGUS}
	 * </ul>
	 *
	 * @param type the type to convert
	 */
	public static String typeToString(Object type) {
		assert (type instanceof String) || (type instanceof Integer) : type;

		if (type instanceof String)
			return (String) type;
		else if (type instanceof Integer){
			switch ((Integer) type) {
			case 0:
				return "T";
			case 1:
				return "I";
			case 2:
				return "F";
			case 3:
				return "D";
			case 4:
				return "J";
			case 5:
				return "N";
			case 6:
				return "U";
			case -1:
				return "X";
			default:
				throw new RuntimeException("Unknown type " + type);
			}
		}
		else {
			throw new RuntimeException("Unsuppoted type " + type);
		}
	}

	/**
	 * Convert a list of types to string.
	 *
	 * @param lst list of types
	 * @return string representation
	 */
	public static String typeListToString(List<Object> lst) {
		StringBuilder buf = new StringBuilder();

		for (Object type: lst) {
			buf.append(typeToString(type))
			   .append(' ');
		}

		if (buf.length() > 0)
			buf.deleteCharAt(buf.length() - 1);

		return buf.toString();
	}

	/**
	 * Convert a map of types to string.
	 *
	 * @param map map of types
	 * @return string representation
	 */
	public static String typeMapToString(Map<Integer, Object> map) {
		StringBuilder buf = new StringBuilder();

		for (Map.Entry<Integer, Object> varAccess: map.entrySet()) {
			buf.append(varAccess.getKey())
			   .append(':')
			   .append(typeToString(varAccess.getValue()))
			   .append(' ');
		}

		if (buf.length() > 0)
			buf.deleteCharAt(buf.length() - 1);

		return buf.toString();
	}

}
