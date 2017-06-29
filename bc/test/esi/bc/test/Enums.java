package esi.bc.test;

public class Enums {

	enum E {
		A,
		B { @Override public String toString() { return "b"; } };

		public static E get() { return B; }
	}

	public void foo(E e) {
		if (e != E.B)
			foo(E.B);

		e.toString();
		foo(e);
		foo(E.A);
	}

}
