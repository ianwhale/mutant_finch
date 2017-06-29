package esi.finch.probs;

import static org.junit.Assert.*;

import org.junit.Test;

import esi.finch.probs.ArtificialAnt.OperationsLimit;

public class ArtificialAntTest {

	@Test
	public void testArtificialAnt() {
		ArtificialAnt ant = new ArtificialAnt(0);
		boolean[][] model = ArtificialAnt.antMap.foodMap;

		for (int i = 0;  i < model.length;  ++i)
			for (int j = 0;  j < model[i].length;  ++j)
				if (i != 0  ||  j != 0)
					assertFalse(ant.visitMap[i][j]);
		assertTrue(ant.visitMap[0][0]);

		assertEquals(0, ant.getEatenCount());
		assertFalse(ant.ateAll());
		System.out.println(ant);
	}

	@Test
	public void testMovement() {
		ArtificialAnt ant = new ArtificialAnt(9+1);

		assertTrue(ant.foodAhead());
		ant.move();
		assertTrue(ant.foodAhead());
		ant.move();
		assertTrue(ant.foodAhead());
		ant.right();
		assertFalse(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());
		ant.left();
		assertTrue(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());

		ant.left();
		assertFalse(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());
		ant.left();
		assertTrue(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());	// already eaten ahead!

		ant.right();
		assertFalse(ant.foodAhead());
		ant.move();
		assertTrue(ant.foodAhead());
		ant.move();
		assertFalse(ant.foodAhead());
		ant.left();
		assertTrue(ant.foodAhead());

		assertEquals(5, ant.getEatenCount());
		System.out.println(ant);
	}

	@Test(expected = OperationsLimit.class)
	public void operationsLimitMove() {
		ArtificialAnt ant = new ArtificialAnt(1);
		ant.right();
		ant.move();
	}

	@Test
	public void operationsLimitTurn() {
		ArtificialAnt ant = new ArtificialAnt(0);

		// Turns do *not* count (contrary to Koza I)
		ant.left();
		ant.right();
	}

	@Test
	public void testAvoider() {
		ArtificialAnt ant = new ArtificialAnt(1000);
		for (int i = 0;  i < 100;  ++i)
			ant.step();

		assertEquals(0, ant.getEatenCount());
		System.out.println(ant);
	}

	@Test
	public void testSolution() {
		// When counting turns, the limit is 545
		// When counting moves, the limit is 144
		// When counting non-eating moves, the limit is 55
		int limit = 55 + 1;
		ArtificialAnt ant = new ArtificialAnt(limit);

		try {
			while (true)
				ant.stepSolution();
		} catch (OperationsLimit e) {
			assertEquals(limit, e.ops);
		}

		assertEquals(ArtificialAnt.antMap.totalFood, ant.getEatenCount());
		assertTrue(ant.ateAll());
		System.out.println(ant);
	}

}
