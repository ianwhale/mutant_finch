package esi.finch.probs.ttt;

import static org.junit.Assert.*;

import static esi.finch.probs.ttt.TicTacToePlayer.Position.*;

import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;

public class TicTacToeGameTest {

	private static class FailingRandomPlayer extends RandomPlayer {
		public FailingRandomPlayer(TicTacToeBoard board, Player player) {
			super(board, player, TicTacToeGameTest.random);
		}

		@Override
		public void makeMove() {
			if (random.nextFloat() < 0.75f)
				super.makeMove();
		}
	}

	private static class DeterministicPlayer extends TicTacToePlayer {
		private final Position[] moves;
		private int              index;

		public DeterministicPlayer(TicTacToeBoard board, Player player, Position... moves) {
			super(board, player);
			this.moves = moves;
			index      = 0;
		}

		@Override
		public void makeMove() {
			assertTrue(play(moves[index++]));
		}
	}

	private static MersenneTwisterFast random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(123);
	}

	@Test
	public void randomGames() {
		int xWins = 0;
		int oWins = 0;
		int draws = 0;

		for (int i = 0;  i < 1000;  ++i) {
			TicTacToeBoard  board   = new TicTacToeBoard();
			TicTacToePlayer xPlayer = new RandomPlayer(board, Player.X, random);
			TicTacToePlayer oPlayer = new RandomPlayer(board, Player.O, random);
			TicTacToeGame   game    = new TicTacToeGame(board, xPlayer, oPlayer);

			int moves = 0;

			assertSame(Player.X, game.getTurn());
			while (!game.makeTurn()) {
				assertEquals(++moves, game.getMoves());
				assertNull(game.getWinner());
			}

			assertEquals(++moves, game.getMoves());
			assertTrue(moves <= 9);

			// Non-filled board is never a draw, and winner should match no. of moves
			Player expectedWinner = (moves % 2 == 1)  ?  Player.X  :  Player.O;
			if (moves < 9  ||  game.getWinner() != null)
				assertSame(expectedWinner, game.getWinner());

			// Can be no winners due to move failures
			assertSame(board.getWinner(), game.getWinner());
			assertSame(board.getTurn(), game.getTurn());

			if (game.getWinner() == Player.X) {
				assertSame(Player.O, game.getTurn());
				++xWins;
			}
			else if (game.getWinner() == Player.O) {
				assertSame(Player.X, game.getTurn());
				++oWins;
			}
			else {
				assertSame(Player.O, game.getTurn());
				assertNull(game.getWinner());
				++draws;
			}
		}

		assertTrue(xWins > oWins + 100);
		assertTrue(oWins > 100);
		assertTrue(draws > 50);
	}

	@Test
	public void failingRandomGames() {
		int xWins = 0;
		int oWins = 0;
		int draws = 0;
		int fails = 0;

		for (int i = 0;  i < 1000;  ++i) {
			TicTacToeBoard  board   = new TicTacToeBoard();
			TicTacToePlayer xPlayer = new FailingRandomPlayer(board, Player.X);
			TicTacToePlayer oPlayer = new FailingRandomPlayer(board, Player.O);
			TicTacToeGame   game    = new TicTacToeGame(board, xPlayer, oPlayer);

			int moves = 0;

			while (!game.makeTurn()) {
				assertEquals(++moves, game.getMoves());
				assertNull(game.getWinner());
			}

			boolean failedMove;
			if (moves+1 == game.getMoves()) {
				assertSame(board.getWinner(), game.getWinner());
				assertTrue(moves <= 9);
				++moves;
				failedMove = false;
			}
			// Can be winners due to move failures
			else {
				assertNotNull(game.getWinner());
				assertNull(board.getWinner());
				assertTrue(moves < 9);
				++fails;
				failedMove = true;
			}

			// Non-filled board is never a draw, and winner should match no. of moves
			Player expectedWinner = (moves % 2 == 1)  ?  Player.X  :  Player.O;
			if (!failedMove) {
				if (moves < 9  ||  game.getWinner() != null)
					assertSame(expectedWinner, game.getWinner());
			}
			else
				assertSame(expectedWinner, game.getWinner());

			if (game.getWinner() == Player.X) {
				assertSame(Player.O, game.getTurn());
				++xWins;
			}
			else if (game.getWinner() == Player.O) {
				assertSame(Player.X, game.getTurn());
				++oWins;
			}
			else {
				assertSame(Player.O, game.getTurn());
				assertNull(game.getWinner());
				++draws;
			}
		}

		assertTrue(xWins < oWins);
		assertTrue(xWins > 300);
		assertTrue(draws > 0);
		assertTrue(fails > 700);
	}

	@Test
	public void lastMoveWin() {
		TicTacToeBoard  board   = new TicTacToeBoard();
		TicTacToePlayer xPlayer = new DeterministicPlayer(board, Player.X,
				POS00, POS02, POS11, POS12, POS20);
		TicTacToePlayer oPlayer = new DeterministicPlayer(board, Player.O,
				POS01, POS10, POS22, POS21);
		TicTacToeGame   game    = new TicTacToeGame(board, xPlayer, oPlayer);

		int moves = 0;
		while (!game.makeTurn()) {
			assertEquals(++moves, game.getMoves());
			assertNull(game.getWinner());
		}

		assertEquals(9, game.getMoves());
		assertSame(Player.X, game.getWinner());
		assertSame(Player.O, game.getTurn());
	}

}
