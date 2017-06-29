package esi.bc.manip;

import org.objectweb.asm.Type;

/**
 * Counts constants in a method.
 *
 * This class implements {@link ConstantsMutator} instead of
 * extending {@link IdentityConstantsMutator} (typically, a preferred approach),
 * to make sure that all constants are handled.
 *
 * @author Michael Orlov
 */
public class ConstantsCounter implements ConstantsMutator {

	// All are 0-initialized
	private int ints;
	private int floats;
	private int strings;
	private int types;
	private int longs;
	private int doubles;

	@Override
	public int mutate(int x) {
		++ints;
		return x;
	}

	@Override
	public float mutate(float x) {
		++floats;
		return x;
	}

	@Override
	public String mutate(String x) {
		++strings;
		return x;
	}

	@Override
	public Type mutate(Type x) {
		++types;
		return x;
	}

	@Override
	public long mutate(long x) {
		++longs;
		return x;
	}

	@Override
	public double mutate(double x) {
		++doubles;
		return x;
	}

	@Override
	public void visitCounts(int ints, int floats, int longs, int doubles, int strings, int types) {
		throw new Error("Unexpected set of counters in constants counter");
	}

	/**
	 * Calls the {@link ConstantsMutator#visitCounts(int, int, int, int, int, int)} method
	 * in the given mutator with gathered counts.
	 *
	 * @param mutator constants mutator to provide counts to
	 */
	public void accept(ConstantsMutator mutator) {
		mutator.visitCounts(ints, floats, longs, doubles, strings, types);
	}

}
