package esi.finch.probs;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

import java.util.Arrays;
import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.util.Config;
import esi.util.SandBox;

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
	
	private static int total_lengths = 0;
	static {
		for (int[] list : testLists) {
			InsertionSortEvaluator.total_lengths += list.length;
		}
	}
	private static int minimal_fitness = -2 * total_lengths;
	
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
		Class<?> klass = ind.getInterruptibleMethod(steps, threadnum, 0).getDeclaringClass();
		
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
		for (int[] list : testLists) {	
			int[] sort_me = Arrays.copyOf(list, list.length);
			int[] original = Arrays.copyOf(list, list.length);
			int[] sorted = Arrays.copyOf(list, list.length);
			Arrays.sort(sorted);
			
			result = sandbox.call(sort_me);
			
			// Timeout 
			if (result == null) {
				// log.debug("Timeout in " + ind);
				score += NOT_MOVED * list.length; // Assign minimal fitness for the list.
			}
			// Runtime error. 
			else if (result.exception != null) {
				// log.info("Exception: " + result.exception + " in " + ind);
				score += NOT_MOVED * list.length; // Assign minimal fitness for the list. 
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
		int score = 0; 
		for (int i = 0; i < sorted.length; i++) {
			if (sorted[i] == candidate[i]) {
				score += CORRECT;
			}
			else if (candidate[i] != original[i]) {
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
