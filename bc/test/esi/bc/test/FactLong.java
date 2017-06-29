package esi.bc.test;

/**
 * A simple recursive factorial function that is used in
 * the ESI paper, where ints are replaced with longs.
 *
 * @author Michael Orlov
 */
public class FactLong {

	public long fact(long n) {
		// 0-1
		long ans = 1;

		// 2-3
		if(n > 0)
			// 6-15
			ans = n * fact(n-1);

		// 16-17
		return ans ;
	}

}
