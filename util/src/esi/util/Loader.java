package esi.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Loader {

	/**
	 * @param <T> class type, may be interface or abstract
	 * @param className fully qualified class name
	 * @param clazz class or interface instance of which to create
	 * @param initArgs constructor arguments
	 * @return a new instance created with corresponding constructor
	 * @throws Error when there is no class with such fully qualified name,
	 * 		when <code>className</code> is not a sub-type of <code>clazz</code>,
	 * 		or an exception occurred during static initialization of the class.
	 * @see #loadClassInstance(Class, Object...) for exceptions possible during
	 * 		instance construction.
	 */
	public static<T> T loadClassInstance(String className, Class<T> clazz, Object... initArgs) {
		try {
			Class<? extends T> newClass = Class.forName(className).asSubclass(clazz);
			return loadClassInstance(newClass, initArgs);
		} catch (ClassNotFoundException e) {
			throw new Error("Unable to load class " + className, e);
		} catch (ClassCastException e) {
			throw new Error("Class " + className + " is not a sub-type of " + clazz, e);
		} catch (ExceptionInInitializerError e) {
			throw new Error("Exception during static initialization of " + className, e);
		}
	}

	/**
	 * Locates a constructor corresponding to <code>initArgs</code>,
	 * and creates an instance of the class.
	 *
	 * If there are several fitting constructors, an unspecified one
	 * will be used.
	 *
	 * @param <T> class type
	 * @param clazz class instance of which to create, must be concrete
	 * @param initArgs constructor arguments
	 * @return a new instance created with corresponding constructor
	 * @throws Error when there are no appropriate constructors, <code>clazz</code>
	 * 		is not a concrete class, is not public, or an exception occurred during
	 * 		construction.
	 */
	@SuppressWarnings("unchecked")
	public static<T> T loadClassInstance(Class<? extends T> clazz, Object... initArgs) {
		try {
			// The cast below is safe (see JavaDoc), however, it requires warning suppression
			Constructor<? extends T>[]  pubConstrs = (Constructor<? extends T>[]) clazz.getConstructors();

			T         instance      = null;
			Throwable noConstructor = null;

			for (Constructor<? extends T> constr: pubConstrs) {
				try {
					instance = constr.newInstance(initArgs);
					break;
				} catch (IllegalArgumentException e) {
					// Wrong number of parameters; unwrapping fails; value conversion fails
					noConstructor = e;
				}
			}

			if (instance == null) {
				if (noConstructor == null)
					noConstructor = new IllegalArgumentException(clazz + " has no constructors");

				throw new Error(clazz + " has no appropriate constructor", noConstructor);
			}

			return instance;
		} catch (InstantiationException e) {
			throw new Error(clazz.getName() + " is not a concrete class", e);
		} catch (IllegalAccessException e) {
			throw new Error(clazz.getName() + " has incorrect access modifiers", e);
		} catch (InvocationTargetException e) {
			throw new Error("Exception during creation of " + clazz, e);
		}
	}

}
