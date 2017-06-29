package esi.finch.probs.ttt;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;

import ec.util.MersenneTwisterFast;
import esi.finch.ecj.bc.BytecodeEvaluator;
import esi.finch.ecj.bc.BytecodeIndividual;
import esi.finch.probs.ttt.TicTacToeBoard.Player;
import esi.util.Config;
import esi.util.Loader;
import esi.util.SandBox;

/**
 * Evaluator for {@link FinchPlayer}.
 *
 * @author Michael Orlov
 */
public class TicTacToeEvaluator implements BytecodeEvaluator {

	private static final Log log = Config.getLogger();

	// Package-level access for testing purposes
	public enum TurnResult {
		OK,			// Game in progress
		X_WIN,		// X won, or O failed/timeout/exception
		O_WIN,		// O won, or X failed/timeout/exception
		DRAW;		// No win after 9 correct moves

		public static TurnResult valueOf(Player player) {
			if (player == Player.X)
				return X_WIN;
			else if (player == Player.O)
				return O_WIN;
			else {
				assert player == null;
				return DRAW;
			}
		}
	}

	// Evaluates a TicTacToePlayer using TicTacToeVerifier (against RAND and BEST)
	private class StandardEvaluator {
		private final BytecodeIndividual ind;

		public StandardEvaluator(BytecodeIndividual ind) {
			this.ind = ind;
		}

		@Override
		public String toString() {
			Method eval;
			try {
				// NOTE: assumes toString() is called after all individuals were evaluated
				Class<? extends TicTacToePlayer> klass = ind.getInterruptibleMethod(0, 0, 0).getDeclaringClass().asSubclass(TicTacToePlayer.class);
				eval = klass.getDeclaredMethod("evaluate");

				return String.valueOf(eval.invoke(null));
			} catch (Exception e) {
				return e.getMessage();
			}
		}
	}

	private static final String TURN_NAME = "makeTurn";
	private static final Method TURN_METHOD;

	static {
		try {
			TURN_METHOD = TicTacToeGame.class.getDeclaredMethod(TURN_NAME);
		} catch (NoSuchMethodException e) {
			throw new Error("Unexpected: method not found", e);
		}
	}

	@Override
	public Result evaluate(BytecodeIndividual ind, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		throw new UnsupportedOperationException("Non-tournament evaluation is not implemented");
	}

	// NOTE: ECJ's random is passed to players, so timeouts definitely shouldn't occur
	//       (otherwise multi-thread access can happen)
	@Override
	public MatchResult evaluate(BytecodeIndividual ind1, BytecodeIndividual ind2, long timeout, long steps, MersenneTwisterFast random, int threadnum) {
		// class1 -> index 0, class2 -> index 1 (to distinguish steps within same thread)
		Class<? extends TicTacToePlayer> class1 = ind1.getInterruptibleMethod(steps, threadnum, 0).getDeclaringClass().asSubclass(TicTacToePlayer.class);
		Class<? extends TicTacToePlayer> class2 = ind2.getInterruptibleMethod(steps, threadnum, 1).getDeclaringClass().asSubclass(TicTacToePlayer.class);

		// No. of moves class1 takes to win (+1), but 0 actually means infinity
		int movesA = playMatch(class1, class2, timeout, random);

		// produce the classes again (to compensate for decreased steps / steps interrupts)
		class1 = ind1.getInterruptibleMethod(steps, threadnum, 0).getDeclaringClass().asSubclass(TicTacToePlayer.class);
		class2 = ind2.getInterruptibleMethod(steps, threadnum, 1).getDeclaringClass().asSubclass(TicTacToePlayer.class);
		int movesB = -playMatch(class2, class1, timeout, random);

		// Moves inverses that can actually be added
		float movesRevA = (movesA == 0)  ?  0  :  1.0f/movesA;
		float movesRevB = (movesB == 0)  ?  0  :  1.0f/movesB;
		float movesRevTotal = movesRevA + movesRevB;

		// Parsimony
		/*
		if (movesRevTotal == 0) {
			if (ind1.size() < ind2.size())
				movesRevTotal += 0.1;
			else if (ind1.size() > ind2.size())
				movesRevTotal -= 0.1;
		}
		*/

		MatchResult result = new MatchResult(movesRevTotal);
		assert !(movesA + movesB == 0)  ||  result.isDraw();

		ind1.setInfo(new StandardEvaluator(ind1));
		ind2.setInfo(new StandardEvaluator(ind2));

		log.trace("Eval: " + class1.getName() + " vs. " + class2.getName() + ": " + result.fitnessDiff);
		return result;
	}

	/**
	 * Performs one match between two {@link TicTacToePlayer}s.
	 *
	 * An invalid move, timeout, or exception end the game.
	 *
	 * @param classX X player class
	 * @param classO O player class
	 * @param timeout sandbox timeout
	 * @param random random number generator
	 * @return positive (X wins + 1) or negative (O wins + 1) number of moves, 0 if draw
	 */
	public int playMatch(Class<? extends TicTacToePlayer> classX, Class<? extends TicTacToePlayer> classO, long timeout, MersenneTwisterFast random) {
		TicTacToeBoard  board   = new TicTacToeBoard();
		TicTacToePlayer playerX = Loader.loadClassInstance(classX, board, Player.X, random);
		TicTacToePlayer playerO = Loader.loadClassInstance(classO, board, Player.O, random);
		TicTacToeGame   game    = new TicTacToeGame(board, playerX, playerO);

		// Make alternating turns.
		TurnResult outcome;
		do {
			outcome = makeTurn(classX.getName(), game, timeout);

			if (outcome == TurnResult.OK)
				outcome = makeTurn(classO.getName(), game, timeout);
		} while (outcome == TurnResult.OK);

		int result;
		// In case of draw, number of moves is 9, but 0 is returned
		if (outcome == TurnResult.DRAW) {
			assert game.getMoves() == 9;
			result = 0;
		}
		else {
			// A win can happen in 0 moves (failure of X to move)
			// Note: no. of moves may be incorrect in case of exception/timeout
			result = game.getMoves()+1;
			assert result >= 1;

			if (outcome == TurnResult.O_WIN)
				result = -result;
		}

		log.trace("Match: X=" + classX.getName() + ", O=" + classO.getName()
				+ ", Res=" + outcome + " (" + result + ")");

		return result;
	}

	// Makes one turn
	private TurnResult makeTurn(String tag, TicTacToeGame game, long timeout) {
		// In case of failure/exception/timeout, the other turn is the winner
		Player otherTurn = game.getTurn().other();

		SandBox        sandbox = new SandBox(game, TURN_METHOD, timeout);
		SandBox.Result result  = sandbox.call();

		// Timeout invalidates the individual (and shouldn't happen with steps)
		if (result == null) {
			log.warn("Timeout in " + tag);
			return TurnResult.valueOf(otherTurn);
		}
		// An exception (including steps timeout) invalidates the individual
		else if (result.exception != null) {
			log.debug("Exception: " + result.exception + " in " + tag);
			return TurnResult.valueOf(otherTurn);
		}
		else {
			// Get result (assert also checks non-null)
			assert  result.retvalue instanceof Boolean;
			boolean gameOver = (Boolean) result.retvalue;

			return gameOver  ?  TurnResult.valueOf(game.getWinner())  :  TurnResult.OK;
		}
	}

}
