package esi.finch.ecj.immutable;

import ec.DefaultsForm;
import ec.util.Parameter;

/**
 * Parameters base: <tt>immutable</tt>
 *
 * @author Michael Orlov
 */
public class ImmutableDefaults implements DefaultsForm {

	private static final String P_IMMUTABLE = "immutable";

    public static final Parameter base() {
    	return new Parameter(P_IMMUTABLE);
    }

}
