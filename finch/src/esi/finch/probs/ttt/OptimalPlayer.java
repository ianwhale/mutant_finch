package esi.finch.probs.ttt;

import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;

/**
 * Optimal Tic-Tac-Toe player, choosing uniformly between moves
 * with minimal number of moves-to-win (if win is possible).
 *
 * @author Michael Orlov
 */
public class OptimalPlayer extends TicTacToePlayer {

	private static final Position[] ALL_POSITIONS = Position.values();

	private final MersenneTwisterFast random;
	private       Position            chosenMove;

	public OptimalPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
		super(board, player);
		this.random = random;
	}

	@Override
	public void makeMove() {
		// Optimization if first move
		if (getOpens(board).size() == ALL_POSITIONS.length)
			chosenMove = ALL_POSITIONS[random.nextInt(ALL_POSITIONS.length)];
		else {
			int result = negamaxAB(board, -Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assert result >= 0;
		}

		boolean check = play(chosenMove);
		assert  check;
	}

	// returns 1+number of cells left when winning (negative when losing, 0 when draw)
	private int negamaxAB(TicTacToeBoard board, int alpha, int beta, boolean first) {
		List<Position> opens = getOpens(board);

		// Negamax leafs
		if (board.getWinner() != null)
			return fitness(board, opens);
		if (opens.isEmpty())
			return 0;

		for (Position pos: opens) {
			TicTacToeBoard newBoard = board.clone();
			boolean check = newBoard.play(pos.row(), pos.col(), newBoard.getTurn());
			assert  check;

			int newFitness = -negamaxAB(newBoard, -beta, -alpha, false);
			if (newFitness > alpha) {
				alpha = newFitness;
				if (first)
					chosenMove = pos;

				if (alpha >= beta)
					return alpha;
			}
		}

		return alpha;
	}

	private List<Position> getOpens(TicTacToeBoard board) {
		List<Position> opens = new ArrayList<Position>(9);

		for (Position pos: ALL_POSITIONS)
			if (board.get(pos.row(), pos.col()) == null)
				opens.add(pos);

		// randomize
		for (int i = 1;  i < opens.size();  ++i) {
			// pick j in [0 .. i]
			int j = random.nextInt(i+1);

			// swap array[i], array[j]
			Position tmp = opens.get(i);
			opens.set(i, opens.get(j));
			opens.set(j, tmp);
		}

		return opens;
	}

	private int fitness(TicTacToeBoard board, List<Position> opens) {
		// the more cells left when winning, the better
		int absFitness = opens.size() + 1;

		return (board.getWinner() == board.getTurn())  ?  absFitness  :  -absFitness;
	}

}
