package esi.bc.test;

public class PopDepths {

	static float xx;
		   float yy;

	static double qq;
		   double rr;

	public void foo() {
		// MULTIANEWARRAY and INVOKEVIRTUAL
		new Object[4][2][][][].clone();

		// INVOKESPECIAL
		prv(3);
		super.hashCode();
		new Integer(2);

		// INVOKESTATIC
		prv2(5);

		// INVOKEINTERFACE
		Comparable<Integer> y = 0;
		y.compareTo(1);

		// AALOAD, LALOAD, AASTORE, LASTORE
		long[][] x = new long[5][6];
		x[3][4] = x[1][2];
		// NEWARRAY, FASTORE
		(new float[7])[6] = x[2][1];

		// PUTSTATIC, PUTFIELD
		xx = 1;
		yy = 2;
		qq = 3;
		rr = 4;
	}

	private void prv(int i) {
	}

	private static int prv2(long j) {
		return (int) j;
	}

}
