package io.unlogged.core;

public interface DiagnosticsReceiver {
	DiagnosticsReceiver CONSOLE = new DiagnosticsReceiver() {
		@Override public void addError(String message) {
			System.err.println("Error: " + message);
		}
		
		@Override public void addWarning(String message) {
			System.out.println("Warning: " + message);
		}
	};
	
	/** Generate a compiler error on this node. */
	void addError(String message);
	
	/** Generate a compiler warning on this node. */
	void addWarning(String message);
}
