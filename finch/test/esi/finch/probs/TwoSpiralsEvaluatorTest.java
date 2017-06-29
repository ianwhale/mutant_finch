package esi.finch.probs;

import static org.junit.Assert.*;

import org.junit.Test;

public class TwoSpiralsEvaluatorTest {

	@Test
	public void getXY() {
		TwoSpiralsEvaluator ev = new TwoSpiralsEvaluator();

		assertEquals(0,      ev.getX(0), 1e-12);
		assertEquals(1.0/13, ev.getY(0), 1e-12);

		assertEquals(0, ev.getX(96), 1e-12);
		assertEquals(1, ev.getY(96), 1e-12);

		double r5 = 13.0 /104;
		double a5 = 5 * Math.PI / 16 + Math.PI / 2;

		assertEquals(r5 * Math.cos(a5), ev.getX(5), 1e-12);
		assertEquals(r5 * Math.sin(a5), ev.getY(5), 1e-12);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void limits() {
		new TwoSpiralsEvaluator().getX(97);
	}

}
