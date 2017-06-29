package esi.finch.probs.ttt;

import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;

/**
 * Tic-Tac-Toe player that plays at random unoccupied locations.
 *
 * @author Michael Orlov
 */
public class RandomPlayer extends TicTacToePlayer {

	private static final Position[] ALL_POSITIONS = Position.values();

	protected final MersenneTwisterFast random;

	public RandomPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
		super(board, player);
		this.random = random;
	}

	@Override
	public void makeMove() {
		List<Position> opens = new ArrayList<Position>(9);

		for (Position pos: ALL_POSITIONS)
			if (open(pos))
				opens.add(pos);

		assert !opens.isEmpty();
		Position selected = opens.get(random.nextInt(opens.size()));

		boolean  success = play(selected);
		assert   success;
	}

}
