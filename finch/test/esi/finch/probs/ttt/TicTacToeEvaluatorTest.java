package esi.finch.probs.ttt;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.finch.probs.ttt.TicTacToeEvaluator.TurnResult;

public class TicTacToeEvaluatorTest {

	// Taken from TicTacToeGameTest
	public static class FailingRandomPlayer extends RandomPlayer {
		public FailingRandomPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
			super(board, player, random);
		}

		@Override
		public void makeMove() {
			if (random.nextFloat() < 0.75f)
				super.makeMove();
		}
	}

	// Immediate failure to make move
	public static class ImmediateFailPlayer extends TicTacToePlayer {
		public ImmediateFailPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
			super(board, player);
		}

		@Override
		public void makeMove() {
			// Do nothing
		}
	}

	// Failure to make move after one good move
	public static class PostponedFailPlayer extends RandomPlayer {
		int count = 2;

		public PostponedFailPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
			super(board, player, random);
		}

		@Override
		public void makeMove() {
			if (--count > 0)
				super.makeMove();
			else
				throw new Error("Imitating move failure");
		}
	}

	private static MersenneTwisterFast random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(456);
	}

	@Test
	public void randomMatches() {
		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();
		assertTrue(evaluator instanceof BytecodeEvaluator);

		int xWins   = 0;
		int oWins   = 0;
		int draws   = 0;
		int immWins = 0;

		for (int i = 0;  i < 1000;  ++i) {
			int moves = evaluator.playMatch(RandomPlayer.class, RandomPlayer.class, 1000, random);
			assertTrue(Math.abs(moves) <= 9+1);

			if (Math.abs(moves) == 1)
				++immWins;

			if (moves > 0)
				++xWins;
			else if (moves < 0)
				++oWins;
			else
				++draws;
		}

		// Taken from TicTacToeGameTest
		assertTrue(xWins > oWins + 100);
		assertTrue(oWins > 100);
		assertTrue(draws > 50);
		assertEquals(0, immWins);
	}

	@Test
	public void failingRandomMatches() {
		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();

		int xWins   = 0;
		int oWins   = 0;
		int draws   = 0;
		int immWins = 0;

		for (int i = 0;  i < 1000;  ++i) {
			int moves = evaluator.playMatch(FailingRandomPlayer.class, FailingRandomPlayer.class, 1000, random);
			assertTrue(Math.abs(moves) <= 9+1);

			if (Math.abs(moves) == 1)
				++immWins;

			if (moves > 0)
				++xWins;
			else if (moves < 0)
				++oWins;
			else
				++draws;
		}

		// Taken from TicTacToeGameTest
		assertTrue(xWins < oWins);
		assertTrue(xWins > 300);
		assertTrue(draws > 0);

		// Immediate win probability is 0.25
		assertTrue(immWins > 150);
		assertTrue(immWins < 350);
	}

	@Test
	public void oneMoveMatch() {
		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();
		assertEquals(1+1, evaluator.playMatch(RandomPlayer.class, ImmediateFailPlayer.class, 1000, random));
		assertEquals(-(0+1), evaluator.playMatch(ImmediateFailPlayer.class, RandomPlayer.class, 1000, random));
	}

	@Test
	public void twoMoveMatch() {
		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();
		assertEquals(3+1, evaluator.playMatch(RandomPlayer.class, PostponedFailPlayer.class, 1000, random));
		assertEquals(-(2+1), evaluator.playMatch(PostponedFailPlayer.class, RandomPlayer.class, 1000, random));
	}

	@Test
	public void valueOf() {
		assertSame(TurnResult.X_WIN, TurnResult.valueOf(Player.X));
		assertSame(TurnResult.O_WIN, TurnResult.valueOf(Player.O));
		assertSame(TurnResult.DRAW,  TurnResult.valueOf((Player) null));
	}

}
