package esi.finch.probs.ttt;

import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.util.SandBox;

/**
 * Evolving Tic-Tac-Toe player, modeled after Angeline and Pollack papers.
 *
 * @author Michael Orlov
 * @see Competitive Environments Evolve Better Solutions for Complex Tasks, ICGA5, ISBN 1-55860-299-2
 * @see The Evolutionary Induction of Subroutines, CogSci92, ISBN 0-805-81291-1
 */
public class FinchPlayer extends TicTacToePlayer {

	private static final Position[] ALL_POSITIONS = Position.values();

	private final MersenneTwisterFast random;
	private       Position            chosenMove;

	public FinchPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
		super(board, player);
		this.random = random;
	}

	@Override
	public void makeMove() {
		// Optimization if first move
		if (getFreeCells(board).length == ALL_POSITIONS.length)
			chosenMove = ALL_POSITIONS[random.nextInt(ALL_POSITIONS.length)];
		else
			// Evolved players may mess up the board!
			negamaxAB(board.clone(), -Integer.MAX_VALUE, Integer.MAX_VALUE, true);

		play(chosenMove);
	}

	// returns 1+number of cells left when winning (negative when losing, 0 when draw)
	private int negamaxAB(TicTacToeBoard board, int alpha, int beta, boolean save) {
		Position[] free = getFreeCells(board);

		// Negamax leafs
		if (board.getWinner() != null)
			alpha = utility(board, free);
		else if (free.length == 0)
			// BUG 1: replace alpha=0 with save=false
			alpha = 0;
		else
			for (Position move: free) {
				TicTacToeBoard boardCopy = board.clone();
				boardCopy.play(move.row(), move.col(), boardCopy.getTurn());

				// BUG 2: remove unary minus before the recursive call
				// BUG 3: pass save instead of false to the recursive call
				int utility = -negamaxAB(boardCopy, -beta, -alpha, false);
				if (utility > alpha) {
					alpha = utility;
					if (save)
						chosenMove = move;

					// BUG 4: swap alpha and beta in conditional
					if (alpha >= beta)
						break;
				}
			}

		return alpha;
	}

	private Position[] getFreeCells(TicTacToeBoard board) {
		List<Position> free = new ArrayList<Position>(9);

		for (Position pos: ALL_POSITIONS)
			if (board.get(pos.row(), pos.col()) == null)
				free.add(pos);

		Position[] freeA = free.toArray(new Position[0]);

		// randomize
		for (int i = 1;  i < freeA.length;  ++i) {
			// pick j in [0 .. i]
			int j = random.nextInt(i+1);

			// swap array[i], array[j]
			Position tmp = freeA[i];
			freeA[i] = freeA[j];
			freeA[j] = tmp;
		}

		return freeA;
	}

	private int utility(TicTacToeBoard board, Position[] free) {
		// the more cells left when winning, the better
		int absFitness = free.length + 1;

		return (board.getWinner() == board.getTurn())  ?  absFitness  :  -absFitness;
	}

	public static void main(String[] args) {
		System.out.print(evaluate());
		System.out.flush();

		SandBox.shutdown();
	}

	// Called from TicTacToeEvaluator by name
	public static String evaluate() {
		// Time-based seed
		TicTacToeVerifier verifier = new TicTacToeVerifier(new MersenneTwisterFast());

		// Class will be renamed here as well
		return verifier.evaluatePlayer(FinchPlayer.class);
	}

}
