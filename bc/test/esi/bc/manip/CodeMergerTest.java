package esi.bc.manip;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.thoughtworks.xstream.XStream;

import esi.bc.AnalyzedClassNode;
import esi.bc.BytesClassLoader;
import esi.bc.flow.CodeSection;
import esi.bc.flow.test.Flow;
import esi.bc.test.Fact;
import esi.util.Config;

public class CodeMergerTest {

	private static final String CONTROL_NAME = "esi.bc.test.Control";

	private static AnalyzedClassNode factNode;
	private static AnalyzedClassNode factNodeNoDebug;
	private static int               FACT_INSTRS;
	private static int               FACT_INSTRS_NODEBUG;

	private static XStream           xstream;
	private static String            factNodeRep;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		factNode        = AnalyzedClassNode.readClass(Fact.class);
		factNodeNoDebug = AnalyzedClassNode.readClass(Fact.class, true);

		// Locate source method node
		Method method = new Method("fact", "(I)I");
		MethodNode srcMethod = factNode.findMethod(method);
		FACT_INSTRS = srcMethod.instructions.size();
		// 23: IRETURN, 24: Label
		assertEquals(25, FACT_INSTRS);

		srcMethod = factNodeNoDebug.findMethod(method);
		FACT_INSTRS_NODEBUG = srcMethod.instructions.size();
		assertEquals(17, FACT_INSTRS_NODEBUG);

