package esi.bc.manip;

import org.objectweb.asm.Type;

/**
 * An interface for classes that implement mutation of constants.
 *
 * NOTE: IINC is not currently supported.
 *
 * @author Michael Orlov
 */
public interface ConstantsMutator {

	// One-word constants
	int mutate(int x);
	float mutate(float x);
	String mutate(String x);
	Type mutate(Type x);

	// Two-word constants
	long mutate(long x);
	double mutate(double x);

	/**
	 * If this method is called, it must be called before any of the
	 * mutating methods.
	 *
	 * @param ints    number of integer constants in the method
	 * @param floats  number of float constants in the method
	 * @param longs   number of long constants in the method
	 * @param doubles number of double constants in the method
	 * @param strings number of String constants in the method
	 * @param types   number of Class constants in the method
	 */
	void visitCounts(int ints, int floats, int longs, int doubles, int strings, int types);

}
