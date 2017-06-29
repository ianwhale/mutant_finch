package esi.bc.xo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import esi.bc.AnalyzedClassNode;
import esi.bc.FrameData;
import esi.bc.test.Fact;

public class TypeVerifierTest {

	private static final String CONTROL_NAME = "esi.bc.test.Control";
	private static AnalyzedClassNode controlNode;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		controlNode = AnalyzedClassNode.readClass(CONTROL_NAME, false, true);
	}

	@Test
	public void testTypeVerifier() {
		new TypeVerifier(controlNode);
	}

	@Test
	public void isNarrowerThanSimple() {
		TypeVerifier tv = new TypeVerifier(controlNode);

		assertTrue(tv.isNarrowerThan(Opcodes.DOUBLE, Opcodes.DOUBLE));
		assertFalse(tv.isNarrowerThan(Opcodes.DOUBLE, Opcodes.INTEGER));
		assertFalse(tv.isNarrowerThan(Opcodes.INTEGER, Opcodes.DOUBLE));

		String objType = Type.getInternalName(Object.class);
		assertFalse(tv.isNarrowerThan(Opcodes.INTEGER, objType));
		assertFalse(tv.isNarrowerThan(objType, Opcodes.INTEGER));
	}

	@Test
	public void isNarrowerThanNull() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		String objType = Type.getInternalName(Object.class);

		assertTrue(tv.isNarrowerThan(Opcodes.NULL, objType));
		assertFalse(tv.isNarrowerThan(objType, Opcodes.NULL));

		assertFalse(tv.isNarrowerThan(Opcodes.NULL, Opcodes.FLOAT));
		assertFalse(tv.isNarrowerThan(Opcodes.FLOAT, Opcodes.NULL));

		assertTrue(tv.isNarrowerThan(Opcodes.NULL, Opcodes.NULL));
	}

	@Test
	public void isNarrowerThanUninitialized() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		String objType  = FrameData.UNINITIALIZED_PREFIX + Type.getInternalName(Object.class);
		String objType2 = FrameData.UNINITIALIZED_PREFIX + Type.getInternalName(Integer.class);

		assertTrue(tv.isNarrowerThan(objType, objType));
		assertFalse(tv.isNarrowerThan(Opcodes.NULL, objType));
		assertFalse(tv.isNarrowerThan(objType2, objType));
	}

	@Test
	public void isNarrowerThanObjects() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		String objType1  = Type.getInternalName(Object.class);
		String objType2  = Type.getInternalName(Integer.class);
		String objType3  = Type.getInternalName(Class.class);
		String ifaceType = Type.getInternalName(Comparable.class);

		// Descriptor *must* be used (vs. internal name)
		assertTrue(tv.isNarrowerThan(objType2, objType1));
		assertFalse(tv.isNarrowerThan(objType1, objType2));

		assertTrue(tv.isNarrowerThan(objType2, ifaceType));
		assertFalse(tv.isNarrowerThan(objType1, ifaceType));
		assertFalse(tv.isNarrowerThan(objType3, ifaceType));
	}

	@Test
	public void isNarrowerThanArray() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		String objType1 = Type.getInternalName(Object[][][].class);
		String objType2 = Type.getInternalName(Integer[][][].class);
		String objType3 = Type.getInternalName(Integer[][].class);

		assertTrue(tv.isNarrowerThan(objType2, objType1));
		assertFalse(tv.isNarrowerThan(objType1, objType2));

		assertFalse(tv.isNarrowerThan(objType3, objType1));
		assertFalse(tv.isNarrowerThan(objType1, objType3));
	}

	@Test
	public void isNarrowerThanNoClass() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		String intName = CONTROL_NAME.replace('.', '/');

		String objType   = Type.getInternalName(Object.class);
		String ifaceType = Type.getInternalName(Cloneable.class);

		assertTrue(tv.isNarrowerThan(intName, objType));
		assertFalse(tv.isNarrowerThan(objType, intName));

		assertTrue(tv.isNarrowerThan(intName, ifaceType));
		assertFalse(tv.isNarrowerThan(ifaceType, intName));
	}

	@Test
	public void isNarrowerThanList() {
		TypeVerifier tv = new TypeVerifier(controlNode);

		Object[] objs1 = { Opcodes.INTEGER, Type.getInternalName(Object.class)  };
		Object[] objs2 = { Opcodes.INTEGER, Type.getInternalName(Integer.class) };

		assertTrue(tv.isNarrowerThan(Arrays.asList(objs2), Arrays.asList(objs1)));
		assertFalse(tv.isNarrowerThan(Arrays.asList(objs1), Arrays.asList(objs2)));

		assertTrue(tv.isNarrowerThan(Collections.emptyList(), Collections.emptyList()));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void isNarrowerThanListDifferentSizes() {
		TypeVerifier tv = new TypeVerifier(controlNode);
		tv.isNarrowerThan(Collections.emptyList(), Arrays.asList((Object) Opcodes.LONG));
	}

	@Test
	public void isNarrowerThanMap() {
		TypeVerifier tv = new TypeVerifier(controlNode);

		Map<Integer, Object> writes = new TreeMap<Integer, Object>();
		Map<Integer, Object> reads  = new TreeMap<Integer, Object>();

		writes.put(1, Opcodes.INTEGER);
		writes.put(3, Type.getInternalName(Float.class));
		writes.put(4, Type.getInternalName(Comparable.class));

		reads.put(1, Opcodes.INTEGER);
		reads.put(3, Type.getInternalName(Number.class));
		reads.put(5, Type.getInternalName(Comparable.class));

		assertTrue(tv.isNarrowerThan(writes, reads, false));
		assertFalse(tv.isNarrowerThan(writes, reads, true));

		writes.put(5, Opcodes.NULL);
		assertTrue(tv.isNarrowerThan(writes, reads, false));
		assertTrue(tv.isNarrowerThan(writes, reads, true));
	}

	@Test
	public void isNarrowerThanAlternative() throws IOException {
		AnalyzedClassNode factNode = AnalyzedClassNode.readClass(Fact.class);
		String contName = CONTROL_NAME.replace('.', '/');

		TypeVerifier tv = new TypeVerifier(controlNode);
		assertFalse(tv.isNarrowerThan(contName, Type.getInternalName(Fact.class)));
		assertFalse(tv.isNarrowerThan(Type.getInternalName(Fact.class), contName));

		tv = new TypeVerifier(controlNode, factNode);
		assertTrue(tv.isNarrowerThan(contName, Type.getInternalName(Fact.class)));
		assertTrue(tv.isNarrowerThan(Type.getInternalName(Fact.class), contName));
	}

}
