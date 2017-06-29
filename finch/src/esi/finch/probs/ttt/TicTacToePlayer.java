package esi.finch.probs.ttt;

import esi.finch.probs.ttt.TicTacToeBoard.Player;

public abstract class TicTacToePlayer {

	protected enum Position {
		POS00 {
			@Override public int row() { return 0; }
			@Override public int col() { return 0; }
		},
		POS01 {
			@Override public int row() { return 0; }
			@Override public int col() { return 1; }
		},
		POS02 {
			@Override public int row() { return 0; }
			@Override public int col() { return 2; }
		},
		POS10 {
			@Override public int row() { return 1; }
			@Override public int col() { return 0; }
		},
		POS11 {
			@Override public int row() { return 1; }
			@Override public int col() { return 1; }
		},
		POS12 {
			@Override public int row() { return 1; }
			@Override public int col() { return 2; }
		},
		POS20 {
			@Override public int row() { return 2; }
			@Override public int col() { return 0; }
		},
		POS21 {
			@Override public int row() { return 2; }
			@Override public int col() { return 1; }
		},
		POS22 {
			@Override public int row() { return 2; }
			@Override public int col() { return 2; }
		};

		public abstract int row();
		public abstract int col();
	}

	// Board must not be directly changed by deriving classes
	protected final TicTacToeBoard board;
	protected final Player         player;

	public TicTacToePlayer(TicTacToeBoard board, Player player) {
		this.board  = board;
		this.player = player;
	}

	protected boolean open(Position pos) {
		return board.get(pos.row(), pos.col()) == null;
	}

	protected boolean mine(Position pos) {
		return board.get(pos.row(), pos.col()) == player;
	}

	protected boolean yours(Position pos) {
		return board.get(pos.row(), pos.col()) == player.other();
	}

	protected boolean play(Position pos) {
		return board.play(pos.row(), pos.col(), player);
	}

	/**
	 * Makes a move. An open cell is guaranteed to be available.
	 */
	public abstract void makeMove();

}
