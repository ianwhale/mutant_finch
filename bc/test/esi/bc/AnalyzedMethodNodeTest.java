package esi.bc;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import esi.bc.flow.FrameActions;
import esi.bc.flow.test.Flow;
import esi.bc.test.Enums;
import esi.bc.test.Fact;
import esi.bc.test.PopDepths;
import esi.bc.xo.test.DecoherentVars;
import esi.util.Config;

public class AnalyzedMethodNodeTest {

	@Test
	public void testToString() throws Exception {
		dumpClass(Fact.class,           false);
		dumpClass(Fact.class,           true);
		dumpClass(Flow.class,           true);
		dumpClass(DecoherentVars.class, true);
		dumpClass(PopDepths.class,      false);
		dumpClass(Enums.class,          true);

		dumpClass("esi.bc.test.Control");
		dumpClass("esi.bc.xo.test.SimpleDecoherence");
		dumpClass("esi.bc.test.RegressionNew");
		dumpClass("esi.bc.flow.test.RegressionFlow");
		dumpClass("esi.bc.flow.test.Unification");
		dumpClass("esi.bc.flow.test.BackFlow");
		dumpClass("esi.bc.test.ComplexUnification");
	}

	@Test
	public void testException() throws IOException {
		AnalyzedClassNode cn = AnalyzedClassNode.readClass("esi.bc.flow.test.SimpleException", true, true);
		for (Object mn: cn.methods)
			System.out.println(mn);
	}

	@Test
	public void frameActionsPropagation() throws IOException {
		AnalyzedClassNode  cn = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedMethodNode mn = cn.findMethod(new Method("foo", "(J)V"));

		assertEquals(AbstractInsnNode.LABEL, mn.instructions.get(17).getType());
		assertEquals(AbstractInsnNode.FRAME, mn.instructions.get(18).getType());

		FrameActions faPreLabel  = mn.getFrameActions(16);
		FrameActions faLabel     = mn.getFrameActions(17);
		FrameActions faFrame     = mn.getFrameActions(18);
		FrameActions faPostFrame = mn.getFrameActions(19);

		// Check that ASTORE gets to keep its "after" frame data
		assertEquals("java/lang/String", faPreLabel.getVarsWrittenAlways().get(3));
		assertEquals("java/lang/Object", faPostFrame.getVarsRead().get(3));

		// Check that only stack and locals is copied from FrameData
		assertTrue(faLabel.getVarsRead()   .isEmpty());
		assertTrue(faLabel.getVarsWritten().isEmpty());
		assertTrue(faFrame.getVarsRead()   .isEmpty());
		assertTrue(faFrame.getVarsWritten().isEmpty());
		assertTrue(faLabel.getStackPops()  .isEmpty());
		assertTrue(faFrame.getStackPops()  .isEmpty());
		assertTrue(faLabel.getStackPushes().isEmpty());
		assertTrue(faFrame.getStackPushes().isEmpty());

		// Check that frame and label have Object in locals (not String)
		// NOTE: frame actions propagation mechanism was changed to frame
		// data propagation, and label thus has String in its frame data.
		FrameActions faFromFrame = new FrameActions(Arrays.asList(faFrame), faPostFrame, null);
		FrameActions faFromLabel = new FrameActions(Arrays.asList(faLabel), faFromFrame, null);

		faFromFrame.setFrameData(mn.getFrameData(18), mn.getFrameData(19+1));
		faFromLabel.setFrameData(mn.getFrameData(17), mn.getFrameData(19+1));

		assertEquals("java/lang/Object", faFromFrame.getVarsRead().get(3));
		assertEquals("java/lang/String", faFromLabel.getVarsRead().get(3));

		// Check that the first frame is ok
		FrameActions firstFrame = mn.getFrameActions(0);
		assertTrue(firstFrame.getVarsRead()   .isEmpty());
		assertTrue(firstFrame.getVarsWritten().isEmpty());
		assertTrue(firstFrame.getStackPops()  .isEmpty());
		assertTrue(firstFrame.getStackPushes().isEmpty());
	}

