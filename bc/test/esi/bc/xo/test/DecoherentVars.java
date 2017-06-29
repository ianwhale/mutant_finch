package esi.bc.xo.test;

public class DecoherentVars {

	// Explicit offsets are used in CompatibleCrossoverTest
	// and CodeAccessesTest
	@SuppressWarnings("unused")
	public void foo() {
		{
			// (1,2) <- (J,T)
			long x = 5;

			// Code decoherenting "T" inserted here
			// (2,3) <- (X,I)

			// (3,4) <- (J,T) <- (1,2)
			long y = x;
		}

		{
			// 1 <- I
			int x = 1;
			// (2 <- T)
			int y;

			// 3 <- I
			int z = 0;
			if (z > 2)
				// 2 <- I
				y = z;

			// 2 is T here

			// 3 <- I
			z = 1;
		}
	}

}
