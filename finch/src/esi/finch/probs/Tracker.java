package esi.finch.probs;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Static class to help track certain things. 
 * @author ian
 *
 */
public class Tracker {
	// Nice little synchronized class to track runtime errors. 
	public static AtomicLong timeout = new AtomicLong(0);
	public static AtomicLong runtime = new AtomicLong(0);
	public static AtomicLong verify = new AtomicLong(0);
	
	public static int pop = 100;
	public static AtomicLong checks = new AtomicLong(0);
	
	public synchronized static void maybe_print() {
		checks.getAndIncrement();
		
		if (checks.get() == pop) {
			new_generation();
		}
	}
	
	public synchronized static void new_generation() {
		System.out.println("Timeouts: " + timeout);
		System.out.println("Runtime Errors: " + runtime);
		System.out.println("Verify Errors: " + verify);
		
		Tracker.timeout.set(0);
		Tracker.runtime.set(0);
		Tracker.verify.set(0);
		Tracker.checks.set(0);
	}	
}
