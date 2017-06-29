package esi.bc.manip.test;

import java.util.AbstractList;

public class RenameGeneric<T extends Comparable<T>> extends AbstractList<T>
	implements Comparable<RenameGeneric<T>>, Iterable<T> {

	private RenameGeneric<T> field;

	protected class Rename {
		// TODO: add internal classes renaming to ASM?
	}

	@Override
	public T get(int index) {
		return null;
	}

	@Override
	public int size() {
		field = new RenameGeneric<T>();
		RenameGeneric<T> x = field;
		RenameGeneric<Integer>[][] y = null;
		Class<Integer> z = Integer.class;

		if (y == null  &&  z != null)
			x = null;

		return x == null  ?  0  :  1;
	}

	@Override
	public int compareTo(RenameGeneric<T> o) {
		return 0;
	}

}