	private void dumpClass(Class<?> klass, boolean skipDebug) throws IOException {
		File outFile = new File(Config.DIR_OUT_TESTS,
				klass.getSimpleName() + (skipDebug ? "-nodebug.txt" : "-debug.txt"));
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

		ClassNode cn = AnalyzedClassNode.readClass(klass, skipDebug);
		dumpMethods(cn, out, skipDebug);
	}

	private void dumpClass(String className) throws Exception {
		String fileName = className.substring(className.lastIndexOf('.')+1) + ".txt";
		File   outFile  = new File(Config.DIR_OUT_TESTS, fileName);
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

		ClassNode cn = AnalyzedClassNode.readClass(className, true, true);
		dumpMethods(cn, out, true);

		ClassWriter wr = new ClassWriter(0);
		cn.accept(wr);
		saveBytes(Config.DIR_OUT_TESTS, className.substring(className.lastIndexOf('.')+1), wr.toByteArray());
	}

	/* private */ void dumpClass(URL url) throws IOException {
		String name = url.getFile();
		name = name.substring(name.lastIndexOf(File.separatorChar) + 1, name.lastIndexOf('.'));

		String fileName = name + ".txt";
		File   outFile  = new File(Config.DIR_OUT_TESTS, fileName);
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

		ClassNode cn = AnalyzedClassNode.readClass(url.openStream(), false, false);
		dumpMethods(cn, out, true);
	}

	private void dumpMethods(ClassNode cn, BufferedWriter out, boolean skipDebug) throws IOException {
		assertTrue(cn.methods.size() >= 2);

		boolean hasConstructor = false;
		for (Object mn : cn.methods) {
			assertTrue(mn instanceof AnalyzedMethodNode);
			AnalyzedMethodNode amn = (AnalyzedMethodNode) mn;

			if (amn.name.equals("<init>"))
				hasConstructor = true;

			assertEquals(amn.instructions.size()-(skipDebug ? 2 : 3), amn.getIndexBeforeReturn());
			verifyPopDepths(amn);

			out.write(amn.toString());
			out.newLine();
		}

		out.close();

		assertTrue(hasConstructor);
	}

	private void verifyPopDepths(AnalyzedMethodNode mn) {
		for (int i = 0;  i < mn.instructions.size();  ++i) {
			AbstractInsnNode insn   = mn.instructions.get(i);
			FrameActions     action = mn.getFrameActions(i);

			if (action != null) {
				List<Object> pops = action.getStackPops();

				if (insn.getOpcode() < 0)
					assertTrue(pops.isEmpty());
				// Try to avoid frame merges... (not guaranteed)
				else if (insn.getOpcode() != Opcodes.GOTO) {
					int popDepth = mn.maxStack;
					for ( ;  popDepth >= 0;  --popDepth)
						if (action.getStackPops(popDepth) != null)
							break;

					List<Object> stackBefore = action.getStackPops(popDepth);
					List<Object> stackAfter  = action.getStackPushes(popDepth);

					stackBefore = stackBefore.subList(0, stackBefore.size() - pops.size());
					stackAfter  = stackAfter.subList(0, stackBefore.size());

					assertEquals(stackBefore, stackAfter);
				}
			}
		}
	}

	public static class RegressionNew {
		public static void foo(int x) {
			new Integer(x == 0 ? 0 : 1);
		}
	}

	@Test
	public void asmRegressionNew() throws IOException {
		ClassReader cr = new ClassReader(RegressionNew.class.getName());
		TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		cr.accept(tcv, ClassReader.SKIP_DEBUG);
	}

	@Test
	public void asmRegressionFrame() throws IOException {
		ClassReader cr = new ClassReader("esi.bc.xo.test.SimpleDecoherence");
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cr.accept(cw, 0);

		ClassReader cr2 = new ClassReader(cw.toByteArray());
		TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		cr2.accept(tcv, 0);
	}

	private void saveBytes(File saveDir, String className, byte[] bytes) throws IOException {
		File output = new File(saveDir, className + ".class");
		OutputStream out = new BufferedOutputStream(
				new FileOutputStream(output));
		out.write(bytes);
		out.close();
	}

}
