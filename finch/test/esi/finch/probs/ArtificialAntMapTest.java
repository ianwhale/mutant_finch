package esi.finch.probs;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArtificialAntMapTest {

	@Test
	public void santaFeLoad() {
		ArtificialAntMap antMap = new ArtificialAntMap(ArtificialAntMap.class.getResource("santafe.trl"));
		boolean[][] trail = antMap.foodMap;

		assertEquals(32, trail.length);
		for (int i = 0;  i < trail.length;  ++i)
			assertEquals(32, trail[i].length);

		assertEquals(32, antMap.height);
		assertEquals(32, antMap.width);

		assertFalse(trail[0][0]);
		assertTrue(trail[0][1]);
		assertFalse(trail[1][0]);

		assertTrue(trail[5][6]);
		assertFalse(trail[5][7]);
		assertTrue(trail[5][8]);

		int pellets = 0;
		for (int i = 0;  i < trail.length;  ++i)
			for (int j = 0;  j < trail[i].length;  ++j)
				pellets += trail[i][j] ? 1 : 0;

		assertEquals(89, pellets);
		assertEquals(89, antMap.totalFood);
	}

}
