package esi.finch.ecj.string;

import ec.DefaultsForm;
import ec.util.Parameter;

public class StringDefaults implements DefaultsForm {

	private static final String P_STRING = "string";

    public static final Parameter base() {
    	return new Parameter(P_STRING);
    }

}
