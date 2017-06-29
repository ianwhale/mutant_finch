package esi.finch.probs;

/**
 * Two intertwined spirals.
 * Koza I, Chapter 17.3.
 *
 * @author Michael Orlov
 */
public class TwoSpirals {

	public int count(double[] xs, double[] ys) {
		assert xs.length == ys.length;

		int hits = 0;

		for (int i = 0;  i < xs.length;  ++i) {
			if (isFirst(xs[i], ys[i]))
				++hits;

			if (!isFirst(-xs[i], -ys[i]))
				++hits;
		}

		return hits;
	}

	public boolean isFirst(double x, double y) {
		double a = Math.hypot(x, y);
		double b = Math.atan2(y, x);
		double c = -a + b * 2;
		double d = -b * Math.sin(c) / a;
		double e = c - Math.cos(d) - 1.2345;

		boolean res = e >= 0;
		return res;
	}

	public boolean isFirstMath(double x, double y) {
		double a = Math.hypot(x, y);
		double b = Math.atan2(y, x);
		double c = -a + b * 2;
		double d = -b * Math.sin(c) / a;
		double e = c - Math.cos(d) - 1.2345;

		c = Math.abs(d + Math.acos(a) * Math.asin(d));
		e = e * Math.cbrt(c) - (Math.ceil(e) + Math.floor(e) + Math.rint(e) + Math.round(e)) * Math.cosh(a + Math.exp(Math.tan(d)));
		b = Math.log(a) + Math.log10(e) - Math.max(a, b) + Math.min(a, b);
		e = Math.pow(b + e, 2.3456);
		e *= Math.signum(e) + Math.sinh(b) - Math.tanh(a);
		e = Math.sqrt(e * e);

		boolean res = e >= 0;
		return res;
	}

}
