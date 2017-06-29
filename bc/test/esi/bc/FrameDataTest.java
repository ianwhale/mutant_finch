package esi.bc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import esi.bc.FrameData;

/**
 * @author wolfsonk
 */
public class FrameDataTest {

	private static Map<Label, String> uninitializedTypes;
	private static AbstractInsnNode   nopInsn;
	private static Label              l1;
	private static Label              l2;
	private static Label              l3;
	private static List<Object>       from;
	private static Object[]           expectedArr;
	private static Object[]           frameArr;
	private static List<Object>		  emptyList;

	@BeforeClass
	public static void setUpBeforeClass() {
		uninitializedTypes = new HashMap<Label, String>();
		nopInsn            = new InsnNode(Opcodes.NOP);

		l1 = new Label();
		l2 = new Label();
		l3 = new Label();
		uninitializedTypes.put(l1, "pkg/Class1");
		uninitializedTypes.put(l2, "pkg/Class2");

		frameArr = new Object[] {
				Opcodes.TOP,
				Opcodes.INTEGER,
				Opcodes.FLOAT,
				Opcodes.DOUBLE,
				Opcodes.LONG,
				Opcodes.NULL,
				Opcodes.UNINITIALIZED_THIS,
				"pkg/Class",
				l1,
				l2
		};

		from = Arrays.asList(frameArr);

		expectedArr = new Object[] {
				Opcodes.TOP,
				Opcodes.INTEGER,
				Opcodes.FLOAT,
				Opcodes.DOUBLE,
				Opcodes.LONG,
				Opcodes.NULL,
				Opcodes.UNINITIALIZED_THIS,
				"pkg/Class",
				FrameData.UNINITIALIZED_PREFIX + l1 + "/pkg/Class1",
				FrameData.UNINITIALIZED_PREFIX + l2 + "/pkg/Class2"
		};

		emptyList = Collections.emptyList();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFrameDataNullFrames() {
		// Check constructor with null stack & locals
		new FrameData(null, null, uninitializedTypes, new LabelNode());
	}

	@Test
	public void testFrameDataWithFrames() {
		// Check constructor with stacks and locals
		List<Object> stack  = Arrays.asList(frameArr);
		List<Object> locals = Arrays.asList(frameArr);

		FrameData fd = new FrameData(stack,locals,uninitializedTypes,nopInsn);

		// Testing only size because copy is tested in another function
		assertEquals(stack .size(), fd.getStack() .size());
		assertEquals(locals.size(), fd.getLocals().size());
	}

	@Test
	public void testFrameDataParams() {
		Object[] objects = new Object[] {
				Opcodes.INTEGER, Opcodes.FLOAT,
				Opcodes.DOUBLE, Opcodes.TOP,
				"pkg/Class"
		};

		List<Object> params = Arrays.asList(objects);
		FrameData fd = new FrameData(params);

		assertTrue(fd.getStack().isEmpty());
		assertEquals(params, fd.getLocals());
		assertEquals(0, fd.getPopDepth());
		assertTrue(fd.getVarsRead().isEmpty());

		Set<Integer> set = fd.getVarsWritten();
		assertEquals(objects.length, set.size());
		for(int var: set)
			assertTrue(var >= 0  &&  var < objects.length);
	}

	@Test
	public void testFrameDataCopy1() {
		List<Object> stack  = Arrays.asList(frameArr);
		List<Object> locals = Arrays.asList(frameArr);

		int    dims = 2;
		String desc = "[[[[Ljava/lang/String;";

		AbstractInsnNode insn = new MultiANewArrayInsnNode(desc, dims);
		FrameData        fd   = new FrameData(stack, locals, uninitializedTypes, insn);
		assertEquals(dims, fd.getPopDepth());

		FrameData fdCopy = new FrameData(fd);
		assertEquals(fd.getStack(),  fdCopy.getStack());
		assertEquals(fd.getLocals(), fdCopy.getLocals());
		assertEquals(0, fdCopy.getPopDepth());
	}

	@Test
	public void testFrameDataCopy2() {
		AbstractInsnNode insn = new IincInsnNode(3, 2);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);

		FrameData fdCopy = new FrameData(fd);
		assertTrue(fdCopy.getVarsRead()   .isEmpty());
		assertTrue(fdCopy.getVarsWritten().isEmpty());
	}

