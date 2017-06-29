package esi.bc.flow;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.commons.Method;

import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.flow.test.Flow;
import esi.bc.flow.test.ReadsWrites;
import esi.bc.test.Fact;

public class BranchAnalyzerTest {

	private static AnalyzedMethodNode factMethod;
	private static AnalyzedMethodNode rwMethod;
	private static AnalyzedMethodNode flowMethod;
	private static AnalyzedMethodNode emptyMethod;

	@Before
	public void setUpBeforeClass() throws IOException {
		AnalyzedClassNode fc = AnalyzedClassNode.readClass(Fact.class, true);
		factMethod = fc.findMethod(new Method("fact", "(I)I"));
		assertNotNull(factMethod);

		AnalyzedClassNode rwc = AnalyzedClassNode.readClass(ReadsWrites.class, true);
		rwMethod = rwc.findMethod(new Method("foo", "()V"));
		assertNotNull(rwMethod);

		AnalyzedClassNode flc = AnalyzedClassNode.readClass(Flow.class, false);
		flowMethod = flc.findMethod(new Method("foo", "(J)V"));
		assertNotNull(flowMethod);

		emptyMethod = rwc.findMethod(new Method("empty", "()V"));
		assertEquals(1, emptyMethod.instructions.size());
	}

	@Test
	public void testBranchAnalyzerFact() {
		BranchAnalyzer ban = new BranchAnalyzer(factMethod, false);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban) {
			assertEquals(new Method("fact", "(I)I"), cs.method);
			assertTrue(0        <= cs.start);
			assertTrue(cs.start <= cs.end+1);
			assertTrue(cs.end   <= 15);

			secs.add(cs);
		}

