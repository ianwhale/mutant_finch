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
import ec.simple.SimpleFitness;

public class SortEvaluator implements BytecodeEvaluator {
	public static final Log log = Config.getLogger();
	
	public static float worst_case = 0;
	
	public static final int LIST_LENGTH = 8;
	
	private static final float RT_PENALTY = 1.5f; // Extra penalty for a runtime error.
	
//	private static int total_lengths = 0;
//	static {
//		for (int[] list : testLists) {
//			InsertionSortEvaluator.total_lengths += list.length;
//		}
//	}
	
//	private static int minimal_fitness = -2 * total_lengths;
	
	/**
	 * Evaluate with some disorder metric. 
	 */
	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		Tracker.maybe_print();
		
		int[][] test_lists = Derange.generateRandomLists(random, 3);
		
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
		catch (ArrayIndexOutOfBoundsException e) {
			// ? 
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
		
		SortScore remscore = new REMScore(test_lists);
		SortScore locoscore = new LocoScore(test_lists);
		
		float score = 0; 
		int i = 0;
		for (int[] list : test_lists) {	
			int[] sort_me = Arrays.copyOf(list, list.length);
			int[] sorted = remscore.sortedTestLists[i];
			
			result = sandbox.call(sort_me);
			
			// Timeout 
			if (result == null) {
				//log.info("Timeout in " + ind);
				Tracker.timeout.getAndIncrement();
				remscore.worstCase(); locoscore.worstCase();
			}
			// Runtime error. 
			else if (result.exception != null) {
				//log.info("Exception: " + result.exception + " in " + ind);
				Tracker.runtime.getAndIncrement();
				remscore.worstCase(); locoscore.worstCase();
			}
			else {
				assert result.retvalue instanceof int[];
				int[] candidate = (int[]) result.retvalue;
				
				remscore.score(candidate, remscore.sortedTestLists[i], list);
				locoscore.score(candidate, remscore.sortedTestLists[i], list);
			}
			
			i++;
		}
		
		float fitness = (remscore.getScore() + locoscore.getScore()) / 2;
		boolean ideal = fitness == 0;
		
		return new Result(fitness, ideal);
	}
	
	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Tournament evaluation is not implemented");
	}
}

/**
 * Class to hold the derangement functions. 
 */
