package esi.finch.ecj.bc;

import static org.junit.Assert.*;

import org.junit.Test;

public class BytecodeProblemTest {

	@Test
	public void testIsPowerOf2() {
		for (int i = 1;  i > 0;  i *= 2)
			assertTrue(BytecodeProblem.isPowerOf2(i));

		assertFalse(BytecodeProblem.isPowerOf2(0));
		assertFalse(BytecodeProblem.isPowerOf2(3));
		assertFalse(BytecodeProblem.isPowerOf2(5));
		assertFalse(BytecodeProblem.isPowerOf2(6));
		assertFalse(BytecodeProblem.isPowerOf2(7));
		assertFalse(BytecodeProblem.isPowerOf2(9));

		assertFalse(BytecodeProblem.isPowerOf2(-1));
		assertFalse(BytecodeProblem.isPowerOf2(-2));
		assertFalse(BytecodeProblem.isPowerOf2(-3));
		assertFalse(BytecodeProblem.isPowerOf2(-4));

		assertFalse(BytecodeProblem.isPowerOf2(Integer.MIN_VALUE));
		assertFalse(BytecodeProblem.isPowerOf2(Integer.MAX_VALUE));
	}

}
