package esi.finch.probs.ttt;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;

import org.junit.Test;

import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.util.Config;

public class TicTacToeBoardTest {

	private final static Log log = Config.getLogger();

	@Test
	public void board() {
		assertEquals(9, TicTacToeBoard.CELLS);
		TicTacToeBoard board = new TicTacToeBoard();

		assertNull(board.get(0, 0));
		assertNull(board.get(2, 2));
		assertNull(board.getWinner());
		assertSame(Player.X, board.getTurn());

		// X in the center
		assertFalse(board.play(1, 1, null));
		assertSame(Player.X, board.getTurn());
		assertFalse(board.play(1, 1, Player.O));
		assertSame(Player.X, board.getTurn());
		assertTrue(board.play(1, 1, Player.X));
		assertSame(Player.O, board.getTurn());

		// O in upper right corner
		assertFalse(board.play(1, 1, Player.X));
		assertFalse(board.play(1, 1, Player.O));
		assertFalse(board.play(1, 1, null));
		assertSame(Player.O, board.getTurn());
		assertFalse(board.play(0, 2, Player.X));
		assertTrue(board.play(0, 2, Player.O));
		assertSame(Player.X, board.getTurn());
		assertNull(board.getWinner());

		// Clone board in the middle
		TicTacToeBoard board2 = board.clone();
		for (int i = 0;  i < 3;  ++i)
			for (int j = 0;  j < 3;  ++j)
				assertSame(board.get(i, j), board2.get(i, j));
		assertSame(board.getTurn(), board2.getTurn());
		assertNull(board2.getWinner());

		// continue game
		assertTrue(board.play(0, 0, Player.X));
		assertTrue(board.play(2, 2, Player.O));
		assertNull(board.getWinner());
		assertTrue(board.play(1, 2, Player.X));
		assertTrue(board.play(2, 1, Player.O));
		assertNull(board.getWinner());
		assertTrue(board.play(1, 0, Player.X));
		assertSame(Player.X, board.getWinner());

		// try to continue after win
		assertFalse(board.play(1, 0, Player.O));
		assertFalse(board.play(2, 0, Player.O));
		assertSame(Player.O, board.getTurn());

		log.debug("Board:\n" + board);

		assertNotSame(board.getTurn(), board2.getTurn());
		assertNull(board2.getWinner());
		assertNull(board2.get(0, 0));
	}

	@Test
	public void columnWin() {
		TicTacToeBoard board = new TicTacToeBoard();

		assertTrue(board.play(0, 0, Player.X));
		assertTrue(board.play(0, 2, Player.O));
		assertTrue(board.play(1, 0, Player.X));
		assertTrue(board.play(1, 2, Player.O));
		assertTrue(board.play(2, 1, Player.X));
		assertTrue(board.play(2, 2, Player.O));
		assertSame(Player.O, board.getWinner());
		assertSame(Player.X, board.getTurn());

		log.debug("Board:\n" + board);
	}

	@Test
	public void diagWin() {
		TicTacToeBoard board = new TicTacToeBoard();

		assertTrue(board.play(2, 2, Player.X));
		assertTrue(board.play(0, 2, Player.O));
		assertTrue(board.play(1, 1, Player.X));
		assertTrue(board.play(2, 0, Player.O));
		assertTrue(board.play(0, 0, Player.X));
		assertSame(Player.X, board.getWinner());
		assertSame(Player.O, board.getTurn());

		assertFalse(board.play(1, 2, Player.O));
		assertSame(Player.X, board.getWinner());
		assertSame(Player.O, board.getTurn());

		log.debug("Board:\n" + board);
	}

	@Test
	public void backDiagWin() {
		TicTacToeBoard board = new TicTacToeBoard();

		assertTrue(board.play(0, 0, Player.X));
		assertTrue(board.play(0, 2, Player.O));
		assertTrue(board.play(1, 0, Player.X));
		assertTrue(board.play(2, 0, Player.O));
		assertTrue(board.play(2, 1, Player.X));
		assertTrue(board.play(1, 1, Player.O));
		assertSame(Player.O, board.getWinner());
		assertSame(Player.X, board.getTurn());

		assertFalse(board.play(2, 2, Player.X));
		assertSame(Player.O, board.getWinner());
		assertSame(Player.X, board.getTurn());

		log.debug("Board:\n" + board);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void getLimit() {
		new TicTacToeBoard().get(0, 3);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void playLimit() {
		new TicTacToeBoard().play(-1, 2, Player.X);
	}

	@Test
	public void player() {
		assertSame(Player.O, Player.X.other());
		assertSame(Player.X, Player.O.other());
	}


}
