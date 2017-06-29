package esi.finch.probs.ttt;

import java.util.InputMismatchException;
import java.util.Scanner;

import ec.util.MersenneTwisterFast;
import esi.finch.probs.ttt.TicTacToeBoard.Player;

/**
 * Player that reads move in form of XX (row-col) from console.
 *
 * @author Michael Orlov
 */
public class HumanPlayer extends TicTacToePlayer {

	private final static String POS_PREFIX;
	private final Scanner sc;

	static {
		String pref = Position.POS00.name();
		pref = pref.substring(0, pref.length()-2);

		POS_PREFIX = pref;
	}

	public HumanPlayer(TicTacToeBoard board, Player player, MersenneTwisterFast random) {
		super(board, player);
		sc = new Scanner(System.in);
	}

	@Override
	public void makeMove() {
		while (true) {
			System.out.flush();
			System.out.print("Your move: ");
			System.out.flush();

			try {
				String move = sc.next("[0-2][0-2]");

				if (play(Position.valueOf(POS_PREFIX + move)))
					break;
				else
					System.out.println("Illegal move");
			} catch (InputMismatchException e) {
				sc.nextLine();
				System.out.println("Enter one of 00 .. 22");
			}
		};
	}

}
