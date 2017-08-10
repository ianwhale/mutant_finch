package esi.util;

import java.util.SortedSet;

import org.apache.commons.logging.Log;

import de.uka.ipd.sdq.ByCounter.execution.BytecodeCounter;
import de.uka.ipd.sdq.ByCounter.execution.CountingResultCollector;
import de.uka.ipd.sdq.ByCounter.results.CountingResult;
import de.uka.ipd.sdq.ByCounter.utils.MethodDescriptor;
import de.uka.ipd.sdq.ByCounter.utils.InvocationResultData;

public class SandBoxByCounter {

	private static Log log = Config.getLogger();
	
	private final long timeout;		
	private final BytecodeCounter counter;
	private final MethodDescriptor method;
	
	/**
	 * @param object, class description.
	 * @param method, method description. 
	 * @param timeout, gets ignored ByCounter handles timout (I don't know if this is good or bad). 
	 */
	public SandBoxByCounter(String className, String methodName, long timeout) {
		this.timeout = timeout;
		this.counter = new BytecodeCounter();
		this.method = new MethodDescriptor(className, methodName);
		this.counter.addEntityToInstrument(this.method);
		this.counter.instrument();
	}
	
	/**
	 * Hands the goods off to ByCounter to execute and count instructions. 
	 * @param args arguments to the method
	 * @return execution result
	 */
	public Result call(Object... args) {
		Result result = new Result();

		InvocationResultData exeResult = null;
		try {
			exeResult = counter.execute(this.method, args);
		}
		catch (Exception e) {
			// Infinite loop or run time error. 
			result.exception = e;
			return result;
		}
		
		Long count = CountingResultCollector.getInstance().
					retrieveAllCountingResults().
					getCountingResults().first().getTotalCount(true);
		
		result.byteCodeCount = count;
		result.nanos = exeResult.duration;
		result.retvalue = exeResult.returnValue;
		
		return result;
	}
	
	/**
	 * Method execution result.
	 */
	public static class Result {
		public  Throwable exception;	// exception if it occurred (ThreadDeath if timeout)
		public  long	  byteCodeCount; // number of bytecode instructions executed
		public  long      nanos;		// execution time (or till timeout/exception, (can be -1 if timeout))
		public  Object    retvalue;		// value returned by the method (null if void)

		public Result() {
			exception = null;
			nanos    = -1;
			byteCodeCount = -1;
			retvalue  = null;
		}

		/**
		 * @return whether method execution was successful
		 */
		public boolean hasRetvalue() {
			return exception == null;
		}
	}
}
