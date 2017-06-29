package esi.finch.probs;

import java.util.ArrayList;
import java.util.List;

/**
 * Array Sum (sumlist).
 *
 * Test and verification lists are taken from p.60 of the paper.
 *
 * @author Michael Orlov
 * @see <a href="http://dx.doi.org/10.1007/s10710-008-9069-7">An Improved Representation for Evolving Programs</a>
 */
public class ArraySum {

	public static final int[][] testLists = {
		{4, 3, 2, 1},
		{1, 2, 55, 3},
		{1, 999, 2, 3},
		{71, 1, 2, 3},
		{1, 2, 33},
		{100, 88, 211},
		{100, 1, 2},
		{13, 7},
		{5, 55},
		{10}
	};

	public static final int[][] verifyLists = {
		{-52, 15},
		{-36, 59, 49, -3},
		{29},
		{24, -1, 60, -72, -63},
		{-43, 54, -11, -16, 56},
		{45, 17, 82, 58, 28, 84, 21, 67, -98},
		{13, -52, 47, -34},
		{32, 16, -64, -11, -53, -32, 45, 61, -36},
		{76, 20, -44, 8},
		{14, -88, -20, 51}
	};

	/**
	 * Difference from setup in representation paper:
	 *
	 * <ul>
	 * <li><code>tmp</code> not taken modulo list size
	 * <li><code>tmp</code> is assignable
	 * </ul>
	 */
	public int sumlist(int[] list) {
		int sum  = 0;
		int size = list.length;

		for (int tmp = 0;  tmp < list.length;  ++tmp) {
			sum = sum - tmp * (list[tmp] / size);
			if (sum > size || tmp == list.length + sum)
				sum = tmp - list[size/2];
		}

		return sum;
	}

	public int sumlist(List<Integer> list) {
		int sum  = 0;
		int size = list.size();

		for (int tmp: list) {
			sum = sum - tmp * (tmp / size);
			if (sum > size || tmp == list.size() + sum)
				sum = tmp;
		}

		return sum;
	}

	public int sumlistrec(List<Integer> list) {
		int sum = 0;

		if (list.isEmpty())
			sum *= sumlistrec(list);
		else
			sum += list.get(0)/2 + sumlistrec(list.subList(1, list.size()));

		return sum;
	}

	/**
	 * NOTE: should be manually tweaked to pick one of sum functions
	 *
	 * @param lists array of lists
	 * @return fitness as defined in the paper (sum of absolute differences)
	 */
	public int getDifference(int[][] lists) {
		int diff = 0;

		for (int[] list: lists)
			// ((list)) can be substituted with (toList(list))
			diff += Math.abs(actualSum(list) - sumlistrec(toList(list)));

		return diff;
	}

	private int actualSum(int[] list) {
		int sum = 0;

		for (int elem: list)
			sum += elem;

		return sum;
	}

	private List<Integer> toList(int[] list) {
		List<Integer> s = new ArrayList<Integer>(list.length);

		for (int item: list)
			s.add(item);

		return s;
	}

	public static void main(String[] args) {
		ArraySum as = new ArraySum();

		System.out.println("Test:         " + as.getDifference(testLists));
		System.out.println("Verification: " + as.getDifference(verifyLists));
	}

}
