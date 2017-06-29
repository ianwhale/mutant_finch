package esi.bc.flow;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.test.Flow;
import esi.bc.test.Fact;
import esi.bc.xo.test.DecoherentVars;
import esi.util.Config;

public class CodeAccessesTest {

	private static final String			REGRESSION_FLOW    = "esi.bc.flow.test.RegressionFlow";
	private static final String			REGRESSION_BKFLOW  = "esi.bc.flow.test.BackFlow";
	private static final String			REGRESSION_ARSUM   = "ArraySum_G1_T0_571.class";

	private static String				factName;
	private static AnalyzedMethodNode	factMethod;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		factName = Type.getInternalName(Fact.class);

		AnalyzedClassNode factClass = AnalyzedClassNode.readClass(Fact.class);
		factMethod = factClass.findMethod(new Method("fact", Type.INT_TYPE, new Type[]{ Type.INT_TYPE }));
	}

	@Test
	public void testCodeAccesses() {
		new CodeAccesses(factMethod);
	}

	@Test
	public void getSectionFact1() {
		// Example 6-15 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(7, 18);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();
		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(2, reads.size());
		assertEquals(factName,        reads.get(0));
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionFact2() {
		// Example 8-15 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(13, 21);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, factName), fa.getStackPops());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(2, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();
		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(1, reads.size());
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionFact3() {
		// Example 3-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(8, 15);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPops());
		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, factName, Opcodes.INTEGER), fa.getStackPushes());
		assertEquals(1, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();

		assertEquals(2, reads.size());
		assertEquals(factName,        reads.get(0));
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertTrue(fa.getVarsWritten()      .isEmpty());
		assertTrue(fa.getVarsWrittenAlways().isEmpty());
	}

	@Test
	public void getSectionFact3minPopDepthTooBig() {
		// Example 3-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(8, 15);

		// There are 3 pushed but popDepth=2 should still result in null
		assertNull(fa.getStackPops  (fa.popDepth + 1));
		assertNull(fa.getStackPushes(fa.popDepth + 1));
		assertEquals(1, fa.popDepth);
	}

	@Test
	public void getSectionFact4() {
		// Example 14-16 from ESI paper (excluding return)
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(17, 22);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, Opcodes.INTEGER), fa.getStackPops());
		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPushes());
		assertEquals(2, fa.popDepth);

		assertTrue(fa.getVarsRead().isEmpty());

		Map<Integer, Object> writes  = fa.getVarsWritten();

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionFact4minPopDepthTooSmall() {
		// Example 14-16 from ESI paper (excluding return)
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(17, 22);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, Opcodes.INTEGER), fa.getStackPops(fa.popDepth - 1));
		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPushes(fa.popDepth - 1));
		assertEquals(2, fa.popDepth);
	}

	@Test
	public void getSectionFact5() {
		// Offsets 9-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(14, 15);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPops());
		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPushes());
		assertEquals(1, fa.popDepth);

		assertTrue(fa.getVarsRead()         .isEmpty());
		assertTrue(fa.getVarsWritten()      .isEmpty());
		assertTrue(fa.getVarsWrittenAlways().isEmpty());
	}

	@Test
	public void getSectionFact5minPopDepthExtra1() {
		// Offsets 9-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(14, 15);

		assertEquals(Arrays.<Object> asList(factName, Opcodes.INTEGER), fa.getStackPops(fa.popDepth + 1));
		assertEquals(Arrays.<Object> asList(factName, Opcodes.INTEGER), fa.getStackPushes(fa.popDepth + 1));
		assertEquals(1, fa.popDepth);
	}

	@Test
	public void getSectionFact5minPopDepthExtra2() {
		// Offsets 9-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(14, 15);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, factName, Opcodes.INTEGER), fa.getStackPops(fa.popDepth + 2));
		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER, factName, Opcodes.INTEGER), fa.getStackPushes(fa.popDepth + 2));
		assertEquals(1, fa.popDepth);
	}

	@Test
	public void getSectionFact5minPopDepthExtra3tooBig() {
		// Offsets 9-10 from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(14, 15);

		assertNull(fa.getStackPops  (fa.popDepth + 3));
		assertNull(fa.getStackPushes(fa.popDepth + 3));
	}

	@Test
	public void getSectionFact6maybeWrite() {
		// Example 3-15 from ESI paper
		// Offsets 3-(end of 15) from ESI paper
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(8, 19);

		assertEquals(Arrays.<Object> asList(Opcodes.INTEGER), fa.getStackPops());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(1, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();
		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(2, reads.size());
		assertEquals(factName, reads.get(0));
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertTrue(fa.getVarsWrittenAlways().isEmpty());
	}

	@Test
	public void getSectionFactParams() {
		// Example 0-3 from ESI paper, with params
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(0, 8, true);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		assertTrue(fa.getVarsRead().isEmpty());

		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(3, writes.size());
		assertEquals(factName,        writes.get(0));
		assertEquals(Opcodes.INTEGER, writes.get(1));
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionFactNoParams() {
		// Example 0-3 from ESI paper, without params
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(0, 9, false);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();
		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(1, reads.size());
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionDecoherence() throws IOException {
		AnalyzedClassNode alphaClassNode = AnalyzedClassNode.readClass(DecoherentVars.class, true);
		Method method = new Method("foo", "()V");
		CodeAccesses ca = new CodeAccesses(alphaClassNode.findMethod(method));
		FrameActions fa = ca.getSection(7, 14);

		Map<Integer, Object> reads   = fa.getVarsRead();
		Map<Integer, Object> writes  = fa.getVarsWritten();
		Map<Integer, Object> writesA = fa.getVarsWrittenAlways();

		assertTrue(reads.isEmpty());

		assertEquals(2, writes.size());
		assertEquals(FrameActions.BOGUS, writes.get(2));
		assertEquals(Opcodes.INTEGER,    writes.get(3));

		assertEquals(1, writesA.size());
		assertEquals(Opcodes.INTEGER,    writes.get(3));
	}

	@Test
	public void getSectionEmptyParams() {
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(0, -1, true);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		assertTrue(fa.getVarsRead().isEmpty());

		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(2, writes.size());
		assertEquals(factName,        writes.get(0));
		assertEquals(Opcodes.INTEGER, writes.get(1));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionEmptyNoParams() {
		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(9, 8, false);

		assertTrue(fa.getStackPops()        .isEmpty());
		assertTrue(fa.getStackPushes()      .isEmpty());
		assertEquals(0, fa.popDepth);
		assertTrue(fa.getVarsRead()         .isEmpty());
		assertTrue(fa.getVarsWritten()      .isEmpty());
		assertTrue(fa.getVarsWrittenAlways().isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getSectionNotStartParams() {
		new CodeAccesses(factMethod).getSection(1, 1, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getSectionStartBeforeEnd() {
		new CodeAccesses(factMethod).getSection(2, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getSectionTooBig() {
		new CodeAccesses(factMethod).getSection(0, factMethod.instructions.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getSectionStartNegative() {
		new CodeAccesses(factMethod).getSection(-1, 5);
	}

	@Test
	public void getSectionEndUnreachable() {
		FrameActions fa = new CodeAccesses(factMethod).getSection(0, factMethod.instructions.size()-1);
		assertNull(fa);
	}

	@Test
	public void getSectionEndNextUnreachable() {
		FrameActions fa = new CodeAccesses(factMethod).getSection(0, factMethod.instructions.size()-2);
		assertNull(fa);
	}

	@Test
	public void getSectionRegression1() throws IOException {
		// A bug in CodeAccesses - actions table should be of instructions.size()
		AnalyzedClassNode  factClass  = AnalyzedClassNode.readClass(Fact.class, true);
		AnalyzedMethodNode factMethod = factClass.findMethod(new Method("fact", "(I)I"));

		CodeAccesses  ca = new CodeAccesses(factMethod);
		FrameActions fa = ca.getSection(0, 4);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		Map<Integer, Object> reads  = fa.getVarsRead();
		Map<Integer, Object> writes = fa.getVarsWritten();

		assertEquals(1, reads.size());
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));

		assertEquals(writes, fa.getVarsWrittenAlways());
	}

	@Test
	public void getSectionRegression2() throws IOException {
		// A bug in CodeAccesses - wrong pop depth is calculated due to control flow
		AnalyzedClassNode cn = AnalyzedClassNode.readClass(REGRESSION_FLOW, false, true);

		Method method = new Method("foo", "()V");

		CodeAccesses ca = new CodeAccesses(cn.findMethod(method));

		FrameActions fa1 = ca.getSection(2, 8);
		FrameActions fa2 = ca.getSection(5, 9);

		assertEquals(fa1.getStackPops(),   fa2.getStackPops());
		assertEquals(fa1.getStackPushes(), fa2.getStackPushes());
	}

	@Test
	public void getSectionRegression3() throws IOException {
		AnalyzedClassNode  cn     = AnalyzedClassNode.readClass(Flow.class, true, true);
		Method             method = new Method("goo", "(DD)Z");
		AnalyzedMethodNode mn     = cn.findMethod(method);

		assertEquals(52, mn.instructions.size());

		CodeAccesses ca = new CodeAccesses(mn);
		FrameActions fa = ca.getSection(33, 41);

		assertNull(fa);
	}

	@Test
	public void getSectionRegression4a() throws IOException {
		// Wrong calculation of W! in getSection
		AnalyzedClassNode  cn     = AnalyzedClassNode.readClass(REGRESSION_BKFLOW, false, true);
		Method             method = new Method("foo", "()V");
		AnalyzedMethodNode mn     = cn.findMethod(method);
		assertEquals(8, mn.instructions.size());

		CodeAccesses ca = new CodeAccesses(mn);
		FrameActions fa = ca.getSection(5, 6);

		assertTrue(fa.getStackPops()  .isEmpty());
		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(0, fa.popDepth);

		Map<Integer, Object> reads        = fa.getVarsRead();
		Map<Integer, Object> writes       = fa.getVarsWritten();
		Map<Integer, Object> writesAlways = fa.getVarsWrittenAlways();

		assertEquals(1, reads.size());
		assertEquals(Opcodes.INTEGER, reads.get(1));

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(1));

		assertTrue(writesAlways.isEmpty());
	}

	@Test
	public void getSectionRegression4b() throws IOException {
		// Wrong calculation of W! in getSection
		AnalyzedClassNode  cn     = AnalyzedClassNode.readClass(REGRESSION_BKFLOW, false, true);
		Method             method = new Method("foo", "()V");
		AnalyzedMethodNode mn     = cn.findMethod(method);
		assertEquals(8, mn.instructions.size());

		CodeAccesses ca = new CodeAccesses(mn);
		FrameActions fa = ca.getSection(6, 6);

		List<Object> pops = fa.getStackPops();

		assertEquals(1, pops.size());
		assertEquals(Opcodes.INTEGER, pops.get(0));

		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(1, fa.popDepth);

		Map<Integer, Object> reads        = fa.getVarsRead();
		Map<Integer, Object> writes       = fa.getVarsWritten();
		Map<Integer, Object> writesAlways = fa.getVarsWrittenAlways();

		assertTrue(reads.isEmpty());

		assertEquals(1, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(1));

		assertTrue(writesAlways.isEmpty());
	}

	public void getSectionRegression5() throws IOException {
		AnalyzedClassNode  cn     = AnalyzedClassNode.readClass(Config.DIR_IN_TESTS.resolve(REGRESSION_ARSUM).openStream(), false, false);
		Method             method = new Method("sumlist", "([I)I");
		AnalyzedMethodNode mn = cn.findMethod(method);

		assertEquals(49, mn.instructions.size());

		CodeAccesses ca = new CodeAccesses(mn);
		FrameActions fa = ca.getSection(30, 33);

		List<Object> pops = fa.getStackPops();

		assertEquals(4, pops.size());
		assertEquals(Opcodes.INTEGER, pops.get(0));
		assertEquals(Opcodes.INTEGER, pops.get(1));
		assertEquals(Opcodes.INTEGER, pops.get(2));
		assertEquals(Opcodes.INTEGER, pops.get(3));

		assertTrue(fa.getStackPushes().isEmpty());
		assertEquals(4, fa.popDepth);

		Map<Integer, Object> reads        = fa.getVarsRead();
		Map<Integer, Object> writes       = fa.getVarsWritten();
		Map<Integer, Object> writesAlways = fa.getVarsWrittenAlways();

		assertEquals(4, reads.size());
		assertEquals(Type.getDescriptor(int[].class), reads.get(1));
		assertEquals(Opcodes.INTEGER, reads.get(2));
		assertEquals(Opcodes.INTEGER, reads.get(3));
		assertEquals(Opcodes.INTEGER, reads.get(4));

		assertEquals(2, writes.size());
		assertEquals(Opcodes.INTEGER, writes.get(2));
		assertEquals(Opcodes.INTEGER, writes.get(4));

		assertTrue(writesAlways.isEmpty());
	}

}
