package esi.finch.probs;

/**
 * Simple Symbolic Regression.
 * Koza I, Chapter 7.3.
 *
 * <ul>
 * <li> Function set: +, -, *, %, SIN, COS, EXP, RLOG
 * <li> Terminal set: X
 * <li> Target:       X^4 + X^3 + X^2 + X
 * </ul>
 *
 * @author Michael Orlov
 */
public class SimpleSymbolicRegression {

	/**
	 * Implements worst generation-0 individual + COS(1).
	 *
	 * <p> Archetypal individual:
	 * (EXP (- (% X (- X (SIN X))) (RLOG (RLOG (* X X)))))
	 *
	 * @param num input (X)
	 * @return regression result
	 */
	public Number simpleRegression(Number num) {
		double x = num.doubleValue();

		double llsq = Math.log(Math.log(x*x));
		double dv = x / (x - Math.sin(x));
		double worst = Math.exp(dv - llsq);

		return Double.valueOf(worst + Math.cos(1));
	}

	public static void main(String[] args) {
		SimpleSymbolicRegression instance = new SimpleSymbolicRegression();

		for (int i = 1;  i <= 10;  ++i)
			System.out.println(instance.simpleRegression(i));
	}

}
