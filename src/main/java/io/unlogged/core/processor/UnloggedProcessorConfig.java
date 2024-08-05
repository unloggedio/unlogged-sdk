package io.unlogged.core.processor;

import io.unlogged.UnloggedLoggingLevel;
import io.unlogged.UnloggedMode;

public class UnloggedProcessorConfig {

	private long defaultCounter;
	private UnloggedLoggingLevel unloggedLoggingLevel;
	private UnloggedMode unloggedMode;

	public UnloggedProcessorConfig (long defaultCounter, UnloggedLoggingLevel unloggedLoggingLevel, UnloggedMode unloggedMode) {
		this.defaultCounter = defaultCounter;
		this.unloggedLoggingLevel = unloggedLoggingLevel;
		this.unloggedMode = unloggedMode;
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

	public void setUnloggedMode (UnloggedMode unloggedMode) {
		this.unloggedMode = unloggedMode;
	}

	public UnloggedMode getUnloggedMode() {
		return this.unloggedMode;
	}
}