	@Test
	public void testFrameDataPopDepthMultiANewArray() {
		// Check popDepth - MultiANewArray
		int    dims = 2;
		String desc = "[[[[Ljava/lang/String;";

		AbstractInsnNode insn = new MultiANewArrayInsnNode(desc, dims);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);
		assertEquals(dims, fd.getPopDepth());
	}

	@Test
	public void testFrameDataPopDepthInvoke() {
		// Check popDepth - static method invocation
		String desc          = "([Ljava/lang/String;II)V";
		int    expectedDepth = 3;

		AbstractInsnNode insn = new MethodInsnNode(Opcodes.INVOKESTATIC, "Class", "method", desc);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);
		assertEquals(expectedDepth, fd.getPopDepth());

		// Check popDepth - non-static method invocation
		expectedDepth = 4;
		insn = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "Class", "method", desc);
		fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);
		assertEquals(expectedDepth, fd.getPopDepth());

		insn = new MethodInsnNode(Opcodes.INVOKESPECIAL, "Class", "method", desc);
		fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);
		assertEquals(expectedDepth, fd.getPopDepth());

		insn = new MethodInsnNode(Opcodes.INVOKEINTERFACE, "Class", "method", desc);
		fd = new FrameData(emptyList, emptyList, uninitializedTypes, insn);
		assertEquals(expectedDepth, fd.getPopDepth());
	}

	@Test
	public void testFrameDataVarsNOP_INSN() {
		// Check varsRead, varsWritten
		FrameData fd = new FrameData(emptyList, emptyList, uninitializedTypes, nopInsn);

		assertTrue(fd.getVarsRead().isEmpty());
		assertTrue(fd.getVarsWritten().isEmpty());
	}

	@Test
	public void testFrameDataVarsIINC_INSN() {
		// Check varsRead, varsWritten
		int              var  = 3;
		AbstractInsnNode insn = new IincInsnNode(var, 2);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);

		assertEquals(1, fd.getVarsRead()   .size());
		assertEquals(1, fd.getVarsWritten().size());

		assertEquals(3, fd.getVarsRead().iterator().next().intValue());
		assertEquals(3, fd.getVarsWritten().iterator().next().intValue());
	}

	@Test
	public void testFrameDataVarsVAR_INSN_ILOAD() {
		// Check varsRead, varsWritten
		int              var  = 1;
		AbstractInsnNode insn = new VarInsnNode(Opcodes.ILOAD, var);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);

		assertEquals(1, fd.getVarsRead()   .size());
		assertEquals(0, fd.getVarsWritten().size());

		assertEquals(1, fd.getVarsRead().iterator().next().intValue());
	}

	@Test
	public void testFrameDataVarsVAR_INSN_DSTORE() {
		// Check varsRead, varsWritten
		int              var  = 4;
		AbstractInsnNode insn = new VarInsnNode(Opcodes.DSTORE, var);
		FrameData        fd   = new FrameData(emptyList, emptyList, uninitializedTypes, insn);

		assertEquals(0, fd.getVarsRead()   .size());
		assertEquals(2, fd.getVarsWritten().size());

		Iterator<Integer> iter = fd.getVarsWritten().iterator();
		assertEquals(4, iter.next().intValue());
		assertEquals(5, iter.next().intValue());
	}

	@Test
	public void testFrameDataVarsVAR_INSN_ALOAD() {
		// Check varsRead, varsWritten
		int              var    = 0;
		AbstractInsnNode insn   = new VarInsnNode(Opcodes.ALOAD, var);
		List<Object>     locals = new ArrayList<Object>(1);

		// Type of array to read in this instruction
		String type = "pkg/Class";
		locals.add(type);

		FrameData fd = new FrameData(emptyList, locals, uninitializedTypes, insn);

		assertEquals(1, fd.getVarsRead()   .size());
		assertEquals(0, fd.getVarsWritten().size());

		assertEquals(0, fd.getVarsRead().iterator().next().intValue());
	}

	@Test
	public void testFrameDataVarsVAR_INSN_ASTORE() {
		// Check varsRead, varsWritten
		int              var   = 5;
		AbstractInsnNode insn  = new VarInsnNode(Opcodes.ASTORE, var);
		List<Object>     stack = new ArrayList<Object>(1);

		// Type of object to write in this insn
		String type = "pkg/Class";
		stack.add(type);

		FrameData fd = new FrameData(stack, emptyList, uninitializedTypes, insn);

		assertEquals(0, fd.getVarsRead()   .size());
		assertEquals(1, fd.getVarsWritten().size());

		assertEquals(5, fd.getVarsWritten().iterator().next().intValue());
	}

	@Test
	public void testCopyFrameListWithLegalLists() {
		FrameData fd = new FrameData(emptyList, emptyList, uninitializedTypes, nopInsn);
		List<Object> to = fd.transformFrameList(from, uninitializedTypes);

		assertArrayEquals(expectedArr, to.toArray());
	}

	@Test(expected = RuntimeException.class)
	public void testCopyFrameListWithMissingLabel() {
		FrameData         fd   = new FrameData(emptyList, emptyList, uninitializedTypes, nopInsn);

		ArrayList<Object> from = new ArrayList<Object>();
		from.add(l3);

		fd.transformFrameList(from, uninitializedTypes);
	}

	@Test
	public void checkPopDepthsTable() {
		int[] popDepths = {
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	// 0
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	// 1
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,	// 2
				2,2,2,2,2,2,1,2,1,2,1,0,0,0,0,0,	// 3
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,	// 4
				4,3,4,3,3,3,3,1,2,1,2,3,2,3,4,2,	// 5
				2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,	// 6
				2,4,2,4,1,2,1,2,2,3,2,3,2,3,2,4,	// 7
				2,4,2,4,0,1,1,1,2,2,2,1,1,1,2,2,	// 8
				2,1,1,1,4,2,2,4,4,1,1,1,1,1,1,2,	// 9
				2,2,2,2,2,2,2,0,0,0,1,1,1,2,1,2,	// A
				1,0,0,0,1,0,0,0,0,0,0,0,1,1,1,1,	// B
				1,1,1,1,0,0,1,1,0,0,0,0,0,0,0,0,	// C
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	// D
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	// E
				0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0		// F
		};

		assertArrayEquals(popDepths, FrameData.popDepthPerOpcode);
	}

}