class Derange {
	public static Map<Long, Long> dn_memo = new HashMap<Long, Long>();   // Save some time on calculating number of derangements for list of n values.
	public static Map<Long, Long> fact_memo = new HashMap<Long, Long>(); // Save some time on calculating factorials.
	
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
			lists[i] = fillList(random, SortEvaluator.LIST_LENGTH); // Random list of length 6
		}
		
		return lists;
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
		int rand;
		for (int i = 0; i < length; i++) {
			do {
				rand = random.nextInt(200) - 100;
			} while (used.containsKey(rand));
			list[i] = rand;
			used.put(rand, true);
		}
		
		Arrays.sort(list);
		return Derange.randomDerangement(random, list);
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
		
		double p;
		
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
				p = (u == 1) ? 0 : (u - 1) * (dn(u - 2) / dn(u));
				if (random.nextDouble() < p) {
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
	 * Our old friend!
	 * 
	 * @param n
	 * @return
	 */
	public static long factorial(long n) {
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
	public static long dn(long n) {
		if (n == 0 || n == 2) {
			return 1;
		}
		else if (n == 1) {
			return 0;
		}
		
		if (dn_memo.containsKey(n)) {
			return dn_memo.get(n);
		}
		
		dn_memo.put(n, (long)Math.floor((factorial(n) + 1) / Math.E));
		return dn_memo.get(n);
	}
}

/**
 * Base class to help organize the different fitness functions.  
 * 
 * 		**  All sorting scores operate on the assumption that 0 **
 * 		**** is the ideal fitness. For now at least (7/19).   ****
 * 
 */
abstract class SortScore {
	/**
	 * Sort the tests lists for comparison purposes. 
	 * @param test_lists
	 */
	public SortScore(int[][] test_lists) { 
		this.testLists = test_lists; 
		
		this.sortedTestLists = new int[test_lists.length][];
		int[] temp;
		for (int i = 0; i < this.testLists.length; i++) {
			temp = Arrays.copyOf(this.testLists[i], this.testLists[i].length);
			Arrays.sort(temp);
			this.sortedTestLists[i] = temp;
		}
	}
	
	// Update internal score appropriately. 
	public abstract void score(int[] candidate, int[] sorted, int[] original);
	public float getScore() { return score; }
	public abstract void worstCase();
	
	public int[][] testLists;
	public int[][] sortedTestLists;
	protected float score = 0;
}

/**
 * Class for calculating the REM score. 
 * 
 * See: 
 * Agapitos, A. and Lucas, S.M., 2006, July. Evolving efficient recursive sorting algorithms. 
 * In Evolutionary Computation, 2006. CEC 2006. IEEE Congress on (pp. 2677-2684). IEEE.
 */
class REMScore extends SortScore {
	public REMScore (int[][] test_lists) { super(test_lists); }
	
	/**
	 * REM score is calculated by determining the the number of elements to 
	 * remove in order to put a list in sorted order.
	 * Simply put, it is the length of the list minus the length of 
	 * the longest increasing subsequence. 
	 */
	public void score(int[] candidate, int[] sorted, int[] original) { 
		score -= (float) (candidate.length - longestIncreasingSubsequence(candidate)); 
	}

	/**
	 * The shortest possible LIS is 1. Worst possible REM is the list length minus 1.
	 */
	@Override
	public void worstCase() { score -= SortEvaluator.LIST_LENGTH - 1; }
	
	/**
	 * Get the length of the longest increasing subsequence.  
	 * 
	 * @param sorted
	 * @param list
	 * @return
	 */
	public static int longestIncreasingSubsequence(int[] list) {
		// Algorithm taken from the pseudocode on the LIS Wikipedia page.
		int[] P = new int[list.length];
		int[] M = new int[P.length + 1];
		
		int L = 0;
		int mid;
		int lo;
		int hi;
		int newL;
		for (int i = 0; i < P.length; i++) {
			// Binary search for the largest positive j <= L
			// such that X[M[j]] < X[i]
			lo = 1;
			hi = L;
			
			while (lo <= hi) {
				mid = (int)Math.ceil((lo + hi) / 2);
			
				if (list[M[mid]] < list[i]) {
					lo = mid + 1;
				}
				else {
					hi = mid - 1;
				}
			}
			
			// After searching, lo is 1 greater than the
			// length of the longest prefix of X[i]
			newL = lo;
			
			// The predecessor of X[i] is the last index of 
			// the subsequence of length newL-1
			P[i] = M[newL - 1];
			M[newL] = i;
			
			if (newL > L) {
				// If we found a subsequence longer than any we've
				// found yet, update L
				L = newL;
			}
		}
		
		// Skip reconstructing the longest increasing subsequence, just return the length.
		return L;
	}
}

/**
 * Class for calculating the "LocoScore". 
 * Which is simply the term I'm using for the sorting fitness function used in:
 * 
 * Cody-Kenny, B., Galván-López, E. and Barrett, S., 2015, July. 
 * locoGP: improving performance by genetic programming java source code. 
 * In Proceedings of the Companion Publication of the 2015 Annual Conference on 
 * Genetic and Evolutionary Computation (pp. 811-818). ACM.
 */
class LocoScore extends SortScore {
	public LocoScore (int[][] test_lists) { super(test_lists); }
	
	private static final int CORRECT = 1;
	private static final int MOVED = 2;
	private static final int NOT_MOVED = 3;
	
	/**
	 * Score is assigned based on each value in the candidate list:
	 * 		| +1 if correct.
	 * 		| +2 if moved, but not correct.
	 * 		| +3 if not moved. 
	 * Then the final score is calculated by subtracted the above 
	 * from the length of the list.  
	 */
	public void score(int[] candidate, int[] sorted, int[] original) {
		// Map used to make sure the candidate can't boost its score
		// by filling values to get all of its positions counted as moved.
		Map<Integer, Boolean> used = new HashMap<Integer, Boolean>();
		for (int num : sorted) {
			used.put(num, true);
		}
		
		int result = 0; 
		for (int i = 0; i < sorted.length; i++) {
			if (sorted[i] == candidate[i]) {
				result += CORRECT;
			}
			else if (used.containsKey(candidate[i]) &&
					candidate[i] != original[i]) {
				result += MOVED;
			}
			else {
				result += NOT_MOVED;
			}
		}
		
		// Value will be negative unless a candidate is sorted.
		// In which case it will be 0. 
		score += (candidate.length - result);
	}
	
	/**
	 * Worst possible LocoScore. 
	 * If all values got a score of 3, list length minus score would 
	 * simply give 2 time list length.
	 */
	public void worstCase() { score -= 2 * SortEvaluator.LIST_LENGTH; }
}

/**
 * Class for calculating mean sorted position distance score.
 * 
 * See: 
 * Agapitos, A. and Lucas, S.M., 2006, July. Evolving efficient recursive sorting algorithms. 
 * In Evolutionary Computation, 2006. CEC 2006. IEEE Congress on (pp. 2677-2684). IEEE.
 */
class MSPDScore extends SortScore {
	public MSPDScore(int[][] test_lists) { 
		super(test_lists); 
		
		int n = SortEvaluator.LIST_LENGTH;
		this.worstCaseValue = (float) ((Math.floor((n * n) / 2)) / n);
	}
	
	/**
	 * Computes the mean sorted position distance. 
	 * 
	 * @param candidate
	 * @param sorted
	 * @param original
	 */
	public void score(int[] candidate, int[] sorted, int[] original) {
		Map<Integer, Integer> candidate_positions = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < sorted.length; i++) {
			candidate_positions.put(candidate[i], i);
		}
		
		int sum_abs_distances = 0;
		try {
			for (int i = 0; i < sorted.length; i++) {
				sum_abs_distances += Math.abs(i - candidate_positions.get(sorted[i]));
			}
		}
		catch (NullPointerException e) {
			score -= worstCaseValue;
		}

		
		score -= (sum_abs_distances / sorted.length);
	}
	
	public void worstCase() { score -= worstCaseValue; }
	private float worstCaseValue;
}

