package esi.finch.probs;

import java.lang.reflect.Method;
import java.util.Arrays;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.util.SandBoxByCounter;

/**
 * Sort Optimizer is used code improvement. 
 * Uses evaluation defined in the locoGP paper. 
 * For bug fixing, see {@link SortEvaluator}. 
 */
public class SortOptimizer implements BytecodeEvaluator {
	
	private static final String SORT_DESC = "public int[] sort(int[] list)";
	private static final String SEED_DESC = "public int[] seed(int[] list)";
	private static final int NUM_LISTS = 3;
	
	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Tracker.maybe_print();
		
		// Get the non-interruptible version as ByCounter will handle infinite loops. 
		Method method;
		try {
			method = ind.getMethod();
		}
		catch (ClassFormatError e) {
			return new Result(Integer.MIN_VALUE, false);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return new Result(Integer.MIN_VALUE, false);
		}
		
		int[][] test_lists = Derange.generateRandomLists(random, NUM_LISTS);
		
		// Get seed score and bytecounts. 
		// The seed always gets a perfect error score, which is just the sum of the length of the lists. 
		long[] seed_counts = new long[NUM_LISTS];
		long seed_exe_count = seedExecutionCount(test_lists, ind.getMethod().getDeclaringClass(), timeout, seed_counts);
		float seed_error = 0;
		for (int[] list : test_lists) {
			seed_error += list.length;
			
		}

		// Get evolving method score and bytecounts.
		LocoScore locoscore = new LocoScore(test_lists);
		
		SandBoxByCounter sandbox = new SandBoxByCounter(method.getDeclaringClass().getCanonicalName(), SORT_DESC, timeout);
		SandBoxByCounter.Result result = null;
		float score = 0; 
		int i = 0;
		for (int[] list : test_lists) {	
			int[] sort_me = Arrays.copyOf(list, list.length);
			
			result = sandbox.call(sort_me);
			
			// Timeout 
			if (result == null) {
				//log.info("Timeout in " + ind);
				Tracker.timeout.getAndIncrement();
				score += 2 * seed_counts[i] + 2 * list.length; // Score on error from LocoGP sorting scheme. 
			}
			// Runtime error. 
			else if (result.exception != null) {
				//log.info("Exception: " + result.exception + " in " + ind);
				Tracker.runtime.getAndIncrement();
				score += 2 * seed_counts[i] + 2 * list.length; // Score on error from LocoGP sorting scheme. 
			}
			else {
				assert result.retvalue instanceof int[];
				int[] candidate = (int[]) result.retvalue;
				
				locoscore.score(candidate, locoscore.sortedTestLists[i], list);
			}
			
			i++;
		}
		
		score += locoscore.getRawScore();
		
		return new Result(-1 * score, false); // Always non-ideal since we're not evolving toward a specific goal. 
	}
	
	/**
	 * Gets the bytecode count for the seed program on the specific test list. 
	 * @param test_lists
	 * @param klass class containing the seed method named "seed"
	 * @return
	 */
	public long seedExecutionCount(int[][] test_lists, Class<?> klass, long timeout, long[] seed_counts) {
		SandBoxByCounter sandbox = new SandBoxByCounter(klass.getCanonicalName(), SEED_DESC, timeout);
		
		long count = 0;
		SandBoxByCounter.Result result = null;
		for (int i = 0; i < test_lists.length; i++) {
			result = sandbox.call(test_lists[i]);
			count += result.byteCodeCount;
			seed_counts[i] = result.byteCodeCount;
		}
		
		return count;
	}
	
	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}
}
