package esi.finch.probs;

import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleSymbolicRegressionTest {

	@Test
	public void simpleRegressionWorst() {
		// (EXP (- (% X (- X (SIN X))) (RLOG (RLOG (* X X)))))
		SimpleSymbolicRegression ssr = new SimpleSymbolicRegression();
		assertEquals(Double.POSITIVE_INFINITY, ssr.simpleRegression(1).doubleValue(), 0);

		// e^(2/(2-sin(2)) - ln(ln(2*2)))+cos(1)
		double expy2 = 5.05368179;
		double y2    = ssr.simpleRegression(2).doubleValue();
		assertEquals(expy2, y2, 0.00000001);
	}

}
