package esi.bc.manip.test;

public class Constants {

	public int getSimpleInt() {
		return 4;
	}

	public int getSimpleInt1() {
		return -1;
	}

	public int getByteInt() {
		return -20;
	}

	public int getShortInt() {
		return 321;
	}

	public int getBigInt() {
		return 1234567;
	}

	public float getFloat0() {
		return 0f;
	}

	public float getFloat1() {
		return 1f;
	}

	public float getFloat2() {
		return 2f;
	}

	public float getFloat3() {
		return 3f;
	}

	public long getLong0() {
		return 0L;
	}

	public long getLong1() {
		return 1L;
	}

	public long getLong2() {
		return 2L;
	}

	public double getDouble0() {
		return 0.0;
	}

	public double getDouble1() {
		return 1.0;
	}

	public double getDouble2() {
		return 2.0;
	}

	public String getString() {
		return "abc".substring(5);
	}

	public Class<?> getKlass() {
		return Constants.class;
	}

	public void foo(int x) {
		x += 2;
		x += 10;

		double y = 1.0;
		y = y * 1.234;
		y = y - 5.678;

		float z = 2.0f;
		z = z - 1.0f;

		long w = 0L;
		w = w + 0L;

		String q = "xxx";
		if (q.equals("xxx"))
			q = "xxx";

		boolean b = (Constants.class == null);
		b = b | false;	// an int constant!
	}

}
