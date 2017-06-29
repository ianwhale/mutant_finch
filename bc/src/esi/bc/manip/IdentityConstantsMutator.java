package esi.bc.manip;

import org.objectweb.asm.Type;

/**
 * Base class for constants mutators, with default behavior
 * of not changing any constant.
 *
 * @author Michael Orlov
 */
public class IdentityConstantsMutator implements ConstantsMutator {

	@Override
	public int mutate(int x) {
		return x;
	}

	@Override
	public float mutate(float x) {
		return x;
	}

	@Override
	public String mutate(String x) {
		return x;
	}

	@Override
	public Type mutate(Type x) {
		return x;
	}

	@Override
	public long mutate(long x) {
		return x;
	}

	@Override
	public double mutate(double x) {
		return x;
	}

	@Override
	public void visitCounts(int ints, int floats, int longs, int doubles, int strings, int types) {
		// Do nothing
	}

}
