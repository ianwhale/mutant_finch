package esi.finch.probs.ttt;

/**
 * Tic-Tac-Toe board.
 *
 * The board is the classic 3x3 X-O game. Players X and O place
 * marks in empty squares consecutively (X starts), and the first
 * to place 3 of his marks in a row, column, or diagonal, wins.
 *
 * Since no moves are allowed after winning, the game can be played
 * until a player fails to change turn (except that there is a problem
 * with draw).
 *
 * @author Michael Orlov
 */
public class TicTacToeBoard implements Cloneable {

	public static final int CELLS = 9;

	public enum Player {
		X { @Override public Player other() { return O; } },
		O { @Override public Player other() { return X; } };

		/**
		 * @return the other player (O for X, X for O)
		 */
		public abstract Player other();
	}

	private Player[][] board;		// 3x3 board (null for empty cells)
	private Player     turn;		// whose turn is it (X or O)?
	private Player     winner;		// game winner (null if none)

	public TicTacToeBoard() {
		board  = new Player[3][3];
		turn   = Player.X;
		winner = null;
	}

	@Override
	public TicTacToeBoard clone() {
		try {
			TicTacToeBoard copy = (TicTacToeBoard) super.clone();

			// Deep-copy the board
			copy.board = copy.board.clone();
			for (int row = 0;  row < copy.board.length;  ++row)
				copy.board[row] = copy.board[row].clone();

			return copy;
		} catch (CloneNotSupportedException e) {
			throw new Error("Unexpected exception", e);
		}
	}

	/**
	 * Returns the current turn, X always has the first turn.
	 *
	 * @return current turn, X or O
	 */
	public Player getTurn() {
		return turn;
	}

	/**
	 * Cell accessor.
	 *
	 * @param row cell row, 0-2
	 * @param col cell column, 0-2
	 * @return player at cell, X, O, or <code>null</code> for open cell
	 */
	public Player get(int row, int col) {
		return board[row][col];
	}

	/**
	 * If <code>true</code> is returned, plays with given player
	 * at specified cell, and flips turn.
	 *
	 * Does not play if any player has already won.
	 *
	 * @param row cell row, 0-2
	 * @param col cell column, 0-2
	 * @param player player, X or O
	 * @return <code>true</code> if the move is correct for the current turn,
	 *         the specified cell was open, and the game was not over prior to play
	 */
	public boolean play(int row, int col, Player player) {
		// not player's turn
		if (getTurn() != player)
			return false;

		// not an open cell
		if (get(row, col) != null)
			return false;

		// game is over
		if (getWinner() != null)
			return false;

		board[row][col] = player;
		turn = player.other();

		if (isWinningMove(row, col))
			winner = player;

		return true;
	}

	// Checks whether last move at (row, col) ended the game
	private boolean isWinningMove(int row, int col) {
		// check row
		if (get(row, 0) == get(row, 1)  &&  get(row, 0) == get(row, 2))
			return true;

		// check column
		if (get(0, col) == get(1, col)  &&  get(0, col) == get(2, col))
			return true;

		// check main diagonal
		if (row == col  &&  get(0, 0) == get(1, 1)  &&  get(0, 0) == get(2, 2))
			return true;

		// check second diagonal
		if (row == 2-col  &&  get(0, 2) == get(1, 1)  &&  get(0, 2) == get(2, 0))
			return true;

		return false;
	}

	/**
	 * Determine whether a player has won the game by placing
	 * 3 X's or O's in a row, column, or diagonal.
	 *
	 * @return the winner, or <code>null</code> if none
	 */
	public Player getWinner() {
		return winner;
	}

	@Override
	public String toString() {
		String        sep = "+---+---+---+\n";
		StringBuilder buf = new StringBuilder(sep);

		for (Player[] row: board) {
			for (Player player: row)
				buf.append("| ").append(player == null  ?  " "  :  player).append(' ');
			buf.append("|\n").append(sep);
		}

		return buf.toString();
	}

}
