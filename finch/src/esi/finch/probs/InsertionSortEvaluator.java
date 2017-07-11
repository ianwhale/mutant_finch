package esi.finch.probs;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.util.Config;
import esi.util.SandBox;
import java.util.concurrent.atomic.AtomicLong;

public class InsertionSortEvaluator implements BytecodeEvaluator {

	private static final int[][] testLists = {
			{250, -69, -114, 126, 255, -22, -280, 277},
			{0, -1},
		    {10, -123, -9, -13, 0},
			{1, 3, 2, 4},
	};
	
	private static final Log log = Config.getLogger();
	private static final int CORRECT = 1;
	private static final int MOVED = 2;
	private static final int NOT_MOVED = 3;
	
	private static final float RT_PENALTY = 1.5f; // Extra penalty for a runtime error.
	
//	private static int total_lengths = 0;
//	static {
//		for (int[] list : testLists) {
//			InsertionSortEvaluator.total_lengths += list.length;
//		}
//	}
	
//	private static int minimal_fitness = -2 * total_lengths;
	
	public static Map<Integer, Integer> dn_memo = new HashMap<Integer, Integer>();
	
	/**
	 * Our old friend!
	 * 
	 * @param n
	 * @return
	 */
	public static long factorial(int n) {
		if (n == 0) {
			return 1;
		}
		return n * factorial(n - 1);
	}
	
	/**
	 * Calculate the number of derangements for n numbers.
	 * @param n
	 * @return
	 */
	public static int dn(int n) {
		if (n == 0 || n == 2) {
			return 1;
		}
		else if (n == 1) {
			return 0;
		}
		
		if (dn_memo.containsKey(n)) {
			return dn_memo.get(n);
		}
		
		dn_memo.put(n, (int)Math.floor((factorial(n) + 1) / Math.E));
		return dn_memo.get(n);
	}
	
	/**
	 * Generate a random derangement of a list.
	 * Algorithm from: C. Martinez, A. Panholzer, and H. Prodinger, Generating Random Derangments. DOI: 10.1137/1.9781611972986.7
	 * 
	 * @param random
	 * @param A
	 * @return int[]
	 */
	public static int[] randomDerangement(MersenneTwisterFast random, int[] list) {
		boolean[] mark = new boolean[list.length];
		int[] A = new int[list.length];
		
		// Initialize place keepers (Line 2).
		for (int i = 0; i < list.length; i++) {
			A[i] = i;
			mark[i] = false;
		}
		
		// Initialize i and u, accounting for 0 indexing in i (Line 3)
		int i = list.length - 1;
		int u = list.length;
		int j;
		int temp;
		
		// u is meant to keep track of the number of unmarked integers in the current subarray A[0..i]
		// Therefore, we don't apply any zero indexing to it like we would normally. 
		while (u >= 2) { // 4
			if (! mark[i]) { // 5
				// Find a random index that is unmarked (Lines 6, 7)
				do {
					j = random.nextInt(i);	
				} while (mark[j]);
				
				temp = A[i]; //
				A[i] = A[j]; // 8
				A[j] = temp; // 
				
				// "Carefully chosen probability" that ensures all possible derangements have the
				// same chance of being chosen. (Lines 9-11)
				if (random.nextDouble() < (u - 1) * (dn(u - 2) / dn(u))) {
					mark[j] = true;
					u--;
				}
				u--; // 12
			}
		i--; // 13
		}
		
		// Fill a new list with the specified permutation.
		int[] deranged = new int[list.length];
		for (int k = 0; k < list.length; k++) {
			deranged[k] = list[A[k]];
		}
		
		return deranged;
	}
	
	/**
	 * Fill a list of a certain length with random numbers. 
	 * 
	 * @param random
	 * @param length
	 * @return
	 */
	public static int[] fillList(MersenneTwisterFast random, int length) {
		int[] list = new int[length];
		
		Map<Integer, Boolean> used = new HashMap<Integer, Boolean>();
		
		// Fill up the list. Needs to have unique values for derangement generation.
		int rand = random.nextInt();
		for (int i = 0; i < length; i++) {
			while (used.containsKey(rand)) {
				rand = random.nextInt();
			}
			list[i] = rand;
			used.put(rand, true);
		}
		
		Arrays.sort(list);
		return InsertionSortEvaluator.randomDerangement(random, list);
	}
	
