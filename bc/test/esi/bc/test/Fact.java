package esi.bc.test;

/**
 * A simple recursive factorial function that is used
 * in the ESI paper.
 *
 * Offsets are used in tests -- code should not be changed.
 *
 * @author Michael Orlov
 */
public class Fact {

	public int fact(int n) {
		// 0-1
		int ans = 1;

		// 2-3
		if(n > 0)
			// 6-15
			ans = n * fact(n-1);

		// 16-17
		return ans ;
	}

}
