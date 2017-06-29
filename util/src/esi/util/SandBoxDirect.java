package esi.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Running method with timeout, while catching exceptions.
 *
 * This is the old sandbox implementation, which creates a
 * thread per evaluation, and kills timed out threads using
 * the deprecated {@link Thread#stop()} method.
 *
 * @author Michael Orlov
 */
public class SandBoxDirect {

	private final Object object;		// call object (may be null)
	private final Method method;		// method to call
	private final long   timeout;		// timeout in milliseconds

	/**
	 * Method execution result.
	 */
	public static class Result {
		private boolean   timeout;		// whether timeout occurred
		public  Throwable exception;	// exception if it occurred (ThreadDeath if timeout)

		public  long      millis;		// execution time (or till timeout/exception, (can be -1 if timeout))
		public  Object    retvalue;		// value returned by the method (null if void)

		public Result() {
			timeout   = true;	// dropped to false if Runner runs ok
			exception = null;
			millis    = -1;
			retvalue  = null;
		}

		/**
		 * @return whether method execution was successful
		 */
		public boolean hasRetvalue() {
			assert !timeout;
			return exception == null;
		}
	}

	private class Runner extends Thread {
		public final Object[] args;		// call arguments
		public final Result   result;	// result method call
		public       Error    problem;	// exception to be rethrown by SandBox

		public Runner(Object[] args) {
			this.args = args;

			// timeout=true, exception=null, retvalue=null
			result  = new Result();
			problem = null;
		}

		@Override
		public void run() {
			long millis = System.currentTimeMillis();

			try {
				// retvalue is non-null only if the call succeeds
				result.retvalue = method.invoke(object, args);
				result.millis   = System.currentTimeMillis() - millis;
				result.timeout  = false;
			} catch (IllegalAccessException e) {
				problem = new Error("Method is inaccessible", e);
			} catch (NullPointerException e) {
				problem = new Error("Calling instance method with null object", e);
			} catch (ExceptionInInitializerError e) {
				problem = new Error("Class initialization failed", e);
			} catch (IllegalArgumentException e) {
				problem = new Error("Method cannot be called with supplied arguments", e);
			} catch (InvocationTargetException e) {
				// exception is non-null iff call fails
				result.exception = e.getCause();
				// calculate millis in any case
				result.millis    = System.currentTimeMillis() - millis;

				// timeout is true iff call timed out
				if (! (result.exception instanceof ThreadDeath))
					result.timeout = false;
			}
		}
	}

	/**
	 * Creates a new sandbox that is ready to execute given
	 * method in a separate thread.
	 *
	 * @param object class instance (null for static method)
	 * @param method method to execute
	 * @param timeout timeout in milliseconds
	 */
	public SandBoxDirect(Object object, Method method, long timeout) {
		this.object  = object;
		this.method  = method;
		this.timeout = timeout;
	}

	/**
	 * Executes the method with timeout.
	 * @param args arguments to the method
	 * @return execution result
	 */
	@SuppressWarnings("deprecation")
	public Result call(Object... args) {
		Runner runner = new Runner(args);

		// run() in separate thread
		runner.start();

		// wait for thread to die, with timeout
		try {
			runner.join(timeout);
		} catch (InterruptedException e) {
			throw new Error("Unexpected: Interrupted Exception");
		}

		// timeout
		if (runner.isAlive()) {
			// Note: doesn't always stop thread
			runner.stop();
			return null;
		}

		// if run() indicated an exception, rethrow it
		if (runner.problem != null)
			throw runner.problem;

		// new version returns null for timeout
		if (runner.result.timeout)
			return null;

		return runner.result;
	}

}
