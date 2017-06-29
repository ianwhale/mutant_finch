package esi.bc.test;

public class UnificationTest {

	public interface X {
		void foo();
	}

	public static abstract class S {
		public abstract void goo();
	}

	public static class A extends S implements X {
		public void goo() {
			System.out.println("goo A");
		}

		public void foo() {
			System.out.println("foo A");
		}
	}

	public static class B extends S implements X {
		public void goo() {
			System.out.println("goo B");
		}

		public void foo() {
			System.out.println("foo B");
		}
	}

}
