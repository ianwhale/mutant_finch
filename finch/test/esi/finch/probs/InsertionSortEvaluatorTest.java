package esi.finch.probs;

import static org.junit.Assert.*;
import org.junit.Test;
import ec.util.MersenneTwisterFast;
import java.util.Arrays;

public class InsertionSortEvaluatorTest {
	public static final MersenneTwisterFast random = new MersenneTwisterFast();

	/**
	 * Need to be sure that the fill list function produces a valid derangement. 
	 */
	@Test
	public void testFillList() {
		int size = 15;
		
		for (int j = 0; j < 100; j++) {
			int[] generated = InsertionSortEvaluator.fillList(random, size);
			int[] sorted = Arrays.copyOf(generated, size);
			Arrays.sort(sorted);
			
			for (int i = 0; i < size - 1; i++) {
				assertTrue(sorted[i] != sorted[i + 1]);
			}
			
			for (int i = 0; i < size; i++) {
				assertTrue(generated[i] != sorted[i]);
			}
		}
	}
	
	@Test
	public void testMeanSortedPositionDistance() {
		int[] x = {6,5,4,3,2,1};
		int[] y = {1,2,3,4,5,6};
		
		// MSPD should be at a maximum with this examples. 
		assertEquals(3.0, InsertionSortEvaluator.mean_sorted_position_distance(y, x), 0.001);
	}
	
	@Test
	public void testLongestIncreasingSubsequence() {
		int[] vdcs = {0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15};
		int len = 6;
		
		int test = InsertionSortEvaluator.longest_increasing_subsequence(vdcs);
		assertEquals(len, test);
	}
}
