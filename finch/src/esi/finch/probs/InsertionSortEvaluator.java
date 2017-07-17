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
	
	public static float worst_case = 0;
	
	private static final int LIST_LENGTH = 8;
	
	private static final float RT_PENALTY = 1.5f; // Extra penalty for a runtime error.
	
//	private static int total_lengths = 0;
//	static {
//		for (int[] list : testLists) {
//			InsertionSortEvaluator.total_lengths += list.length;
//		}
//	}
	
//	private static int minimal_fitness = -2 * total_lengths;
	
	public static Map<Long, Long> dn_memo = new HashMap<Long, Long>();
	
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
			lists[i] = fillList(random, LIST_LENGTH); // Random list of length 6
		}
		
		return lists;
	}
	
	/**
	 * Evaluate with some disorder metric. 
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
		
		worst_case = 3 * (((LIST_LENGTH - get_worst_LISS(LIST_LENGTH)) + 2 * LIST_LENGTH) / 2);
		
		float score = 0; 
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
				score += worst_case; // * RT_PENALTY; // Assign minimal fitness for the list.
			}
			// Runtime error. 
			else if (result.exception != null) {
				//log.info("Exception: " + result.exception + " in " + ind);
				Tracker.runtime.getAndIncrement();
				score += worst_case; // * RT_PENALTY; // Assign minimal fitness for the list. 
			}
			else {
				assert result.retvalue instanceof int[];
				int[] candidate = (int[]) result.retvalue;
				
				score += getScore(sorted, candidate, original);
			}
		}
		
		float fitness = -1 * score;
		
		// Try fitness sharing...
//		SimpleFitness sfit = (SimpleFitness) ind.fitness;
//		
//		float fitness;
//		if (sfit.fitness() == 0.0f) {
//			fitness = -1 * score;
//		}
//		else {
//			fitness = (-1 * score + sfit.fitness()) / 2; 
//		}
		
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
	private float getScore(int[] sorted, int[] candidate, int[] original) {
		return (LIST_LENGTH - longest_increasing_subsequence(candidate) + -1 * (LIST_LENGTH - loco_score(sorted, candidate, original))) / 2;
	}

	/**
	 * Keeping some consistency...
	 * @param len
	 * @return
	 */
	public static int get_worst_LISS(int len) {
		return 1; 
	}
	
	/**
	 * Get the longest increasing subsequence.  
	 * Score will be length of the list minus longest ascending subsequence.
	 * 
	 * @param sorted
	 * @param candidate
	 * @return
	 */
	public static int longest_increasing_subsequence(int[] candidate) {
		// Algorithm taken from the pseudocode from Wikipedia.
		int[] P = new int[candidate.length];
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
			
				if (candidate[M[mid]] < candidate[i]) {
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
	
	/**
	 * Get the worst possible mean sorted position distance for a list length.
	 * @param len
	 * @return
	 */
	public static float get_worst_MSPD(int len) {
		int[] sorted = new int[len];
		int[] rev_sorted = new int[len];
		
		for (int i = 0; i < len; i++) {
			sorted[i] = i;
			rev_sorted[i] = len - i - 1;
		}
		
		return mean_sorted_position_distance(sorted, rev_sorted);
	}
	
	/**
	 * Calculate the mean sorted position distance.
	 * The maximum of this function is obtained when the list is sorted in reverse order. 
	 * 
	 * @param sorted
	 * @param candidate
	 * @return
	 */
	public static float mean_sorted_position_distance(int[] sorted, int[] candidate) {
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
			return worst_case;
		}

		
		return sum_abs_distances / sorted.length;
	}
	
	/**
	 * Calculates the mean inversion distance of the list. 
	 * Inversions are when an element comes before when it should in the sorted list.
	 * The longest possible such inversion is obtained by swapping the first and last elements. 
	 * 
	 * @param candidate
	 * @return
	 */
	public static int mean_inversion_distance(int[] candidate) {
		return 0;
	}
	
	/**
	 * Find the longest common subsequence between the sorted and candidate.
	 * Don't really know what this would accomplish, but its at least a guess. 
	 * @param sorted
	 * @param candidate 
	 * @return
	 */
	public static int lcs(int[] sorted, int[] candidate) {
		int score = 0; 
		
		int[][] memo = new int[sorted.length + 1][candidate.length + 1];
		for (int i = 0; i <= sorted.length; i++) {
			for ( int j = 0; j <= candidate.length; j++) {
				if (i == 0 || j == 0) {
					memo[i][j] = 0;
				}
				else if (sorted[i - 1] == candidate[j - 1]) {
					memo[i][j] = memo[i - 1][j - 1] + 1;
					score = Math.max(score, memo[i][j]);
				}
				else {
					memo[i][j] = 0;
				}
			}
		}
		
		return score;
	}
	
	/**
	 * Simple approach, just score based on number of differences.
	 * @param sorted
	 * @param candidate
	 * @return
	 */
	public static int difference(int[] sorted, int[] candidate) {
		int score = 0;
		
		for (int i = 0; i < sorted.length; i++) {
			if (sorted[i] != candidate[i]) {
				score++;
			}
		}
		
		return score;
	}
	
	/**
	 * This should not be used. Just here for historical reasons. :)
	 * @param sorted
	 * @param candidate
	 * @param original
	 * @return
	 */
	public static int loco_score(int[] sorted, int[] candidate, int[] original) {
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