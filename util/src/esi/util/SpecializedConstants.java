package esi.util;

/**
 * Per-class constants, specified as class.SimpleClassName.constant.
 *
 * @author orlovm
 */
public class SpecializedConstants {

	/**
	 * Returns the value of property class.SimpleClassName.constant,
	 * with leading and trailing whitespace omitted.
	 *
	 * @param clazz
	 * @param constant
	 * @return the corresponding value
	 */
	public static String get(Class<?> clazz, String constant) {
		return Config.esiProperties.getProperty("class." + clazz.getSimpleName() + '.' + constant).trim();
	}

	/**
	 * Returns the value of property class.SimpleClassName.constant,
	 * split into array of strings by spaces and/or commas, without
	 * empty strings.
	 *
	 * @param clazz
	 * @param constant
	 * @return the corresponding array
	 */
	public static String[] getList(Class<?> clazz, String constant) {
		return getList(get(clazz, constant));
	}

	public static int getInt(Class<?> clazz, String constant) {
		return Integer.parseInt(get(clazz, constant));
	}

	public static float getFloat(Class<?> clazz, String constant) {
		return Float.parseFloat(get(clazz, constant));
	}

	public static boolean getBoolean(Class<?> clazz, String constant) {
		String value = get(clazz, constant).toLowerCase();

		if (value.equals("true")  ||  value.equals("yes")  ||  value.equals("on"))
			return true;
		else if (value.equals("false")  ||  value.equals("no")  ||  value.equals("off"))
			return false;
		else
			throw new NumberFormatException("Not one of: true,yes,on,false,no,off");
	}

	/**
	 * Returns the argument split into array of strings by spaces and/or commas,
	 * without empty strings.
	 *
	 * @param value
	 * @return the corresponding array
	 */
	static String[] getList(String value) {
		final String regex = "[\\s,]+";
		return value.replaceFirst("^" + regex, "").split(regex);
	}

}
