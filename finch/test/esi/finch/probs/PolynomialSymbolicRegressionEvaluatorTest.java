package esi.finch.probs;

import static org.junit.Assert.*;

import org.junit.Test;

public class PolynomialSymbolicRegressionEvaluatorTest {

	@Test
	public void getValue() {
		PolynomialSymbolicRegressionEvaluator ev =
			new PolynomialSymbolicRegressionEvaluator();

		assertEquals(2.5 + (2.5 * (2.5 + 2.5 * 2.5)), ev.getValue(2.5, 3), 0);
	}

	@Test
	public void maxMatch() {
		PolynomialSymbolicRegressionEvaluator ev =
			new PolynomialSymbolicRegressionEvaluator();

		for (int i = 1;  i <= 9;  ++i)
			assertEquals(i, ev.maxMatch(2.5, ev.getValue(2.5, i)));
		assertEquals(0, ev.maxMatch(2.5, 5.0));
	}

}
