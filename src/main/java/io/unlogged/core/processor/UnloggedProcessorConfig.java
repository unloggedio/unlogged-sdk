package io.unlogged.core.processor;

import io.unlogged.UnloggedLoggingLevel;

public class UnloggedProcessorConfig {

	private long defaultCounter;
	private UnloggedLoggingLevel unloggedLoggingLevel;

	public UnloggedProcessorConfig (long defaultCounter, UnloggedLoggingLevel unloggedLoggingLevel) {
		this.defaultCounter = defaultCounter;
		this.unloggedLoggingLevel = unloggedLoggingLevel;
	}

	public void setDefaultCounter (long defautlCounter) {
		this.defaultCounter = defautlCounter;
	}

	public long getDefaultCounter() {
		return this.defaultCounter;
	}

	public void setUnloggedLoggingLevel (UnloggedLoggingLevel unloggedLoggingLevel) {
		this.unloggedLoggingLevel = unloggedLoggingLevel;
	}

	public UnloggedLoggingLevel getUnloggedLoggingLevel() {
		return this.unloggedLoggingLevel;
	}
}
