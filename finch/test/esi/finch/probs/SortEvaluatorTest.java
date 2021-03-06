package esi.finch.probs;

import static org.junit.Assert.*;
import org.junit.Test;
import ec.util.MersenneTwisterFast;
import java.util.Arrays;

public class SortEvaluatorTest {
	public static final MersenneTwisterFast random = new MersenneTwisterFast(42);

	/**
	 * Need to be sure that the fill list function produces a valid derangement. 
	 */
	@Test
	public void testFillList() {
		int size = 15;
		
		for (int j = 0; j < 100; j++) {
			int[] generated = Derange.fillList(random, size);
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
		
		int[][] lists = {x};
		SortScore mspd = new MSPDScore(lists);
		
		mspd.score(x, y, x);
		
		// MSPD should be at a maximum with this examples. 
		assertEquals(3.0, -mspd.getScore(), 0.001);
	}
	
	@Test
	public void testLongestIncreasingSubsequence() {
		int[] vdcs = {0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15};
		int len = 6;
		
		int test = REMScore.longestIncreasingSubsequence(vdcs);
		assertEquals(len, test);
	}
	
	@Test 
	public void testGetRawLocoScore() {
		int[] original = {4, 2, -1, 0, 3};
		int[] sorted = {-1, 0, 2, 3, 4};
		int[] candidate = {0, -1, 2, 3, 4};
		int[][] test_lists = {original};
		
		LocoScore ls = new LocoScore(test_lists);
		ls.score(candidate, sorted, original);
		
		assertEquals(7.0, ls.getRawScore(), 0.0);
	}
}
