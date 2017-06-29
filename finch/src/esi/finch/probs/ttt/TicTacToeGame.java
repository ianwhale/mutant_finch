package esi.finch.probs.ttt;

import esi.finch.probs.ttt.TicTacToeBoard.Player;

/**
 * Tic-Tac-Toe games maker.
 *
 * @author Michael Orlov
 */
public class TicTacToeGame {

	private final TicTacToeBoard  board;
	private final TicTacToePlayer xPlayer;
	private final TicTacToePlayer oPlayer;

	private int     moves;
	private boolean gameOver;
	private Player  winner;

	public TicTacToeGame(TicTacToeBoard board, TicTacToePlayer xPlayer, TicTacToePlayer oPlayer) {
		this.board   = board;
		this.xPlayer = xPlayer;
		this.oPlayer = oPlayer;

		moves    = 0;
		gameOver = false;
		winner   = null;
	}

	/**
	 * @return current turn
	 */
	public Player getTurn() {
		return board.getTurn();
	}

	/**
	 * Returns game winner, due to actual win, or due to the
	 * other player failing to make a correct move.
	 *
	 * @return the winner, or <code>null</code> if none
	 */
	public Player getWinner() {
		return winner;
	}

	/**
	 * @return number of moves made in the game
	 */
	public int getMoves() {
		return moves;
	}

	/**
	 * Makes one move by the player whose turn it is now.
	 *
	 * @return <code>true</code> if game is over after the move
	 * @throws IllegalStateException if game was over prior to the call
	 */
	public boolean makeTurn() {
		if (gameOver)
			throw new IllegalStateException("Game is over");

		// Make one move according to the turn
		final Player    turn   = board.getTurn();
		TicTacToePlayer player = (turn == Player.X)  ?  xPlayer  :  oPlayer;
		player.makeMove();

		// Turn remains unchanged if player failed to make a correct move
		if (turn == board.getTurn()) {
			gameOver = true;
			winner   = turn.other();
		}
		// Maximum number of moves also implies game over
		else if (++moves == TicTacToeBoard.CELLS) {
			gameOver = true;
		}

		// Check for regular win
		if (board.getWinner() != null) {
			assert winner == null;
			assert board.getWinner() == turn;

			gameOver = true;
			winner   = turn;
		}

		return gameOver;
	}

}
