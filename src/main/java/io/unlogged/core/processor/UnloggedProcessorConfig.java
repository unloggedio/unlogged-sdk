package io.unlogged.core.processor;

import io.unlogged.ExperimentalSDKFlagType;

public class UnloggedProcessorConfig {

	private long defaultCounter;
	private ExperimentalSDKFlagType experimentalSDKFlagType;

	public UnloggedProcessorConfig (long defaultCounter, ExperimentalSDKFlagType experimentalSDKFlagType) {
		this.defaultCounter = defaultCounter;
		this.experimentalSDKFlagType = experimentalSDKFlagType;
	}

	public void setDefaultCounter (long defautlCounter) {
		this.defaultCounter = defautlCounter;
	}

	public long getDefaultCounter() {
		return this.defaultCounter;
	}

	public void setExperimentalSDKFlagType (ExperimentalSDKFlagType experimentalSDKFlagType) {
		this.experimentalSDKFlagType = experimentalSDKFlagType;
	}

	public ExperimentalSDKFlagType getExperimentalSDKFlagType() {
		return this.experimentalSDKFlagType;
	}
}
