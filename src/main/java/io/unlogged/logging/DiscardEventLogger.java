package io.unlogged.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.weaver.ClassInfo;

import java.util.List;

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
        System.out.println("recordWeaveInfo type-2 called");
    }

    @Override
    public void setRecordingPaused(boolean b) {

    }


    @Override
    public ClassLoader getTargetClassLoader() {
        return this.getClass().getClassLoader();
    }
}
