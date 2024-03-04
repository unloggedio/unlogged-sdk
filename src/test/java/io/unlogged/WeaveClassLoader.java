package io.unlogged;

/**
 * A class loader for testing a woven class
 */
public class WeaveClassLoader extends ClassLoader {
	
	/**
	 * Read a given class as a Java class 
	 * @param name specifies a class name
	 * @param bytecode is the bytecode of the class
	 * @return a Class object to create an instance of the Java class
	 */
	public Class<?> createClass(String name, byte[] bytecode) {
		Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
		resolveClass(c);
		return c;
	}
}