package esi.bc.flow.test;

public class Flow {

	// Explicit offsets are used in CompatibleCrossoverTest
	public void foo(long x) {
		Object z;

		if (x > 0)
			z = new Flow();
		else
			z = new String();

		switch (z.hashCode()) {
		case 5:
			z = new Integer(1);

			if (z == null)
				throw new RuntimeException();
		case 6:
		case 7:
			z = null;
			break;

		default:
			z = new Object();
		}

		x = z.hashCode();
	}

	// Explicit offsets are used in CodeAccessesTest
	public boolean goo(double x, double y) {
		boolean res;

		if (x <= y) {
			double z = (x + Math.sin(y)) * (x - Math.cos(y));
			res = z <= x / (y + 0.123);
		}
		else {
			res = (x - y) <= 0.245;
		}

		return res;
	}

}