	/**
	 * Get back some random lists. 
	 * 
	 * @param random
	 * @param num_lists
	 * @return
	 */
	public static int[][] generateRandomLists(MersenneTwisterFast random, int num_lists) {
		int[][] lists = new int[num_lists][];
		
		for (int i = 0; i < num_lists; i++) {
			lists[i] = fillList(random, 8); // Random list of length 6
		}
		
		return lists;
	}
	
	/**
	 * Use LocoGP's sort evaluation. 
	 *   Fitness calculated as follows by summing:
	 *   	| +1 if value is in correct spot
	 *   	| +2 if value moved, but incorrect
	 *   	| +3 if value not moved (and in incorrect spot)
	 *   Then for any list l and error sum e, the score for that list is
	 *   		f = | l | - e
	 *   So the minimal fitness is -2 * | l_i | for all l_i in L, the collection of all unsorted lists. 
	 *   The maximal fitness is 0; 
	 */
	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Tracker.maybe_print();
		
		int[][] test_lists = generateRandomLists(random, 3);
		
		int total_lengths = 0;
		for (int[] list : test_lists) {
			total_lengths += list.length;
		}
		
		Class<?> klass;
		try {
			klass = ind.getInterruptibleMethod(steps, threadnum, 0).getDeclaringClass();
		}
		catch (VerifyError e) {
			// Malformed method from mutation, mark with minimal fitness. 
			//log.info("Verify error caught...");
			Tracker.verify.getAndIncrement();
			return new Result(Integer.MIN_VALUE, false);
		}
		
		Method sort; 
		try {
			sort = klass.getDeclaredMethod("sort", int[].class);
		}
		catch (Exception e) {
			throw new Error("Whoops! ", e);
		}
		
		Object instance;
		try {
			instance = klass.newInstance();
		} catch (InstantiationException e) {
			throw new Error("Unexpected exception", e);
		} catch (IllegalAccessException e) {
			throw new Error("Unexpected exception", e);
		}
		
		SandBox sandbox = new SandBox(instance, sort, timeout);
		SandBox.Result result; 
		
		
		int score = 0; 
		// Test the individual on all the lists.
		for (int[] list : test_lists) {	
			int[] sort_me = Arrays.copyOf(list, list.length);
			int[] original = Arrays.copyOf(list, list.length);
			int[] sorted = Arrays.copyOf(list, list.length);
			Arrays.sort(sorted);
			
			result = sandbox.call(sort_me);
			
			// Timeout 
			if (result == null) {
				//log.info("Timeout in " + ind);
				Tracker.timeout.getAndIncrement();
				score += NOT_MOVED * list.length; // * RT_PENALTY; // Assign minimal fitness for the list.
			}
			// Runtime error. 
			else if (result.exception != null) {
				//log.info("Exception: " + result.exception + " in " + ind);
				Tracker.runtime.getAndIncrement();
				score += NOT_MOVED * list.length; // * RT_PENALTY; // Assign minimal fitness for the list. 
			}
			else {
				assert result.retvalue instanceof int[];
				int[] candidate = (int[]) result.retvalue;
				
				score += getScore(sorted, candidate, original);
			}
		}
		
		int fitness = total_lengths - score;
		boolean ideal = fitness == 0;
		
		return new Result(fitness, ideal);
//		// Errors are ok, just assign the worst possible fitness. 
//		catch (VerifyError e) {
//			ind.evaluated = true;
//			return new Result(-2 * total_lengths, false);
//		}
//		catch (ClassFormatError e) {
//			ind.evaluated = true;
//			return new Result(-2 * total_lengths, false);
//		}
	}

	/**
	 * Given the individual's solution to the problem, determine the score of the solution. 
	 * @param sorted
	 * @param candidate
	 * @param original, the original unsorted list
	 * @return 
	 */
	private int getScore(int[] sorted, int[] candidate, int[] original) {
		Map<Integer, Boolean> used = new HashMap<Integer, Boolean>();
		for (int num : sorted) {
			used.put(num, true);
		}
		
		int score = 0; 
		for (int i = 0; i < sorted.length; i++) {
			if (sorted[i] == candidate[i]) {
				score += CORRECT;
			}
			else if (used.containsKey(candidate[i]) &&
					candidate[i] != original[i]) {
				score += MOVED;
			}
			else { // Candidate solution did not move the entry.
				score += NOT_MOVED;
			}
		}
		
		return score;
	}
	
	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}
}