		assertEquals(108, secs.size());
		assertEquals(legalBetaSections(factMethod), secs);
	}

	@Test
	public void testBranchAnalyzerFactReversed() {
		BranchAnalyzer ban = new BranchAnalyzer(factMethod, true);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban) {
			assertEquals(new Method("fact", "(I)I"), cs.method);
			assertTrue(0        <= cs.start);
			assertTrue(cs.start <= cs.end+1);
			assertTrue(cs.end   <= 15);

			secs.add(cs);
		}

		assertEquals(126, secs.size());
		assertEquals(legalAlphaSections(factMethod), secs);
	}

	@Test
	public void getSortedSections() {
		BranchAnalyzer ban = new BranchAnalyzer(factMethod, false);
		Map<Integer, List<CodeSection>> secs  = ban.getSortedSections();

		for (int key: secs.keySet())
			for (CodeSection sec: secs.get(key))
				assertEquals(key, sec.size());

		List<CodeSection> allSecs = new ArrayList<CodeSection>();
		for (List<CodeSection> subSecs: secs.values())
			allSecs.addAll(subSecs);
		Collections.sort(allSecs);

		List<CodeSection> check = legalBetaSections(factMethod);
		Collections.sort(check);

		assertEquals(check, allSecs);
	}

	@Test
	public void testBranchAnalyzerReadsWrites() {
		BranchAnalyzer ban = new BranchAnalyzer(rwMethod, false);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban)
			secs.add(cs);

		assertEquals(legalBetaSections(rwMethod), secs);
	}

	@Test
	public void testBranchAnalyzerReadsWritesReversed() {
		BranchAnalyzer ban = new BranchAnalyzer(rwMethod, true);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban)
			secs.add(cs);

		assertEquals(legalAlphaSections(rwMethod), secs);
	}

	@Test
	public void testBranchAnalyzerFlow() {
		BranchAnalyzer ban = new BranchAnalyzer(flowMethod, false);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban)
			secs.add(cs);

		assertEquals(legalBetaSections(flowMethod), secs);
	}

	@Test
	public void testBranchAnalyzerFlowReversed() {
		BranchAnalyzer ban = new BranchAnalyzer(flowMethod, true);

		List<CodeSection> secs = new ArrayList<CodeSection>();
		for (CodeSection cs: ban)
			secs.add(cs);

		assertEquals(legalAlphaSections(flowMethod), secs);
	}

	@Test(expected = NoSuchElementException.class)
	public void testBranchIterator() {
		Iterable<CodeSection> ban  = new BranchAnalyzer(factMethod, false);
		Iterator<CodeSection> iter = ban.iterator();

		while (iter.hasNext())
			iter.next();

		assertFalse(iter.hasNext());
		assertFalse(iter.hasNext());

		iter.next();
	}

	@Test(expected = NoSuchElementException.class)
	public void testBranchAnalyzerEmpty() {
		Iterable<CodeSection> ban  = new BranchAnalyzer(emptyMethod, false);
		Iterator<CodeSection> iter = ban.iterator();

		assertTrue(iter.hasNext());
		CodeSection cs = iter.next();
		assertEquals( 0, cs.start);
		assertEquals(-1, cs.end);

		assertFalse(iter.hasNext());
		iter.next();
	}

	@Test
	public void testBranchAnalyzerEmptyReversed() {
		Iterable<CodeSection> ban  = new BranchAnalyzer(emptyMethod, true);
		Iterator<CodeSection> iter = ban.iterator();

		assertTrue(iter.hasNext());
		CodeSection cs = iter.next();
		assertEquals( 0, cs.start);
		assertEquals(-1, cs.end);

		assertFalse(iter.hasNext());
	}

	@Test
	public void aggregateForwardIndexes() {
		int[] offsets = new int[19];
		Arrays.fill(offsets, -1);
		offsets [0] =  8;
		offsets [2] =  4;
		offsets [6] = 11;
		offsets[13] = 15;
		offsets[14] = 16;
		offsets[16] = 17;

		int[] newOffsets = offsets.clone();
		for (int i = 0;  i < newOffsets.length;  ++i)
			if (newOffsets[i] == -1)
				newOffsets[i] = i;
		newOffsets [0] = 11;
		newOffsets[13] = 17;
		newOffsets[14] = 17;

		BranchAnalyzer.aggregateForwardIndexes(offsets);
		assertArrayEquals(newOffsets, offsets);
	}

	@Test
	public void aggregateForwardIndexesNoNegative() {
		int[] offsets = new int[19];
		for (int i = 0;  i < offsets.length;  ++i)
			offsets[i] = i;

		offsets [0] =  8;
		offsets [2] =  4;
		offsets [6] = 11;
		offsets[13] = 15;
		offsets[14] = 16;
		offsets[16] = 17;

		int[] newOffsets = offsets.clone();
		newOffsets [0] = 11;
		newOffsets[13] = 17;
		newOffsets[14] = 17;

		BranchAnalyzer.aggregateForwardIndexes(offsets);
		assertArrayEquals(newOffsets, offsets);
	}

	@Test
	public void aggregateBackwardIndexes() {
		int[] offsets = new int[14];
		Arrays.fill(offsets, -1);
		offsets [5] =  2;
		offsets [8] =  3;
		offsets[12] = 10;

		int[] newOffsets = offsets.clone();
		for (int i = 0;  i < newOffsets.length;  ++i)
			if (newOffsets[i] == -1)
				newOffsets[i] = i;
		newOffsets[8] = 2;

		BranchAnalyzer.aggregateBackwardIndexes(offsets);
		assertArrayEquals(newOffsets, offsets);
	}

	@Test
	public void aggregateBackwardIndexesNoNegative() {
		int[] offsets = new int[14];
		for (int i = 0;  i < offsets.length;  ++i)
			offsets[i] = i;

		offsets [5] =  2;
		offsets [8] =  3;
		offsets[12] = 10;

		int[] newOffsets = offsets.clone();
		newOffsets[8] = 2;

		BranchAnalyzer.aggregateBackwardIndexes(offsets);
		assertArrayEquals(newOffsets, offsets);
	}

	@Test
	public void getBranchesCount() throws IOException {
		AnalyzedClassNode  flc = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedMethodNode flm = flc.findMethod(new Method("foo", "(J)V"));

		BranchAnalyzer ba     = new BranchAnalyzer(flm, false);
		Method         method = new Method("foo", "(J)V");
		CodeSection    cs     = new CodeSection(method, 3, 22);

		assertEquals(5, ba.getBranchesCount(cs));

		ba = new BranchAnalyzer(flm, true);
		cs = new CodeSection(method, 4, 21);

		assertEquals(5, ba.getBranchesCount(cs));
	}

	@Test
	public void extendRange() {
		int[] offsets     = { 0, 3, 2, 3, 5, 5, 6, 7 };
		int[] backOffsets = { 0, 1, 2, 3, 4, 1, 0, 7 };

		assertEquals( 0, BranchAnalyzer.extendRange(0, -1, offsets, backOffsets));
		assertEquals( 3, BranchAnalyzer.extendRange(0,  0, offsets, backOffsets));
		assertEquals( 5, BranchAnalyzer.extendRange(0,  3, offsets, backOffsets));
		assertEquals( 5, BranchAnalyzer.extendRange(1,  3, offsets, backOffsets));
		assertEquals(-1, BranchAnalyzer.extendRange(2,  3, offsets, backOffsets));
		assertEquals( 6, BranchAnalyzer.extendRange(0,  5, offsets, backOffsets));
		assertEquals(-1, BranchAnalyzer.extendRange(1,  5, offsets, backOffsets));
		assertEquals( 7, BranchAnalyzer.extendRange(0,  6, offsets, backOffsets));
		assertEquals(-1, BranchAnalyzer.extendRange(0,  7, offsets, backOffsets));
	}

	private static List<CodeSection> legalBetaSections(AnalyzedMethodNode mn) {
		List<CodeSection> cs = new ArrayList<CodeSection>();
		Method method = new Method(mn.name, mn.desc);
		int lastIndex = mn.getIndexBeforeReturn();

		for (int start = 0;  start <= lastIndex;  ++start)
			for (int end = start-1;  end <= lastIndex;  ++end) {
				boolean okSection = true;

				for (int index = start;  okSection && index <= end;  ++index)
					for (int branch: mn.getNextLabelIndexes(index))
						if (! (start <= branch  &&  branch <= end))
							okSection = false;

				if (okSection)
					cs.add(new CodeSection(method, start, end));
			}

		cs.add(new CodeSection(method, lastIndex+1, lastIndex));

		return cs;
	}

	private static List<CodeSection> legalAlphaSections(AnalyzedMethodNode mn) {
		List<CodeSection> cs = new ArrayList<CodeSection>();
		Method method = new Method(mn.name, mn.desc);
		int lastIndex = mn.getIndexBeforeReturn();

		for (int start = 0;  start <= lastIndex;  ++start)
			for (int end = start-1;  end <= lastIndex;  ++end) {
				boolean okSection = true;

				for (int index = 0;  okSection && index <= mn.getIndexBeforeReturn();  ++index)
					if (index < start  ||  index > end)
						for (int branch: mn.getNextLabelIndexes(index))
							if (! (branch < start  ||  branch > end))
								okSection = false;

				if (okSection)
					cs.add(new CodeSection(method, start, end));
			}

		cs.add(new CodeSection(method, lastIndex+1, lastIndex));

		return cs;
	}

}
