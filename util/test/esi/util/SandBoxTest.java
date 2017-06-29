package esi.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import esi.util.SandBox.Result;

public class SandBoxTest {

	static class TestClass {
		public volatile int counter = 0;

		public int testMethod(long millis, Throwable exception) throws Throwable {
			if (millis > 0) {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
					throw new Error("Interrupted", e);
				}
			}

			if (exception != null)
				throw exception;

			privateMethod();
			return 5;
		}

		public int identityMethod(int x) {
			return x;
		}

		public void voidMethod(int[] x) {
			++x[0];
		}

		private static void privateMethod() {
		}

		public void loopMethod() throws InterruptedException {
			while (true) {
				++counter;

				// SandBox expects sleeps before backjumps
				Thread.sleep(0);
			}
		}
	}

	static class TestClassException {
		static {
			(new int[0])[0] = 2;;
		}

		public static void method() {
		}
	}

	@Test
	public void testSandBox() throws NoSuchMethodException {
		Method method = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		new SandBox(new TestClass(), method, 1000);
	}

	@Test
	public void call() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		Result result  = sandbox.call(0, null);
		assertNotNull(result);
		assertNull  (result.exception);
		assertTrue  (result.millis < 100);
		assertTrue  (result.hasRetvalue());
		assertEquals(5, result.retvalue);
	}

	@Test
	public void callVoid() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("voidMethod", int[].class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		int[] arr = { 5 };
		Result result  = sandbox.call(arr);
		assertNotNull(result);
		assertNull (result.exception);
		assertTrue (result.millis < 100);
		assertTrue (result.hasRetvalue());
		assertNull (result.retvalue);
		assertEquals(6, arr[0]);
	}

	@Test
	public void callException() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		Exception exception = new IndexOutOfBoundsException();
		Result    result    = sandbox.call(0, exception);
		assertNotNull(result);
		assertSame (exception, result.exception);
		assertTrue (result.millis < 100);
		assertFalse(result.hasRetvalue());
		assertNull (result.retvalue);
	}

	@Test
	public void callTimeout() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 5);

		Result result = sandbox.call(100, null);
		assertNull(result);
	}

	@Test
	public void callTimeoutLoop() throws NoSuchMethodException, InterruptedException {
		Method    method  = TestClass.class.getDeclaredMethod("loopMethod");
		TestClass obj     = new TestClass();
		SandBox   sandbox = new SandBox(obj, method, 5);

		Result result = sandbox.call();
		assertNull(result);

		Thread.sleep(50);
		int counter1 = obj.counter;

		Thread.sleep(100);
		int counter2 = obj.counter;

		assertEquals(counter1, counter2);
	}

	@Test
	public void callSimultaneous() throws NoSuchMethodException, InterruptedException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		final SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		// Run a long call asynchronously
		final Result[] results = new Result[1];
		Thread thread = new Thread() {
			@Override
			public void run() {
				results[0] = sandbox.call(250, null);
			}
		};
		thread.start();

		// Make sure the call() in thread is executed first
		Thread.sleep(50);

		// If calls are done synchronously, the assignment to results[0]
		// will take some small time (< 100ms)
		Result result = sandbox.call(100, null);
		assertNull(results[0]);
		assertNotNull(result);
		assertNotNull(result.retvalue);

		thread.join();
		assertNotNull(results[0]);
		assertNotNull(results[0].retvalue);
	}

	@Test(expected = IllegalAccessException.class)
	public void callIllegalAccess() throws Throwable {
		Method    method  = TestClass.class.getDeclaredMethod("privateMethod");
		SandBox   sandbox = new SandBox(null, method, 1000);

		try {
			sandbox.call();
		} catch (Error e) {
			throw e.getCause();
		}
	}

	@Test(expected = NullPointerException.class)
	public void callNoObject() throws Throwable {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(null, method, 1000);

		try {
			sandbox.call(0, null);
		} catch (Error e) {
			throw e.getCause();
		}
	}

	@Test(expected = ExceptionInInitializerError.class)
	public void callClassException() throws Throwable {
		Method    method  = TestClassException.class.getDeclaredMethod("method");
		SandBox   sandbox = new SandBox(null, method, 1000);

		try {
			sandbox.call();
		} catch (Error e) {
			throw e.getCause();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void callIllegalObject() throws Throwable {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(0, method, 1000);

		try {
			sandbox.call(0, null);
		} catch (Error e) {
			throw e.getCause();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void callIllegalArgs() throws Throwable {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		try {
			sandbox.call(0.1, null);
		} catch (Error e) {
			throw e.getCause();
		}
	}

	@Test
	public void callConsecutive() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("identityMethod", Integer.TYPE);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		Result result  = sandbox.call(23);
		assertNotNull(result);
		assertNull  (result.exception);
		assertTrue  (result.millis < 100);
		assertTrue  (result.hasRetvalue());
		assertEquals(23, result.retvalue);

		Result result2  = sandbox.call(42);
		assertNotNull(result2);
		assertNull  (result2.exception);
		assertTrue  (result2.millis < 100);
		assertTrue  (result.hasRetvalue());
		assertEquals(42, result2.retvalue);

		assertNotSame(result, result2);
	}

	@Test
	public void stressTest() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 1000);

		for (int i = 0;  i < 10000;  ++i) {
			Result result  = sandbox.call(0, null);
			assertNotNull(result.retvalue);
		}
	}

	@Test
	public void stressTestTricky() throws NoSuchMethodException {
		Method    method  = TestClass.class.getDeclaredMethod("testMethod", Long.TYPE, Throwable.class);
		SandBox   sandbox = new SandBox(new TestClass(), method, 0);

		for (int i = 0;  i < 10000;  ++i) {
			Result result  = sandbox.call(0, null);
			assertFalse(result != null  &&  result.retvalue == null);
		}
	}

}
