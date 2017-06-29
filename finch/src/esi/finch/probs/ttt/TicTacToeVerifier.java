package esi.finch.probs.ttt;

import ec.util.MersenneTwisterFast;
import esi.util.SpecializedConstants;

/**
 * Verifier for Tic-Tac-Tow evolved solutions.
 * The players are evaluated against RAND (random) and BEST (optimal) players.
 *
 * <p>Constants:
 * <ul>
 * <li><code>class.TicTacToeVerifier.timeout</code>: timeout during single match
 * <li><code>class.TicTacToeVerifier.count</code>: number of matches (half as X, half as O)
 * </ul>
 *
 * @author Michael Orlov
 */
public class TicTacToeVerifier {

	private static final long   TIMEOUT     = SpecializedConstants.getInt(TicTacToeVerifier.class, "timeout");
	private static final int    COUNT       = SpecializedConstants.getInt(TicTacToeVerifier.class, "count");

	private final MersenneTwisterFast random;

	public TicTacToeVerifier(MersenneTwisterFast random) {
		this.random = random;
	}

	public String evaluatePlayer(Class<? extends TicTacToePlayer> testPlayer) {
		Class<? extends TicTacToePlayer> randomPlayer  = RandomPlayer.class;
		Class<? extends TicTacToePlayer> optimalPlayer = OptimalPlayer.class;

		return evaluatePlayer("RAND", testPlayer, randomPlayer,  COUNT, TIMEOUT) + "\n"
			 + evaluatePlayer("BEST", testPlayer, optimalPlayer, COUNT, TIMEOUT) + "\n";
	}

	private String evaluatePlayer(String tag, Class<? extends TicTacToePlayer> testPlayer, Class<? extends TicTacToePlayer> expertPlayer,
			int count, long timeout) {
		TicTacToeEvaluator evaluator = new TicTacToeEvaluator();

		int wins   = 0;
		int losses = 0;
		int draws  = 0;

		for (int i = 0;  i < count;  ++i) {
			int moves = (i%2 == 0)  ?  evaluator.playMatch(testPlayer, expertPlayer, timeout, random)
					:  -evaluator.playMatch(expertPlayer, testPlayer, timeout, random);

			if (moves > 0)
				++wins;
			else if (moves < 0)
				++losses;
			else
				++draws;
		}

		return tag + " - Wins: " + wins + ", Draws: " + draws + ", Losses: " + losses;
	}

}
