package io.unlogged.logging;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.weaver.ClassInfo;

public class DiscardEventLogger implements IEventLogger {

    private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();


    @Override
    public void close() {

    }

    @Override
    public Object getObjectByClassName(String name) {
        return null;
    }

    @Override
    public Object recordEvent(int dataId, Object value) {
        return value;
    }

    @Override
    public void recordEvent(int dataId, int value) {

    }

    @Override
    public void recordEvent(int dataId, long value) {

    }

    @Override
    public void recordEvent(int dataId, byte value) {

    }

    @Override
    public void recordEvent(int dataId, short value) {

    }

    @Override
    public void recordEvent(int dataId, char value) {

    }

    @Override
    public void registerClass(Integer id, Class<?> type) {

    }

    @Override
    public void recordEvent(int dataId, boolean value) {

    }

    @Override
    public void recordEvent(int dataId, double value) {

    }

    @Override
    public void recordEvent(int dataId, float value) {

    }

    @Override
    public void recordWeaveInfo(byte[] byteArray, ClassInfo classIdEntry, List<Integer> probeIdsToRecord) {

    }

    @Override
    public void setRecordingPaused(boolean b) {

    }


    @Override
    public ClassLoader getTargetClassLoader() {
        return this.getClass().getClassLoader();
    }

	@Override
	public void modifyThreadDepth(long delta) {
		return;
	}
}
