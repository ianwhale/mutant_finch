package esi.bc.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import esi.bc.flow.FrameActions;

public class TypeRepTest {

	@Test
	public void typeToStringPrimitive() {
		assertEquals("T", TypeRep.typeToString(Opcodes.TOP));
		assertEquals("I", TypeRep.typeToString(Opcodes.INTEGER));
		assertEquals("F", TypeRep.typeToString(Opcodes.FLOAT));
		assertEquals("D", TypeRep.typeToString(Opcodes.DOUBLE));
		assertEquals("J", TypeRep.typeToString(Opcodes.LONG));
		assertEquals("N", TypeRep.typeToString(Opcodes.NULL));
		assertEquals("U", TypeRep.typeToString(Opcodes.UNINITIALIZED_THIS));
		assertEquals("X", TypeRep.typeToString(FrameActions.BOGUS));
	}

	@Test
	public void typeToStringComposite() {
		String type1 = "[[I";
		String type2 = "Ljava/lang/Integer;";

		assertSame(type1, TypeRep.typeToString(type1));
		assertSame(type2, TypeRep.typeToString(type2));
	}

	@Test(expected = RuntimeException.class)
	public void typeToStringBadPrimitiveLow() {
		TypeRep.typeToString(FrameActions.BOGUS - 1);
	}

	@Test(expected = RuntimeException.class)
	public void typeToStringBadPrimitiveHigh() {
		TypeRep.typeToString(Opcodes.UNINITIALIZED_THIS + 1);
	}

	@Test
	public void typeListToString() {
		Object[] types = { Opcodes.FLOAT, "java/lang/String"};
		assertEquals("F java/lang/String", TypeRep.typeListToString(Arrays.asList(types)));
	}

	@Test
	public void typeMapToString() {
		Map<Integer, Object> map = new TreeMap<Integer, Object>();
		map.put(2, "[[I");
		map.put(4, Opcodes.LONG);
		map.put(6, Opcodes.NULL);

		assertEquals("2:[[I 4:J 6:N", TypeRep.typeMapToString(map));
	}

}
