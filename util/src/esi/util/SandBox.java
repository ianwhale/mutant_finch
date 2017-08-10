package esi.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.logging.Log;

/**
 * Running method with timeout, while catching exceptions.
 *
 * Important: uninterruptible thread will not die in case of timeout!
 * Therefore, timeout should only be used as a safety guard for exceptional
 * circumstances.
 *
 * @author Michael Orlov
 */
public class SandBox {

	private static Log log = Config.getLogger();

	// Thread Pool (thread-safe)
	private static final ExecutorService executor;

	private final Object object;		// call object (may be null)
	private final Method method;		// method to call
	private final long   timeout;		// timeout in milliseconds

	static {
		log.info("Creating sandbox thread pool");
		executor = Executors.newCachedThreadPool();
	}

	/**
	 * Method execution result.
	 */
	public static class Result {
		public Throwable exception;		// exception if it occurred
		public long      millis;		// execution time (or till exception)
		public Object    retvalue;		// value returned by the method (null if void)

		public Result() {
			exception = null;
			millis    = -1;
			retvalue  = null;
		}

		/**
		 * @return whether method execution was successful
		 */
		public boolean hasRetvalue() {
			return exception == null;
		}
	}

	// Note: instance inner class
	private class Runner implements Callable<Result> {
		public final Object[] args;		// call arguments
		public       Error    problem;	// exception to be rethrown by SandBox

		public Runner(Object[] args) {
			this.args = args;
			problem = null;
		}

		@Override
		public Result call() /* throws Exception */ {
			// exception=null, retvalue=null
			Result result = new Result();
			long   millis = System.currentTimeMillis();

			try {
				// retvalue is non-null only if the call succeeds
				result.retvalue = method.invoke(object, args);
				result.millis   = System.currentTimeMillis() - millis;
			} catch (IllegalAccessException e) {
				problem = new Error("Method is inaccessible", e);
			} catch (NullPointerException e) {
				problem = new Error("Calling instance method with null object", e);
			} catch (ExceptionInInitializerError e) {
				problem = new Error("Class initialization failed", e);
			} catch (IllegalArgumentException e) {
				problem = new Error("Method cannot be called with supplied arguments", e);
			} catch (InvocationTargetException e) {
				// If it's a timeout, this result won't be returned from call()

				// exception is non-null iff call fails
				result.exception = e.getCause();
				// calculate millis in any case
				result.millis    = System.currentTimeMillis() - millis;
			}

			return result;
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
	public SandBox(Object object, Method method, long timeout) {
		this.object  = object;
		this.method  = method;
		this.timeout = timeout;
	}

	/**
	 * Executes the method with timeout.
	 *
	 * Important: uninterruptible thread will not die in case of timeout!
	 * Therefore, timeout should only be used as a safety guard.
	 *
	 * @param args arguments to the method
	 * @return execution result, or <code>null</code> if timeout occurred
	 */
	public Result call(Object... args) {
		Runner runner = new Runner(args);

		// run() in separate thread
		Future<Result> future = executor.submit(runner);
		Result         result;

		// wait for thread to complete, with timeout
		try {
			result = future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new Error("Unexpected: Interrupted Exception");
		} catch (ExecutionException e) {
			throw new Error("Unexpected: Execution Exception");
		} catch (TimeoutException e) {
			// cancellation can fail if method stopped running in between
			future.cancel(true);

			// cancellation doesn't happen immediately,
			// so must return explicit timeout
			return null;
		}

		// if run() indicated an exception, rethrow it
		if (runner.problem != null)
			throw runner.problem;

		return result;
	}

	/**
	 * Initiates an orderly shutdown of the thread pool.
	 * No further calls to {@link #call(Object...)} are possible.
	 */
	public static void shutdown() {
		log.info("Shutting down sandbox thread pool");
		executor.shutdown();
	}

}
