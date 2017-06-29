package esi.bc.manip.test;

public class Loops {

	public void simple() {
		while (true);
	}

	public void complex() {
		for (int i = 0;  i < 1000000;  ++i) {
			if (i%3 != 2)
				for (int j = i;  j < 1000000;  ++j) {
					int k = j - i;
					while (k < 1000000  &&  k > i)
						k = k + 2;
				}

			switch (i % 3) {
			case 0:
				foo();
			case 1:
				break;
			case 2:
				i = Math.max(i, i+1);
				break;
			default:
				throw new Error("Impossible!");
			}
		}
	}

	private void foo() {
		// Nothing
	}

}
