package esi.bc;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import esi.bc.test.Fact;

public class AnalyzedClassNodeTest {

	private static final String CONTROL_NAME = "esi.bc.test.Control";

	@Test
	public void testReadClassSimple() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(Fact.class);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", Type.INT_TYPE, new Type[]{ Type.INT_TYPE }));

		assertEquals(25, mn.instructions.size());
		assertEquals(22, mn.getIndexBeforeReturn());
	}

	@Test
	public void testReadClassSkipDebug() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(Fact.class, true);

		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));
		assertEquals(17, mn.instructions.size());
		assertEquals(15, mn.getIndexBeforeReturn());

		mn = cn.findMethod(new Method("<init>", "()V"));
		assertEquals(3, mn.instructions.size());
		assertEquals(1, mn.getIndexBeforeReturn());
	}

	@Test
	public void testReadClassRecomputeFrames() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(Fact.class, false, true);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));

		assertEquals(25, mn.instructions.size());
	}

	@Test
	public void testReadClassSkipDebugRecomputeFrames() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(Fact.class, true, true);
		AnalyzedMethodNode mn = cn.findMethod(new Method("fact", "(I)I"));

		assertEquals(17, mn.instructions.size());
	}

	@Test
	public void testReadClassRecomputeFramesJasmin() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(CONTROL_NAME, false, true);
		AnalyzedMethodNode mn = cn.findMethod(new Method("main", "([Ljava/lang/String;)V"));
		mn.toString();
	}

	@Test(expected = NullPointerException.class)
	public void testReadClassNoRecomputeFramesJasmin() throws IOException {
		AnalyzedClassNode.readClass(CONTROL_NAME, false, false);
	}

}
