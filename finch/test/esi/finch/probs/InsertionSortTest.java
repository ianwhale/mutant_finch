package esi.finch.probs;

import static org.junit.Assert.*;
import org.junit.Test;
import ec.util.MersenneTwisterFast;
import java.util.Arrays;

public class InsertionSortTest {
	public static final MersenneTwisterFast random = new MersenneTwisterFast();

	/**
	 * Need to be sure that the fill list function produces a valid derangement. 
	 */
	@Test
	public void testFillList() {
		int size = 10;
		
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
}
