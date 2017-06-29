package esi.finch.probs.ttt;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;

import org.junit.Test;

import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.finch.probs.ttt.TicTacToePlayer.Position;
import esi.util.Config;

public class TicTacToePlayerTest {

	private final static Log log = Config.getLogger();

	private static class TrivialTicTacToePlayer extends TicTacToePlayer {
		public TrivialTicTacToePlayer(TicTacToeBoard board, Player player) {
			super(board, player);
		}

		@Override
		public void makeMove() {
		}
	}

	@Test
	public void position() {
		String prefix = Position.values()[0].toString();
		prefix = prefix.substring(0, prefix.length()-2);

		for (int row = 0;  row < 3;  ++row)
			for (int col = 0;  col < 3;  ++col) {
				Position pos = Position.valueOf(prefix + row + col);
				assertEquals(row, pos.row());
				assertEquals(col, pos.col());
			}

		assertEquals(9, Position.values().length);

		for (Position pos: Position.values()) {
			String rep = pos.toString();
			int    row = rep.charAt(rep.length() - 2) - '0';
			int    col = rep.charAt(rep.length() - 1) - '0';
			assertEquals(row, pos.row());
			assertEquals(col, pos.col());
		}
	}

	@Test
	public void open() {
		TicTacToeBoard  board   = new TicTacToeBoard();
		TicTacToePlayer xPlayer = new TrivialTicTacToePlayer(board, Player.X);

		for (Position pos: Position.values()) {
			assertTrue(xPlayer.open(pos));
			assertFalse(xPlayer.mine(pos));
			assertFalse(xPlayer.yours(pos));
		}
	}

	@Test
	public void play() {
		TicTacToeBoard  board   = new TicTacToeBoard();
		TicTacToePlayer xPlayer = new TrivialTicTacToePlayer(board, Player.X);
		TicTacToePlayer oPlayer = new TrivialTicTacToePlayer(board, Player.O);

		assertFalse(oPlayer.play(Position.POS02));
		assertTrue(oPlayer.open(Position.POS02));

		assertTrue(xPlayer.play(Position.POS02));
		assertFalse(xPlayer.open(Position.POS02));
		assertTrue(xPlayer.mine(Position.POS02));
		assertFalse(xPlayer.yours(Position.POS02));
		assertFalse(oPlayer.open(Position.POS02));
		assertFalse(oPlayer.mine(Position.POS02));
		assertTrue(oPlayer.yours(Position.POS02));

		assertTrue(oPlayer.play(Position.POS00));
		assertTrue(xPlayer.play(Position.POS11));
		assertTrue(oPlayer.play(Position.POS10));
		assertTrue(xPlayer.play(Position.POS20));

		assertSame(Player.X, board.getWinner());
		assertFalse(oPlayer.play(Position.POS22));

		log.debug("Board:\n" + board);
	}

}
