package esi.finch.probs;

import java.math.BigInteger;

public class Biggest {

	public float foo() {
		float x = 3;

		float y = x - 5;

		if (y > 0)
			x = sumsq((long) x, (long) (y+x));
		else {
			float z = (float) Math.pow(y, x);
			x = z / y;
		}

		return x;
	}

	public long sumsq(long x, long y) {
		return x*x + y*y;
	}

	public Number toInt(double x, int radix) {
		String radixRep = String.valueOf(radix);
		Number v = new Double(x);

		if (Math.abs(x) > Integer.MAX_VALUE) {
			BigInteger b = new BigInteger("1");

			while (v.doubleValue() >= 1) {
				int rem = (int) (v.doubleValue() % radix);

				v = new Double(v.doubleValue() / radix);
				b = b.multiply(new BigInteger(radixRep)).add(new BigInteger(String.valueOf(rem)));
			}

			v = b;
		}
		else
			v = new Integer(v.intValue());

		return v;
	}

}
