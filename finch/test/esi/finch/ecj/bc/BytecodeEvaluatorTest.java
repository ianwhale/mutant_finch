package esi.finch.ecj.bc;

import static org.junit.Assert.*;

import org.junit.Test;

import esi.finch.ecj.bc.BytecodeEvaluator.MatchResult;
import esi.finch.ecj.bc.BytecodeEvaluator.Result;

public class BytecodeEvaluatorTest {

	@Test
	public void testResult() {
		Result res = new Result(1.23f, true);
		assertEquals(1.23f, res.fitness, 0);
		assertTrue(res.ideal);
	}

	@Test
	public void testResultNaN() {
		Result res = new Result(Float.NaN, false);
		assertEquals(-Float.MAX_VALUE, res.fitness, 0);
		assertFalse(res.ideal);
	}

	@Test
	public void testInfinite() {
		Result res = new Result(Float.POSITIVE_INFINITY, true);
		assertEquals(-Float.MAX_VALUE, res.fitness, 0);
		assertTrue(res.ideal);

		res = new Result(Float.NEGATIVE_INFINITY, false);
		assertEquals(-Float.MAX_VALUE, res.fitness, 0);
		assertFalse(res.ideal);
	}

	@Test
	public void testMatchResult() {
		MatchResult res = new MatchResult(1.23f);
		assertEquals(1.23f, res.fitnessDiff, 0);
		assertTrue(res.isWin());
		assertFalse(res.isLoss());
		assertFalse(res.isDraw());

		res = new MatchResult(-0.23f);
		assertFalse(res.isWin());
		assertTrue(res.isLoss());
		assertFalse(res.isDraw());

		res = new MatchResult(-0.0f);
		assertFalse(res.isWin());
		assertFalse(res.isLoss());
		assertTrue(res.isDraw());
	}

}
