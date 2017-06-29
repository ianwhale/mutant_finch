package esi.util;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import org.junit.Test;

public class LoaderTest {

	public static class Test1 {
	}
	public static class Test2 {
	}
	public static class Test3 {
		public Test3(int x, Test1 y) {
		}
	}
	public static abstract class Test4 {
	}
	public interface Test5 {
	}
	public static class Test7 {
		protected Test7() {
		}
	}
	public static class Test8 {
		public Test8() {
			Integer.parseInt("NaN");
		}
	}
	public static class Test9 {
		static {
			Integer.parseInt("NaN");
		}

		public Test9() {
		}
	}
	public static class Test10 {
		public Test10(Random random) {
		}
	}
	private static class SimpleRandom extends Random {
		private static final long serialVersionUID = 1L;
	}
	private static interface Test11I {
	}
	static class Test11 implements Test11I {
		public Test11(int x, int y) {
		}
		public Test11(int x, int y, int z) {
		}
	}

	@Test
	public void loadClassInstance1() {
		Test1 class1 = Loader.loadClassInstance(Test1.class.getName(), Test1.class);
		assertNotNull(class1);
	}

	@Test
	public void loadClassInstance1a() {
		Test1 class1 = Loader.loadClassInstance(Test1.class);
		assertNotNull(class1);
	}

	@Test
	public void loadClassInstance2() {
		try {
			Loader.loadClassInstance("no.such.Class", Object.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof ClassNotFoundException);
		}
	}

	@Test
	public void loadClassInstance3() {
		try {
			Loader.loadClassInstance(Test1.class.getName(), Test2.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof ClassCastException);
		}
	}

	@Test
	public void loadClassInstance4() {
		try {
			Loader.loadClassInstance(Test3.class.getName(), Test3.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance4a() {
		try {
			Loader.loadClassInstance(Test3.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance5() {
		Test3 class3 = Loader.loadClassInstance(Test3.class.getName(), Test3.class, 3, new Test1());
		assertNotNull(class3);
	}

	@Test
	public void loadClassInstance5a() {
		Test3 class3 = Loader.loadClassInstance(Test3.class, 3, new Test1());
		assertNotNull(class3);
	}

	@Test
	public void loadClassInstance6() {
		try {
			Loader.loadClassInstance(Test4.class.getName(), Test4.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof InstantiationException);
		}
	}

	@Test
	public void loadClassInstance6a() {
		try {
			Loader.loadClassInstance(Test4.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof InstantiationException);
		}
	}

	@Test
	public void loadClassInstance7() {
		try {
			Loader.loadClassInstance(Test5.class.getName(), Test5.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance7a() {
		try {
			Loader.loadClassInstance(Test5.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance8() {
		try {
			Loader.loadClassInstance(getClass().getPackage().getName() + ".test.Test6", Object.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalAccessException);
		}
	}

	@Test
	public void loadClassInstance9() {
		try {
			Loader.loadClassInstance(Test7.class.getName(), Test7.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance10() {
		try {
			Loader.loadClassInstance(Test8.class.getName(), Test8.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof InvocationTargetException);
			assertTrue(e.getCause().getCause() instanceof NumberFormatException);
		}
	}

	@Test
	public void loadClassInstance10a() {
		try {
			Loader.loadClassInstance(Test8.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof InvocationTargetException);
			assertTrue(e.getCause().getCause() instanceof NumberFormatException);
		}
	}

	@Test
	public void loadClassInstance11() {
		try {
			Loader.loadClassInstance(Test9.class.getName(), Test9.class);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof ExceptionInInitializerError);
			assertTrue(e.getCause().getCause() instanceof NumberFormatException);
		}
	}

	@Test
	public void loadClassInstance12() {
		Test10 class10 = Loader.loadClassInstance(Test10.class.getName(), Test10.class, new SimpleRandom());
		assertNotNull(class10);
	}

	@Test
	public void loadClassInstance13() {
		Test11I iface = Loader.loadClassInstance(Test11.class.getName(), Test11I.class, 1, 2, 3);
		assertNotNull(iface);
		assertTrue(iface instanceof Test11);
	}

	@Test
	public void loadClassInstance13a() {
		try {
			Loader.loadClassInstance(Test11I.class, 1, 2, 3);
			fail();
		} catch (Error e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void loadClassInstance13b() {
		Test11I iface = Loader.loadClassInstance(Test11.class, 1, 2, 3);
		assertNotNull(iface);
		assertTrue(iface instanceof Test11);
	}

}
