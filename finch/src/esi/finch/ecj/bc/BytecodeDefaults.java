package esi.finch.ecj.bc;

import ec.DefaultsForm;
import ec.util.Parameter;

/**
 * Parameters base: <tt>bytecode</tt>
 *
 * @author Michael Orlov
 */
public class BytecodeDefaults implements DefaultsForm {

	private static final String P_BYTECODE = "bytecode";

    public static final Parameter base() {
    	return new Parameter(P_BYTECODE);
    }

}