/**
 * Class for calculating the longest common subsequence.
 */
class LCSScore extends SortScore {
	public LCSScore(int[][] test_lists) { super(test_lists); }
	
	/**
	 * Score is simply list length minus the length of the lcs. 
	 */
	public void score(int[] candidate, int[] sorted, int[] original) {
		int lcs = 0; 
		
		int[][] memo = new int[sorted.length + 1][candidate.length + 1];
		for (int i = 0; i <= sorted.length; i++) {
			for ( int j = 0; j <= candidate.length; j++) {
				if (i == 0 || j == 0) {
					memo[i][j] = 0;
				}
				else if (sorted[i - 1] == candidate[j - 1]) {
					memo[i][j] = memo[i - 1][j - 1] + 1;
					lcs = Math.max(lcs, memo[i][j]);
				}
				else {
					memo[i][j] = 0;
				}
			}
		}
		
		score -= (candidate.length - lcs);
	}
	
	// Worst possible LCS of a random list and the sorted version of the list.
	public void worstCase() { score -= SortEvaluator.LIST_LENGTH - 1; }
}

/**
 * Class for calculating "simple difference".
 */
class SimpleDifferenceScore extends SortScore {
	public SimpleDifferenceScore(int[][] test_lists) { super(test_lists); }
	
	/**
	 * Simply subtract one for every index that differs from the sorted list.
	 */
	public void score(int[] candidate, int[] sorted, int[] original) {
		for (int i = 0; i < sorted.length; i++) {
			if (sorted[i] != candidate[i]) {
				score--;
			}
		}
	}
	
	/**
	 * Every element differs. 
	 */
	public void worstCase() { score -= SortEvaluator.LIST_LENGTH; }
}