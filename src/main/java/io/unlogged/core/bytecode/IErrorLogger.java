package io.unlogged.core.bytecode;

/**
 * An interface for recording errors reported by SELogger components.
 * Since SELogger works as a Java agent, it should not directly use STDOUT/STDERR.    
 */
public interface IErrorLogger {

	/**
	 * Record an exception.
	 * @param throwable object to be logged
	 */
	void log(Throwable throwable);
	
	/**
	 * Record a message
	 * @param message string to be logged
	 */
	void log(String message);
	
	/**
	 * This method is called when the program is terminated.
	 */
	void close();
}
