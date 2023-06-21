package io.unlogged.logging.util;

/**
 * A utility class to deal with type id.
 */
public class TypeIdUtil {

	/**
	 * @param className specifies a class name (whose package names are separated by "/")
	 * @return a string representation
	 */
	public static String getClassLoaderIdentifier(String className) {
		return "Loader@" + "javac" + ":" + className.replace('.', '/');
	}
}
