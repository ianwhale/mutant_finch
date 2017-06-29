package esi.finch.probs.ttt;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.util.Config;

public class OptimalPlayerTest {

	private final static Log log = Config.getLogger();

	private static MersenneTwisterFast random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new MersenneTwisterFast(780);
	}

	@Test
	public void makeMove() {
		for (int i = 0;  i < 50;  ++i) {
			TicTacToeBoard  board   = new TicTacToeBoard();
			TicTacToePlayer playerX = new OptimalPlayer(board, Player.X, random);
			TicTacToePlayer playerO = new OptimalPlayer(board, Player.O, random);

			int count = 9;
			do {
				playerX.makeMove();
				assertNull(board.getWinner());
				assertTrue(board.getTurn() == Player.O);

				if (--count > 0) {
					playerO.makeMove();
					assertNull(board.getWinner());
					assertTrue(board.getTurn() == Player.X);
				}
			} while (--count > 0);

			assertNull(board.getWinner());
			assertTrue(board.getTurn() == Player.O);
		}
	}

	@Test
	public void optimalGames() {
		Class<? extends TicTacToePlayer> optimalPlayer = OptimalPlayer.class;
		Class<? extends TicTacToePlayer> randomPlayer  = RandomPlayer.class;

		final int  COUNT   = 100;
		final long TIMEOUT = 5000;

		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();

		int wins   = 0;
		int losses = 0;
		int draws  = 0;

		for (int i = 0;  i < COUNT;  ++i) {
			int moves = (i%2 == 0)  ?  evaluator.playMatch(optimalPlayer, randomPlayer, TIMEOUT, random)
					:  -evaluator.playMatch(randomPlayer, optimalPlayer, TIMEOUT, random);

			if (moves > 0)
				++wins;
			else if (moves < 0)
				++losses;
			else
				++draws;
		}

		assertTrue(draws < 15);
		assertEquals(0, losses);
	}

	// @Test
	public void humanGame() {
		TicTacToeBoard  board   = new TicTacToeBoard();
		TicTacToePlayer xPlayer = new HumanPlayer(board, Player.X, random);
		TicTacToePlayer oPlayer = new OptimalPlayer(board, Player.O, random);
		TicTacToeGame   game    = new TicTacToeGame(board, xPlayer, oPlayer);

		while (!game.makeTurn())
			log.info("Board:\n" + board);
		log.info("Board:\n" + board);

		log.info("Winner: " + game.getWinner());
	}

}
