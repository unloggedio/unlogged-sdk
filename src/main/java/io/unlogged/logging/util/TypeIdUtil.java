package io.unlogged.logging.util;

/**
 * A utility class to deal with type id.
 */
public class TypeIdUtil {

	/**
	 * @param loader specifies a class loader 
	 * @param className specifies a class name (whose package names are separated by "/")
	 * @return a string representation
	 */
	public static String getClassLoaderIdentifier(ClassLoader loader, String className) {
		return "Loader@" + Integer.toHexString(System.identityHashCode(loader)) + ":" + className.replace('.', '/');
	}
}