		xstream     = new XStream();
		factNodeRep = xstream.toXML(factNode);
	}

	@Test
	public void testCodeMerger() throws Exception {
		// dest=[fact(n-1)], src=[n]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 12, 16);
		CodeSection src  = new CodeSection(fact, 11, 11);

		String newName = Fact.class.getName() + "Test";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);

		BytesClassLoader  loader          = merger.getClassLoader(Config.DIR_OUT_TESTS);
		Class<?>          mergedClass     = loader.loadClass(newName);
		AnalyzedClassNode mergedClassNode = merger.getClassNode();

		assertEquals(newName,                   mergedClass.getName());
		assertEquals(newName.replace('.', '/'), mergedClassNode.name);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(5 * 5, method.invoke(instance, 5));

		// Check that ClassNode of factNode wasn't changed
		assertEquals(factNodeRep, xstream.toXML(factNode));
	}

	@Test
	public void testCodeMergerEmptyDest() throws Exception {
		// dest=[just after *fact(n-1)], src=[*fact(n-1)]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 18, 17);	// insert before 18
		CodeSection src  = new CodeSection(fact, 12, 17);

		String newName = Fact.class.getName() + "DoubleTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);

		Class<?>  mergedClass = merger.getClassLoader(Config.DIR_OUT_TESTS).loadClass(newName);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(1658880, method.invoke(instance, 5));

		// Check that ClassNode of factNode wasn't changed
		assertEquals(factNodeRep, xstream.toXML(factNode));
	}

	@Test
	public void testCodeMergerEmptyDestStart() throws Exception {
		// dest=[before code start], src=[iconst_1]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 0, -1);
		CodeSection src  = new CodeSection(fact, 3,  3);

		// Leaves extra value on stack after return (this is ok)
		String newName = Fact.class.getName() + "EmptyStartTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);

		Class<?> mergedClass = merger.getClassLoader().loadClass(newName);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(120, method.invoke(instance, 5));

		// Check that ClassNode of factNode wasn't changed
		assertEquals(factNodeRep, xstream.toXML(factNode));
	}

	@Test
	public void testCodeMergerEmptySrc() throws Exception {
		// dest=[*fact(n-1)], src=[]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 12, 17);
		CodeSection src  = new CodeSection(fact, 15, 14);

		// Leaves extra value on stack after return (this is ok)
		String newName = Fact.class.getName() + "EmptySrcTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);

		Class<?> mergedClass = merger.getClassLoader().loadClass(newName);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(5, method.invoke(instance, 5));
	}

	@Test
	public void testCodeMergerEmptyDestEnd() throws Exception {
		// dest=[just after last label], src=[iconst_1, istore_2]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, FACT_INSTRS, FACT_INSTRS-1);
		CodeSection src  = new CodeSection(fact, 3, 4);

		// [iconst_1, istore_2] convert to [nop, athrow] since they are uncreachable
		String newName = Fact.class.getName() + "EmptyEndTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);

		Class<?> mergedClass = merger.getClassLoader().loadClass(newName);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(120, method.invoke(instance, 5));
	}

	@Test(expected = LinkageError.class)
	public void testCodeMergerSameName() throws Exception {
		// dest=[fact(n-1)], src=[n]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 12, 16);
		CodeSection src  = new CodeSection(fact, 11, 11);

		CodeMerger merger  = new CodeMerger(Fact.class.getName(), Fact.class.getName(), factNode, factNode, dest, src);
		merger.getClassLoader();
	}

	@Test
	public void testCodeMergerFull() throws Exception {
		ClassNode classNodeDest = factNode;
		ClassNode classNodeSrc  = AnalyzedClassNode.readClass(Fact.class);

		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection full = new CodeSection(fact, 0, FACT_INSTRS-1);

		String newName = Fact.class.getName() + "CopyTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), classNodeDest, classNodeSrc, full, full);

		// Check verification
		Class<?> mergedClass = merger.getClassLoader(Config.DIR_OUT_TESTS).loadClass(newName);
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertEquals(120, method.invoke(instance, 5));

		// Check that ClassNode of factNode wasn't changed
		assertEquals(factNodeRep, xstream.toXML(factNode));
	}

	@Test
	public void testCodeMergerFullNoDebug() throws Exception {
		ClassNode classNodeDest = factNodeNoDebug;
		ClassNode classNodeSrc  = factNodeNoDebug;

		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection full = new CodeSection(fact, 0, FACT_INSTRS_NODEBUG-1);

		String newName = Fact.class.getName() + "NoDebugCopyTest";
		CodeMerger merger = new CodeMerger(newName, Fact.class.getName(), classNodeDest, classNodeSrc, full, full);

		// Check verification
		Class<?> mergedClass = merger.getClassLoader(Config.DIR_OUT_TESTS).loadClass(newName);
		Object instance = mergedClass.newInstance();

		// Invoke FactTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertEquals(120, method.invoke(instance, 5));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCodeMergerNoDestMethod() throws IOException {
		// dest=[all], src=[all]
		Method      goodFact = new Method("fact", "(I)I");
		Method      badFact  = new Method("fact", "(I)V");
		CodeSection goodFull = new CodeSection(goodFact, 0, FACT_INSTRS-1);
		CodeSection badFull  = new CodeSection(badFact,  0, FACT_INSTRS-1);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, badFull, goodFull);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCodeMergerNoSrcMethod() throws IOException {
		// dest=[all], src=[all]
		Method      badFact  = new Method("fact", "(I)V");
		Method      goodFact = new Method("fact", "(I)I");
		CodeSection badFull  = new CodeSection(badFact,  0, FACT_INSTRS-1);
		CodeSection goodFull = new CodeSection(goodFact, 0, FACT_INSTRS-1);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, goodFull, badFull);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeMergerDestOutOfRange1() throws Exception {
		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, FACT_INSTRS, FACT_INSTRS);
		CodeSection src  = new CodeSection(fact, 0, FACT_INSTRS-1);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeMergerDestOutOfRange2() throws Exception {
		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, FACT_INSTRS/2, FACT_INSTRS);
		CodeSection src  = new CodeSection(fact, 0, FACT_INSTRS-1);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeMergerSrcOutOfRange1() throws Exception {
		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 0, FACT_INSTRS-1);
		CodeSection src  = new CodeSection(fact, FACT_INSTRS, FACT_INSTRS);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeMergerSrcOutOfRange2() throws Exception {
		// dest=[all], src=[all]
		Method      fact = new Method("fact", "(I)I");
		CodeSection dest = new CodeSection(fact, 0, FACT_INSTRS-1);
		CodeSection src  = new CodeSection(fact, FACT_INSTRS/2, FACT_INSTRS);

		String newName = Fact.class.getName() + "CopyTest";
		new CodeMerger(newName, Fact.class.getName(), factNode, factNode, dest, src);
	}

	@Test
	public void duplicateClassNode() throws IOException {
		duplicateClassNode(AnalyzedClassNode.readClass(Flow.class));
		duplicateClassNode(AnalyzedClassNode.readClass(Flow.class, true));
		duplicateClassNode(AnalyzedClassNode.readClass(Flow.class, true,  true));
		duplicateClassNode(AnalyzedClassNode.readClass(Flow.class, false, true));

		duplicateClassNode(AnalyzedClassNode.readClass(CONTROL_NAME, true, true));
	}

	private void duplicateClassNode(ClassNode cn) throws IOException {
		ClassNode dupCn = CodeMerger.duplicateClassNode(cn);

		assertEquals(cn.name,           dupCn.name);
		assertEquals(cn.methods.size(), dupCn.methods.size());

		for (int i = 0;  i < cn.methods.size();  ++i) {
			MethodNode mn    = (MethodNode) cn.methods.get(i);
			MethodNode dupMn = (MethodNode) dupCn.methods.get(i);

			assertEquals(mn.name,                dupMn.name);
			assertEquals(mn.maxLocals,           dupMn.maxLocals);
			assertEquals(mn.maxStack,            dupMn.maxStack);
			assertEquals(mn.instructions.size(), dupMn.instructions.size());

			for (int j = 0;  j < mn.instructions.size();  ++j) {
				AbstractInsnNode insn    = mn.instructions.get(j);
				AbstractInsnNode dupInsn = dupMn.instructions.get(j);

				assertEquals(insn.getType(),   dupInsn.getType());
				assertEquals(insn.getOpcode(), dupInsn.getOpcode());
				assertNotSame(insn, dupInsn);
			}
		}
	}

}
